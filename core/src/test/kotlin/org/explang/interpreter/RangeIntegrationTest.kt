package org.explang.interpreter

import org.explang.array.LongArrayValue
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
  fun mapToLong() {
    assertArray(listOf(2, 4, 6), "mapr(1:3, x: long -> 2*x)")
  }

  private fun assertRange(expected: LongRangeValue, expression: String) {
    val result = interpreter.eval(expression, env)
    assertEquals(expected, result)
  }

  private fun assertArray(expected: Iterable<Long>, expression: String) {
    val result = interpreter.eval(expression, env)
    assertEquals(expected, (result as LongArrayValue).toList())
  }
}

