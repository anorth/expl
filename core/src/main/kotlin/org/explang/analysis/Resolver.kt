package org.explang.analysis

import org.explang.intermediate.ILambda
import org.explang.intermediate.ISymbol

/** Resolves symbol occurences to bindings or arguments. */
interface Resolver {
  /** Resolves a symbol. */
  fun resolve(symbol: ISymbol): Scope.Resolution

  /** Returns resolutions of symbols captured by a lambda. */
  fun captured(lambda: ILambda): Collection<Scope.Resolution>

  /** Returns any failed resolutions. */
  fun unresolved(): Collection<Scope.Resolution.Unresolved>
}

/** A resolver which looks up precomputed maps */
class LookupResolver(
    private val resolutions: Map<ISymbol, Scope.Resolution>,
    private val captured: Map<ILambda, Set<Scope.Resolution>>
) : Resolver {
  override fun resolve(symbol: ISymbol): Scope.Resolution {
    return resolutions[symbol]
        ?: // This shouldn't happen because an unresolved symbol is explicitly represented
        throw RuntimeException("Unresolved symbol $symbol")
  }

  override fun captured(lambda: ILambda): Collection<Scope.Resolution> {
    return captured[lambda] ?: listOf()
  }

  override fun unresolved(): Collection<Scope.Resolution.Unresolved> {
    return resolutions.values.mapNotNull { it as? Scope.Resolution.Unresolved }
  }

  override fun toString(): String {
    return """Resolutions: ${resolutions.values}
        |Captures: ${captured.values}
      """.trimMargin()
  }
}
