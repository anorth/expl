package org.explang.truffle.compiler

import com.oracle.truffle.api.frame.FrameDescriptor
import com.oracle.truffle.api.frame.FrameSlotKind
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.explang.parser.ExplBaseVisitor
import org.explang.parser.ExplLexer
import org.explang.parser.ExplParser
import org.explang.truffle.Type
import org.explang.truffle.nodes.BindingNode
import org.explang.truffle.nodes.ExpressionNode
import org.explang.truffle.nodes.FactorNode
import org.explang.truffle.nodes.FunctionCallNode
import org.explang.truffle.nodes.LetNode
import org.explang.truffle.nodes.LiteralDoubleNode
import org.explang.truffle.nodes.NegationNode
import org.explang.truffle.nodes.ProductNode
import org.explang.truffle.nodes.SumNode
import org.explang.truffle.nodes.SymbolNode
import org.explang.truffle.nodes.builtin.StaticBound
import java.lang.Double.parseDouble
import java.util.LinkedList

class CompileError(msg: String, val context: ParserRuleContext): Exception(msg)

class ExplCompiler {
  fun compile(parse: ExplParser.ExpressionContext) : Pair<ExpressionNode, FrameDescriptor> {
    val builder = AstBuilder()
    return builder.build(parse)
  }
}

/** A parse tree visitor that constructs an AST */
class AstBuilder : ExplBaseVisitor<ExpressionNode>() {
  // TODO: scope this better to avoid mutability.
  private var frameStack: LinkedList<FrameDescriptor>? = null

  fun build(tree: ParseTree): Pair<ExpressionNode, FrameDescriptor> {
    frameStack = LinkedList()
    frameStack!!.push(FrameDescriptor())
    try {
      val ast = tree.accept(this)
      assert(frameStack!!.size == 1) { "Mangled frame stack" }
      return ast to frameStack!!.last
    } finally {
      frameStack = null
    }
  }

  override fun visitExpression(ctx: ExplParser.ExpressionContext): ExpressionNode = when {
    ctx.let() != null -> visitLet(ctx.let())
    else -> visitSum(ctx.sum())
  }

  override fun visitLet(ctx: ExplParser.LetContext): ExpressionNode {
    // Let bindings go in the same frame as the bound expression.
    // Bindings are keyed by frame slots in this frame.
    // Symbol nodes resolved in the subsequent expression will find those frame slots.
    // Bindings in the let clause are also visible to each other.
    // TODO: rewrite names in nested let clauses to avoid name re-use trashing the frame for
    // subsequent bindings (but it works ok for nested clauses). I.e scoped identifiers.
    // TODO: Statically resolve access to outer scopes
    val frame = frameStack!!.last
    val bindingNodes = arrayOfNulls<BindingNode>(ctx.binding().size)
    val names = mutableListOf<String>() // Faster than a set for small size?
    ctx.binding().forEachIndexed { i, it ->
      val name = it.symbol().text
      if (name in names) throw CompileError("Duplicate binding for $name", it)
      names.add(name)
      // FIXME: this construction means a recursive visitExpression might set its own slot
      // without type information. This can happen generally if the bound expression references
      // another binding. Need to define semantics for mutual accessibility and shadowing.
      val value = visitExpression(it.expression())
      val slot = frame.findOrAddFrameSlot(name, value.type, value.asSlotKind())
      bindingNodes[i] = BindingNode(slot, value)
    }
    val expressionNode = visitExpression(ctx.expression())
    return LetNode(bindingNodes, expressionNode)
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
    ctx.fcall() != null -> visitFcall(ctx.fcall())
    else -> visitAtom(ctx.atom())
  }

  override fun visitFcall(ctx: ExplParser.FcallContext): ExpressionNode {
    val symbol = visitAtom(ctx.atom()) // TODO: check type of symbol is function with these args
    val args = ctx.expression().map { visitExpression(it) }.toTypedArray()
    return FunctionCallNode(symbol, args)
  }

  override fun visitAtom(ctx: ExplParser.AtomContext): ExpressionNode = when {
    ctx.number() != null -> visitNumber(ctx.number())
    ctx.symbol() != null -> visitSymbol(ctx.symbol())
    else -> visitExpression(ctx.expression())
  }

  override fun visitSymbol(ctx: ExplParser.SymbolContext): ExpressionNode {
    val name = ctx.text
    return if (name in BUILT_INS) {
      StaticBound.builtIn(BUILT_INS[name]!!)
    } else {
      // FIXME propagate type information
      // TODO: Add FrameSlotKind based on symbol type info
      val frame = frameStack!!.last()
      return SymbolNode(Type.DOUBLE, frame.findOrAddFrameSlot(ctx.text))
    }
  }

  override fun visitNumber(ctx: ExplParser.NumberContext): ExpressionNode =
      LiteralDoubleNode(parseDouble(ctx.text))
}

private fun ExpressionNode.asSlotKind() = when (type) {
  Type.DOUBLE -> FrameSlotKind.Double
  else -> FrameSlotKind.Object
}
