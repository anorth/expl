package org.explang.array

import org.explang.syntax.Type

/** An array with no elements (hence no element type). */
class EmptyArray(
    shape: Shape
) : AbstractArray(Type.NONE, shape) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as EmptyArray

    if (shape != other.shape) return false
    return true
  }

  override fun hashCode(): Int {
    return shape.hashCode()
  }

  override fun toString(): String {
    return "[".repeat(shape.dimensions) + "]".repeat(shape.dimensions)
  }
}
