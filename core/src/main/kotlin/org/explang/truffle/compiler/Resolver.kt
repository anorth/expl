package org.explang.truffle.compiler

import org.explang.syntax.ExLambda
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree

/** Resolves symbol occurences to bindings or arguments. */
interface Resolver {
  @Deprecated("Use resolve() method instead")
  val rootScope: RootScope

  /** Scopes keyed by the syntactic tree which introduces them */
  @Deprecated("Use resolve() method instead")
  fun scope(tree: ExTree<*>): Scope

  /** Resolves a symbol. */
  fun resolve(symbol: ExSymbol<*>): Scope.Resolution

  /** Returns resolutions of symbols captured by a lambda. */
  fun captured(lambda: ExLambda<*>): Collection<Scope.Resolution>
}

/** A resolver which looks up precomputed maps */
class LookupResolver(
    override val rootScope: RootScope,
    private val scopes: Map<ExTree<*>, Scope>,
    private val resolutions: Map<ExSymbol<*>, Scope.Resolution>,
    private val captured: Map<ExLambda<*>, Set<Scope.Resolution>>
) : Resolver {
  override fun scope(tree: ExTree<*>): Scope {
    return scopes[tree]!!
  }

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
