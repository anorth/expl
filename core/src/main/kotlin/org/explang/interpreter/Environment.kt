package org.explang.interpreter

import org.explang.compiler.CompilationEnvironment
import org.explang.intermediate.IBuiltin
import org.explang.syntax.ExTree
import org.explang.syntax.FuncType
import org.explang.syntax.Type

class Environment : CompilationEnvironment {
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

  private val builtins = mutableMapOf<String, Builtin>()
  private val values = mutableMapOf<String, Value>()

  fun addBuiltin(f: Builtin) {
    builtins[f.name] = f
  }

  fun addValue(name: String, type: Type, value: Any) {
    values[name] = Value(type, value)
  }

  fun getBuiltin(name: String, type: Type): Callable {
    val opts = builtins[name]!!
    return opts.forType(type as FuncType)
  }

  fun getValue(name: String) = values[name]!!

  ///// Compilation environment /////

  override fun types(): Map<String, List<Type>> {
    val m = mutableMapOf<String, MutableList<Type>>()
    for ((_, b) in builtins) {
      m.getOrPut(b.name, { mutableListOf() }).addAll(b.types())
    }
    values.mapValuesTo(m) { mutableListOf(it.value.type) }
    return m
  }

  override fun builtin(name: String, type: Type, syntax: ExTree?): IBuiltin<*> {
    val bif = getBuiltin(name, type)
    return IBuiltin(syntax, type, name, bif)
  }
}
