package org.explang.truffle.compiler

import org.explang.syntax.ExLambda
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree
import org.explang.truffle.Type

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
      val captured: Map<ExLambda<*>, Set<Scope.Resolution>>
  ) {
    override fun toString(): String {
      return """Resolutions: ${resolutions.values}
        |Captures: ${captured.values}
      """.trimMargin()
    }
  }

  fun analyze(tree: ExTree<Tag>): Analysis {
    val scopes = Scoper.computeScopes(tree)
    return Analysis(scopes.rootScope, scopes.scopes, scopes.resolutions, scopes.captured)
  }
}

