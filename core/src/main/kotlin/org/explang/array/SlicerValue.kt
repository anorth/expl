package org.explang.array

/**
 * Specifies a slice view of a 1-dimensional array or slice.
 *
 * The slicer doesn't care whether indices are zero- or one-based, or even positive. An absent
 * first or last stands for the first/last element of the sliced array, in the direction of
 * the step sign.
 *
 * Indices are ints (rather than longs) only due to the JVM limitation on array size.
 */
class SlicerValue(
    val first: Int? = null,
    val last: Int? = null,
    val step: Int = 1
) {
  companion object {
    val ALL = SlicerValue(null, null, 1)
  }

  init {
    check(step != 0) { "Slice step must be non-zero" }
    check(step != Int.MIN_VALUE) { "Slice step must be greater than ${Long.MIN_VALUE}" }
  }

  fun reversed() = SlicerValue(last, first, -step)

  override fun toString(): String {
    val b = StringBuilder()
    b.append(first?.toString() ?: "")
    b.append(":")
    b.append(last?.toString() ?: "")
    if (step != 1) {
      b.append(":")
      b.append(step)
    }
    return b.toString()
  }
}
