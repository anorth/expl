package org.explang.syntax

import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.misc.Interval
import org.antlr.v4.runtime.tree.ParseTree
import org.explang.parser.ExplBaseVisitor
import org.explang.parser.ExplLexer
import org.explang.parser.ExplParser


/**
 * Parses text into abstract syntax trees.
 *
 * The parser doesn't do any name resolution or type analysis, just syntax.
 */
class Parser(
    private val printTokens: Boolean = false,
    private val printParse: Boolean = false,
    private val printAst: Boolean = false,
    private val trace: Boolean = false
) {
  class Result(
    val tokens: List<Token>,
    val parse: ExplParser.ExpressionContext,
    val tree: ExTree,
    val error: String? = null
  )

  fun parse(s: String): Result {
    val lexer = ExplLexer(CharStreams.fromString(s))
    val tokens = CommonTokenStream(lexer)

    val parser = ExplParser(tokens)
    parser.buildParseTree = true
    parser.isTrace = trace

    // Complete lex up-front so we can detect trailing tokens. Note that the last token is an EOF.
    tokens.fill()

    if (printTokens) {
      println("*Tokens*")
      for (tok in tokens.tokens) {
        if (tok is CommonToken) {
          println(tok.toString(lexer))
        } else {
          println(tok)
        }
      }
    }

    val parseTree = parser.expression()
    var error: String? = null
    if (parseTree.sourceInterval != Interval(0, tokens.size() - 2)) {
      println("${parseTree.sourceInterval}, ${tokens.size()}")
      val trailing = tokens.get(parseTree.sourceInterval.b + 1, tokens.size() - 2)
      error = "Parse failed with trailing tokens: ${trailing.joinToString(" ") { it.text }}"
    }

    if (printParse) {
      println("*Parse*")
      println(parseTree.toStringTree(parser))
    }

    val ast = toSyntax(parseTree)
    if (printAst) {
      println("*AST*")
      println(ast)
    }

    return Result(tokens.tokens, parseTree, ast, error)
  }

  fun toSyntax(tree: ParseTree) = AstBuilder().visit(tree)
}

/** A parse tree visitor that constructs an AST */
private class AstBuilder : ExplBaseVisitor<ExTree>() {
  override fun visit(tree: ParseTree) = super.visit(tree)!!

  override fun visitCallEx(ctx: ExplParser.CallExContext): ExCall {
    val fn = visit(ctx.expression())
    val args = makeArguments(ctx.arguments())
    return ExCall(ctx.range(), fn, args)
  }

  override fun visitUnaryPlusEx(ctx: ExplParser.UnaryPlusExContext) =
      ExUnaryOp(ctx.range(), "+", visit(ctx.expression()))

  override fun visitUnaryMinusEx(ctx: ExplParser.UnaryMinusExContext) =
      ExUnaryOp(ctx.range(), "-", visit(ctx.expression()))

  override fun visitExponentiationEx(ctx: ExplParser.ExponentiationExContext): ExBinaryOp {
    // Right-associativity is handled by the grammar.
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return ExBinaryOp(ctx.range(), ctx.POW().text, left, right)
  }

  override fun visitMultiplicativeEx(ctx: ExplParser.MultiplicativeExContext): ExBinaryOp {
    // Left-associativity is handled by the grammar.
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return ExBinaryOp(ctx.range(), ctx.getChild(1).text, left, right)
  }

  override fun visitAdditiveEx(ctx: ExplParser.AdditiveExContext): ExBinaryOp {
    // Left-associativity is handled by the grammar.
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return ExBinaryOp(ctx.range(), ctx.getChild(1).text, left, right)
  }

  override fun visitComparativeEx(ctx: ExplParser.ComparativeExContext): ExBinaryOp {
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return ExBinaryOp(ctx.range(), ctx.getChild(1).text, left, right)
  }

  override fun visitEqualityEx(ctx: ExplParser.EqualityExContext): ExTree {
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return ExBinaryOp(ctx.range(), ctx.getChild(1).text, left, right)
  }

  override fun visitLiteralEx(ctx: ExplParser.LiteralExContext): ExTree {
    return visit(ctx.literal())
  }

  fun visitSymbolEx(ctx: ExplParser.SymbolContext) =
      ExSymbol(ctx.range(), ctx.text)

  override fun visitIfEx(ctx: ExplParser.IfExContext) = ExIf(ctx.range(),
      visit(ctx.expression(0)),
      visit(ctx.expression(1)),
      visit(ctx.expression(2)))

  override fun visitLetEx(ctx: ExplParser.LetExContext) =
      ExLet(ctx.range(), ctx.binding().map(this::visitBinding), visit(ctx.expression()))

  override fun visitParenthesizedEx(ctx: ExplParser.ParenthesizedExContext): ExTree {
    return visit(ctx.expression())
  }

  override fun visitLambdaEx(ctx: ExplParser.LambdaExContext): ExLambda {
    val params = ctx.lambdaParameters().let { p ->
      if (p.symbol() != null) {
        // Sugar for single-parameter anonymous lambdas
        listOf(p.symbol())
      } else {
        p.formalParameters().symbol()
      }
    }
    return makeLambda(ctx, params, ctx.expression())
  }

  override fun visitBinding(ctx: ExplParser.BindingContext): ExBinding {
    val value = if (ctx.formalParameters() != null) {
      // Sugar for named function definitions.
      makeLambda(ctx, ctx.formalParameters().symbol(), ctx.expression())
    } else {
      // Simple binding.
      visit(ctx.expression())
    }
    return ExBinding(ctx.range(), visitSymbol(ctx.symbol()), value)
  }

  override fun visitSymbol(ctx: ExplParser.SymbolContext) = ExSymbol(ctx.range(), ctx.text)

  override fun visitBool(ctx: ExplParser.BoolContext) =
      ExLiteral(ctx.range(), Boolean::class.java, ctx.text!!.toBoolean())

  override fun visitNumber(ctx: ExplParser.NumberContext) = ExLiteral(ctx.range(),
      Double::class.java, ctx.text.toDouble())

  ///// Internals /////

  private fun makeArguments(ctx: ExplParser.ArgumentsContext) = ctx.expression().map(::visit)

  private fun makeFormalParameters(paramCtxs: List<ExplParser.SymbolContext>) =
      paramCtxs.map(this::visitSymbolEx)

  private fun makeLambda(ctx: ParserRuleContext, paramCtxs: List<ExplParser.SymbolContext>,
      bodyCtx: ExplParser.ExpressionContext) =
      ExLambda(ctx.range(), makeFormalParameters(paramCtxs), visit(bodyCtx))

  override fun visitFormalParameters(ctx: ExplParser.FormalParametersContext?): ExTree {
    assert(false) { "Unused" }
    return super.visitFormalParameters(ctx)
  }

  override fun visitArguments(ctx: ExplParser.ArgumentsContext?): ExTree {
    assert(false) { "Unused" }
    return super.visitArguments(ctx)
  }
}

// Both Interval and IntRange are closed (inclusive) ranges.
private fun ParserRuleContext.range() = sourceInterval.a..sourceInterval.b