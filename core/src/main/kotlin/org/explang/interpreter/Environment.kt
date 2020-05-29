package org.explang.interpreter

import org.explang.syntax.Type

class Environment {
  // Builtin functions are keyed by name and type.
  data class BuiltinKey(val name: String, val type: Type)

  class Value(val type: Type, val value: Any)

  companion object {
    /** Returns an environment with operator intrinsics initialized (but no other built-ins). */
    fun withOperators(): Environment {
      val env = Environment()
      OPERATORS.forEach(env::addBuiltin)
      return env
    }
    /** Returns an environment with built-ins initialized. */
    fun withBuiltins(): Environment {
      val env = withOperators()
      BUILTINS.forEach(env::addBuiltin)
      return env
    }
  }

  private val builtins = mutableMapOf<BuiltinKey, BuiltinFunction>()
  private val values = mutableMapOf<String, Value>()

  fun addBuiltin(f: BuiltinFunction) {
    builtins[BuiltinKey(f.name, f.type)] = f
  }

  fun addValue(name: String, type: Type, value: Any) {
    values[name] = Value(type, value)
  }

  fun getBuiltin(name: String, type: Type) = builtins[BuiltinKey(name, type)]!!

  fun getValue(name: String) = values[name]!!

  /** Returns the names and types of all environment items. */
  fun types(): Map<String, List<Type>> {
    val m = mutableMapOf<String, MutableList<Type>>()
    for ((name, b) in builtins) {
      m.getOrPut(b.name, { mutableListOf() }).add(b.type)
    }
    values.mapValuesTo(m) { mutableListOf(it.value.type) }
    return m
  }
}
