package org.explang.truffle.compiler

import org.explang.syntax.ExLambda
import org.explang.syntax.ExSymbol

/** Resolves symbol occurences to bindings or arguments. */
interface Resolver {
  /** Resolves a symbol. */
  fun resolve(symbol: ExSymbol<*>): Scope.Resolution

  /** Returns resolutions of symbols captured by a lambda. */
  fun captured(lambda: ExLambda<*>): Collection<Scope.Resolution>
}

/** A resolver which looks up precomputed maps */
class LookupResolver(
    private val resolutions: Map<ExSymbol<*>, Scope.Resolution>,
    private val captured: Map<ExLambda<*>, Set<Scope.Resolution>>
) : Resolver {
  override fun resolve(symbol: ExSymbol<*>): Scope.Resolution {
    return resolutions[symbol]
        ?: // This shouldn't happen because an unresolved symbol is explicitly represented
        throw RuntimeException("Unresolved symbol $symbol")
  }

  override fun captured(lambda: ExLambda<*>): Collection<Scope.Resolution> {
    return captured[lambda] ?: listOf()
  }

  override fun toString(): String {
    return """Resolutions: ${resolutions.values}
        |Captures: ${captured.values}
      """.trimMargin()
  }
}
