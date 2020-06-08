package org.explang.compiler

import org.explang.intermediate.*

/**
 * Intermediate tree visitor which resolves symbols to scopes in which they are defined.
 */
class Scoper(rootScope: RootScope) : ITree.Visitor<Unit> {
  companion object {
    /** Computes scopes and symbol resolutions for a tree. */
    fun buildResolver(tree: ITree, builtins: Set<String>): LookupResolver {
      val rootScope = RootScope(tree, builtins)
      val scoped = Scoper(rootScope)
      tree.accept(scoped)
      assert(scoped.currentScope == rootScope) { "Scope visitor corrupt" }

      val captured = computeCapturedSymbols(scoped.resolutions.values)
      return LookupResolver(scoped.resolutions, captured)
    }

    @Suppress("UNCHECKED_CAST")
    private fun computeCapturedSymbols(resolutions: Iterable<Scope.Resolution>):
        MutableMap<ILambda, Set<Scope.Resolution>> {
      val captured = mutableMapOf<ILambda, MutableSet<Scope.Resolution>>()
      resolutions.forEach {
        var res = it
        while (res is Scope.Resolution.Closure) {
          captured.getOrPut(res.scope.tree) { mutableSetOf() }.add(res.capture)
          res = res.capture
        }
        if (res is Scope.Resolution.Unresolved) {
          throw CompileError(res.toString(), res.symbol)
        }
      }
      return captured as MutableMap<ILambda, Set<Scope.Resolution>>
    }
  }

  // Scopes introduced by syntactic trees (functions and bindings)
  private val scopes = mutableMapOf<ITree, Scope>()

  // Maps symbols to resolutions. The "same" symbol string may occur multiple times with the
  // same resolution if it occurs multiple times in the tree.
  private val resolutions = mutableMapOf<ISymbol, Scope.Resolution>()

  private var currentScope: Scope = rootScope

  // Tracks symbols resolved during the processing of a binding value.
  private var resolutionsInBoundValue = mutableSetOf<ISymbol>()

  override fun visitCall(call: ICall) = visitChildren(call)

  override fun visitIf(iff: IIf) = visitChildren(iff)

  override fun visitLet(let: ILet) {
    val scope = BindingScope(let, currentScope)
    scopes[let] = scope
    currentScope = scope
    // Define all the bindings before visiting the bound values, thus supporting recursive resolution.
    for (b in let.bindings) {
      val resolution = currentScope.define(b.symbol)
      resolutions[b.symbol] = resolution // Resolve the bound symbol to "itself"
    }
    // Track local resolutions made while visiting bound values, as the dependencies of each binding on others
    // from the same let.
    val bindingDeps = mutableMapOf<ISymbol, Set<ISymbol>>()
    for (b in let.bindings) {
      resolutionsInBoundValue.clear()
      visitBinding(b)
      bindingDeps[b.symbol] = resolutionsInBoundValue.toSet()
    }
    // TODO: check for circular references when visiting the bound values in this scope.
    // Consider JGraphT or Guava for graph data structures.
    // For now, just let them be and fail at type checking.

    // Referenences inside function bodies are ok, and resolve in the function scope.
    // Visit bound expression
    let.bound.accept(this)
    currentScope = currentScope.parent
  }

  override fun visitBinding(binding: IBinding) {
    binding.value.accept(this)
  }

  override fun visitLambda(lambda: ILambda) {
    val lambdaScope = FunctionScope(lambda, currentScope)

    currentScope = lambdaScope
    visitChildren(lambda) // Visit parameters and then body
    currentScope = currentScope.parent

    scopes[lambda] = lambdaScope
  }

  override fun visitParameter(parameter: IParameter) {
    val resolution = currentScope.define(parameter.symbol)
    resolutions[parameter.symbol] = resolution
  }

  override fun visitSymbol(symbol: ISymbol) {
    val res = currentScope.resolve(symbol)
    resolutions[symbol] = res
    if (res is Scope.Resolution.Local) {
      resolutionsInBoundValue.add(res.symbol)
    }
  }

  override fun visitLiteral(literal: ILiteral<*>) {}
  override fun visitArgRead(read: IArgRead) {}
  override fun visitLocalRead(read: ILocalRead) {}
  override fun visitClosureRead(read: IClosureRead) {}
  override fun visitBuiltin(builtin: IBuiltin<*>) {}
  override fun visitNil(n: INil) {}
}
