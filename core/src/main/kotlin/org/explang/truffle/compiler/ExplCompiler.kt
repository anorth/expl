package org.explang.truffle.compiler

import org.antlr.v4.runtime.tree.TerminalNode
import org.explang.parser.ExplBaseVisitor
import org.explang.parser.ExplLexer
import org.explang.parser.ExplParser
import org.explang.truffle.nodes.ExpressionNode
import org.explang.truffle.nodes.FactorNode
import org.explang.truffle.nodes.LiteralDoubleNode
import org.explang.truffle.nodes.NegationNode
import org.explang.truffle.nodes.ProductNode
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

  override fun visitProduct(ctx: ExplParser.ProductContext): ExpressionNode<Double> {
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

  override fun visitFactor(ctx: ExplParser.FactorContext): ExpressionNode<Double> {
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

  override fun visitSigned(ctx: ExplParser.SignedContext): ExpressionNode<Double> = when {
    ctx.PLUS() != null -> visitSigned(ctx.signed())
    ctx.MINUS() != null -> NegationNode(visitSigned(ctx.signed()))
    else -> visitAtom(ctx.atom())
  }

  override fun visitAtom(ctx: ExplParser.AtomContext): ExpressionNode<Double> = when {
    ctx.number() != null -> visitNumber(ctx.number())
    else -> visitExpression(ctx.expression())
  }

  override fun visitNumber(ctx: ExplParser.NumberContext): ExpressionNode<Double> =
      LiteralDoubleNode(parseDouble(ctx.text))
}
