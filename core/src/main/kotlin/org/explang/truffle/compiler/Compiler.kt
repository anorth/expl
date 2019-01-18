package org.explang.truffle.compiler

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameDescriptor
import org.explang.array.ArrayValue
import org.explang.syntax.ArrayType
import org.explang.syntax.ExBinaryOp
import org.explang.syntax.ExBinding
import org.explang.syntax.ExCall
import org.explang.syntax.ExIf
import org.explang.syntax.ExLambda
import org.explang.syntax.ExLet
import org.explang.syntax.ExLiteral
import org.explang.syntax.ExParameter
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree
import org.explang.syntax.ExUnaryOp
import org.explang.syntax.FuncType
import org.explang.syntax.NoneType
import org.explang.syntax.PrimType
import org.explang.syntax.Type
import org.explang.truffle.Discloser
import org.explang.truffle.Encloser
import org.explang.truffle.ExplFunction
import org.explang.truffle.FrameBinding
import org.explang.truffle.nodes.ArgReadNode
import org.explang.truffle.nodes.ArrayNodes
import org.explang.truffle.nodes.BindingNode
import org.explang.truffle.nodes.Booleans
import org.explang.truffle.nodes.CallRootNode
import org.explang.truffle.nodes.Doubles
import org.explang.truffle.nodes.ExpressionNode
import org.explang.truffle.nodes.FunctionCallNode
import org.explang.truffle.nodes.FunctionDefinitionNode
import org.explang.truffle.nodes.FunctionNodes
import org.explang.truffle.nodes.IfNode
import org.explang.truffle.nodes.LetNode
import org.explang.truffle.nodes.Longs
import org.explang.truffle.nodes.SymbolNode
import org.explang.truffle.nodes.builtin.StaticBound
import java.util.Arrays


/**
 * Compiles an AST to Truffle node tree for interpretation/JIT.
 */
class Compiler(
    private val printAnalysis: Boolean = false
) {
  private val analyzer = Analyzer()

  /**
   * Compiles a syntax tree to Truffle nodes.
   *
   * @param tree the tree to compile
   * @param env environment symbols, including built-ins and external data
   */
  @Throws(CompileError::class)
  fun compile(tree: ExTree<Analyzer.Tag>, env: Environment): ExpressionNode {
    val analysis = analyzer.analyze(tree, env.types())
    if (printAnalysis) {
      println("*Analysis*")
      println(analysis)
    }

    val (ast, topFrameDescriptor) = TruffleBuilder.build(tree, analysis, env)

    // Wrap the evaluation in an anonymous function call providing the root frame.
    val entryRoot = CallRootNode(ast, topFrameDescriptor, Discloser.EMPTY)
    val callTarget = Truffle.getRuntime().createCallTarget(entryRoot)
    val entryPoint = ExplFunction(Type.function(ast.type()), callTarget, Encloser.EMPTY)
    return FunctionCallNode(StaticBound.function(entryPoint), arrayOfNulls(0))
  }
}

/**
 * Converts an AST with analysis into a Truffle node tree.
 */
