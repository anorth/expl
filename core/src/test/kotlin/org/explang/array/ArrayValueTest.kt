package org.explang.array

import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrayValueTest {
  @Test
  fun array() {
    val a = longArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val all = LongRangeValue(null, null, 1)
    val allExplicit = LongRangeValue(1, 10, 1)
    val first = LongRangeValue(1, 1, 1)
    val second = LongRangeValue(2, 2, 1)
    val three = LongRangeValue(null, 3, 1)
    val last = LongRangeValue(10, null, 1)
    val secondLast = LongRangeValue(9, 9, 1)
    val lastThree = LongRangeValue(8, null, 1)

    val skipFirst = LongRangeValue(2, null, 1)
    val skipLast = LongRangeValue(null, 9, 1)
    val skipEnds = LongRangeValue(2, 9, 1)
    val skipTwoEnds = LongRangeValue(3, 8, 1)

    val odds = LongRangeValue(1, null, 2)
    val evens = LongRangeValue(2, null, 2)
    val threeOdds = LongRangeValue(1, 5, 2)
    val threeEvens = LongRangeValue(2, 6, 2)
    val lastThreeOdds = LongRangeValue(5, null, 2)
    val lastThreeEvens = LongRangeValue(6, null, 2)
    // Descending versions are exercised in verification.


    verifySlice(listOf(1L, 2, 3, 4, 5, 6, 7, 8, 9, 10), a, all)
    verifySlice(listOf(1L, 2, 3, 4, 5, 6, 7, 8, 9, 10), a, allExplicit)
    verifySlice(listOf(1L), a, first)
    verifySlice(listOf(2L), a, second)
    verifySlice(listOf(1L, 2, 3), a, three)
    verifySlice(listOf(10L), a, last)
    verifySlice(listOf(9L), a, secondLast)
    verifySlice(listOf(8L, 9, 10), a, lastThree)

    verifySlice(listOf(2L, 3, 4, 5, 6, 7, 8, 9, 10), a, skipFirst)
    verifySlice(listOf(1L, 2, 3, 4, 5, 6, 7, 8, 9), a, skipLast)
    verifySlice(listOf(2L, 3, 4, 5, 6, 7, 8, 9), a, skipEnds)
    verifySlice(listOf(3L, 4, 5, 6, 7, 8), a, skipTwoEnds)

    verifySlice(listOf(1L, 3, 5, 7, 9), a, odds, false)
    verifySlice(listOf(10L, 8, 6, 4, 2), a, odds.reversed(), false)
    verifySlice(listOf(2L, 4, 6, 8, 10), a, evens)
    verifySlice(listOf(1L, 3, 5), a, threeOdds)
    verifySlice(listOf(2L, 4, 6), a, threeEvens)
    verifySlice(listOf(5L, 7, 9), a, lastThreeOdds, false)
    verifySlice(listOf(10L, 8, 6), a, lastThreeOdds.reversed(), false)
    verifySlice(listOf(6L, 8, 10), a, lastThreeEvens)

    val outOfBounds = listOf(
        LongRangeValue(0, 1, 1),
        LongRangeValue(1, 0, -1),
        LongRangeValue(null, 11, 1),
        LongRangeValue(11, null, -1)
    )

    outOfBounds.forEach {
      try {
        LongArrayValue.of(a, it)
        Assert.fail("Expected IOOB exception")
      } catch (e: IndexOutOfBoundsException) {
        // expected
      }
    }
  }

  @Test
  fun emptySlices() {
    val a = longArrayOf(1, 2, 3)

    val before1 = LongRangeValue(1, 0, 1)

    val after1 = LongRangeValue(1, 2, -1)
    val before2 = LongRangeValue(2, 1, 1)

    val after2 = LongRangeValue(2, 3, -1)
    val before3 = LongRangeValue(3, 2, 1)

    val after3 = LongRangeValue(3, 4, -1)

    verifySlice(listOf(), a, before1, reversible = false)
    verifySlice(listOf(), a, after1)
    verifySlice(listOf(), a, before2)
    verifySlice(listOf(), a, after2)
    verifySlice(listOf(), a, before3)
    verifySlice(listOf(), a, after3, reversible = false)
  }

  @Test
  fun emptyArray() {
    val a = longArrayOf()
    val all = LongRangeValue(null, null, 1)
    val before1  = LongRangeValue(1, 0, 1)

    verifySlice(listOf(), a, all)
    verifySlice(listOf(), a, before1, reversible = false)
  }

  @Test
  fun singleton() {
    val a = longArrayOf(1L)
    val all = LongRangeValue(null, null, 1)
    val first = LongRangeValue(null, 1, 1)
    val last = LongRangeValue(1, null, 1)

    verifySlice(listOf(1L), a, all)
    verifySlice(listOf(1L), a, first)
    verifySlice(listOf(1L), a, last)

    val before1 = LongRangeValue(1, 0, 1)
    val after1 = LongRangeValue(1, 2, -1)

    verifySlice(listOf(), a, before1, reversible = false)
    verifySlice(listOf(), a, after1, reversible = false)
  }

  @Test
  fun reslice() {
    val a = longArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val all = LongRangeValue(null, null, 1)
    val first = LongRangeValue(1, 1, 1)
    val three = LongRangeValue(1, 3, 1)
    val evens = LongRangeValue(2, null, 2)

    val base = LongArrayValue.of(a, all)
    verifySlice(a.toList(), base)
    verifySlice(a.toList(), base.slice(all))
    verifySlice(listOf(1L), base.slice(first))
    verifySlice(listOf(1L, 2, 3), base.slice(three))
    verifySlice(listOf(2L, 4, 6, 8, 10), base.slice(evens))

    verifySlice(listOf(2L), base.slice(evens).slice(first))
    verifySlice(listOf(2L, 4, 6), base.slice(evens).slice(three))
    verifySlice(listOf(4L, 8L), base.slice(evens).slice(evens))

    verifySlice(listOf(1L), base.slice(three).slice(first))
    verifySlice(listOf(1L, 2, 3), base.slice(three).slice(three))
    verifySlice(listOf(2L), base.slice(three).slice(evens))
  }
}

fun verifySlice(expected: List<Long>, data: LongArray, range: LongRangeValue,
    reversible: Boolean = true) {
  val slice = LongArrayValue.of(data, range)
  assertEquals(range.toString(), expected.size, slice.size)
  // Verify the slice as specified
  verifySlice(expected, slice)

  // Not all reversed slices are equal to the reverse of the expected value. E.g. if selecting
  // odds, the reversed slice is only equivalent to reversed expectation if the target array is
  // odd-length.
  if (reversible) {
    // Verify the reversed (descending) slice matches the reversed expectation.
    val rexpected = expected.reversed()
    val rslice = LongArrayValue.of(data, range.reversed())
    verifySlice(rexpected, rslice)
  }
}

private fun verifySlice(expected: List<Long>, array: ArrayValue<Long>) {
  assertEquals(expected, array.toList())
  for (i in expected.indices) {
    assertEquals(expected[i], array[i + 1])
  }
}
