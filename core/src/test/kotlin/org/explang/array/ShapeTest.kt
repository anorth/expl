package org.explang.array

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ShapeTest {
  @Test
  fun eq() {
    assertEquals(Shape(), Shape())
    assertEquals(Shape(1), Shape(1))
    assertEquals(Shape(1, 2), Shape(1, 2))

    assertNotEquals(Shape(), Shape(1))
    assertNotEquals(Shape(1), Shape(1, 1))
    assertNotEquals(Shape(1, 2), Shape(2, 1))
  }
}
