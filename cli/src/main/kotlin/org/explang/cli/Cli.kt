package org.explang.cli

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameDescriptor
import com.xenomachina.argparser.ArgParser
import org.explang.syntax.Parser
import org.explang.truffle.compiler.CompileError
import org.explang.truffle.compiler.Compiler

// See https://github.com/xenomachina/kotlin-argparser
class Args(parser: ArgParser) {
  val trace by parser.flagging("--trace", help = "Print parser trace")
  val showTokens by parser.flagging("--show-lex", help = "Print tokens to standard output")
  val showParse by parser.flagging("--show-parse", help = "Print parse tree to standard output")
  val showAst by parser.flagging("--show-ast", help = "Print syntax tree to standard output")
  val showAnalysis by parser.flagging("--show-analysis", help = "Print analysis standard output")
  val showAll by parser.flagging("--show-all", help = "Print *everything* standard output")
  val expression by parser.positional("EXPRESSION", help = "Expression to evaluate")
}

class Cli {
  fun run(args: Args) {
    val parser = Parser(
        printTokens = args.showTokens || args.showAll,
        printParse = args.showParse || args.showAll,
        printAst = args.showAst || args.showAll,
        trace = args.trace || args.showAll
    )

    val compiler = Compiler(
        printAnalysis = args.showAnalysis || args.showAll
    )

    val parse = parser.parse(args.expression)
    try {
      val ast = compiler.compile(parse.tree)

      val topFrame = Truffle.getRuntime().createVirtualFrame(arrayOfNulls(0), FrameDescriptor())
      val result = ast.executeDeclaredType(topFrame)
      println("*Result*")
      println(result)
    } catch (e: CompileError) {
      println("*Compile failed*")
      println(args.expression)
      // TODO: plumb source location through tree
//        println(" ".repeat(e.tree.tokens.start) + "^")
      println(e.message)
    }
  }
}

fun main(args: Array<String>) {
  val pargs = ArgParser(args).parseInto(::Args)
  Cli().run(pargs)
}
