package org.explang.intermediate

import org.explang.common.mapArr
import org.explang.syntax.*

/**
 * Translates a pure syntax tree into an intermediate tree for further compilation steps.
 */
class SyntaxTranslator {
  fun translate(syntax: ExTree): ITree {
    return syntax.accept(Translator())
  }
}

private class Translator : ExTree.Visitor<ITree> {
  override fun visitCall(call: ExCall): ICall {
    val callee = call.callee.accept(this)
    val args = call.args.mapArr(ITree::class.java) { it.accept(this) }
    return ICall(call, Type.NONE, callee, args)
  }

  override fun visitIndex(index: ExIndex): ITree {
    // Desugar operators into calls.
    // They may be optimized to direct intrinsics after static scope resolution.
    val callee = ISymbol(index, Type.NONE, "[]")
    val args = arrayOf(index.indexee.accept(this), index.indexer.accept(this))
    return ICall(index, Type.NONE, callee, args)
  }

  override fun visitUnaryOp(op: ExUnaryOp): ITree {
    val callee = ISymbol(op, Type.NONE, op.operator)
    val args = arrayOf(op.operand.accept(this))
    return ICall(op, Type.NONE, callee, args)
  }

  override fun visitBinaryOp(op: ExBinaryOp): ITree {
    val callee = ISymbol(op, Type.NONE, op.operator)
    val args = arrayOf(op.left.accept(this), op.right.accept(this))
    return ICall(op, Type.NONE, callee, args)
  }

  override fun visitRangeOp(op: ExRangeOp): ITree {
    val callee = ISymbol(op, Type.NONE, ":")
    val args = arrayOf(op.first?.accept(this) ?: INull(null, Type.NONE),
        op.last?.accept(this) ?: INull(null, Type.NONE),
        op.step?.accept(this) ?: INull(null, Type.NONE))
    return ICall(op, Type.NONE, callee, args)
  }

  override fun visitIf(iff: ExIf) =
      IIf(iff, Type.NONE, iff.test.accept(this), iff.left.accept(this), iff.right.accept(this))

  override fun visitLet(let: ExLet) =
      ILet(let, Type.NONE, let.bindings.map(this::visitBinding), let.bound.accept(this))

  override fun visitBinding(binding: ExBinding) =
      IBinding(binding, Type.NONE, this.visitSymbol(binding.symbol), binding.value.accept(this))

  override fun visitLambda(lambda: ExLambda) =
      ILambda(lambda, Type.NONE, lambda.parameters.map(this::visitParameter), lambda.body.accept(this),
          lambda.annotation)

  override fun visitParameter(parameter: ExParameter) =
      IParameter(parameter, parameter.annotation, visitSymbol(parameter.symbol))

  override fun visitLiteral(literal: ExLiteral<*>) = iliteral(literal)

  override fun visitSymbol(symbol: ExSymbol) = ISymbol(symbol, Type.NONE, symbol.name)
}

private fun <L : Any> iliteral(syntax: ExLiteral<L>): ILiteral<L> {
  return ILiteral(syntax, Type.NONE, syntax.type, syntax.value)
}