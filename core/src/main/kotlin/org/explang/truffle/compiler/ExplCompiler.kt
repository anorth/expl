package org.explang.truffle.compiler

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameDescriptor
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.explang.parser.ExplBaseVisitor
import org.explang.parser.ExplLexer
import org.explang.parser.ExplParser
import org.explang.truffle.Discloser
import org.explang.truffle.Encloser
import org.explang.truffle.ExplFunction
import org.explang.truffle.SlotBinding
import org.explang.truffle.Type
import org.explang.truffle.nodes.ArgReadNode
import org.explang.truffle.nodes.BindingNode
import org.explang.truffle.nodes.CallRootNode
import org.explang.truffle.nodes.ExpressionNode
import org.explang.truffle.nodes.FactorNode
import org.explang.truffle.nodes.FunctionCallNode
import org.explang.truffle.nodes.FunctionDefinitionNode
import org.explang.truffle.nodes.LetNode
import org.explang.truffle.nodes.LiteralDoubleNode
import org.explang.truffle.nodes.NegationNode
import org.explang.truffle.nodes.ProductNode
import org.explang.truffle.nodes.SumNode
import org.explang.truffle.nodes.SymbolNode
import org.explang.truffle.nodes.builtin.StaticBound
import java.lang.Double.parseDouble
import java.util.Arrays

class CompileError(msg: String, val context: ParserRuleContext): Exception(msg)

class ExplCompiler {
  @Throws(CompileError::class)
  fun compile(parse: ExplParser.ExpressionContext) : Pair<ExpressionNode, FrameDescriptor> {
    return AstBuilder.build(parse)
  }
}

/** A parse tree visitor that constructs an AST */
private class AstBuilder private constructor(tree: ParseTree) : ExplBaseVisitor<ExpressionNode>() {
  companion object {
    fun build(tree: ParseTree): Pair<ExpressionNode, FrameDescriptor> {
      val builder = AstBuilder(tree)
      val ast = tree.accept(builder)
      val topFrameDescriptor = builder.scope.popTopFrame()
      return ast to topFrameDescriptor
    }
  }

  private val scope: Scope = Scope(tree)

  override fun visitExpression(ctx: ExplParser.ExpressionContext): ExpressionNode = when {
    ctx.let() != null -> visitLet(ctx.let())
    else -> visitSum(ctx.sum())
  }

  override fun visitLet(ctx: ExplParser.LetContext): LetNode {
    // Let bindings go in the current function scope.
    // Bindings are keyed by frame slots in this frame.
    // Local nodes resolved in the subsequent expression will find those frame slots.
    // Bindings in the let clause are also visible to each other.
    // TODO: rewrite names in nested let clauses to avoid name re-use trashing the frame for
    // subsequent bindings (but it works ok for nested clauses). I.e scoped resolved identifiers.
    scope.enterBinding(ctx)
    val bindingNodes = ctx.binding().map(this::visitBinding)
    val names = mutableSetOf<String>()
    val dupNames = mutableSetOf<String>()
    for (name in names) {
      if (name in names) {
        dupNames.add(name)
      } else {
        names.add(name)
      }
    }
    if (dupNames.isNotEmpty()) {
      throw CompileError("Duplicate bindings for ${names.joinToString(",")}", ctx)
    }

    val expressionNode = visitExpression(ctx.expression())
    scope.exit()
    return LetNode(bindingNodes.toTypedArray(), expressionNode)
  }

  override fun visitBinding(ctx: ExplParser.BindingContext): BindingNode {
    // FIXME make bound symbol visible inside binding expression, for recursive definitions
    val expression = visitExpression(ctx.expression())
    val binding = scope.defineBinding(expression.type, ctx)
    return BindingNode(binding.slot, expression)
  }

  override fun visitSum(ctx: ExplParser.SumContext): ExpressionNode {
    // Build left-associative tree.
    val itr = ctx.children.iterator()
    var left = visitProduct(itr.next() as ExplParser.ProductContext)
    while (itr.hasNext()) {
      val op = itr.next()
      val right = visitProduct(itr.next() as ExplParser.ProductContext)
      left = when ((op as TerminalNode).symbol.type) {
        ExplLexer.PLUS -> SumNode.addDouble(left, right)
        else -> SumNode.subDouble(left, right)
      }
    }
    return left
  }

  override fun visitProduct(ctx: ExplParser.ProductContext): ExpressionNode {
    // Build left-associative tree.
    val itr = ctx.children.iterator()
    var left = visitFactor(itr.next() as ExplParser.FactorContext)
    while (itr.hasNext()) {
      val op = itr.next()
      val right = visitFactor(itr.next() as ExplParser.FactorContext)
      left = when ((op as TerminalNode).symbol.type) {
        ExplLexer.TIMES -> ProductNode.mulDouble(left, right)
        else -> ProductNode.divDouble(left, right)
      }
    }
    return left
  }

  override fun visitFactor(ctx: ExplParser.FactorContext): ExpressionNode {
    // Exponentiation is right-associative.
    val itr = ctx.children.asReversed().iterator()
    var rt = visitSigned(itr.next() as ExplParser.SignedContext)
    while (itr.hasNext()) {
      val op = itr.next()
      val left = visitSigned(itr.next() as ExplParser.SignedContext)
      require((op as TerminalNode).symbol.type == ExplLexer.POW)
      rt = FactorNode.expDouble(rt, left)
    }
    return rt
  }

