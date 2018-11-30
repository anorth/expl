package org.explang.cli

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameDescriptor
import com.xenomachina.argparser.ArgParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.misc.Interval
import org.explang.parser.ExplLexer
import org.explang.parser.ExplParser
import org.explang.syntax.Parser
import org.explang.truffle.compiler.Analyzer
import org.explang.truffle.compiler.CompileError2
import org.explang.truffle.compiler.Compiler2

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

    // Complete lex up-front so we can detect trailing tokens. Note that the last token is an EOF.
    tokens.fill()

    // Show tokens
    if (args.showTokens) {
      println("*Tokens*")
      for (tok in tokens.tokens) {
        if (tok is CommonToken) {
          println(tok.toString(lexer))
        } else {
          println(tok)
        }
      }
    }

    val parse = parser.expression()
    if (parse.sourceInterval != Interval(0, tokens.size() - 2)) {
      println("${parse.sourceInterval}, ${tokens.size()}")
      val trailing = tokens.get(parse.sourceInterval.b + 1, tokens.size() - 2)
      println("Parse failed with trailing tokens: ${trailing.joinToString(" ") { it.text }}")
      return
    }
    if (args.showParse) {
      println("*Parse*")
      println(parse.toStringTree(parser))
    }

    if (args.showAst) {
      val synParser = Parser()
      val tree = synParser.parse(parse)
      println("*AST*")
      println(tree)

      val analysis = Analyzer().analyze(tree)
      println("*Analysis*")
      println(analysis)

      val compiler = Compiler2()
      try {
        val ast = compiler.compile(tree)
        if (args.showAst) {
          println("*AST*")
          println(ast)
        }

        val topFrame = Truffle.getRuntime().createVirtualFrame(arrayOfNulls(0), FrameDescriptor())
        val result = ast.executeDeclaredType(topFrame)
        println("*Result*")
        println(result)
      } catch (e: CompileError2) {
        println("*Compile failed*")
        println(args.expression)
        // TODO: plumb source location through tree
//        println(" ".repeat(e.tree.range.start) + "^")
        println(e.message)
      }
    }
  }

}

fun main(args: Array<String>) {
  val pargs = ArgParser(args).parseInto(::Args)
  Cli().run(pargs)
}
