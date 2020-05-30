package org.explang.intermediate

import org.explang.syntax.*

/**
 * Translates a pure syntax tree into an intermediate tree for further compilation steps.
 */
class SyntaxTranslator {
  fun translate(syntax: ExTree): ITree {
    return syntax.accept(SyntaxTransformer())
  }
}

private class SyntaxTransformer : ExTree.Visitor<ITree> {
  override fun visitCall(call: ExCall): ICall {
    val callee = call.callee.accept(this)
    val args = call.args.map { it.accept(this) }
    return ICall(call, callee, args)
  }

  override fun visitIndex(index: ExIndex): ITree {
    // Desugar operators into calls.
    // They may be optimized to direct intrinsics after static scope resolution.
    val callee = ISymbol(index, "[]")
    val args = listOf(index.indexee.accept(this), index.indexer.accept(this))
    return ICall(index, callee, args)
  }

  override fun visitUnaryOp(op: ExUnaryOp): ITree {
    val callee = ISymbol(op, op.operator)
    val args = listOf(op.operand.accept(this))
    return ICall(op, callee, args)
  }

  override fun visitBinaryOp(op: ExBinaryOp): ITree {
    val callee = ISymbol(op, op.operator)
    val args = listOf(op.left.accept(this), op.right.accept(this))
    return ICall(op, callee, args)
  }

  override fun visitRangeOp(op: ExRangeOp): ITree {
    val callee = ISymbol(op, ":")
    val args = listOf(op.first?.accept(this) ?: INull(null),
        op.last?.accept(this) ?: INull(null),
        op.step?.accept(this) ?: INull(null))
    return ICall(op, callee, args)
  }

  override fun visitIf(iff: ExIf) =
      IIf(iff, iff.test.accept(this), iff.left.accept(this),
          iff.right.accept(this))

  override fun visitLet(let: ExLet) =
      ILet(let, let.bindings.map(this::visitBinding),
          let.bound.accept(this))

  override fun visitBinding(binding: ExBinding) =
      IBinding(binding, this.visitSymbol(binding.symbol),
          binding.value.accept(this))

  override fun visitLambda(lambda: ExLambda): ITree {
    return ILambda(lambda, lambda.parameters.map(this::visitParameter),
        lambda.annotation, lambda.body.accept(this))
  }

  override fun visitParameter(parameter: ExParameter) =
      IParameter(parameter, visitSymbol(parameter.symbol),
          parameter.annotation)

  override fun visitLiteral(literal: ExLiteral<*>) = iliteral(literal)

  override fun visitSymbol(symbol: ExSymbol) = ISymbol(symbol, symbol.name)
}

private fun <L : Any> iliteral(syntax: ExLiteral<L>): ILiteral<L> {
  return ILiteral(syntax, syntax.type, syntax.value)
}