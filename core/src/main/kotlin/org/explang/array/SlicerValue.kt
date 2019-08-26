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
data class SlicerValue(
    val first: Int? = null,
    val last: Int? = null,
    // Defaults to -1 if last < first, else 1.
    val step: Int
) {
  companion object {
    fun of(first: Int? = null, last: Int? = null, step: Int? = null) =
        SlicerValue(first, last, step ?: impliedStep(first, last))
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
    if (step != impliedStep(first, last)) {
      b.append(":")
      b.append(step)
    }
    return b.toString()
  }
}

private fun impliedStep(first: Int?, last: Int?) =
    if ((last ?: Int.MAX_VALUE) < (first ?: -Int.MAX_VALUE)) -1 else 1