  override fun visitSigned(ctx: ExplParser.SignedContext): ExpressionNode = when {
    ctx.PLUS() != null -> visitSigned(ctx.signed())
    ctx.MINUS() != null -> NegationNode(visitSigned(ctx.signed()))
    else -> visitAtom(ctx.atom())
  }

  override fun visitFcall(ctx: ExplParser.FcallContext): FunctionCallNode {
    val symbol = visitSymbol(ctx.symbol())
    val args = ctx.expression().map(this::visitExpression).toTypedArray()
    check(ctx, symbol.type.isFunction) { "Call to a non-function" }
    val actualTypes = args.map(ExpressionNode::type).toTypedArray()
    val declaredTypes = symbol.type.arguments()
    check(ctx, Arrays.equals(declaredTypes, actualTypes)) {
      "Actual parameters (${actualTypes.joinToString(",")}) don't match " +
          "declared (${declaredTypes.joinToString(",")})"
    }
    return FunctionCallNode(symbol, args)
  }

  override fun visitAtom(ctx: ExplParser.AtomContext): ExpressionNode = when {
    ctx.lambda() != null -> visitLambda(ctx.lambda())
    ctx.fcall() != null -> visitFcall(ctx.fcall())
    ctx.number() != null -> visitNumber(ctx.number())
    ctx.symbol() != null -> visitSymbol(ctx.symbol())
    else -> visitExpression(ctx.expression())
  }

  override fun visitLambda(ctx: ExplParser.LambdaContext): FunctionDefinitionNode {
    val callDescriptor = FrameDescriptor()
    scope.enterFunction(callDescriptor, ctx)
    // The body expression contains symbol nodes which refer to formal parameters by name.
    // Visit those formal parameter declarations first to define their indices in the scope.
    // Within this scope, matching symbols will resolve to those indices.
    val argTypes = arrayOfNulls<Type>(ctx.argnames().symbol().size)
    ctx.argnames().symbol().forEachIndexed { i, it ->
      val type = Type.DOUBLE // FIXME type annotation or inference
      argTypes[i] = type
      scope.defineArgument(type, it)
    }
    // Visit the body of the function.
    val body = visitExpression(ctx.expression())
    val closedOver = scope.exit()

    // Function bodies can close over non-local values. The references are evaluated at the time
    // the function is defined. The value is closed over, not the reference.
    //
    // Values which are closed over are copied to a closure frame allocated when function
    // *definition* node is executed (which might be multiple times in a loop).
    // At function *call* time, a function call preamble copies values from the closure
    // into the executing (callee) frame, where normal symbol nodes will find them.

    // An alternative could be a third symbol-resolution type of "closure", resolving directly
    // into the closure frame. This would save a copy at call time but indirect symbol
    // resolution at lookup time.
    // The closure frame could also be passed to the call frame as an argument or a special
    // frame slot, rather than the current reference-to-call-root side channel.
    val closureDescriptor = FrameDescriptor()
    val closureBindings = arrayOfNulls<SlotBinding>(closedOver.size)
    val calleeBindings = arrayOfNulls<SlotBinding>(closedOver.size)
    closedOver.forEachIndexed { i, resolution ->
      val closureSlot = closureDescriptor.addSlot(resolution.name, resolution.type)
      closureBindings[i] = when (resolution) {
        is Scope.Resolution.Local -> SlotBinding(resolution.slot, closureSlot)
        is Scope.Resolution.Argument -> TODO("Closed arguments")
      }
      calleeBindings[i] = SlotBinding(closureSlot, callDescriptor.findFrameSlot(resolution.name))
    }

    val type = Type.function(body.type, *argTypes)
    val callRoot = CallRootNode(body, callDescriptor, Discloser(calleeBindings))
    val callTarget = Truffle.getRuntime().createCallTarget(callRoot)
    val fn = ExplFunction.create(type, callTarget);
    return FunctionDefinitionNode(fn, Encloser(closureDescriptor, closureBindings))
  }

  override fun visitNumber(ctx: ExplParser.NumberContext): ExpressionNode =
      LiteralDoubleNode(parseDouble(ctx.text))

  override fun visitSymbol(ctx: ExplParser.SymbolContext): ExpressionNode {
    val name = ctx.text
    return if (name in BUILT_INS) {
      StaticBound.builtIn(BUILT_INS[name]!!) // FIXME allow bindings to shadow builtins
    } else {
      val resolution = scope.resolve(ctx)
      return resolutionAsNode(resolution, name, ctx)
    }
  }
}

private fun resolutionAsNode(resolution: Scope.Resolution?, name: String,
    ctx: ParserRuleContext): ExpressionNode {
  return when (resolution) {
    is Scope.Resolution.Argument -> ArgReadNode(resolution.type, resolution.index, name)
    is Scope.Resolution.Local -> SymbolNode(resolution.type, resolution.slot)
    null -> throw CompileError("Unbound symbol $name", ctx)
  }
}

private fun FrameDescriptor.findOrAddSlot(identifier: String, type: Type) =
    this.findOrAddFrameSlot(identifier, type, type.asSlotKind())

private fun FrameDescriptor.addSlot(identifier: String, type: Type) =
    this.addFrameSlot(identifier, type, type.asSlotKind())

private inline fun check(ctx: ParserRuleContext, predicate: Boolean, msg: () -> String) {
  if (!predicate) {
    throw CompileError(msg(), ctx)
  }
}
