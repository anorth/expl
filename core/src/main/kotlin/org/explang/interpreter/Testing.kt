package org.explang.interpreter

import org.explang.analysis.Analyzer
import org.explang.analysis.CompileError
import org.explang.syntax.Parser
import org.explang.syntax.TestParser

class TestInterpreter(debug: Boolean) {
  private val parser = TestParser(debug)
  private val interpreter = Interpreter(debug)

  fun eval(expression: String, env: Environment): Any {
    val parse = parser.parse(expression)
    return evaluate(parse, env)
  }

  fun evaluate(parse: Parser.Result<Analyzer.Tag>, env: Environment): Any {
    try {
      return interpreter.evaluate(parse.syntax!!, env).value
    } catch (e: CompileError) {
      println("*Evaluation failed*")
      println(e.message)
      println(parse.input)
      println(" ".repeat(parse.tokens[e.tree.tokenRange.start].startIndex) + "^")
      throw e
    }
  }
}
