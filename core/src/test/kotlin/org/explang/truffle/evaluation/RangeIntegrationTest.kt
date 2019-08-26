package org.explang.truffle.evaluation

import org.explang.array.LongRangeValue
import org.explang.truffle.compiler.Environment
import org.explang.truffle.compiler.TestCompiler
import org.junit.Assert.assertEquals
import org.junit.Test

class RangeIntegrationTest {
  private val compiler = TestCompiler(debug = false)
  private val env = Environment.withBuiltins()

  @Test
  fun builtin() {
    assertResult(LongRangeValue(1, 10, 1), "range(1, 10, 1)")
    assertResult(LongRangeValue(1, 0, -1), "range(1, 0, -1)")
  }

  @Test
  fun construction() {
    assertResult(LongRangeValue(2, 5, 1), "2:5:1")
    assertResult(LongRangeValue(2, 5, 2), "2:5:2")

    assertResult(LongRangeValue(2, 5, 1), "2:5")
    assertResult(LongRangeValue(5, 2, -1), "5:2")
    assertResult(LongRangeValue(5, null, 2), "5:*:2")
    assertResult(LongRangeValue(null, 5, 2), "*:5:2")

    assertResult(LongRangeValue(5, null, 1), "5:*")
    assertResult(LongRangeValue(null, 5, 1), "*:5")
    assertResult(LongRangeValue(null, null, 2), "*:*:2")
    assertResult(LongRangeValue(null, null, -2), "*:*:-2")
    assertResult(LongRangeValue(null, null, 1), "*:*")
  }

  private fun assertResult(expected: Any, expression: String) {
    val result = compiler.eval(expression, env)
    assertEquals(expected, result)
  }
}