private class TruffleBuilder private constructor(
    private val analysis: Analyzer.Analysis,
    private val env: Environment
) : ExTree.Visitor<Analyzer.Tag, ExpressionNode> {
  companion object {
    fun build(tree: ExTree<Analyzer.Tag>, analysis: Analyzer.Analysis,
        environment: Environment): Pair<ExpressionNode, FrameDescriptor> {
      val b = TruffleBuilder(analysis, environment)
      val node = b.visit(tree)
      val topFrameDescriptor = b.frame
      return node to topFrameDescriptor
    }
  }

  private val resolver = analysis.resolver

  private var frame = FrameDescriptor()

  override fun visitCall(call: ExCall<Analyzer.Tag>): ExpressionNode {
    val fn = visit(call.callee)
    val type = fn.type() as? FuncType ?: throw CompileError("Call to a non-function", call)
    val args = call.args.map(::visit).toTypedArray()
    val actualTypes = args.map(ExpressionNode::type).toTypedArray()
    val declaredTypes = type.parameters()
    check(call, Arrays.equals(declaredTypes, actualTypes)) {
      "Actual parameters (${actualTypes.joinToString(",")}) don't match " +
          "declared (${declaredTypes.joinToString(",")})"
    }
    return FunctionCallNode(fn, args)
  }

  override fun visitUnaryOp(unop: ExUnaryOp<Analyzer.Tag>): ExpressionNode {
    val child = visit(unop.operand)
    return UNOPS[child.type()]!![unop.operator]!!(child)
  }

  override fun visitBinaryOp(binop: ExBinaryOp<Analyzer.Tag>): ExpressionNode {
    val left = visit(binop.left)
    val right = visit(binop.right)
    return BINOPS[left.type()]!![binop.operator]!!(left, right)
  }

  override fun visitIf(iff: ExIf<Analyzer.Tag>) =
      IfNode(visit(iff.test), visit(iff.left), visit(iff.right))

  override fun visitLet(let: ExLet<Analyzer.Tag>): LetNode {
    // Let bindings go in the current function scope.
    // Bindings are keyed by frame slots in this frame.
    // Local nodes resolved in the subsequent expression will find those frame slots.
    // Bindings in the let clause are also visible to each other.
    // TODO: rewrite names in nested let clauses to avoid name re-use trashing the frame for
    // subsequent bindings (but it works ok for nested clauses). I.e scoped resolved identifiers.
    val bindingNodes = let.bindings.map(this::visitBinding)
    checkNameUniqueness(let, bindingNodes.map { it.slot.identifier as String })

    val expressionNode = visit(let.bound)
    return LetNode(bindingNodes.toTypedArray(), expressionNode)
  }

  override fun visitBinding(binding: ExBinding<Analyzer.Tag>): BindingNode {
    // Add symbol to frame before visiting bound value (for recursion)
    val resolution = resolver.resolve(binding.symbol)
    val type = analysis.symbolTypes[resolution]!!
    val slot = frame.addFrameSlot(resolution.identifier, type, type.asSlotKind())

    // Note: the symbol node is not visited.
    val valueNode = visit(binding.value)
    return BindingNode(slot, valueNode)
  }

  override fun visitLambda(lambda: ExLambda<Analyzer.Tag>): ExpressionNode {
    val prevFrame = frame
    frame = FrameDescriptor()

    val argTypes = lambda.parameters.map(ExParameter<*>::annotation).toTypedArray()

    // Function bodies can capture non-local values. The references are evaluated at the time
    // the function is defined. The value is closed over, not the reference.
    //
    // Values which are closed over are copied to a closure frame allocated when function
    // *definition* node is executed (which might be multiple times in a loop).
    // At function *call* time, a function call preamble copies values from the closure
    // into the executing (callee) frame, where normal symbol nodes will find them.
    val closureDescriptor = FrameDescriptor()
    val captured = resolver.captured(lambda)
    val closureBindings = arrayOfNulls<FrameBinding>(captured.size)
    val calleeBindings = arrayOfNulls<FrameBinding.SlotBinding>(captured.size)
    captured.forEachIndexed { i, resolution ->
      // The analysis.symbolTypes map is required only because the symbols
      // attached to the scope resolutions are not bound in their tag type (<*>).
      // An alternative of attaching types to resolutions was rejected as it requires
      // resolutions to be either mutable, or at least replacable in scopes, which breaks
      // other references to them.
      // Another alternative is to concretely type ExTrees in the scope objects.
      val type = analysis.symbolTypes[resolution]!!
      val closureSlot = closureDescriptor.addSlot(resolution.identifier, type)
      closureBindings[i] = when (resolution) {
        is Scope.Resolution.Local,
        is Scope.Resolution.Closure ->
          FrameBinding.SlotBinding(prevFrame.findSlot(resolution.identifier), closureSlot)
        is Scope.Resolution.Argument -> FrameBinding.ArgumentBinding(resolution.index, closureSlot)
        is Scope.Resolution.Unresolved ->
          throw CompileError("Unbound capture ${resolution.symbol}", lambda)
        is Scope.Resolution.Environment ->
          throw CompileError("Capture ${resolution.symbol} is a builtin", lambda)
      }
      calleeBindings[i] =
          FrameBinding.SlotBinding(closureSlot, frame.addSlot(resolution.identifier, type))
    }

    val body = visit(lambda.body)

    val type = Type.function(body.type(), *argTypes)
    val callRoot = CallRootNode(body, frame, Discloser(calleeBindings))
    val callTarget = Truffle.getRuntime().createCallTarget(callRoot)
    val fn = ExplFunction(type, callTarget, Encloser(closureDescriptor, closureBindings))

    frame = prevFrame
    return FunctionDefinitionNode(fn)
  }

  override fun visitParameter(parameter: ExParameter<Analyzer.Tag>) =
      throw RuntimeException("Unused")

  override fun visitLiteral(literal: ExLiteral<Analyzer.Tag, *>): ExpressionNode {
    return when (literal.tag.type as? PrimType) {
      PrimType.BOOL -> Booleans.literal(literal.value as Boolean)
      PrimType.LONG -> Longs.literal(literal.value as Long)
      PrimType.DOUBLE -> Doubles.literal(literal.value as Double)
      null -> throw CompileError("Invalid literal type ${literal.tag.type}", literal)
    }
  }

  override fun visitSymbol(symbol: ExSymbol<Analyzer.Tag>): ExpressionNode {
    val resolution = resolver.resolve(symbol)
    val id = resolution.identifier
    val type = symbol.tag.type
    return when (resolution) {
      is Scope.Resolution.Argument -> ArgReadNode(type, resolution.index, id)
      is Scope.Resolution.Local -> SymbolNode(type, frame.findFrameSlot(id))
      is Scope.Resolution.Closure -> SymbolNode(type, frame.findFrameSlot(id))
      is Scope.Resolution.Environment -> environmentNode(id)
      is Scope.Resolution.Unresolved ->
        throw CompileError("Unbound symbol ${resolution.symbol}", symbol)
    }
  }

  ///// Helpers /////
  private fun checkNameUniqueness(let: ExLet<Analyzer.Tag>, names: List<String>) {
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

  // Builds a node for an environment data value.
  // Note that this short-circuits looking up the value in any frame. Alternatively, the
  // environment values could be captured in a closure, though maybe
  // a small change could add an EnvironmentSymbolNode that looks in a pre-defined frame.
  private fun environmentNode(id: String): ExpressionNode {
    return env.getBuiltin(id)?.let { StaticBound.builtIn(it) } ?: //
    env.getValue(id)?.let { value -> environmentValueNode(value, id) } ?: //
    throw RuntimeException("Can't find environment symbol $id")
  }
}

private fun environmentValueNode(value: Environment.Value, id: String): ExpressionNode {
  return when (value.type) {
    is PrimType -> primitiveNode(value.type, value.value)
    is ArrayType -> arrayNode(value.type.element(), value.value)
    is FuncType -> FunctionNodes.literal(value.value as ExplFunction)
    is NoneType -> throw RuntimeException("Unexpected environment value with type NONE $id: $value")
  }
}

private fun primitiveNode(type: PrimType, value: Any) = when(type) {
  PrimType.BOOL -> Booleans.literal(value as Boolean)
  PrimType.LONG -> Longs.literal(value as Long)
  PrimType.DOUBLE -> Doubles.literal(value as Double)
}

@Suppress("UNCHECKED_CAST")
private fun arrayNode(elType: Type, value: Any) = when(elType) {
  PrimType.BOOL -> ArrayNodes.booleans(value as BooleanArray)
  PrimType.LONG -> ArrayNodes.longs(value as LongArray)
  PrimType.DOUBLE -> ArrayNodes.doubles(value as DoubleArray)
  is FuncType -> ArrayNodes.functions(elType, value as Array<out ExplFunction>)
  is ArrayType -> ArrayNodes.arrays(elType, value as Array<out ArrayValue>)
  NoneType -> throw RuntimeException("Unexpected environment array with value type NONE")
}

private val UNOPS = mapOf(
    Type.BOOL to mapOf(
        "not" to Booleans::invert
    ),
    Type.LONG to mapOf(
        "-" to Longs::negate
    ),
    Type.DOUBLE to mapOf(
        "-" to Doubles::negate
    )
)

private val BINOPS = mapOf(
    Type.BOOL to mapOf(
        "==" to Booleans::eq,
        "<>" to Booleans::ne,
        "and" to Booleans::and,
        "or" to Booleans::or,
        "xor" to Booleans::xor
    ),
    Type.LONG to mapOf(
        "^" to Longs::exp,
        "*" to Longs::mul,
        "/" to Longs::div,
        "+" to Longs::add,
        "-" to Longs::sub,
        "<" to Longs::lt,
        "<=" to Longs::le,
        ">" to Longs::gt,
        ">=" to Longs::ge,
        "==" to Longs::eq,
        "<>" to Longs::ne
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
        "<>" to Doubles::ne
    )
)
