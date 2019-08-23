package org.explang.truffle.evaluation

import org.explang.array.DoubleSliceValue
import org.explang.array.SliceValue
import org.explang.truffle.compiler.Environment
import org.explang.truffle.compiler.TestCompiler
import org.junit.Assert
import org.junit.Test

class ArraysIntegrationTest {
  private val compiler = TestCompiler(debug = false)
  private val env = Environment.withBuiltins()

  @Test
  fun construction() {
    val res = zeros(3)
    assertSlice(res, "zeros(3)")
  }

  @Test
  fun mapDouble() {
    val res = DoubleSliceValue.of(doubleArrayOf(1.0, 1.0, 1.0))
    assertSlice(res, "map(zeros(3), x: double -> x + 1.0)")
  }

  @Test
  fun filterDouble() {
    assertSlice(zeros(0), "filter(zeros(3), x: double -> x > 0.0)")
    assertSlice(zeros(3), "filter(zeros(3), x: double -> x == 0.0)")
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

  private fun assertResult(expected: Any, expression: String) {
    val result = compiler.eval(expression, env)
    Assert.assertEquals(expected, result)
  }

  private fun <T> assertSlice(expected: SliceValue<T>, expression: String) {
    val result = compiler.eval(expression, env) as SliceValue<*>
    Assert.assertEquals(expected.toList(), result.toList())
  }
}

private fun zeros(n: Int) = DoubleSliceValue.of(DoubleArray(n))

