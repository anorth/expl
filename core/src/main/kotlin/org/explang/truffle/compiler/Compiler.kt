package org.explang.truffle.compiler

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameDescriptor
import org.explang.syntax.ExBinaryOp
import org.explang.syntax.ExBinding
import org.explang.syntax.ExCall
import org.explang.syntax.ExIf
import org.explang.syntax.ExLambda
import org.explang.syntax.ExLet
import org.explang.syntax.ExLiteral
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree
import org.explang.syntax.ExUnaryOp
import org.explang.truffle.Discloser
import org.explang.truffle.Encloser
import org.explang.truffle.ExplFunction
import org.explang.truffle.FrameBinding
import org.explang.truffle.Type
import org.explang.truffle.nodes.ArgReadNode
import org.explang.truffle.nodes.BindingNode
import org.explang.truffle.nodes.Booleans
import org.explang.truffle.nodes.CallRootNode
import org.explang.truffle.nodes.Doubles
import org.explang.truffle.nodes.ExpressionNode
import org.explang.truffle.nodes.FunctionCallNode
import org.explang.truffle.nodes.FunctionDefinitionNode
import org.explang.truffle.nodes.IfNode
import org.explang.truffle.nodes.LetNode
import org.explang.truffle.nodes.SymbolNode
import org.explang.truffle.nodes.builtin.StaticBound
import java.util.Arrays

class CompileError(msg: String, val tree: ExTree) : Exception(msg)

/**
 * Compiles an AST to Truffle node tree for interpretation/JIT.
 */
class Compiler(
    private val printAnalysis: Boolean = false
) {
  private val analyzer = Analyzer()

  @Throws(CompileError::class)
  fun compile(tree: ExTree): ExpressionNode {
    val analysis = analyzer.analyze(tree)
    if (printAnalysis) {
      println("*Analysis*")
      println(analysis)
    }

    val (ast, topFrameDescriptor) = TruffleBuilder.build(tree, analysis)

    // Wrap the evaluation in an anonymous function call providing the root frame.
    val entryRoot = CallRootNode(ast, topFrameDescriptor, Discloser.EMPTY)
    val callTarget = Truffle.getRuntime().createCallTarget(entryRoot)
    val entryPoint = ExplFunction.create(Type.function(ast.type()), callTarget)
    return FunctionCallNode(StaticBound.function(entryPoint), arrayOfNulls(0))
  }
}

/**
 * Converts an AST with analysis into a Truffle node tree.
 */
