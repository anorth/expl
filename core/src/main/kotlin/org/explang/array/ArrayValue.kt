package org.explang.array

import org.explang.syntax.ArrayType
import org.explang.syntax.PrimType
import org.explang.syntax.PrimType.BOOL
import org.explang.syntax.Type
import org.explang.syntax.Type.Companion.DOUBLE
import org.explang.syntax.Type.Companion.LONG
import org.explang.syntax.Type.Companion.array
import java.util.Arrays

/**
 * Array value superclass.
 *
 * This does not extend AbstractList to avoid inheriting [AbstractList.equals].
 */
sealed class ArrayValue<T>(val type: ArrayType): Iterable<T> {
  abstract val size: Int
  abstract operator fun get(index: Int): T

  abstract fun filter(predicate: (T) -> Boolean): ArrayValue<T>
  abstract fun <R> fold(initial: R, operation: (R, T) -> R): R
  abstract fun reduce(acc: (T, T) -> T): T
}

@Suppress("OVERRIDE_BY_INLINE")
class BooleanArrayValue(
    val data: BooleanArray
) : ArrayValue<Boolean>(array(BOOL, data.size)) {
  override val size get() = data.size
  override fun get(index: Int) = data[index]
  override fun iterator() = data.iterator()

  override inline fun filter(predicate: (Boolean) -> Boolean) =
      // BooleanArray.filter() copies to a List and boxes values under the hood.
      // See ObjectArrayValue for a manual alternative.
      BooleanArrayValue(data.filter(predicate).toBooleanArray())

  override inline fun <R> fold(initial: R, operation: (R, Boolean) -> R): R =
      data.fold(initial, operation)

  override inline fun reduce(acc: (Boolean, Boolean) -> Boolean): Boolean =
      data.reduce(acc)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!data.contentEquals((other as BooleanArrayValue).data)) return false
    return true
  }

  override fun hashCode() = data.contentHashCode()
  override fun toString(): String = Arrays.toString(data)
}

@Suppress("OVERRIDE_BY_INLINE")
class LongArrayValue(
    val data: LongArray
) : ArrayValue<Long>(array(LONG, data.size)) {
  override val size get() = data.size
  override fun get(index: Int) = data[index]
  override fun iterator() = data.iterator()

  override inline fun filter(predicate: (Long) -> Boolean) =
      LongArrayValue(data.filter(predicate).toLongArray())

  override inline fun <R> fold(initial: R, operation: (R, Long) -> R): R =
      data.fold(initial, operation)

  override inline fun reduce(acc: (Long, Long) -> Long): Long =
      data.reduce(acc)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!data.contentEquals((other as LongArrayValue).data)) return false
    return true
  }

  override fun hashCode() = data.contentHashCode()
  override fun toString(): String = Arrays.toString(data)
}

@Suppress("OVERRIDE_BY_INLINE")
class DoubleArrayValue(
    val data: DoubleArray
) : ArrayValue<Double>(array(DOUBLE, data.size)) {
  override val size get() = data.size
  override fun get(index: Int) = data[index]
  override fun iterator() = data.iterator()

  override inline fun filter(predicate: (Double) -> Boolean) =
      DoubleArrayValue(data.filter(predicate).toDoubleArray())

  override inline fun <R> fold(initial: R, operation: (R, Double) -> R): R =
      data.fold(initial, operation)

  override inline fun reduce(acc: (Double, Double) -> Double): Double =
      data.reduce(acc)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!data.contentEquals((other as DoubleArrayValue).data)) return false
    return true
  }

  override fun hashCode() = data.contentHashCode()
  override fun toString(): String = Arrays.toString(data)
}

@Suppress("OVERRIDE_BY_INLINE")
class ObjectArrayValue<T>(
    val elementType: Type,
    val implType: Class<T>,
    val data: Array<T>
) : ArrayValue<T>(array(elementType, data.size)) {
  init {
    check(elementType !is PrimType) {"Expected non-primitive element type but got $elementType"}
  }
  override val size get() = data.size
  override fun get(index: Int) = data[index]
  override fun iterator() = data.iterator()

  @Suppress("UNCHECKED_CAST")
  override inline fun filter(predicate: (T) -> Boolean): ObjectArrayValue<T> {
    val arr = java.lang.reflect.Array.newInstance(implType, data.size) as Array<T>
    var nextIdx = 0
    for (d in data) {
      if (predicate(d)) {
        arr[nextIdx++] = d
      }
    }

    return ObjectArrayValue(elementType, implType, Arrays.copyOfRange(arr, 0, nextIdx))
  }

  override inline fun <R> fold(initial: R, operation: (R, T) -> R): R =
      data.fold(initial, operation)

  override inline fun reduce(acc: (T, T) -> T): T = data.reduce(acc)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!data.contentEquals((other as ObjectArrayValue<*>).data)) return false
    return true
  }

  override fun hashCode() = data.contentHashCode()
  override fun toString(): String = Arrays.toString(data)
}


// These are not extensions so as to be visible from Java, and not members because the generic
// type parameters need more flexibility (e.g. see reduce).

// Note: pushing some of these iterations down into the subclasses could then use
// `for (item in array)`, which Kotlin promises will compile to an index-based loop, no
// iterator object.
// NOTE: these can only be seen from Java as member functions, but not if declared as
// extension functions here or elsewhere.

inline fun <T> mapToDouble(arr: ArrayValue<T>, mapper: (T) -> Double): DoubleArrayValue {
  val mapped = DoubleArray(arr.size)
  for (i in 0 until arr.size) {
    mapped[i] = mapper(arr[i])
  }
  return DoubleArrayValue(mapped)
}
