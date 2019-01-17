package org.explang.array

import org.explang.syntax.Type
import java.util.Arrays

/**
 * A multi-dimensional array of doubles.
 *
 * When iterating, the index in the first dimension changes most rapidly (i.e. the row for a
 * matrix), meaning the values are stored in later-dimension-major order (matrix columns are
 * contiguous).
 */
class ArrayOfDouble private constructor(
    shape: Shape,
    private val data: DoubleArray
) : AbstractArray(Type.DOUBLE, shape) {
  companion object {
    /** Allocates a new array of zeros. */
    @JvmStatic
    fun zeros(vararg shape: Int): ArrayOfDouble {
      val s = Shape(*shape)
      return ArrayOfDouble(s, DoubleArray(s.length))
    }

    /** Wraps a flattened DoubleArray. */
    @JvmStatic
    fun of(shape: IntArray, data: DoubleArray): ArrayOfDouble {
      val s = Shape(*shape)
      return ArrayOfDouble(s, data)
    }

    /** Wraps a 1-d DoubleArray */
    @JvmStatic
    fun of(data: DoubleArray): ArrayOfDouble {
      return ArrayOfDouble(Shape(data.size), data)
    }
  }

  init {
    require(data.size == shape.length) {
      "Data with length ${data.size} but shape $shape"
    }
  }

  fun get(vararg index: Int): Double {
    return data[index(*index)]
  }

  fun sum() = data.sum()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ArrayOfDouble

    if (shape != other.shape) return false
    if (!data.contentEquals(other.data)) return false
    return true
  }

  override fun hashCode(): Int {
    return shape.hashCode() * 31 + data.contentHashCode()
  }

  override fun toString(): String {
    // TODO: format with shape
    return Arrays.toString(data)
  }
}
