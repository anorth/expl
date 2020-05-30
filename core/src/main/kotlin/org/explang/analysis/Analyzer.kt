package org.explang.analysis

import org.explang.syntax.Type

/**
 * Analyses a syntax tree for binding and type information.
 */
class Analyzer {
  class Tag(
      // Resolved concrete type.
      var type: Type = Type.NONE,
      // Candidate concrete types.
      val typeCandidates: MutableList<Type> = mutableListOf()
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

  fun analyze(tree: ITree, environment: Map<String, List<Type>>): Analysis {
    val resolver = Scoper.buildResolver(tree, environment.keys)
    val types = TypeChecker.computeTypes(tree, resolver, environment) // Updates node tags in-place
    return Analysis(resolver, types.resolutions)
  }
}
