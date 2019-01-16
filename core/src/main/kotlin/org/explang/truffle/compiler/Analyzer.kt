package org.explang.truffle.compiler

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
      // Symbol resolver
      val resolver: Resolver,
      // Inferred types for bound symbol resolutions.
      val symbolTypes: Map<Scope.Resolution, Type>
  ) {
    override fun toString(): String {
      return """Resolver: $resolver
        |Types: $symbolTypes
      """.trimMargin()
    }
  }

  fun analyze(tree: ExTree<Tag>, builtins: Map<String, Type>): Analysis {
    val resolver = Scoper.buildResolver(tree, builtins.keys)
    val types = TypeChecker.computeTypes(tree, resolver, builtins) // Updates node tags in-place
    return Analysis(resolver, types.resolutions)
  }
}
