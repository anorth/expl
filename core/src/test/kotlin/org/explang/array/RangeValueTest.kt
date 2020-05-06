package org.explang.array

import org.junit.Assert.assertEquals
import org.junit.Test

class RangeValueTest {
  @Test
  fun strings() {
    assertEquals("*:*", LongRangeValue.of(null, null).toString())
    assertEquals("1:*", LongRangeValue.of(1, null).toString())
    assertEquals("*:1", LongRangeValue.of(null, 1).toString())
    assertEquals("1:4", LongRangeValue.of(1, 4, 1).toString())
    assertEquals("1:4:2", LongRangeValue.of(1, 4, 2).toString())

    assertEquals("*:*", DoubleRangeValue.of(null, null).toString())
    assertEquals("1.0:*", DoubleRangeValue.of(1.0, null).toString())
    assertEquals("*:1.0", DoubleRangeValue.of(null, 1.0).toString())
    assertEquals("1.0:4.5", DoubleRangeValue.of(1.0, 4.5, 1.0).toString())
    assertEquals("1.0:4.5:2.1", DoubleRangeValue.of(1.0, 4.5, 2.1).toString())
  }

  @Test
  fun iter() {
    assertEquals(listOf(1L), LongRangeValue.of(1, 1).toList())
    assertEquals(listOf(1L, 2), LongRangeValue.of(1, 2).toList())
    assertEquals(listOf(1L, 0), LongRangeValue.of(1, 0).toList())
    assertEquals(listOf<Long>(), LongRangeValue.of(1, 0, 1).toList())
    assertEquals(LongRange(1, 10).toList(), LongRangeValue.of(1, 10, 1).toList())
    assertEquals(listOf(1L, 3, 5, 7, 9), LongRangeValue.of(1, 10, 2).toList())
    assertEquals(listOf(2L, 4, 6, 8, 10), LongRangeValue.of(2, 10, 2).toList())
    assertEquals(listOf(10L, 8, 6, 4, 2), LongRangeValue.of(10, 1, -2).toList())
  }
}
