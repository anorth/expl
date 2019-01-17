package org.explang.array

import org.explang.syntax.Type
import org.junit.Assert.assertEquals
import org.junit.Test

class AbstractArrayTest {
  val EMPTY = AbstractArray(Type.NONE, Shape())
  val SINGLETON = AbstractArray(Type.NONE, Shape(1))
  val ROW = AbstractArray(Type.NONE, Shape(10))
  val ROW2 = AbstractArray(Type.NONE, Shape(1, 10))
  val COLUMN = AbstractArray(Type.NONE, Shape(10, 1)) // 10 rows of 1 column
  val BOX = AbstractArray(Type.NONE, Shape(3, 4)) // 3 rows of 4 columns
  val CUBE = AbstractArray(Type.NONE, Shape(7, 3, 4)) // 7 plane of 3 rows of 4 columns

  @Test
  fun shape() {
    assertEquals(0, EMPTY.shape.dimensions)

    assertEquals(1, SINGLETON.shape.dimensions)
    assertEquals(1, SINGLETON.size(0))
    assertEquals(1, SINGLETON.stride(0))

    assertEquals(1, ROW.shape.dimensions)
    assertEquals(10, ROW.size(0))
    assertEquals(1, ROW.stride(0))

    assertEquals(2, ROW2.shape.dimensions)
    assertEquals(1, ROW2.size(0))
    assertEquals(10, ROW2.stride(0))
    assertEquals(10, ROW2.size(1))
    assertEquals(1, ROW2.stride(1))

    assertEquals(2, COLUMN.shape.dimensions)
    assertEquals(10, COLUMN.size(0))
    assertEquals(1, COLUMN.stride(0))
    assertEquals(1, COLUMN.size(1))
    assertEquals(1, COLUMN.stride(1))

    assertEquals(2, BOX.shape.dimensions)
    assertEquals(3, BOX.size(0))
    assertEquals(4, BOX.stride(0))
    assertEquals(4, BOX.size(1))
    assertEquals(1, BOX.stride(1))

    assertEquals(3, CUBE.shape.dimensions)
    assertEquals(7, CUBE.size(0))
    assertEquals(12, CUBE.stride(0))
    assertEquals(3, CUBE.size(1))
    assertEquals(4, CUBE.stride(1))
    assertEquals(4, CUBE.size(2))
    assertEquals(1, CUBE.stride(2))
  }

  @Test
  fun index() {
    assertEquals(0, ROW.index(0))
    assertEquals(1, ROW.index(1))
    assertEquals(9, ROW.index(9))

    // Same as ROW
    assertEquals(0, ROW2.index(0, 0))
    assertEquals(1, ROW2.index(0, 1))
    assertEquals(9, ROW2.index(0, 9))

    assertEquals(0, COLUMN.index(0, 0))
    assertEquals(1, COLUMN.index(1, 0))
    assertEquals(9, COLUMN.index(9, 0))

    assertEquals(0, BOX.index(0, 0))
    assertEquals(1, BOX.index(0, 1))
    assertEquals(2, BOX.index(0, 2))
    assertEquals(3, BOX.index(0, 3))
    assertEquals(4, BOX.index(1, 0))
  }
}
