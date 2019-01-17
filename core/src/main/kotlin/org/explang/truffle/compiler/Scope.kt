package org.explang.truffle.compiler

import org.explang.syntax.ExLambda
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree

class NameError(msg: String, val tree: ExTree<*>) : RuntimeException(msg)

/**
 * Compile-time scopes, resolving symbols to to arguments or bindings.
 *
 * Multiple symbol instances with the same name may share a resolution.
 * Once defined, the resolution for a symbol is immutable and constant.
 */
sealed class Scope {
  /** The syntax tree introducing the scope. */
  abstract val tree: ExTree<*>

  /**
   * The result of resolving a name in some scope.
   *
   * A resolution is immutable, although the referenced [Scope] is not.
   */
  sealed class Resolution {
    abstract val symbol: ExSymbol<*> // The symbol resolved
    abstract val scope: Scope // The scope in which it was resolved, out to the immediate function
    val identifier get() = symbol.name // Frame identifier

    data class Argument(
        override val symbol: ExSymbol<*>,
        override val scope: FunctionScope,
        val index: Int
    ) : Resolution() {
      override fun toString() = "Argument[$symbol=$index]"
    }

    data class Local(
        override val symbol: ExSymbol<*>,
        override val scope: BindingScope
    ) : Resolution() {
      override fun toString() = "Local[$symbol]"
    }

    data class Closure(
        override val scope: FunctionScope, // The immediately enclosing function scope
        val capture: Resolution // The outer resolution (which might chain more than once)
    ) : Resolution() {
      override val symbol get() = capture.symbol
      override fun toString() = "Closure[$symbol=$capture]"
    }

    data class Environment(
        override val symbol: ExSymbol<*>,
        override val scope: RootScope
    ) : Resolution() {
      override fun toString(): String {
        return "Environment[$symbol]"
      }
    }

    data class Unresolved(
        override val symbol: ExSymbol<*>,
        override val scope: RootScope
    ) : Resolution() {
      override fun toString() = "Unresolved[$symbol]"
    }
  }

  /** The parent of this scope (self for root scope) */
  abstract val parent: Scope

  /** Defines a new binding in this scope. */
  abstract fun define(symbol: ExSymbol<*>): Scope.Resolution

  /**
   * Resolves a symbol in this scope, otherwise falls back to the parent scope.
   */
  abstract fun resolve(symbol: ExSymbol<*>): Scope.Resolution
}

/**
 * Anonymous scope enclosing the entry point. Environment symbols (e.g. built-ins) resolve here.
 */
class RootScope(
    override val tree: ExTree<*>,
    private val builtins: Set<String>
) : Scope() {
  override fun define(symbol: ExSymbol<*>) =
      throw RuntimeException("Can't define binding in root scope")

  override fun resolve(symbol: ExSymbol<*>): Resolution {
    return if (symbol.name in builtins)
      Resolution.Environment(symbol, this)
    else
      Resolution.Unresolved(symbol, this)
  }

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
  override fun define(symbol: ExSymbol<*>): Resolution.Argument {
    val name = symbol.name
    if (name in args) throw NameError("Duplicate argument name $name", symbol)
    val arg = Resolution.Argument(symbol, this, args.size)
    args[name] = arg
    return arg
  }

  override fun toString() = "FunctionScope"
}

/**
 * A scope for bindings.
 */
class BindingScope(override val tree: ExTree<*>, override val parent: Scope) : Scope() {
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
  override fun define(symbol: ExSymbol<*>): Resolution.Local {
    val name = symbol.name
    if (name in bindings) throw NameError("Duplicate binding for $name", symbol)
    val binding = Resolution.Local(symbol, this)
    bindings[name] = binding
    return binding
  }

  override fun toString() = "BindingScope"
}
