package org.explang.interpreter

import org.explang.array.ArrayValue
import org.explang.array.LongRangeValue
import org.junit.Assert.assertEquals
import org.junit.Test

class RangeIntegrationTest {
  private val interpreter = TestInterpreter(debug = false)
  private var env = Environment.withBuiltins()

  @Test
  fun builtin() {
    assertRange(LongRangeValue(1, 10, 1), "range(1, 10, 1)")
    assertRange(LongRangeValue(1, 0, -1), "range(1, 0, -1)")
  }

  @Test
  fun construction() {
    assertRange(LongRangeValue(2, 5, 1), "2:5:1")
    assertRange(LongRangeValue(2, 5, 2), "2:5:2")

    assertRange(LongRangeValue(2, 5, 1), "2:5")
    assertRange(LongRangeValue(5, 2, -1), "5:2")
    assertRange(LongRangeValue(5, null, 2), "5:*:2")
    assertRange(LongRangeValue(null, 5, 2), "*:5:2")

    assertRange(LongRangeValue(5, null, 1), "5:*")
    assertRange(LongRangeValue(null, 5, 1), "*:5")
    assertRange(LongRangeValue(null, null, 2), "*:*:2")
    assertRange(LongRangeValue(null, null, -2), "*:*:-2")
    assertRange(LongRangeValue(null, null, 1), "*:*")
  }

  @Test
  fun mapLongs() {
    assertArray(listOf(2L, 4L, 6L), "map(1:3, x: long -> 2*x)")
    assertArray(listOf(0.0, 0.0, 0.0), "map(1:3, x: long -> 0.0)")
    assertArray(listOf(true, true, true), "map(1:3, x: long -> true)")
  }

  @Test
  fun mapDoubles() {
    assertArray(listOf(2.0, 4.0, 6.0), "map(1.0:3.0, x: double -> 2.0*x)")
    assertArray(listOf(0L, 0L, 0L), "map(1.0:3.0, x: double -> 0)")
    assertArray(listOf(true, true, true), "map(1.0:3.0, x: double -> true)")
  }

  private fun assertRange(expected: LongRangeValue, expression: String) {
    val result = interpreter.eval(expression, env)
    assertEquals(expected, result)
  }

  private fun <T> assertArray(expected: Iterable<T>, expression: String) {
    val result = interpreter.eval(expression, env)
    assertEquals(expected.toList(), (result as ArrayValue<*>).toList())
  }
}

