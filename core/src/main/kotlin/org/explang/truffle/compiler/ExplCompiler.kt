package org.explang.truffle.compiler

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameDescriptor
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.explang.parser.ExplBaseVisitor
import org.explang.parser.ExplParser
import org.explang.truffle.Discloser
import org.explang.truffle.Encloser
import org.explang.truffle.ExplFunction
import org.explang.truffle.FrameBinding
import org.explang.truffle.FrameBinding.SlotBinding
import org.explang.truffle.Type
import org.explang.truffle.nodes.ArgReadNode
import org.explang.truffle.nodes.BindingNode
import org.explang.truffle.nodes.Booleans
import org.explang.truffle.nodes.CallRootNode
import org.explang.truffle.nodes.Doubles
import org.explang.truffle.nodes.ExpressionNode
import org.explang.truffle.nodes.FunctionCallNode
import org.explang.truffle.nodes.FunctionDefinitionNode
import org.explang.truffle.nodes.LetNode
import org.explang.truffle.nodes.LiteralDoubleNode
import org.explang.truffle.nodes.NegationNode
import org.explang.truffle.nodes.SymbolNode
import org.explang.truffle.nodes.builtin.StaticBound
import java.lang.Double.parseDouble
import java.util.Arrays

class CompileError(msg: String, val context: ParserRuleContext): Exception(msg)

class ExplCompiler {
  @Throws(CompileError::class)
  fun compile(parse: ExplParser.ExpressionContext): ExpressionNode {
    val (ast, topFrameDescriptor) = AstBuilder.build(parse)

    // Wrap the evaluation in an anonymous function call providing the root frame.
    val entryRoot = CallRootNode(ast, topFrameDescriptor, Discloser.EMPTY)
    val callTarget = Truffle.getRuntime().createCallTarget(entryRoot)
    val entryPoint = ExplFunction.create(Type.function(ast.type()), callTarget);
    return FunctionCallNode(StaticBound.function(entryPoint), arrayOfNulls(0))
  }
}

/** A parse tree visitor that constructs an AST */
private class AstBuilder private constructor(tree: ParseTree) : ExplBaseVisitor<ExpressionNode>() {
  companion object {
    /** Builds an AST from a parse tree. */
    fun build(tree: ParseTree): Pair<ExpressionNode, FrameDescriptor> {
      val builder = AstBuilder(tree)
      val ast = tree.accept(builder)
      val topFrameDescriptor = builder.scope.popTopFrame()
      return ast to topFrameDescriptor
    }
  }

  private val scope: Scope = Scope(tree)

  override fun visitCallEx(ctx: ExplParser.CallExContext): ExpressionNode {
    val fn = visit(ctx.expression())
    val args = ctx.arguments().expression().map(::visit).toTypedArray()
    check(ctx, fn.type().isFunction) { "Call to a non-function" }
    val actualTypes = args.map(ExpressionNode::type).toTypedArray()
    val declaredTypes = fn.type().arguments()
    check(ctx, Arrays.equals(declaredTypes, actualTypes)) {
      "Actual parameters (${actualTypes.joinToString(",")}) don't match " +
          "declared (${declaredTypes.joinToString(",")})"
    }
    return FunctionCallNode(fn, args)
  }

  override fun visitUnaryPlusEx(ctx: ExplParser.UnaryPlusExContext): ExpressionNode {
    return visit(ctx.expression())
  }

  override fun visitUnaryMinusEx(ctx: ExplParser.UnaryMinusExContext): ExpressionNode {
    return NegationNode(visit(ctx.expression()))
  }

  override fun visitExponentiationEx(ctx: ExplParser.ExponentiationExContext): ExpressionNode {
    // Right-associativity is handled by the grammar.
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return Doubles.exp(left, right)
  }

  override fun visitMultiplicativeEx(ctx: ExplParser.MultiplicativeExContext): ExpressionNode {
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return when {
      ctx.TIMES() != null -> Doubles.mul(left, right)
      ctx.DIV() != null -> Doubles.div(left, right)
      else -> throw CompileError("No operator for multiplicative", ctx)
    }
  }

  override fun visitAdditiveEx(ctx: ExplParser.AdditiveExContext): ExpressionNode {
    // Left-associativity is handled by the grammar.
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return when {
      ctx.PLUS() != null -> Doubles.add(left, right)
      ctx.MINUS() != null -> Doubles.sub(left, right)
      else -> throw CompileError("No operator for additive", ctx)
    }
  }

  override fun visitComparativeEx(ctx: ExplParser.ComparativeExContext): ExpressionNode {
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return when {
      ctx.LT() != null -> Doubles.lt(left, right)
      ctx.LE() != null -> Doubles.le(left, right)
      ctx.GT() != null -> Doubles.gt(left, right)
      ctx.GE() != null -> Doubles.ge(left, right)
      else -> throw CompileError("No operator for comparative", ctx)
    }
  }

