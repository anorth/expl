package org.explang.truffle.evaluation

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameDescriptor
import org.explang.syntax.Parser
import org.explang.truffle.compiler.Analyzer
import org.explang.truffle.compiler.CompileError
import org.explang.truffle.compiler.Compiler
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Smoke test of evaluations of well-formed expressions.
 */
class EvaluationIntegrationTest {
  @Test
  fun literals() {
    assertResult(0.0, "0")
    assertResult(1.0, "1")
    assertResult(1.3, "1.3")
    assertResult(true, "true")
    assertResult(false, "false")
  }

  @Test
  fun arithmetic() {
    assertResult(3.0, "1+2")
    assertResult(0.0, "1+2-3")
    assertResult(4.0, "1+2-3+4")
    assertResult(-4.0, "1+2-(3+4)")

    assertResult(13.0, "1+2*6")
    assertResult(18.0, "(1+2)*6")
    assertResult(12.0, "4*6/2")
    assertResult(1.5, "6/(2*2)")

    assertResult(9.0, "3^2")
    assertResult(18.0, "3^2*2")
    assertResult(81.0, "3^(2*2)")
  }

  @Test
  fun comparisons() {
    assertResult(true, "false == false")
    assertResult(false, "false <> false")
    assertResult(false, "false == true")

    assertResult(false, "1 > 1")
    assertResult(true, "1 >= 1")
    assertResult(true, "2 > 1")
  }

  @Test
  fun conditional() {
    assertResult(1.0, "if true then 1 else 2")
    assertResult(2.0, "if false then 1 else 2")
    assertResult(2.0, "if 2 == 3 then 1 else 2")
  }

  @Test
  fun binding() {
    assertResult(1.0, "let a = 1 in a")
    assertResult(4.0, "let a = 2 in a*a")
    assertResult(3.0, "let a = 1, b = 2 in a+b")

    assertResult(1.0, "let a = 1 in let b = a in b")
    assertResult(2.0, "let a = 1 in let b = a+1 in b")
    assertResult(3.0, "let a = 1 in let b = a+1 in a+b")

//    assertResult(2.0, "let a = 1 in let a = a*2 in a")
  }
}

private fun assertResult(expected: Any, expression: String) {
  val result = evaluate(expression)
  assertEquals(expected, result)
}

private fun evaluate(expression: String): Any {
  val debug = false
  val parser = Parser(
      printTokens = debug,
      printParse = debug,
      printAst = debug,
      trace = debug
  )

  val compiler = Compiler(
      printAnalysis = debug
  )

  val parse = parser.parse(expression) { Analyzer.Tag() }
  try {
    val truffleEntry = compiler.compile(parse.syntax!!)
    val topFrame = Truffle.getRuntime().createVirtualFrame(arrayOfNulls(0), FrameDescriptor())
    val result = truffleEntry.executeDeclaredType(topFrame)
    if (debug) {
      println("*Result*")
      println(result)
    }
    return result
  } catch (e: CompileError) {
    println("*Compile failed*")
    println(expression)
    println(" ".repeat(parse.tokens[e.tree.tokenRange.start].startIndex) + "^")
    println(e.message)
    throw e
  }
}
