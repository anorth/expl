package org.explang.cli

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import org.explang.analysis.Analyzer
import org.explang.analysis.CompileError
import org.explang.interpreter.Environment
import org.explang.interpreter.Interpreter
import org.explang.syntax.Parser

// See https://github.com/xenomachina/kotlin-argparser
class Args(parser: ArgParser) {
  val trace by parser.flagging("--trace", help = "Print parser trace")
  val showTokens by parser.flagging("--show-lex", help = "Print tokens to standard output")
  val showParse by parser.flagging("--show-parse", help = "Print parse tree to standard output")
  val showAst by parser.flagging("--show-ast", help = "Print syntax tree to standard output")
  val showAnalysis by parser.flagging("--show-analysis", help = "Print analysis standard output")
  val showAll by parser.flagging("--show-all", help = "Print *everything* standard output")

  val data by parser.storing("-d", "--data", help = "Path to JSON data file")
      .default(null as String?)

  val expression by parser.positional("EXPRESSION", help = "Expression to evaluate")
}

class Cli(args: Args) {
  private val parser = Parser(
      printTokens = args.showTokens || args.showAll,
      printParse = args.showParse || args.showAll,
      printAst = args.showAst || args.showAll,
      trace = args.trace || args.showAll
  )

  private val interpreter = Interpreter(
      printAnalysis = args.showAnalysis || args.showAll
  )

  private val loader = DataLoader()

  fun load(path: String) {
    loader.loadJsonToEnv(path)
  }

  fun run(expression: String) {
    val env = Environment.withBuiltins()
    loader.forEach(env::addValue)

    val parse = parser.parse(expression) { Analyzer.Tag() }
    parse.error?.let { error ->
      println("*Parse failed*")
      println("Line ${error.line}:${error.charPositionInLine} ${error.msg}")
    }
    parse.syntax?.let { syntax ->
      try {
        val result = interpreter.evaluate(syntax, env)
        println("*Result*")
        println(result)
      } catch (e: CompileError) {
        println("*Compile failed*")
        println(expression)
        println(" ".repeat(parse.tokens[e.tree.tokenRange.start].startIndex) + "^")
        println(e.message)
      }
    }
  }
}

fun main(args: Array<String>) {
  val pargs = ArgParser(args).parseInto(::Args)
  val cli = Cli(pargs)
  pargs.data?.let(cli::load)
  cli.run(pargs.expression)
}
