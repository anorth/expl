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
  /**
   * @param <T> type of the AST tag data structure
   */
  class Result<T>(
    val tokens: List<Token>,
    val parse: ExplParser.ExpressionContext,
    val tree: ExTree<T>,
    val error: String? = null
  )

  /**
   * @param s string to parse
   * @param tag factory for empty analysis tags
   */
  fun <T> parse(s: String, tag: () -> T ): Result<T> {
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

    val ast = toSyntax(parseTree, tag)
    if (printAst) {
      println("*AST*")
      println(ast)
    }

    return Result(tokens.tokens, parseTree, ast, error)
  }

  fun <T> toSyntax(tree: ParseTree,  tag: () -> T) = AstBuilder<T>(tag).visit(tree)
}

/** A parse tree visitor that constructs an AST */
private class AstBuilder<T>(private val tag: () -> T) : ExplBaseVisitor<ExTree<T>>() {
  override fun visit(tree: ParseTree) = super.visit(tree)!!

  override fun visitCallEx(ctx: ExplParser.CallExContext): ExCall<T> {
    val fn = visit(ctx.expression())
    val args = makeArguments(ctx.arguments())
    return ExCall(ctx.range(), tag(), fn, args)
  }

  override fun visitUnaryPlusEx(ctx: ExplParser.UnaryPlusExContext) =
      ExUnaryOp(ctx.range(), tag(), "+", visit(ctx.expression()))

  override fun visitUnaryMinusEx(ctx: ExplParser.UnaryMinusExContext) =
      ExUnaryOp(ctx.range(), tag(), "-", visit(ctx.expression()))

  override fun visitExponentiationEx(ctx: ExplParser.ExponentiationExContext): ExBinaryOp<T> {
    // Right-associativity is handled by the grammar.
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return ExBinaryOp(ctx.range(), tag(), ctx.POW().text, left, right)
  }

  override fun visitMultiplicativeEx(ctx: ExplParser.MultiplicativeExContext): ExBinaryOp<T> {
    // Left-associativity is handled by the grammar.
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return ExBinaryOp(ctx.range(), tag(), ctx.getChild(1).text, left, right)
  }

  override fun visitAdditiveEx(ctx: ExplParser.AdditiveExContext): ExBinaryOp<T> {
    // Left-associativity is handled by the grammar.
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return ExBinaryOp(ctx.range(), tag(), ctx.getChild(1).text, left, right)
  }

  override fun visitComparativeEx(ctx: ExplParser.ComparativeExContext): ExBinaryOp<T> {
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return ExBinaryOp(ctx.range(), tag(), ctx.getChild(1).text, left, right)
  }

  override fun visitEqualityEx(ctx: ExplParser.EqualityExContext): ExTree<T> {
    val left = visit(ctx.expression(0))
    val right = visit(ctx.expression(1))
    return ExBinaryOp(ctx.range(), tag(), ctx.getChild(1).text, left, right)
  }

  override fun visitLiteralEx(ctx: ExplParser.LiteralExContext): ExTree<T> {
    return visit(ctx.literal())
  }

  override fun visitIfEx(ctx: ExplParser.IfExContext) = ExIf<T>(ctx.range(), tag(),
      visit(ctx.expression(0)),
      visit(ctx.expression(1)),
      visit(ctx.expression(2)))

  override fun visitLetEx(ctx: ExplParser.LetExContext) =
      ExLet(ctx.range(), tag(), ctx.binding().map(this::visitBinding), visit(ctx.expression()))

  override fun visitParenthesizedEx(ctx: ExplParser.ParenthesizedExContext): ExTree<T> {
    return visit(ctx.expression())
  }

  override fun visitLambdaEx(ctx: ExplParser.LambdaExContext): ExLambda<T> {
    val params = ctx.lambdaParameters().let { p ->
      if (p.parameter() != null) {
        // Sugar for single-parameter anonymous lambdas
        listOf(p.parameter())
      } else {
        p.formalParameters().parameter()
      }
    }
    return makeLambda(ctx, params, ctx.expression())
  }

  override fun visitParameter(ctx: ExplParser.ParameterContext): ExParameter<T> {
    return ExParameter(ctx.range(), tag(), visitSymbol(ctx.symbol()),
        makeTypeAnnotation(ctx.typeAnnotation()))
  }

  override fun visitBinding(ctx: ExplParser.BindingContext): ExBinding<T> {
    val value = if (ctx.formalParameters() != null) {
      // Sugar for named function definitions.
      makeLambda(ctx, ctx.formalParameters().parameter(), ctx.expression())
    } else {
      // Simple binding.
      visit(ctx.expression())
    }
    return ExBinding(ctx.range(), tag(), visitSymbol(ctx.symbol()), value)
  }

  override fun visitSymbol(ctx: ExplParser.SymbolContext) =
      ExSymbol(ctx.range(), tag(), ctx.text)

  override fun visitBool(ctx: ExplParser.BoolContext) =
      ExLiteral(ctx.range(), tag(), Boolean::class.java, ctx.text!!.toBoolean())

  override fun visitNumber(ctx: ExplParser.NumberContext) =
      ExLiteral(ctx.range(), tag(), Double::class.java, ctx.text.toDouble())

  ///// Internals /////

  private fun makeArguments(ctx: ExplParser.ArgumentsContext) = ctx.expression().map(::visit)

  private fun makeFormalParameters(paramCtxs: List<ExplParser.ParameterContext>) =
      paramCtxs.map(this::visitParameter)

  private fun makeTypeAnnotation(ctx: ExplParser.TypeAnnotationContext): Type {
    val prim = ctx.typeExpression().typePrimitive()
    return when {
      prim.BOOL() != null -> Type.BOOL
      prim.DOUBLE() != null -> Type.DOUBLE
      else -> throw RuntimeException("Unhandled type literal ${prim.text}")
    }
  }

  private fun makeLambda(ctx: ParserRuleContext, paramCtxs: List<ExplParser.ParameterContext>,
      bodyCtx: ExplParser.ExpressionContext) =
      ExLambda(ctx.range(), tag(), makeFormalParameters(paramCtxs), visit(bodyCtx))

  override fun visitFormalParameters(ctx: ExplParser.FormalParametersContext?): ExTree<T> {
    assert(false) { "Unused" }
    return super.visitFormalParameters(ctx)
  }

  override fun visitArguments(ctx: ExplParser.ArgumentsContext?): ExTree<T> {
    assert(false) { "Unused" }
    return super.visitArguments(ctx)
  }

  override fun visitTypeAnnotation(ctx: ExplParser.TypeAnnotationContext?): ExTree<T> {
    assert(false) { "Unused" }
    return super.visitTypeAnnotation(ctx)
  }

  override fun visitTypePrimitive(ctx: ExplParser.TypePrimitiveContext?): ExTree<T> {
    assert(false) { "Unused" }
    return super.visitTypePrimitive(ctx)
  }
}

// Both Interval and IntRange are closed (inclusive) ranges.
private fun ParserRuleContext.range() = sourceInterval.a..sourceInterval.b
