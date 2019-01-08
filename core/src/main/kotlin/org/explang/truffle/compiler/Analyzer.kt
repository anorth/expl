package org.explang.truffle.compiler

import org.explang.syntax.ExLambda
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree
import org.explang.syntax.Type

/**
 * Analyses a syntax tree for binding and type information.
 */
class Analyzer {
  class Tag(
      var type: Type = Type.NONE
  )

  class Analysis(
      val rootScope: RootScope,
      // Scopes introduced by syntax elements.
      val scopes: Map<ExTree<*>, Scope>,
      // Maps symbols to resolutions.
      val resolutions: Map<ExSymbol<*>, Scope.Resolution>,
      // Maps function definitions to a collection of symbols which resolved outside the
      // function's scope, so must be captured in a closure at function definition.
      val captured: Map<ExLambda<*>, Set<Scope.Resolution>>,
      // Inferred types for bound symbol resolutions.
      val bindings: Map<Scope.Resolution, Type>
  ) {
    override fun toString(): String {
      return """Resolutions: ${resolutions.values}
        |Captures: ${captured.values}
      """.trimMargin()
    }
  }

  fun analyze(tree: ExTree<Tag>): Analysis {
    val scopes = Scoper.computeScopes(tree)
    val resolver = LookupResolver(scopes.resolutions)
    val types = TypeChecker.computeTypes(tree, resolver) // Updates node tags in-place
    return Analysis(scopes.rootScope, scopes.scopes, scopes.resolutions, scopes.captured,
        types.bindings)
  }
}

private class LookupResolver(private val resolutions: Map<ExSymbol<*>, Scope.Resolution>) :
    TypeChecker.SymbolResolver {
  override fun resolve(symbol: ExSymbol<*>): Scope.Resolution {
    return resolutions[symbol]
        ?: // This shouldn't happen because an unresolved symbol is explicitly represented
        throw RuntimeException("Unresolved symbol $symbol")
  }
}
