package org.explang.array

import org.explang.syntax.Type
import kotlin.math.absoluteValue
import kotlin.math.sign

/**
 * An indexable view of an underlying array.
 *
 * At present, slices always retain a reference to the underlying array provided at construction.
 * Multiple slices of the same array thus share the same storage. However, a small slice may pin a
 * very large array in memory.
 *
 * Indices are 1-based and positive, though the implementation leaves room for arbitrary indexes
 * in the future (c.f. Julia, FORTRAN).
 */
sealed class SliceValue<T>(
    // Zero-based indices into an underlying array giving a traditional half-open range,
    // but that range might be descending with a negative step.
    private val start: Int,
    private val end: Int,
    private val step: Int
) : Iterable<T> {
  val size = maxOf(0, ((end - start) / step) + if ((end - start) % step != 0) 1 else 0)

  // Gets a value by 1-based index.
  operator fun get(index: Int): T {
    if (index < 1 || index > size)
      throw IndexOutOfBoundsException("Index $index out of range for slice of $size")
    return getUnmapped(start + (index - 1) * step) // Convert to zero-based
  }

  override fun iterator(): Iterator<T> = SliceIterator()
  override fun toString() = toList().toString()

  /** Takes a slice of this slice. */
  fun slice(slicer: SlicerValue): SliceValue<T> {
    return makeSlice(size, slicer) { newStart, newEnd, newStep ->
      reslice(start + newStart * step, start + newEnd * step, step * newStep)
    }
  }

  abstract fun filter(predicate: (T) -> Boolean): SliceValue<T>

  protected abstract fun getUnmapped(index: Int): T
  protected abstract fun reslice(start: Int, end: Int, step: Int): SliceValue<T>

  // Needs to be an inner class to access getUnmapped. An alternative would be to pass that
  // method by reference.
  private inner class SliceIterator : Iterator<T> {
    private var nextIdx = start
    private var hasNext = if (step > 0) start < end else start > end

    override fun hasNext() = hasNext

    override fun next(): T {
      if (!hasNext) throw NoSuchElementException()
      return getUnmapped(nextIdx).also {
        nextIdx += step
        // The two comparisons could be reduced to one if the exact final element were computed
        // at construction.
        hasNext = if (step > 0) nextIdx < end else nextIdx > end
      }
    }
  }
}

class BooleanSliceValue(private val array: BooleanArray, start: Int, end: Int, step: Int) :
    SliceValue<Boolean>(start, end, step) {
  companion object {

    fun of(vararg vs: Boolean) = BooleanSliceValue(vs, 0, vs.size, 1)
    fun of(array: BooleanArray, slicer: SlicerValue) =
        makeSlice(array.size, slicer) { start: Int, end: Int, step: Int ->
          BooleanSliceValue(array, start, end, step)
        }
  }

  override fun getUnmapped(index: Int) = array[index]

  override fun reslice(start: Int, end: Int, step: Int) = BooleanSliceValue(array, start, end, step)

  @Suppress("OVERRIDE_BY_INLINE")
  override inline fun filter(predicate: (Boolean) -> Boolean): SliceValue<Boolean> {
    val arr = BooleanArray(size)
    var nextIdx = 0
    for (d in this) {
      if (predicate(d)) {
        arr[nextIdx++] = d
      }
    }
    return of(*arr.copyOfRange(0, nextIdx))
  }
}

class LongSliceValue(private val array: LongArray, start: Int, end: Int, step: Int) :
    SliceValue<Long>(start, end, step) {
  companion object {
    fun of(vararg vs: Long) = LongSliceValue(vs, 0, vs.size, 1)
    fun of(array: LongArray, slicer: SlicerValue) =
        makeSlice(array.size, slicer) { start: Int, end: Int, step: Int ->
          LongSliceValue(array, start, end, step)
        }
  }

  override fun getUnmapped(index: Int) = array[index]

  override fun reslice(start: Int, end: Int, step: Int) = LongSliceValue(array, start, end, step)

  @Suppress("OVERRIDE_BY_INLINE")
  override inline fun filter(predicate: (Long) -> Boolean): SliceValue<Long> {
    val arr = LongArray(size)
    var nextIdx = 0
    for (d in this) {
      if (predicate(d)) {
        arr[nextIdx++] = d
      }
    }
    return of(*arr.copyOfRange(0, nextIdx))
  }
}

