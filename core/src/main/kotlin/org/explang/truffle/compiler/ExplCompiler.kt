package org.explang.truffle.compiler

import org.antlr.v4.runtime.tree.TerminalNode
import org.explang.parser.ExplBaseVisitor
import org.explang.parser.ExplLexer
import org.explang.parser.ExplParser
import org.explang.truffle.nodes.ExpressionNode
import org.explang.truffle.nodes.LiteralDoubleNode
import org.explang.truffle.nodes.SumNode
import java.lang.Double.parseDouble

class ExplCompiler {
  fun compile(parse: ExplParser.ExpressionContext) : ExpressionNode<*> {
    val builder = AstBuilder()
    return builder.visit(parse)
  }
}

/** A parse tree visitor that constructs an AST */
class AstBuilder : ExplBaseVisitor<ExpressionNode<*>>() {
  override fun visitExpression(ctx: ExplParser.ExpressionContext) =
      visitSum(ctx.sum())

  override fun visitSum(ctx: ExplParser.SumContext): ExpressionNode<Double> {
    // Build left-associative tree.
    val itr = ctx.children.iterator()
    var left = visitSigned(itr.next() as ExplParser.SignedContext)
    while (itr.hasNext()) {
      val op = itr.next()
      val right = visitSigned(itr.next() as ExplParser.SignedContext)
      left = when ((op as TerminalNode).symbol.type) {
        ExplLexer.PLUS -> SumNode.addDouble(left, right)
        else -> SumNode.subDouble(left, right)
      }
    }
    return left
  }

  override fun visitSigned(ctx: ExplParser.SignedContext): ExpressionNode<Double> = when {
    ctx.PLUS() != null -> visitSigned(ctx.signed())
    ctx.MINUS() != null -> TODO("Unsupported unary minus operator")
    else -> visitAtom(ctx.atom())
  }

  override fun visitAtom(ctx: ExplParser.AtomContext): ExpressionNode<Double> = when {
    ctx.number() != null -> visitNumber(ctx.number())
    else -> visitExpression(ctx.expression())
  }

  override fun visitNumber(ctx: ExplParser.NumberContext): ExpressionNode<Double> =
      LiteralDoubleNode(parseDouble(ctx.text))
}
