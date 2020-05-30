package org.explang.analysis

import org.explang.syntax.*

class IntermediateCompiler {
  fun <T> transform(syntax: ExTree<T>): ITree {
    val itree = syntax.accept(SyntaxTransformer())
    return itree
  }
}

private class SyntaxTransformer<T> : ExTree.Visitor<T, ITree> {
  override fun visitCall(call: ExCall<T>): ICall {
    val callee = call.callee.accept(this)
    val args = call.args.map { it.accept(this) }
    return ICall(call, callee, args)
  }

  override fun visitIndex(index: ExIndex<T>): ITree {
    // Desugar operators into calls.
    // They may be optimized to direct intrinsics after static scope resolution.
    val callee = ISymbol(index, "[]")
    val args = listOf(index.indexee.accept(this), index.indexer.accept(this))
    return ICall(index, callee, args)
  }

  override fun visitUnaryOp(op: ExUnaryOp<T>): ITree {
    val callee = ISymbol(op, op.operator)
    val args = listOf(op.operand.accept(this))
    return ICall(op, callee, args)
  }

  override fun visitBinaryOp(op: ExBinaryOp<T>): ITree {
    val callee = ISymbol(op, op.operator)
    val args = listOf(op.left.accept(this), op.right.accept(this))
    return ICall(op, callee, args)
  }

  override fun visitRangeOp(op: ExRangeOp<T>): ITree {
    val callee = ISymbol(op, ":")
    val args = listOf(op.first?.accept(this) ?: INull(null),
        op.last?.accept(this) ?: INull(null),
        op.step?.accept(this) ?: INull(null))
    return ICall(op, callee, args)
  }

  override fun visitIf(iff: ExIf<T>) =
      IIf(iff, iff.test.accept(this), iff.left.accept(this), iff.right.accept(this))

  override fun visitLet(let: ExLet<T>) =
      ILet(let, let.bindings.map(this::visitBinding), let.bound.accept(this))

  override fun visitBinding(binding: ExBinding<T>) =
      IBinding(binding, this.visitSymbol(binding.symbol), binding.value.accept(this))

  override fun visitLambda(lambda: ExLambda<T>): ITree {
    return ILambda(lambda, lambda.parameters.map(this::visitParameter), lambda.annotation, lambda.body.accept(this))
  }

  override fun visitParameter(parameter: ExParameter<T>) =
      IParameter(parameter, visitSymbol(parameter.symbol), parameter.annotation)

  override fun visitLiteral(literal: ExLiteral<T, *>) = iliteral(literal)

  override fun visitSymbol(symbol: ExSymbol<T>) = ISymbol(symbol, symbol.name)
}

private fun <T, L : Any> iliteral(syntax: ExLiteral<T, L>): ILiteral<L> {
  return ILiteral(syntax, syntax.type, syntax.value)
}