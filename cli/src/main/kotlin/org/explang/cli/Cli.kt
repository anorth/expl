package org.explang.cli

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameDescriptor
import com.xenomachina.argparser.ArgParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.explang.parser.ExplLexer
import org.explang.parser.ExplParser
import org.explang.truffle.compiler.ExplCompiler
import org.explang.truffle.nodes.ExpressionNode
import org.explang.truffle.nodes.ExpressionRootNode

// See https://github.com/xenomachina/kotlin-argparser
class Args(parser: ArgParser) {
  val trace by parser.flagging("--trace", help = "Print parser trace")
  val showTokens by parser.flagging("--show-lex", help = "Print tokens to standard output")
  val showParse by parser.flagging("--show-parse", help = "Print parse tree to standard output")
  val showAst by parser.flagging("--show-ast", help = "Print syntax tree to standard output")
  val expression by parser.positional("EXPRESSION", help = "Expression to evaluate")
}

class Cli {
  fun run(args: Args) {
    val lexer = ExplLexer(CharStreams.fromString(args.expression))
    val tokens = CommonTokenStream(lexer)
    val parser = ExplParser(tokens)
    parser.buildParseTree = true // For visiting in in compiler
    parser.isTrace = args.trace

    // Show tokens
    if (args.showTokens) {
      println("*Tokens*")
      tokens.fill()
      for (tok in tokens.tokens) {
        if (tok is CommonToken) {
          println(tok.toString(lexer))
        } else {
          println(tok)
        }
      }
    }

    val parse = parser.expression()
    if (args.showParse) {
      println("*Parse*")
      println(parse.toStringTree(parser))
    }

    val compiler = ExplCompiler()
    val (ast, topFrameDescriptor) = compiler.compile(parse)
    if (args.showAst) {
      println("*AST*")
      println(ast)
    }

    val result = evaluate(ast, topFrameDescriptor)
    println("*Result*")
    println(result)
  }

  private fun evaluate(expr: ExpressionNode, topFrameDescriptor: FrameDescriptor): Any {
    // Evaluate the expression
    val rootNode = ExpressionRootNode(expr, topFrameDescriptor)
    val target = Truffle.getRuntime().createCallTarget(rootNode)
    return target.call()
  }
}

fun main(args: Array<String>) {
  val pargs = ArgParser(args).parseInto(::Args)
  Cli().run(pargs)
}
