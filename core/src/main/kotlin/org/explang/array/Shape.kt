package org.explang.array

import java.util.Arrays

/** An immutable tuple of integers representing n-dimensional array shape. */
class Shape(vararg dims: Int) : Iterable<Int> {
  init {
    dims.forEach {
      require(it > 0) { "Invalid array dimension $it in ${Arrays.toString(dims)}" }
    }
  }

  private val dims: IntArray = dims.copyOf()

  /** The length of a flattened representation of this shape, the product of the dimensions. */
  val length = if (dims.isEmpty()) 0 else dims.fold(1) { acc, i -> acc * i }

  val dimensions get() = dims.size

  operator fun get(i: Int) = dims[i]

  override fun iterator() = dims.iterator()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    if (!dims.contentEquals((other as Shape).dims)) return false
    return true
  }

  override fun hashCode(): Int {
    return dims.contentHashCode()
  }

  override fun toString(): String {
    return Arrays.toString(dims)
  }
}
