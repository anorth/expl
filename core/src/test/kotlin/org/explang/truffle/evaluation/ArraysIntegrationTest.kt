package org.explang.truffle.evaluation

import org.explang.array.DoubleArrayValue
import org.explang.truffle.compiler.Environment
import org.explang.truffle.compiler.TestCompiler
import org.explang.truffle.nodes.builtin.ArrayBuiltins
import org.junit.Assert
import org.junit.Test

class ArraysIntegrationTest {
  private val compiler = TestCompiler(debug = false)
  private val env = Environment()

  init {
    env.addBuiltin(ArrayBuiltins.zeros())
    env.addBuiltin(ArrayBuiltins.map())
    env.addBuiltin(ArrayBuiltins.filter())
    env.addBuiltin(ArrayBuiltins.fold())
    env.addBuiltin(ArrayBuiltins.reduce())
  }

  @Test
  fun construction() {
    val res = zeros(3)
    assertResult(res, "zeros(3)")
  }

  @Test
  fun mapDouble() {
    val res = DoubleArrayValue(doubleArrayOf(1.0, 1.0, 1.0))
    assertResult(res, "map(zeros(3), x: double -> x + 1.0)")
  }

  @Test
  fun filterDouble() {
    assertResult(zeros(0), "filter(zeros(3), x: double -> x > 0.0)")
    assertResult(zeros(3), "filter(zeros(3), x: double -> x == 0.0)")
  }

  @Test
  fun foldDouble() {
    assertResult(1.0, "fold(zeros(3), 1.0, (x: double, y: double) -> x + y)")
  }

  @Test
  fun reduceDouble() {
    assertResult(0.0, "reduce(zeros(3), (x: double, y: double) -> x + y)")
  }

  private fun assertResult(expected: Any, expression: String) {
    val result = compiler.eval(expression, env)
    Assert.assertEquals(expected, result)
  }
}

private fun zeros(n: Int) = DoubleArrayValue(DoubleArray(n))

