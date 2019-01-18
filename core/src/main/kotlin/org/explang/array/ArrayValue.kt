package org.explang.array

import org.explang.syntax.ArrayType
import org.explang.syntax.FuncType
import org.explang.syntax.PrimType.BOOL
import org.explang.syntax.Type.Companion.DOUBLE
import org.explang.syntax.Type.Companion.LONG
import org.explang.syntax.Type.Companion.array
import org.explang.truffle.ExplFunction
import java.util.Arrays

sealed class ArrayValue(val type: ArrayType) {

}

class BooleanArrayValue(
    val data: BooleanArray
) : ArrayValue(array(BOOL, data.size)) {
  fun sum() = data.count { it }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!data.contentEquals((other as BooleanArrayValue).data)) return false
    return true
  }
  override fun hashCode() = data.contentHashCode()
  override fun toString() = Arrays.toString(data)
}

class LongArrayValue(
    val data: LongArray
) : ArrayValue(array(LONG, data.size)) {
  fun sum() = data.sum()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!data.contentEquals((other as LongArrayValue).data)) return false
    return true
  }
  override fun hashCode() = data.contentHashCode()
  override fun toString() = Arrays.toString(data)
}

class DoubleArrayValue(
    val data: DoubleArray
) : ArrayValue(array(DOUBLE, data.size)) {
  fun sum() = data.sum()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!data.contentEquals((other as DoubleArrayValue).data)) return false
    return true
  }
  override fun hashCode() = data.contentHashCode()
  override fun toString() = Arrays.toString(data)
}

class FunctionArrayValue(
    elementType: FuncType,
    val data: Array<ExplFunction>
) : ArrayValue(array(elementType, data.size)) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!data.contentEquals((other as FunctionArrayValue).data)) return false
    return true
  }
  override fun hashCode() = data.contentHashCode()
  override fun toString() = Arrays.toString(data)
}

class ArrayArrayValue(
    elementType: ArrayType,
    val data: Array<ArrayValue>
) : ArrayValue(array(elementType, data.size)) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!data.contentEquals((other as ArrayArrayValue).data)) return false
    return true
  }
  override fun hashCode() = data.contentHashCode()
  override fun toString() = Arrays.toString(data)
}
