package org.explang.syntax

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
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
    val syntax: ExTree<T>?,
    val error: ParseError? = null
  )

  /** A failure to parse. */
  class ParseError(
      val line: Int,
      val charPositionInLine: Int,
      val msg: String
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

    val errors = ErrorAccumulator()
    parser.removeErrorListeners() // Console listener is added by default
    parser.addErrorListener(errors)

    val parseTree = parser.expression()
    var error: ParseError? = null
    if (parser.numberOfSyntaxErrors > 0) {
      error = errors.errors.first()
    } else if (parseTree.sourceInterval != Interval(0, tokens.size() - 2)) {
      val trailing = tokens.get(parseTree.sourceInterval.b + 1, tokens.size() - 2)
      error = ParseError(
          parseTree.stop.line,
          parseTree.stop.charPositionInLine,
          "Parse failed with trailing tokens: ${trailing.joinToString(" ") { it.text }}"
      )
    }

    if (printParse) {
      println("*Parse*")
      println(parseTree.toStringTree(parser))
    }

    // The parser emits a tree even if it encounters errors, but in that case the tree might not
    // be syntactically valid, so the AST builder can fail. I must decide whether to make the AST
    // builder robust to partial trees, or just fail here before the AST.
    val ast = if (error == null) {
      AstBuilder(tag).visit(parseTree).also {
        if (printAst) {
          println("*AST*")
          println(it)
        }
      }
    } else {
      null
    }

    return Result(tokens.tokens, parseTree, ast, error)
  }
}

/** A parse tree visitor that constructs an AST */
private class AstBuilder<T>(private val tag: () -> T) : ExplBaseVisitor<ExTree<T>>() {
  override fun visit(tree: ParseTree) = super.visit(tree)!!

  ///// expression /////

  override fun visitCallEx(ctx: ExplParser.CallExContext): ExCall<T> {
    val fn = visit(ctx.expression())
    val args = makeArguments(ctx.arguments())
    return ExCall(ctx.range(), tag(), fn, args)
  }

  override fun visitUnaryEx(ctx: ExplParser.UnaryExContext) =
      ExUnaryOp(ctx.range(), tag(), ctx.getChild(0).text, visit(ctx.expression()))

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

  override fun visitConjunctionEx(ctx: ExplParser.ConjunctionExContext): ExTree<T> {
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
    val (params, ret) = ctx.lambdaParameters().let { p ->
      if (p.parameter() != null) {
        // Sugar for single-parameter anonymous lambdas
        listOf(p.parameter()) to Type.NONE
      } else {
        p.formalParameters().parameter() to makeType(p.formalParameters().typeAnnotation())
      }
    }
    return makeLambda(ctx, params, ret, ctx.expression())
  }

  ///// Sub-rules /////

  override fun visitBinding(ctx: ExplParser.BindingContext): ExBinding<T> {
    val value = if (ctx.formalParameters() != null) {
      // Sugar for named function definitions.
      makeLambda(ctx, ctx.formalParameters().parameter(),
          makeType(ctx.formalParameters().typeAnnotation()),
          ctx.expression())
    } else {
      // Simple binding.
      visit(ctx.expression())
    }
    return ExBinding(ctx.range(), tag(), visitSymbol(ctx.symbol()), value)
  }

  override fun visitParameter(ctx: ExplParser.ParameterContext): ExParameter<T> {
    return ExParameter(ctx.range(), tag(), visitSymbol(ctx.symbol()),
        makeType(ctx.typeAnnotation()))
  }

  override fun visitSymbol(ctx: ExplParser.SymbolContext) =
      ExSymbol(ctx.range(), tag(), ctx.text)

  override fun visitNumber(ctx: ExplParser.NumberContext): ExLiteral<T, *> {
    return when {
      ctx.INTEGER() != null -> ExLiteral(ctx.range(), tag(), Long::class.java, ctx.text.toLong())
      ctx.FLOAT() != null -> ExLiteral(ctx.range(), tag(), Double::class.java, ctx.text.toDouble())
      else -> throw RuntimeException("Unrecognized numeric literal ${ctx.text}")
    }
  }

  override fun visitBool(ctx: ExplParser.BoolContext) =
      ExLiteral(ctx.range(), tag(), Boolean::class.java, ctx.text!!.toBoolean())

  ///// Internals /////

  private fun makeArguments(ctx: ExplParser.ArgumentsContext) = ctx.expression().map(::visit)

  private fun makeFormalParameters(paramCtxs: List<ExplParser.ParameterContext>) =
      paramCtxs.map(this::visitParameter)

  private fun makeType(ctx: ExplParser.TypeAnnotationContext?): Type {
    return if (ctx == null) Type.NONE
    else makeType(ctx.typeExpression())
  }

  private fun makeType(ctx: ExplParser.TypeExpressionContext): Type {
    val prim = ctx.typePrimitive()
    if (prim != null) { // Primitive
      return when {
        prim.BOOL() != null -> Type.BOOL
        prim.LONG() != null -> Type.LONG
        prim.DOUBLE() != null -> Type.DOUBLE
        else -> throw RuntimeException("Unrecognized type literal ${prim.text}")
      }
    } else if (ctx.ARROW() != null) { // Function
      val children = ctx.typeExpression()
      val params = children.subList(0, children.lastIndex)
      val ret = children.last()
      return Type.function(makeType(ret), *params.map(this::makeType).toTypedArray())
    } else { // Array
      val elType = ctx.typeExpression(0)
      return Type.array(makeType(elType))
    }
  }

  private fun makeLambda(ctx: ParserRuleContext, paramCtxs: List<ExplParser.ParameterContext>,
      annotation: Type, bodyCtx: ExplParser.ExpressionContext) =
      ExLambda(ctx.range(), tag(), makeFormalParameters(paramCtxs), annotation, visit(bodyCtx))

  override fun visitArguments(ctx: ExplParser.ArgumentsContext?): ExTree<T> {
    assert(false) { "Unused" }
    return super.visitArguments(ctx)
  }

  override fun visitFormalParameters(ctx: ExplParser.FormalParametersContext?): ExTree<T> {
    assert(false) { "Unused" }
    return super.visitFormalParameters(ctx)
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

private class ErrorAccumulator : BaseErrorListener() {
  private val accumulated = mutableListOf<Parser.ParseError>()

  val errors: List<Parser.ParseError> get() = accumulated

  override fun syntaxError(recognizer: Recognizer<*, *>?, offendingSymbol: Any?, line: Int,
      charPositionInLine: Int, msg: String?, e: RecognitionException?) {
    accumulated.add(Parser.ParseError(line, charPositionInLine, msg ?: "unknown"))
  }
}


// Both Interval and IntRange are closed (inclusive) ranges.
private fun ParserRuleContext.range() = sourceInterval.a..sourceInterval.b