  override fun visitEqualityEx(ctx: ExplParser.EqualityExContext): ExpressionNode {
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    assert(left.type() == right.type()) {
      "Mismatched types for equality: ${left.type()}, ${right.type()}"
    }
    return when {
      ctx.EQ() != null -> {
        when (left.type()) {
          Type.DOUBLE -> Doubles.eq(left, right)
          Type.BOOL -> Booleans.eq(left, right)
          else -> throw CompileError("Can't compare ${left.type()} for equality", ctx)
        }
      }
      ctx.NEQ() != null -> {
        when (left.type()) {
          Type.DOUBLE -> Doubles.ne(left, right)
          Type.BOOL -> Booleans.ne(left, right)
          else -> throw CompileError("Can't compare ${left.type()} for equality", ctx)
        }
      }
      else -> throw CompileError("No operator for equality", ctx)
    }
  }

  override fun visitLiteralEx(ctx: ExplParser.LiteralExContext): ExpressionNode {
    return visitNumber(ctx.literal().number())
  }

  override fun visitSymbolEx(ctx: ExplParser.SymbolExContext): ExpressionNode {
    val name = ctx.text
    val resolution = scope.resolve(ctx)
    return when {
      resolution != null -> resolutionAsNode(resolution, name, ctx)
      name in BUILT_INS -> StaticBound.builtIn(BUILT_INS[name]!!)
      else -> throw CompileError("Unbound symbol $name", ctx)
    }
  }

  override fun visitLetEx(ctx: ExplParser.LetExContext): ExpressionNode {
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

    val expressionNode = visit(ctx.expression())
    scope.exit()
    return LetNode(bindingNodes.toTypedArray(), expressionNode)
  }

  override fun visitParenthesizedEx(ctx: ExplParser.ParenthesizedExContext): ExpressionNode {
    return visit(ctx.expression())
  }

  override fun visitLambdaEx(ctx: ExplParser.LambdaExContext): ExpressionNode {
    val params = ctx.lambdaParameters().let { p ->
      if (p.symbol() != null) {
        listOf(p.symbol())
      } else {
        p.formalParameters().symbol()
      }
    }
    return visitLambda(ctx, params, ctx.expression())
  }

  override fun visitBinding(ctx: ExplParser.BindingContext): BindingNode {
    // FIXME make bound symbol visible inside binding expression, for recursive definitions
    val value = if (ctx.formalParameters() != null) {
      // Sugar for lambda definitions.
      visitLambda(ctx, ctx.formalParameters().symbol(), ctx.expression())
    } else {
      // Simple binding.
      visit(ctx.expression())
    }
    val binding = scope.defineBinding(value.type(), ctx)
    return BindingNode(binding.slot, value)
  }

  override fun visitNumber(ctx: ExplParser.NumberContext) =
    LiteralDoubleNode(parseDouble(ctx.text))


  ///// Internals /////
  // TODO: move this code somewhere it can be used for building ASTs outside this parser.

  private fun visitLambda(ctx: ParserRuleContext, argCtxs: List<ExplParser.SymbolContext>,
      bodyCtx: ExplParser.ExpressionContext): FunctionDefinitionNode {
    val callDescriptor = FrameDescriptor()
    scope.enterFunction(callDescriptor, ctx)
    // The body expression contains symbol nodes which refer to formal parameters by name.
    // Visit those formal parameter declarations first to define their indices in the scope.
    // Within this scope, matching symbols will resolve to those indices.
    val argTypes = arrayOfNulls<Type>(argCtxs.size)
    argCtxs.forEachIndexed { i, it ->
      val type = Type.DOUBLE // FIXME type annotation or inference
      argTypes[i] = type
      scope.defineArgument(type, it)
    }
    // Visit the body of the function.
    val body = visit(bodyCtx)
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
    val closureBindings = arrayOfNulls<FrameBinding>(closedOver.size)
    val calleeBindings = arrayOfNulls<SlotBinding>(closedOver.size)
    closedOver.forEachIndexed { i, resolution ->
      val closureSlot = closureDescriptor.addSlot(resolution.name, resolution.type)
      closureBindings[i] = when (resolution) {
        is Scope.Resolution.Local -> SlotBinding(resolution.slot, closureSlot)
        is Scope.Resolution.Argument -> FrameBinding.ArgumentBinding(resolution.index, closureSlot)
      }
      calleeBindings[i] = SlotBinding(closureSlot, callDescriptor.findFrameSlot(resolution.name))
    }

    val type = Type.function(body.type(), *argTypes)
    val callRoot = CallRootNode(body, callDescriptor, Discloser(calleeBindings))
    val callTarget = Truffle.getRuntime().createCallTarget(callRoot)
    val fn = ExplFunction.create(type, callTarget);
    return FunctionDefinitionNode(fn, Encloser(closureDescriptor, closureBindings))
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

private inline fun check(ctx: ParserRuleContext, predicate: Boolean, msg: () -> String) {
  if (!predicate) {
    throw CompileError(msg(), ctx)
  }
}
