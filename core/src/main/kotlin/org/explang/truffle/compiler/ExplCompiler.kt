package org.explang.truffle.compiler

import com.oracle.truffle.api.frame.FrameDescriptor
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.explang.parser.ExplBaseVisitor
import org.explang.parser.ExplLexer
import org.explang.parser.ExplParser
import org.explang.truffle.nodes.ExpressionNode
import org.explang.truffle.nodes.FactorNode
import org.explang.truffle.nodes.FunctionCallNode
import org.explang.truffle.nodes.LiteralDoubleNode
import org.explang.truffle.nodes.NegationNode
import org.explang.truffle.nodes.ProductNode
import org.explang.truffle.nodes.SumNode
import org.explang.truffle.nodes.SymbolNode
import org.explang.truffle.nodes.builtin.StaticBoundNode
import java.lang.Double.parseDouble

class ExplCompiler {
  fun compile(parse: ExplParser.ExpressionContext) : ExpressionNode<*> {
    val builder = AstBuilder()
    return builder.build(parse)
  }
}

/** A parse tree visitor that constructs an AST */
class AstBuilder : ExplBaseVisitor<ExpressionNode<*>>() {
  // TODO: scope this better to avoid mutability.
  private var frameStack: MutableList<FrameDescriptor>? = null

  fun build(tree: ParseTree): ExpressionNode<*> {
    frameStack = mutableListOf(FrameDescriptor())
    try {
      return tree.accept(this)
    } finally {
      frameStack = null
    }
  }

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
      rt = FactorNode.expDouble(rt as ExpressionNode<Double>, left as ExpressionNode<Double>)
    }
    return rt as ExpressionNode<Double>
  }

  override fun visitSigned(ctx: ExplParser.SignedContext): ExpressionNode<*> = when {
    ctx.PLUS() != null -> visitSigned(ctx.signed())
    ctx.MINUS() != null -> NegationNode(visitSigned(ctx.signed()))
    ctx.fcall() != null -> visitFcall(ctx.fcall())
    else -> visitAtom(ctx.atom())
  }

  override fun visitFcall(ctx: ExplParser.FcallContext): ExpressionNode<*> {
    val symbol = visitAtom(ctx.atom()) // TODO: check type of symbol is function with these args
    val args = ctx.expression().map { visitExpression(it) }.toTypedArray()
    return FunctionCallNode<Any>(symbol, args)
  }

  override fun visitAtom(ctx: ExplParser.AtomContext): ExpressionNode<*> = when {
    ctx.number() != null -> visitNumber(ctx.number())
    ctx.symbol() != null -> visitSymbol(ctx.symbol())
    else -> visitExpression(ctx.expression())
  }

  override fun visitSymbol(ctx: ExplParser.SymbolContext): ExpressionNode<*> {
    val name = ctx.text
    return if (name in BUILT_INS) {
      StaticBoundNode.builtIn(BUILT_INS[name]!!)
    } else {
      // TODO: Add FrameSlotKind based on symbol type info
      val frame = frameStack!!.last()
      return SymbolNode(frame.findOrAddFrameSlot(ctx.text))
    }
  }

  override fun visitNumber(ctx: ExplParser.NumberContext): ExpressionNode<Double> =
      LiteralDoubleNode(parseDouble(ctx.text))
}
