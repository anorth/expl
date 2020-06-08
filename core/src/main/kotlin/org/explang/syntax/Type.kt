package org.explang.syntax

import org.explang.common.mapArr

/**
 * Describes a type.
 *
 * Primitive types compare equal on identity. Function types compare equal on argument and
 * result types.
 */
sealed class Type constructor(
    val name: String,
    /** Whether the type has no parameters. */
    val concrete: Boolean
) {
  companion object {
    val NONE = NoneType
    val NIL = NilType
    val BOOL = PrimType.BOOL
    val DOUBLE = PrimType.DOUBLE
    val LONG = PrimType.LONG

    fun function(result: Type, vararg arguments: Type) = FuncType(result, arguments)
    fun range(element: Type) = RangeType(element)
    fun array(element: Type) = ArrayType(element)
  }

  /** Replaces a parameter in this type. */
  abstract fun replace(bindings: Map<TypeParameter, Type>): Type

  /** Unifies a type with this one, writing any new bindings into a provided map. */
  abstract fun unify(other: Type, bindings: MutableMap<TypeParameter, Type> = mutableMapOf()): Boolean

  override fun toString() = name
}

// TODO: rename to undefined, to avoid confusion with nil/null
object NoneType : Type("none", concrete = false) {
  override fun replace(bindings: Map<TypeParameter, Type>) = this
  override fun unify(other: Type, bindings: MutableMap<TypeParameter, Type>) = other == this
}

object NilType : Type("nil", concrete = true) {
  override fun replace(bindings: Map<TypeParameter, Type>) = this
  override fun unify(other: Type, bindings: MutableMap<TypeParameter, Type>) = other == this
}

/** A primitive type */
sealed class PrimType(name: String) : Type(name, concrete = true) {
  object BOOL : PrimType("boolean")
  object LONG : PrimType("long")
  object DOUBLE : PrimType("double") // Consider naming these "float" and "int" if 32-bit versions are excluded

  companion object {
    fun all() = listOf(BOOL, LONG, DOUBLE)
  }

  override fun replace(bindings: Map<TypeParameter, Type>) = this

  override fun unify(other: Type, bindings: MutableMap<TypeParameter, Type>) =
      when (other) {
        this -> true
        is TypeParameter -> bindings.bind(other, this)
        else -> false
      }

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
    private val arguments: Array<out Type>
) : Type(funcTypeName(result, arguments), concrete = result.concrete && arguments.all(Type::concrete)) {
  fun result() = result
  fun arguments() = arguments

  override fun replace(bindings: Map<TypeParameter, Type>) =
      FuncType(result.replace(bindings), arguments.mapArr(Type::class) { it.replace(bindings) })

  override fun unify(other: Type, bindings: MutableMap<TypeParameter, Type>): Boolean {
    return when (other) {
      this -> true
      is TypeParameter -> bindings.bind(other, this)
      is FuncType -> result.unify(other.result, bindings) &&
          arguments.size == other.arguments.size &&
          arguments.zip(other.arguments) { a, b -> a.unify(b, bindings) }.all { it }
      else -> false
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as FuncType
    if (result != other.result) return false
    if (!arguments.contentEquals(other.arguments)) return false
    return true
  }

  override fun hashCode(): Int {
    var result1 = result.hashCode()
    result1 = 31 * result1 + arguments.contentHashCode()
    return result1
  }
}

/** A range of numbers type. */
class RangeType(
    private val element: Type
) : Type("range($element)", concrete = true) {
  override fun replace(bindings: Map<TypeParameter, Type>) = RangeType(element.replace(bindings))

  override fun unify(other: Type, bindings: MutableMap<TypeParameter, Type>) =
      when (other) {
        this -> true
        is TypeParameter -> bindings.bind(other, this)
        is RangeType -> element.unify(other.element, bindings)
        else -> false
      }

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
) : Type(arrayTypeName(element), concrete = element.concrete) {
  override fun replace(bindings: Map<TypeParameter, Type>) = ArrayType(element.replace(bindings))

  override fun unify(other: Type, bindings: MutableMap<TypeParameter, Type>) =
      when (other) {
        this -> true
        is TypeParameter -> bindings.bind(other, this)
        is ArrayType -> element.unify(other.element, bindings)
        else -> false
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

class TypeParameter(
    name: String
) : Type(name, concrete = false) {
  override fun replace(bindings: Map<TypeParameter, Type>) = bindings.getOrDefault(this, this)

  override fun unify(other: Type, bindings: MutableMap<TypeParameter, Type>) =
      when (other) {
        this -> true
        is TypeParameter -> bindings.bind(other, this) // TODO and other way too?
        else -> bindings.bind(this, other)
      }

  // Instance-based equality/hashcode
}

/** Computes the name for a function type. */
private fun funcTypeName(result: Type, parameters: Array<out Type>) =
    "(${parameters.joinToString(",")}→$result)"

private fun arrayTypeName(element: Type): String {
  return "$element[]"
}

private fun <K, V> MutableMap<K, V>.bind(key: K, value: V) : Boolean {
  val found = this[key]
  if (found == null) this[key] = value
  else if (found != value) return false
  return true
}