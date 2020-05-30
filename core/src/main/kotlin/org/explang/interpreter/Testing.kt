package org.explang.interpreter

import org.explang.compiler.CompileError
import org.explang.compiler.Compiler
import org.explang.syntax.Parser
import org.explang.syntax.TestParser

class TestInterpreter(debug: Boolean) {
  private val parser = TestParser(debug)
  private val interpreter = Interpreter(debug)

  fun eval(expression: String, env: Environment): Any {
    val parse = parser.parse(expression)
    return eval(parse, env)
  }

  fun eval(parse: Parser.Result, env: Environment): Any {
    val compiled = compile(parse, env)
    return evaluate(compiled, env)
  }

  fun compile(parse: Parser.Result, env: Environment): Compiler.CompilationResult {
    try {
      return interpreter.compile(parse.syntax!!, env)
    } catch (e: CompileError) {
      println("*Compilation failed*")
      println(e.message)
      // TODO: figure out how to extract only the line of input in question and point to the right character.
      println(parse.input.replace("\n", " "))
      e.tree.syntax?.let {
        println(" ".repeat(parse.tokens[it.tokenRange.first].startIndex) + "^")
      }
      throw e
    }
  }

  fun evaluate(compiled: Compiler.CompilationResult, env: Environment): Any {
    try {
      return interpreter.evaluate(compiled, env).value
    } catch (e: EvalError) {
      println("*Evaluation failed*")
      println(e.message)
      throw e
    }
  }
}
