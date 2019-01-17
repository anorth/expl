package org.explang.truffle.compiler

import org.explang.syntax.Type
import org.explang.truffle.nodes.builtin.BuiltInNode
import org.explang.truffle.nodes.builtin.Builtins

class Environment {
  class Value(val type: Type, val value: Any)

  companion object {
    /** Returns an environment with built-ins initialized. */
    fun withBuiltins(): Environment {
      val env = Environment()
      Builtins.ALL.forEach(env::addBuiltin)
      return env
    }
  }

  private val builtins = mutableMapOf<String, BuiltInNode>()
  private val values = mutableMapOf<String, Value>()

  fun addBuiltin(node: BuiltInNode) {
    builtins[node.name] = node
  }

  fun addValue(name: String, type: Type, value: Any) {
    values[name] = Value(type, value)
  }

  fun getBuiltin(name: String) = builtins[name]

  fun getValue(name: String) = values[name]

  /** Returns the names and types of all environment items. */
  fun types(): Map<String, Type> {
    val m = mutableMapOf<String, Type>()
    builtins.mapValuesTo(m) { it.value.funcType }
    values.mapValuesTo(m) { it.value.type }
    return m
  }
}
