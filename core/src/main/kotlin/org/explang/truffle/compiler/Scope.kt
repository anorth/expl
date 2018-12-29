package org.explang.truffle.compiler

import org.explang.syntax.ExLambda
import org.explang.syntax.ExLet
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree
import org.explang.truffle.Type

class NameError(msg: String, val tree: ExTree<*>) : RuntimeException(msg)

/**
 * Compile-time scopes, resolving to arguments or bindings.
 */
sealed class Scope {
  /** The syntax tree introducing the scope. */
  abstract val tree: ExTree<*>

  /** The result of resolving a name in some scope. */
  sealed class Resolution {
    abstract val symbol: ExSymbol<*> // The symbol resolved
    abstract val scope: Scope // The scope in which it was resolved, out to the immediate function
    abstract val type: Type // The resolved type (which may be NONE)
    val identifier get() = symbol.name // Frame identifier

    data class Argument(
        override val symbol: ExSymbol<*>,
        override val scope: FunctionScope,
        override val type: Type,
        val index: Int
    ) : Resolution() {
      override fun toString() = "Argument[$symbol=$index: $type]"
    }

    data class Local(
        override val symbol: ExSymbol<*>,
        override val scope: BindingScope,
        override val type: Type
    ) : Resolution() {
      override fun toString() = "Local[$symbol: $type]"
    }

    data class Closure(
        override val scope: FunctionScope, // The immediately enclosing function scope
        val capture: Resolution // The outer resolution (which might chain more than once)
    ) : Resolution() {
      override val symbol get() = capture.symbol
      override val type get() = capture.type
      override fun toString() = "Closure[$symbol=$capture]"
    }

    data class Unresolved(
        override val symbol: ExSymbol<*>,
        override val scope: RootScope,
        override val type: Type = Type.NONE
    ) : Resolution() {
      override fun toString() = "Unresolved[$symbol]"
    }
  }

  /** The parent of this scope (self for root scope) */
  abstract val parent: Scope

  /**
   * Resolves a symbol in this scope, otherwise falls back to the parent scope.
   */
  abstract fun resolve(symbol: ExSymbol<*>): Resolution
}

/**
 * Anonymous scope enclosing the entry point. Nothing can resolve here.
 */
class RootScope(override val tree: ExTree<*>) : Scope() {
  override fun resolve(symbol: ExSymbol<*>): Resolution = Resolution.Unresolved(symbol, this)

  override val parent: Scope get() = this
  override fun toString() = "RootScope"
}

/**
 * A scope for a new function, which can resolve arguments.
 */
class FunctionScope(override val tree: ExLambda<*>, override val parent: Scope) : Scope() {
  private val args: MutableMap<String, Resolution.Argument> = mutableMapOf()

  override fun resolve(symbol: ExSymbol<*>): Resolution {
    val argument = args[symbol.name]
    return if (argument != null) {
      argument
    } else {
      val capture = parent.resolve(symbol)
      capture as? Resolution.Unresolved ?: Resolution.Closure(this, capture)
    }
  }

  /**
   * Defines an argument name in this scope. Names resolve to indices in the order they
   * are defined.
   */
  fun defineArgument(type: Type, symbol: ExSymbol<*>): Resolution.Argument {
    val name = symbol.name
    if (name in args) throw NameError("Duplicate argument name $name", symbol)
    val arg = Resolution.Argument(symbol, this, type, args.size)
    args[name] = arg
    return arg
  }

  override fun toString() = "FunctionScope"
}

/**
 * A scope for bindings.
 */
class BindingScope(override val tree: ExLet<*>, override val parent: Scope) : Scope() {
  private val bindings: MutableMap<String, Resolution.Local> = mutableMapOf()

  override fun resolve(symbol: ExSymbol<*>): Resolution {
    return bindings[symbol.name] ?: parent.resolve(symbol)
  }

  /**
   * Defines a binding name in the current level. Bindings resolve to descriptor slots in the
   * enclosing function descriptor.
   *
   * TODO: resolve colliding names for different bindings in the same function (shadowing).
   */
  fun defineBinding(type: Type, symbol: ExSymbol<*>): Resolution.Local {
    val name = symbol.name
    if (name in bindings) throw NameError("Duplicate binding for $name", symbol)
    val binding = Resolution.Local(symbol, this, type)
    bindings[name] = binding
    return binding
  }

  override fun toString() = "BindingScope"
}
