package org.explang.interpreter

import org.explang.compiler.CompileError
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import kotlin.math.sqrt

class InterpreterIntegrationTest {
  private val interpreter = TestInterpreter(debug = false)
  private var env = Environment()

  @Before
  fun setup() {
    env = Environment.withBuiltins()
  }

  @Test
  fun foo() {
    assertResult(2L, "1 + 1")
    assertResult(2L, "1 * 2")
  }

  @Test
  fun literals() {
    assertResult(0L, "0")
    assertResult(1L, "1")
    assertResult(1.3, "1.3")
    assertResult(true, "true")
    assertResult(false, "false")
  }

  @Test
  fun arithmetic() {
    assertResult(3L, "1+2")
    assertResult(0L, "1+2-3")
    assertResult(4L, "1+2-3+4")
    assertResult(-4L, "1+2-(3+4)")

    assertResult(13L, "1+2*6")
    assertResult(18L, "(1+2)*6")
    assertResult(12L, "4*6/2")

    assertResult(1L, "6/(2*2)") // truncated
    assertResult(1.5, "6.0/(2.0*2.0)")

    assertResult(9L, "3^2")
    assertResult(18L, "3^2*2")
    assertResult(81L, "3^(2*2)")
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
  fun conjunctions() {
    assertResult(true, "true and true")
    assertResult(false, "true and false")
    assertResult(true, "true or false")
    assertResult(false, "false or false")

    assertResult(false, "false xor false")
    assertResult(false, "true xor true")
    assertResult(true, "true xor false")
  }

  @Test
  fun unary() {
    assertResult(-1L, "-1")
    assertResult(1L, "--1")
    assertResult(true, "not false")
    assertResult(false, "not true")
    assertResult(true, "not not true")
    assertResult(true, "not true == false")
  }

  @Test
  fun conditional() {
    assertResult(1L, "if true then 1 else 2")
    assertResult(2L, "if false then 1 else 2")
    assertResult(2L, "if 2 == 3 then 1 else 2")
  }

  @Test
  fun binding() {
    assertResult(1L, "let a = 1 in a")
    assertResult(4L, "let a = 2 in a*a")
    assertResult(3L, "let a = 1, b = 2 in a+b")

    assertResult(1L, "let a = 1 in let b = a in b")
    assertResult(2L, "let a = 1 in let b = a+1 in b")
    assertResult(3L, "let a = 1 in let b = a+1 in a+b")

    // Shadowing
    assertResult(2L, "let a = 1 in let a = 2 in a")

    // Mutual reference
    assertResult(1L, "let a = 1, b = a in b")
    // For now, declarations must be made in dependency-sorted order.
//    assertResult(1L, "let a = b, b = 1 in a")

    // Dependency cycles
    assertCompileError("let a = b, b = a in a")
    assertCompileError("let a = b+1, b = a+1 in a")
    assertCompileError("let a = a in a")
    assertCompileError("let a = 1 in let a = a in a")
  }

  @Test
  fun call() {
    assertResult(true, "(() -> true)()")
    assertResult(true, "(x: bool -> x)(true)")
    assertResult(false, "(x: bool -> x)(false)")
    assertResult(3L, "((a: long, b: long) -> a+b)(1, 2)")
  }

  @Test
  fun closure() {
    assertResult(1L, "(let a = 1 in () -> a)()")
    assertResult(1L, "let a = 1 in (() -> a)()")
    assertResult(3L, "let a = 1 in let b = 2 in ((c: long) -> a+c)(b)")
    assertResult(1L, "let a = 1 in (() -> (() -> a)()) ()")
    assertResult(3L, "let f(x: long) = x + 1 in let s(x: long) = f(x) in s(2)")
    assertResult(3L, "let f(x: long) = x + 1 in let s = f in s(2)")

    assertResult(1L, "(x: long -> () -> x)(1)()")
  }

  @Test
  fun recursion() {
    // Simple recursion
    assertResult(0L, "let f = (x: long): long -> if x == 0 then 0 else f(x-1) in f(1)")

    // Mutual recursion
    assertResult(true, """let 
      |even = (x: long): bool -> if x == 0 then true else not odd(x-1),
      |odd = (x: long): bool -> not even(x)
      |in even(2)""".trimMargin())

    // Factorial
    assertResult(120L, "let f = (x: long): long -> if x <= 1 then 1 else x * f(x-1) in f(5)")
    // Fibonacci
    assertResult(5L, "let f = (x: long): long -> if x <= 2 then 1 else f(x-1) + f(x-2) in f(5)")
  }

  @Test
  fun higherOrderFunctions() {
    assertResult(1L, """let
      |f = (inner: (->long)) -> inner,
      |g = () -> 1,
      |in f(g)()""".trimMargin())
    assertResult(2L, """let
      |apply = (f: (long->long), x: long) -> f(x),
      |inc = (x: long) -> x + 1,
      |in apply(inc, 1)""".trimMargin())
    assertResult(6L, """let
      |adder = (x: long): (long->long) -> (y: long) -> x + y,
      |in adder(1)(5)""".trimMargin())
  }

  @Test
  fun builtins() {
    assertResult(sqrt(2.0), "sqrt(2.0)")
    assertResult(sqrt(2.0), "let s(x: double) = sqrt(x) in s(2.0)")
    // TODO: enable this test when type unification spans more than immediate context.
//    assertResult(sqrt(2.0), "let s = sqrt in s(2.0)")
  }

  private fun assertResult(expected: Any, expression: String) {
    val result = interpreter.eval(expression, env)
    Assert.assertEquals(expected, result)
  }

  private fun assertCompileError(expression: String) {
    try {
      val result = interpreter.eval(expression, env)
      Assert.fail("Expected compilation error, got result $result")
    } catch (expected: CompileError) {
    }
  }
}
