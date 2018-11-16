package org.explang.truffle.compiler

import org.explang.parser.ExplBaseVisitor
import org.explang.parser.ExplParser
import org.explang.truffle.nodes.ExpressionNode
import org.explang.truffle.nodes.LiteralDoubleNode
import java.lang.Double.parseDouble

class ExplCompiler {
  fun compile(parse: ExplParser.ExpressionContext) : ExpressionNode {
    val builder = AstBuilder()
    return builder.visit(parse)
  }
}

/** A parse tree visitor that constructs an AST */
class AstBuilder : ExplBaseVisitor<ExpressionNode>() {
  override fun visitExpression(ctx: ExplParser.ExpressionContext) =
      visitSum(ctx.sum())

  override fun visitSum(ctx: ExplParser.SumContext): ExpressionNode = when {
    ctx.PLUS().isNotEmpty() -> TODO("Sum expressions")
    ctx.MINUS().isNotEmpty() -> TODO("Sum expressions")
    else -> visitSigned(ctx.signed(0))
  }

  override fun visitSigned(ctx: ExplParser.SignedContext): ExpressionNode = when {
    ctx.PLUS() != null -> visitSigned(ctx.signed())
    ctx.MINUS() != null -> TODO("Unsupported unary minus operator")
    else -> visitAtom(ctx.atom())
  }

  override fun visitAtom(ctx: ExplParser.AtomContext): ExpressionNode = when {
    ctx.number() != null -> visitNumber(ctx.number())
    else -> visitExpression(ctx.expression())
  }

  override fun visitNumber(ctx: ExplParser.NumberContext): ExpressionNode =
      LiteralDoubleNode(parseDouble(ctx.text)) // Integer or float
}
