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
    val NONE = None()
    @JvmField
    val BOOL = PrimType("boolean")
    @JvmField
    val DOUBLE = PrimType("double")

    @JvmStatic
    fun function(result: Type, vararg arguments: Type) = FuncType(result, arguments)
    @JvmStatic
    fun array(element: Type, dims: Int) = ArrayType(element, dims)
  }

  fun name() = name
  override fun toString() = name

  open fun asFunc(): FuncType = throw RuntimeTypeError("$this is not a function")
  open fun asArray(): ArrayType = throw RuntimeTypeError("$this is not an array")
}

class None : Type("none")

/** A primitive type */
class PrimType(name: String) : Type(name) {
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

class ArrayType(
    private val element: Type,
    private val dims: Int
    // Add optional dimension sizes here so that double[3] is a concrete type?
) : Type(arrayTypeName(element, dims)) {
  fun element() = element
  fun dims() = dims

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ArrayType

    if (element != other.element) return false
    if (dims != other.dims) return false
    return true
  }

  override fun hashCode(): Int {
    var result = element.hashCode()
    result = 31 * result + dims
    return result
  }
}

/** Computes the name for a function type. */
private fun funcTypeName(result: Type, parameters: Array<out Type>) =
    "(${parameters.joinToString(",")}->$result)"

private fun arrayTypeName(element: Type, dims: Int) : String {
  return if (dims == 0) {
    "[]"
  } else {
    element.name + "[]".repeat(dims)
  }
}
