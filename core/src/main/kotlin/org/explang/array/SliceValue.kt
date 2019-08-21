package org.explang.array

import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * An indexable view of an underlying array.
 *
 * At present, slices always retain a reference to the underlying array provided at construction.
 * Multiple slices of the same array thus share the same storage. However, a small slice may pin a
 * very large array in memory.
 *
 * Indices are zero-based and positive, though the implementation leaves room for arbitrary indexes
 * in the future (c.f. Julia, FORTRAN).
 */
class SliceValue<T>(
    private val array: ArrayValue<T>,
    // Indices into [array] giving a traditional half-open range, but that range might
    // be descending with a negative step.
    private val start: Int,
    private val end: Int,
    private val step: Int
) : Iterable<T> {
  companion object {
    /**
     * Resolves a slicer against an array to produce a slice.
     *
     * Indices in the slicer must be within bounds of the array, except that the inter-index
     * location before the first element may be referenced by 1:0, and after the last by
     * n:n+1:-1.
     *
     * An empty slice at the inter-index location before element n is specified by n:n-1
     * (or n-1:n:-1).
     */
    fun <T> of(array: ArrayValue<T>, slicer: SlicerValue): SliceValue<T> {
      // Resolve nulls to 1-based, inclusive indices and check bounds.
      val step = slicer.step
      val up = step.sign > 0
      val size = array.size
      val first = when {
        slicer.first != null -> slicer.first.also {
          // Index 1 is always a valid first, even for (empty slices of) empty arrays.
          if (it < 1 || it > maxOf(1, size))
            throw IndexOutOfBoundsException("Index $it out of range for array of $size")
        }
        up -> 1
        else -> size
      }
      val last = when {
        slicer.last != null -> slicer.last.also {
          // Last may be out of range for empty slices, if |step| is 1 and the last is `first-step`.
          if ((step.absoluteValue == 1 && it != first - step) && (it < 1 || it > size))
            throw IndexOutOfBoundsException("Index $it out of range for array of $size")

        }
        up -> size
        else -> 1
      }

      // Transform one-based, closed range indices to zero-based indices for JVM arrays.
      return SliceValue(array, first - 1, last - 1 + step.sign, step)
    }
  }

  val size = maxOf(0, ((end - start) / step) + if ((end - start) % step != 0) 1 else 0)

  operator fun get(index: Int): T {
    if (index < 0 || index >= size)
        throw IndexOutOfBoundsException("Index $index too large for slice of $size")
    return array[start + (index * step)]
  }

  override fun iterator(): Iterator<T> = SliceIterator(array, start, end, step)

  override fun toString() = toList().toString()
}

private class SliceIterator<T>(val array: ArrayValue<T>, start: Int, val end: Int, val step: Int) :
    Iterator<T> {
  private var nextIdx = start
  private var hasNext = if (step > 0) start < end else start > end

  override fun hasNext() = hasNext

  override fun next(): T {
    if (!hasNext) throw NoSuchElementException()
    return array[nextIdx].also {
      nextIdx += step
      // The two comparisons could be reduced to one if the exact final element were computed
      // at construction.
      hasNext = if (step > 0) nextIdx < end else nextIdx > end
    }
  }
}
