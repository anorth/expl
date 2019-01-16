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
    val BOOL = Primitive("boolean")
    @JvmField
    val DOUBLE = Primitive("double")

    @JvmStatic
    fun function(result: Type, vararg arguments: Type): Func {
      return Func(result, arguments)
    }
  }

  fun name() = name
  override fun toString() = name

  open fun asFunc(): Func = throw RuntimeTypeError("$this is not a function")
}

class None : Type("none")

/** A primitive type */
class Primitive(name: String) : Type(name) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    return name == (other as Primitive).name
  }

  override fun hashCode(): Int {
    return name.hashCode()
  }
}

/** A function type */
class Func(
    private val result: Type,
    private val parameters: Array<out Type>
) : Type(typeName(result, parameters)) {
  fun result() = result
  fun parameters() = parameters

  override fun asFunc() = this

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Func

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

/** Computes the name for a function type. */
private fun typeName(result: Type, parameters: Array<out Type>) =
    "(${parameters.joinToString(",")}->$result)"