private class TruffleBuilder private constructor(
    val analysis: Analyzer.Analysis
) : ExTree.Visitor<ExpressionNode> {
  companion object {
    fun build(tree: ExTree, analysis: Analyzer.Analysis): Pair<ExpressionNode, FrameDescriptor> {
      val b = TruffleBuilder(analysis)
      val node = b.visit(tree)
      val topFrameDescriptor = b.frame
      return node to topFrameDescriptor
    }
  }

  private var scope: Scope = analysis.rootScope
  private var frame = FrameDescriptor()

  override fun visitCall(call: ExCall): ExpressionNode {
    val fn = visit(call.callee)
    check(call, fn.type().isFunction) { "Call to a non-function" }
    val args = call.args.map(::visit).toTypedArray()
    val actualTypes = args.map(ExpressionNode::type).toTypedArray()
    val declaredTypes = fn.type().arguments()
    check(call, Arrays.equals(declaredTypes, actualTypes)) {
      "Actual parameters (${actualTypes.joinToString(",")}) don't match " +
          "declared (${declaredTypes.joinToString(",")})"
    }
    return FunctionCallNode(fn, args)
  }

  override fun visitUnaryOp(unop: ExUnaryOp): ExpressionNode {
    val child = visit(unop.operand)
    return UNOPS[child.type()]!![unop.operator]!!(child)
  }

  override fun visitBinaryOp(binop: ExBinaryOp): ExpressionNode {
    val left = visit(binop.left)
    val right = visit(binop.right)
    return BINOPS[left.type()]!![binop.operator]!!(left, right)
  }

  override fun visitIf(iff: ExIf) = IfNode(visit(iff.test), visit(iff.left), visit(iff.right))

  override fun visitLet(let: ExLet): LetNode {
    // Let bindings go in the current function scope.
    // Bindings are keyed by frame slots in this frame.
    // Local nodes resolved in the subsequent expression will find those frame slots.
    // Bindings in the let clause are also visible to each other.
    // TODO: rewrite names in nested let clauses to avoid name re-use trashing the frame for
    // subsequent bindings (but it works ok for nested clauses). I.e scoped resolved identifiers.
    val prevScope = scope
    scope = analysis.scopes[let]!!
    val bindingNodes = let.bindings.map(this::visitBinding)
    checkNameUniqueness(let, bindingNodes.map { it.slot.identifier as String })

    val expressionNode = visit(let.bound)
    scope = prevScope
    return LetNode(bindingNodes.toTypedArray(), expressionNode)
  }

  override fun visitBinding(binding: ExBinding): BindingNode {
    // Add symbol to frame before visiting bound value (for recursion)
    val resolution = (scope as BindingScope).resolve(binding.symbol)
    val slot = frame.addFrameSlot(resolution.identifier, resolution.type,
        resolution.type.asSlotKind())

    val valueNode = visit(binding.value)
    return BindingNode(slot, valueNode)
  }

  override fun visitLambda(lambda: ExLambda): ExpressionNode {
    val prevFrame = frame
    val prevScope = scope
    frame = FrameDescriptor()
    scope = analysis.scopes[lambda]!!

    // The body expression contains symbol nodes which refer to formal parameters by name.
    // Visit those formal parameter declarations first to define their indices in the scope.
    // Within this scope, matching symbols will resolve to those indices.
    val argResolutions = lambda.parameters.map {
      (scope as FunctionScope).resolve(it) as Scope.Resolution.Argument
    }
    val argTypes = argResolutions.map(Scope.Resolution.Argument::type).toTypedArray()

    // Function bodies can capture non-local values. The references are evaluated at the time
    // the function is defined. The value is closed over, not the reference.
    //
    // Values which are closed over are copied to a closure frame allocated when function
    // *definition* node is executed (which might be multiple times in a loop).
    // At function *call* time, a function call preamble copies values from the closure
    // into the executing (callee) frame, where normal symbol nodes will find them.
    val closureDescriptor = FrameDescriptor()
    val captured = analysis.captured[lambda] ?: setOf()
    val closureBindings = arrayOfNulls<FrameBinding>(captured.size)
    val calleeBindings = arrayOfNulls<FrameBinding.SlotBinding>(captured.size)
    captured.forEachIndexed { i, resolution ->
      val closureSlot = closureDescriptor.addSlot(resolution.identifier, resolution.type)
      closureBindings[i] = when (resolution) {
        is Scope.Resolution.Local ->
          FrameBinding.SlotBinding(prevFrame.findSlot(resolution.identifier), closureSlot)
        is Scope.Resolution.Argument -> FrameBinding.ArgumentBinding(resolution.index, closureSlot)
        is Scope.Resolution.Closure ->
          FrameBinding.SlotBinding(prevFrame.findSlot(resolution.identifier), closureSlot)
        is Scope.Resolution.Unresolved ->
          throw CompileError("Unbound capture ${resolution.symbol}", lambda)
      }
      calleeBindings[i] =
          FrameBinding.SlotBinding(closureSlot, frame.addSlot(resolution.identifier, resolution.type))
    }

    val body = visit(lambda.body)

    val type = Type.function(body.type(), *argTypes)
    val callRoot = CallRootNode(body, frame, Discloser(calleeBindings))
    val callTarget = Truffle.getRuntime().createCallTarget(callRoot)
    val fn = ExplFunction.create(type, callTarget)

    frame = prevFrame
    scope = prevScope
    return FunctionDefinitionNode(fn, Encloser(closureDescriptor, closureBindings))  }

  override fun visitLiteral(literal: ExLiteral<*>): ExpressionNode {
    return when {
      literal.type == Boolean::class.java -> Booleans.literal(literal.value as Boolean)
      literal.type == Double::class.java-> Doubles.literal(literal.value as Double)
      else -> throw CompileError("Unrecognized literal", literal)
    }
  }

  override fun visitSymbol(symbol: ExSymbol): ExpressionNode {
    val resolution = scope.resolve(symbol)
    val id = resolution.identifier
    return when (resolution) {
      is Scope.Resolution.Argument -> ArgReadNode(resolution.type, resolution.index, id)
      is Scope.Resolution.Local ->
        SymbolNode(resolution.type, frame.findFrameSlot(id))
      is Scope.Resolution.Closure ->
        SymbolNode(resolution.type, frame.findFrameSlot(id))
      is Scope.Resolution.Unresolved -> {
        if (id in BUILT_INS) {
          StaticBound.builtIn(BUILT_INS[id]!!)
        } else {
          throw CompileError("Unbound symbol ${resolution.symbol}", symbol)
        }
      }
    }
  }

  ///// Helpers /////
  private fun checkNameUniqueness(let: ExLet, names: List<String>) {
    val found = mutableSetOf<String>()
    val duplicated = mutableSetOf<String>()
    for (name in names) {
      if (name in found) {
        duplicated.add(name)
      } else {
        found.add(name)
      }
    }
    if (duplicated.isNotEmpty()) {
      throw CompileError("Duplicate bindings for ${duplicated.joinToString(",")}", let)
    }
  }
}

private val UNOPS = mapOf(
    Type.DOUBLE to mapOf(
        "-" to Doubles::negate
    )
)

private val BINOPS = mapOf(
    Type.BOOL to mapOf(
        "==" to Booleans::eq,
        "!=" to Booleans::ne
    ),
    Type.DOUBLE to mapOf(
        "^" to Doubles::exp,
        "*" to Doubles::mul,
        "/" to Doubles::div,
        "+" to Doubles::add,
        "-" to Doubles::sub,
        "<" to Doubles::lt,
        "<=" to Doubles::le,
        ">" to Doubles::gt,
        ">=" to Doubles::ge,
        "==" to Doubles::eq,
        "!=" to Doubles::ne
    )
)

private inline fun check(tree: ExTree, predicate: Boolean, msg: () -> String) {
  if (!predicate) {
    throw CompileError(msg(), tree)
  }
}
