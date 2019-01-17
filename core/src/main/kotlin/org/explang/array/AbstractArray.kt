package org.explang.array

import java.util.Arrays

/**
 * Superclass for a multi-dimensional array.
 *
 * When iterating, the index in the last dimension changes most rapidly (i.e. the column for a
 * matrix), meaning the values are stored in first-dimension-major order (matrix rows are
 * contiguous, row-major). This matches how they appear when written as text, reading
 * across rows as the innermost dimension.
 *
 * Indexes are zero-based.
 */
open class AbstractArray(
    val shape: Shape
) {
  // The distance between consecutive elements in each dimension.
  private val stride = IntArray(shape.dimensions)

  init {
    shape.forEachIndexed { index, i -> assert(i > 0) { "Invalid dimension $index: $i" } }

    var thisStride = 1
    for (i in shape.dimensions-1 downTo 0) {
      stride[i] = thisStride
      thisStride *= shape[i]
    }
  }

  fun size(dim: Int) = shape[dim]

  fun stride(dim: Int) = stride[dim]

  fun index(vararg indices: Int): Int {
    check(indices.size == shape.dimensions) {
      "Invalid index for array with ${shape.dimensions} dims: ${Arrays.toString(indices)}"
    }
    var idx = 0
    for (i in 0..stride.lastIndex) {
      check(indices[i] < shape[i]) {
        "Index ${indices[i]} out of bounds for dimension $i of ${shape[i]}"
      }
      idx += indices[i] * stride[i]
    }
    return idx
  }
}

