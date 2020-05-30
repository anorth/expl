package org.explang.compiler

import org.explang.intermediate.IBuiltin
import org.explang.intermediate.ITree
import org.explang.syntax.ExTree
import org.explang.syntax.Type

interface CompilationEnvironment {
  /** Returns the names and types of all environment items. */
  fun types(): Map<String, List<Type>>

  /** Builds a builtin node. */
  fun builtin(name: String, type: Type, syntax: ExTree?): IBuiltin<*>
}

/**
 * Analyses a syntax tree for binding and type information.
 */
class Compiler {
  class CompilationResult(
      // Transformed intermediate tree
      val tree: ITree,
      // Symbol resolver
      val resolver: Resolver,
      // Inferred types for bound symbol resolutions.
      private val symbolTypes: Map<Scope.Resolution, Type>
  ) {
    override fun toString(): String {
      return """Resolver: $resolver
        |Types: $symbolTypes
      """.trimMargin()
    }
  }

  fun compile(tree: ITree, env: CompilationEnvironment): CompilationResult {
    val resolver = Scoper.buildResolver(tree, env.types().keys)
    val types = TypeChecker.computeTypes(tree, resolver, env.types()) // Updates type tags in-place
    val opt = Optimizer(resolver, env).optimize(tree) // Builds new tree
    return CompilationResult(opt, resolver, types.resolutions)
  }
}
