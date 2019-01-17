package org.explang.array

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
) : AbstractArray(shape) {
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
  }

  init {
    require(data.size == shape.length) {
      "Data with length ${data.size} but shape $shape"
    }
  }

  fun get(vararg index: Int): Double {
    return data[index(*index)]
  }

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
}
