package org.explang.cli

import com.xenomachina.argparser.ArgParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.explang.parser.ExplLexer
import org.explang.parser.ExplParser

// See https://github.com/xenomachina/kotlin-argparser
class Args(parser: ArgParser) {
  val trace by parser.flagging("--trace", help = "Print parser trace")
  val showTokens by parser.flagging("--show-tokens", help = "Print tokens to standard output")
  val showTree by parser.flagging("--show-tree", help = "Print parse tree to standard output")
  val expression by parser.positional("EXPRESSION", help = "Expression to evaluate")
}

class Cli {
  fun run(args: Args) {
    val lexer = ExplLexer(CharStreams.fromString(args.expression))
    val tokens = CommonTokenStream(lexer)
    val parser = ExplParser(tokens)
    parser.buildParseTree = args.showTree
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

    val expression = parser.expression()
    if (args.showTree) {
      println("*Tree*")
      println(expression.toStringTree(parser))
    }
  }
}

fun main(args: Array<String>) {
  val pargs = ArgParser(args).parseInto(::Args)
  Cli().run(pargs)
}