class DoubleSliceValue(private val array: DoubleArray, start: Int, end: Int, step: Int) :
    SliceValue<Double>(start, end, step) {
  companion object {
    fun of(vararg vs: Double) = DoubleSliceValue(vs, 0, vs.size, 1)
    fun of(array: DoubleArray, slicer: SlicerValue) =
        makeSlice(array.size, slicer) { start: Int, end: Int, step: Int ->
          DoubleSliceValue(array, start, end, step)
        }
  }

  override fun getUnmapped(index: Int) = array[index]

  override fun reslice(start: Int, end: Int, step: Int) = DoubleSliceValue(array, start, end, step)

  @Suppress("OVERRIDE_BY_INLINE")
  override inline fun filter(predicate: (Double) -> Boolean): SliceValue<Double> {
    val arr = DoubleArray(size)
    var nextIdx = 0
    for (d in this) {
      if (predicate(d)) {
        arr[nextIdx++] = d
      }
    }
    return of(*arr.copyOfRange(0, nextIdx))
  }
}

class ObjectSliceValue<T>(
    private val array: Array<T>,
    val implType: Class<T>,
    val elementType: Type,
    start: Int, end: Int, step: Int) : SliceValue<T>(start, end, step) {
  companion object {
    fun <T> of(implType: Class<T>, elementType: Type, array: Array<T>) =
        ObjectSliceValue(array, implType, elementType, 0, array.size, 1)

    fun <T> of(implType: Class<T>, elementType: Type, array: Array<T>, slicer: SlicerValue) =
        makeSlice(array.size, slicer) { start: Int, end: Int, step: Int ->
          ObjectSliceValue(array, implType, elementType, start, end, step)
        }
  }

  override fun getUnmapped(index: Int) = array[index]

  override fun reslice(start: Int, end: Int, step: Int) =
      ObjectSliceValue(array, implType, elementType, start, end, step)

  @Suppress("OVERRIDE_BY_INLINE")
  override inline fun filter(predicate: (T) -> Boolean): SliceValue<T> {
    @Suppress("UNCHECKED_CAST")
    val arr = java.lang.reflect.Array.newInstance(implType, size) as Array<T>
    var nextIdx = 0
    for (d in this) {
      if (predicate(d)) {
        arr[nextIdx++] = d
      }
    }
    return of(implType, elementType, arr.copyOfRange(0, nextIdx))
  }
}

private inline fun <T> makeSlice(size: Int, slicer: SlicerValue,
    construct: (Int, Int, Int) -> SliceValue<T>): SliceValue<T> {
  // Resolve nulls to 1-based, inclusive indices and check bounds.
  val step = slicer.step
  val up = step.sign > 0
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

  // Resolve to zero-based indices into the underlying.
  return construct(first - 1, last - 1 + step.sign, step)
}

// Not Kotlin extensions so as to be visible from Java.
inline fun <T, R> fold(slice: SliceValue<T>, initial: R, operation: (R, T) -> R): R =
    slice.fold(initial, operation)

inline fun <T> reduce(slice: SliceValue<T>, acc: (T, T) -> T): T =
    slice.reduce(acc)

inline fun <T> mapToDouble(slice: SliceValue<T>, mapper: (T) -> Double): SliceValue<Double> {
  val mapped = DoubleArray(slice.size)
  for (i in 0 until slice.size) {
    mapped[i] = mapper(slice[i + 1])
  }
  return DoubleSliceValue.of(*mapped)
}
