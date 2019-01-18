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
    @JvmOverloads
    fun array(element: Type, length: Int? = null) = ArrayType(element, length)
  }

  fun name() = name
  override fun toString() = name

  open fun asFunc(): FuncType = throw RuntimeTypeError("$this is not a function")
  open fun asArray(): ArrayType = throw RuntimeTypeError("$this is not an array")
}

object NoneType : Type("none")

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

/** An n-dimensional array type. */
class ArrayType(
    private val element: Type,
    private val length: Int?
) : Type(arrayTypeName(element, length)) {
  fun element() = element
  fun length() = length

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
    result = 31 * result + (length ?: 0)
    return result
  }
}

/** Computes the name for a function type. */
private fun funcTypeName(result: Type, parameters: Array<out Type>) =
    "(${parameters.joinToString(",")}->$result)"

private fun arrayTypeName(element: Type, dims: Int?): String {
  return "$element[${dims ?: ""}]"
}
