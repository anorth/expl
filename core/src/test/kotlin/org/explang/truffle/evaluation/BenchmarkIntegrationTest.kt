package org.explang.truffle.evaluation

import org.explang.truffle.compiler.Environment
import org.explang.truffle.compiler.TestCompiler
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import kotlin.system.measureNanoTime

/**
 * Smoke test of evaluations of well-formed expressions.
 */
@Ignore("Benchmarks are slow")
class BenchmarkIntegrationTest {
  private val compiler = TestCompiler(debug = false)

  @Test
  fun fibonacci() {
    val node = compiler.compile("""let
      |f = (x: long): long -> if x <= 2 then 1 else f(x-1) + f(x-2)
      |in f(30)""".trimMargin(), Environment())
    val ret = compiler.eval(node)
    assertEquals(832040L, ret)

    // Warm-up
    println("Warming up")
    for (i in 1..15) {
      val duration = measureNanoTime {
        compiler.eval(node)
      }
      println("$duration ns")
    }

    println("Here we go for reals")
    val iterations = 20
    var totalDuration = 0L
    for (i in 1..iterations) {
      totalDuration += measureNanoTime {
        compiler.eval(node)
      }
    }

    val millis = totalDuration / 1000000
    println("$iterations iterations in $millis ms, ${millis / iterations} ms each")
  }
}

