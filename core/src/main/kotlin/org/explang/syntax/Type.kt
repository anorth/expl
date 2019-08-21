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
    fun array(element: Type, length: Int) = ArrayType(element, length)
    @JvmStatic
    fun slice(element: Type) = SliceType(element)
  }

  fun name() = name
  override fun toString() = name

  open fun asFunc(): FuncType = throw RuntimeTypeError("$this is not a function")
  open fun asArray(): ArrayType = throw RuntimeTypeError("$this is not an array")

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

/** A 1-dimensional fixed-length array type. */
class ArrayType(
    private val element: Type,
    private val length: Int
) : Type(arrayTypeName(element, length)) {
  fun element() = element
  fun length() = length

  override fun satisfies(other: Type): Boolean {
    return other is ArrayType &&
        element == other.element &&
        length == other.length
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ArrayType
    if (element != other.element) return false
    if (length != other.length) return false
    return true
  }

  override fun hashCode(): Int {
    var result = element.hashCode()
    result = 31 * result + length
    return result
  }
}

/** A 1-dimensional slice of an array */
class SliceType(
    private val element: Type
) : Type(arrayTypeName(element, null)) {
  fun element() = element

  override fun satisfies(other: Type): Boolean {
    return other is SliceType && element == other.element
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SliceType
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
