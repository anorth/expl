package org.explang.interpreter

import org.explang.array.ArrayValue
import org.explang.array.DoubleArrayValue
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Test
import kotlin.reflect.KClass

class ArrayIntegrationTest {
  private val interpreter = TestInterpreter(debug = false)
  private var env = Environment.withBuiltins()

  @Test
  fun construction() {
    val res = zeros(3)
    assertArray(res, "zeros(3)")
  }

  @Test
  fun mapDouble() {
    val res = DoubleArrayValue.of(1.0, 1.0, 1.0)
    assertArray(res, "map(zeros(3), x: double -> x + 1.0)")
    assertArray(zeros(3), "map(zeros(3), sign)")
  }

  @Test
  fun filterDouble() {
    assertArray(zeros(0), "filter(zeros(3), x: double -> x > 0.0)")
    assertArray(zeros(3), "filter(zeros(3), x: double -> x == 0.0)")
    assertArray(zeros(0), "filter(zeros(3), positive)")
  }

  @Test
  fun foldDouble() {
    assertResult(1.0, "fold(zeros(3), 1.0, (x: double, y: double) -> x + y)")
  }

  @Test
  fun reduceDouble() {
    assertResult(0.0, "reduce(zeros(3), (x: double, y: double) -> x + y)")
  }

  @Test
  fun sumDouble() {
    assertResult(0.0, """let
      |sum = (a: double[]) -> fold(a, 0.0, (x: double, y: double) -> x + y)
      |in sum(zeros(3))""".trimMargin())
  }

  @Test
  fun index() {
    assertResult(0.0, "zeros(3)[1]")
    assertResult(0.0, "zeros(3)[2]")
    assertResult(0.0, "zeros(3)[3]")
    assertException(IndexOutOfBoundsException::class, "zeros(0)[1]")
    assertException(IndexOutOfBoundsException::class, "zeros(3)[0]")
    assertException(IndexOutOfBoundsException::class, "zeros(3)[4]")
  }

  @Test
  fun slice() {
    assertArray(zeros(5), "zeros(5)[*:*]")
    assertArray(zeros(5), "zeros(5)[1:5]")
    assertArray(zeros(1), "zeros(5)[1:1]")
    assertArray(zeros(2), "zeros(5)[1:2]")
    assertArray(zeros(2), "zeros(5)[4:5]")
    assertArray(zeros(3), "zeros(5)[3:*]")
    assertArray(zeros(3), "zeros(5)[*:3]")
    assertArray(zeros(1), "zeros(5)[*:3][3:*]")
  }

  private fun assertResult(expected: Any, expression: String) {
    val result = interpreter.eval(expression, env)
    Assert.assertEquals(expected, result)
  }

  private fun <T> assertArray(expected: ArrayValue<T>, expression: String) {
    val result = interpreter.eval(expression, env) as ArrayValue<*>
    Assert.assertEquals(expected.toList(), result.toList())
  }

  private fun assertException(expected: KClass<out Throwable>, expression: String) {
    try {
      val result = interpreter.eval(expression, env)
      Assert.fail("Unexpected success: $result, expected $expected")
    } catch (e: Throwable) {
      Assert.assertThat(e, Matchers.instanceOf(expected.java))
    }
  }
}

private fun zeros(n: Int) = DoubleArrayValue.of(*DoubleArray(n))

