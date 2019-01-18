package org.explang.array

import org.explang.syntax.ArrayType
import org.explang.syntax.FuncType
import org.explang.syntax.PrimType.BOOL
import org.explang.syntax.Type.Companion.DOUBLE
import org.explang.syntax.Type.Companion.LONG
import org.explang.syntax.Type.Companion.array
import org.explang.truffle.ExplFunction
import java.util.Arrays

sealed class ArrayValue<out T>(val type: ArrayType) : AbstractList<T>() {
  abstract fun filter(predicate: (T) -> Boolean): ArrayValue<T>
}

@Suppress("OVERRIDE_BY_INLINE")
class BooleanArrayValue(
    val data: BooleanArray
) : ArrayValue<Boolean>(array(BOOL, data.size)) {
  override val size get() = data.size
  override fun get(index: Int) = data[index]
  override fun iterator() = data.iterator()

  override inline fun filter(predicate: (Boolean) -> Boolean) =
      BooleanArrayValue(data.filter(predicate).toBooleanArray())

  fun sum() = data.count { it }

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

  fun sum() = data.sum()

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

  fun sum() = data.sum()

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
class FunctionArrayValue(
    val elementType: FuncType,
    val data: Array<ExplFunction>
) : ArrayValue<ExplFunction>(array(elementType, data.size)) {
  override val size get() = data.size
  override fun get(index: Int) = data[index]
  override fun iterator() = data.iterator()

  override inline fun filter(predicate: (ExplFunction) -> Boolean) =
      FunctionArrayValue(elementType, data.filter(predicate).toTypedArray())

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!data.contentEquals((other as FunctionArrayValue).data)) return false
    return true
  }

  override fun hashCode() = data.contentHashCode()
  override fun toString(): String = Arrays.toString(data)
}

@Suppress("OVERRIDE_BY_INLINE")
class ArrayArrayValue(
    val elementType: ArrayType,
    val data: Array<ArrayValue<*>>
) : ArrayValue<ArrayValue<*>>(array(elementType, data.size)) {
  override val size get() = data.size
  override fun get(index: Int) = data[index]
  override fun iterator() = data.iterator()

  override inline fun filter(predicate: (ArrayValue<*>) -> Boolean) =
      ArrayArrayValue(elementType, data.filter(predicate).toTypedArray())

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!data.contentEquals((other as ArrayArrayValue).data)) return false
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
  for (i in arr.indices) {
    mapped[i] = mapper(arr[i])
  }
  return DoubleArrayValue(mapped)
}

inline fun <T, R> fold(arr: ArrayValue<T>, initial: R, operation: (R, T) -> R): R =
    arr.fold(initial, operation)

inline fun <S, T : S> reduce(arr: ArrayValue<T>, acc: (S, T) -> S) = arr.reduce(acc)
