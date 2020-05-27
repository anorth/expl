package org.explang.interpreter

import org.explang.syntax.TestParser
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import kotlin.system.measureNanoTime

@Ignore("Benchmarks are slow")
class BenchmarkIntegrationTest {
  private val parser = TestParser(debug = false)
  private val interpreter = TestInterpreter(debug = false)
  private val env = Environment()

  @Test
  fun fibonacci() {
    val tree = parser.parse("""let
      |f = (x: long): long -> if x <= 2 then 1 else f(x-1) + f(x-2)
      |in f(30)""".trimMargin())
    val ret = interpreter.evaluate(tree, env)
    assertEquals(832040L, ret)

    // Warm-up
    println("Warming up")
    for (i in 1..15) {
      val duration = measureNanoTime {
        interpreter.evaluate(tree, env)
      }
      println("$duration ns")
    }

    println("Here we go for reals")
    val iterations = 20
    var totalDuration = 0L
    for (i in 1..iterations) {
      totalDuration += measureNanoTime {
        interpreter.evaluate(tree, env)
      }
    }

    val millis = totalDuration / 1000000
    println("$iterations iterations in $millis ms, ${millis / iterations} ms each")
  }
}
