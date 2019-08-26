package org.explang.truffle.evaluation

import org.explang.array.SlicerValue
import org.explang.truffle.compiler.Environment
import org.explang.truffle.compiler.TestCompiler
import org.junit.Assert.assertEquals
import org.junit.Test

class RangeIntegrationTest {
  private val compiler = TestCompiler(debug = false)
  private val env = Environment.withBuiltins()

  @Test
  fun builtin() {
    assertResult(SlicerValue(1, 10, 1), "range(1, 10, 1)")
    assertResult(SlicerValue(1, 0, -1), "range(1, 0, -1)")
  }

  @Test
  fun construction() {
    assertResult(SlicerValue(2, 5, 1), "2:5:1")
    assertResult(SlicerValue(2, 5, 2), "2:5:2")

    assertResult(SlicerValue(2, 5, 1), "2:5")
    assertResult(SlicerValue(5, 2, -1), "5:2")
    assertResult(SlicerValue(5, null, 2), "5:*:2")
    assertResult(SlicerValue(null, 5, 2), "*:5:2")

    assertResult(SlicerValue(5, null, 1), "5:*")
    assertResult(SlicerValue(null, 5, 1), "*:5")
    assertResult(SlicerValue(null, null, 2), "*:*:2")
    assertResult(SlicerValue(null, null, -2), "*:*:-2")
    assertResult(SlicerValue.ALL, "*:*")
  }

  private fun assertResult(expected: Any, expression: String) {
    val result = compiler.eval(expression, env)
    assertEquals(expected, result)
  }
}

