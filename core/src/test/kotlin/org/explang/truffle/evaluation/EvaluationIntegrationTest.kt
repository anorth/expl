package org.explang.truffle.evaluation

import org.explang.syntax.Type.BOOL
import org.explang.syntax.Type.function
import org.explang.truffle.ExplFunction
import org.explang.truffle.compiler.TestCompiler
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Smoke test of evaluations of well-formed expressions.
 */
class EvaluationIntegrationTest {
  private val compiler = TestCompiler(debug = false)

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

    // Not yet implemented
//    assertResult(2.0, "let a = 1 in let a = a*2 in a")
  }

  @Test
  fun lambda() {
    assertEquals(function(BOOL), (compiler.eval("() -> true") as ExplFunction).type())
    assertEquals(function(BOOL, BOOL), (compiler.eval("(x: bool) -> true") as ExplFunction).type())
    assertEquals(function(BOOL, BOOL), (compiler.eval("x: bool -> x") as ExplFunction).type())
  }

  @Test
  fun call() {
    assertResult(true, "(() -> true)()")
    assertResult(true, "(x: bool -> x)(true)")
    assertResult(false, "(x: bool -> x)(false)")
    assertResult(3.0, "((a: double, b: double) -> a+b)(1, 2)")
  }

  @Test
  fun closure() {
    assertResult(1.0, "(let a = 1 in () -> a)()")
    assertResult(1.0, "let a = 1 in (() -> a)()")
    assertResult(3.0, "let a = 1 in let b = 2 in ((c: double) -> a+c)(b)")
    assertResult(1.0, "let a = 1 in (() -> (() -> a)()) ()")

    assertResult(1.0, "(x: double -> () -> x)(1)()")
  }

  @Test
  fun recursion() {
    // Factorial
    assertResult(120.0, "let f = (x: double): double -> if x <= 1 then 1 else x * f(x-1) in f(5)")
    // Fibonacci
    assertResult(5.0, "let f = (x: double): double -> if x <= 2 then 1 else f(x-1) + f(x-2) in f(5)")
  }

  @Test
  fun higherOrderFunctions() {
    assertResult(1.0, """let
      |f = (inner: (->double)) -> inner,
      |g = () -> 1,
      |in f(g)()""".trimMargin())
    assertResult(2.0, """let
      |apply = (f: (double->double), x: double) -> f(x),
      |inc = (x: double) -> x + 1,
      |in apply(inc, 1)""".trimMargin())
    assertResult(6.0, """let
      |adder = (x: double): (double->double) -> (y: double) -> x + y,
      |in adder(1)(5)""".trimMargin())
  }

  private fun assertResult(expected: Any, expression: String) {
    val result = compiler.eval(expression)
    assertEquals(expected, result)
  }
}

