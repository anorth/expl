package org.explang.syntax

/**
 * Describes a guest language type.
 *
 * Primitive types compare equal on identity. Function types compare equal on argument and
 * result types.
 */
sealed class Type constructor(
    val name: String
) {
  companion object {
    @JvmField
    val NONE = NoneType
    @JvmField
    val BOOL = PrimType.BOOL
    @JvmField
    val DOUBLE = PrimType.DOUBLE
    @JvmField
    val LONG = PrimType.LONG

    @JvmStatic
    fun function(result: Type, vararg arguments: Type) = FuncType(result, arguments)
    @JvmStatic
    fun range(element: Type) = RangeType(element)
    @JvmStatic
    fun array(element: Type) = ArrayType(element)
  }

  fun name() = name
  override fun toString() = name

  open fun isFunc(): Boolean = false
  open fun isArray(): Boolean = false

  open fun asRange(): RangeType = throw RuntimeTypeError("$this is not a range")
  open fun asArray(): ArrayType = throw RuntimeTypeError("$this is not a range")
  open fun asFunc(): FuncType = throw RuntimeTypeError("$this is not a function")

  /** Whether a value of this type is acceptable where an [other] is required */
  open fun satisfies(other: Type) = this == other
}

object NoneType : Type("none") {
  override fun satisfies(other: Type) = false
}

/** A primitive type */
sealed class PrimType(name: String) : Type(name) {
  object BOOL : PrimType("boolean")
  object LONG : PrimType("long")
  object DOUBLE : PrimType("double") // Consider naming these "float" and "int" if 32-bit versions are excluded

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    return name == (other as PrimType).name
  }

  override fun hashCode(): Int {
    return name.hashCode()
  }
}

/** A function type */
class FuncType(
    private val result: Type,
    private val parameters: Array<out Type>
) : Type(funcTypeName(result, parameters)) {
  fun result() = result
  fun parameters() = parameters

  override fun isFunc() = true
  override fun asFunc() = this

  override fun satisfies(other: Type) = other is FuncType &&
      result.satisfies(other.result) &&
      other.parameters.zip(parameters).all { (o, t) -> o.satisfies(t) }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as FuncType
    if (result != other.result) return false
    if (!parameters.contentEquals(other.parameters)) return false
    return true
  }

  override fun hashCode(): Int {
    var result1 = result.hashCode()
    result1 = 31 * result1 + parameters.contentHashCode()
    return result1
  }
}

/** A range of numbers type. */
class RangeType(
    private val element: Type
) : Type("range($element)") {
  override fun asRange() = this

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RangeType
    if (element != other.element) return false
    return true
  }

  override fun hashCode(): Int {
    return element.hashCode()
  }
}

/** A 1-dimensional array. */
class ArrayType(
    private val element: Type
) : Type(arrayTypeName(element, null)) {
  fun element() = element

  override fun isArray() = true
  override fun asArray() = this

  override fun satisfies(other: Type): Boolean {
    return other is ArrayType && element == other.element
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ArrayType
    if (element != other.element) return false
    return true
  }

  override fun hashCode(): Int {
    return element.hashCode() * 31
  }
}

/** Computes the name for a function type. */
private fun funcTypeName(result: Type, parameters: Array<out Type>) =
    "(${parameters.joinToString(",")}->$result)"

private fun arrayTypeName(element: Type, dims: Int?): String {
  return "$element[${dims ?: ""}]"
}
