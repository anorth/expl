package org.explang.analysis

import org.explang.syntax.*

/**
 * AST visitor which resolves symbols to scopes in which they are defined.
 *
 * @param <T> type of the AST tag (opaque)
 */
class Scoper<T>(rootScope: RootScope) : ExTree.Visitor<T, Unit> {
  companion object {
    /** Computes scopes and symbol resolutions for a tree. */
    fun <T> buildResolver(tree: ExTree<T>, builtins: Set<String>): Resolver {
      val rootScope = RootScope(tree, builtins)
      val scoped = Scoper<T>(rootScope)
      tree.accept(scoped)
      assert(scoped.currentScope == rootScope) { "Scope visitor corrupt" }

      val captured = computeCapturedSymbols(scoped.resolutions.values)
      return LookupResolver(scoped.resolutions, captured)
    }

    private fun computeCapturedSymbols(resolutions: Iterable<Scope.Resolution>):
        Map<ExLambda<*>, Set<Scope.Resolution>> {
      val captured = mutableMapOf<ExLambda<*>, MutableSet<Scope.Resolution>>()
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
      return captured
    }
  }

  // Scopes introduced by syntactic trees (functions and bindings)
  private val scopes = mutableMapOf<ExTree<*>, Scope>()
  // Maps symbols to resolutions. The "same" symbol string may occur multiple times with the
  // same resolution if it occurs multiple times in the tree.
  private val resolutions = mutableMapOf<ExSymbol<*>, Scope.Resolution>()

  private var currentScope: Scope = rootScope
  // Tracks symbols resolved during the processing of a binding value.
  private var resolutionsInBoundValue = mutableSetOf<ExSymbol<*>>()

  override fun visitCall(call: ExCall<T>) = visitChildren(call, Unit)
  override fun visitIndex(index: ExIndex<T>) = visitChildren(index, Unit)
  override fun visitUnaryOp(op: ExUnaryOp<T>) = visitChildren(op, Unit)
  override fun visitBinaryOp(op: ExBinaryOp<T>) = visitChildren(op, Unit)
  override fun visitRangeOp(op: ExRangeOp<T>) = visitChildren(op, Unit)

  override fun visitIf(iff: ExIf<T>) = visitChildren(iff, Unit)

  override fun visitLet(let: ExLet<T>) {
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
    val bindingDeps = mutableMapOf<ExSymbol<*>, Set<ExSymbol<*>>>()
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

  override fun visitBinding(binding: ExBinding<T>) {
    binding.value.accept(this)
  }

  override fun visitLambda(lambda: ExLambda<T>) {
    val lambdaScope = FunctionScope(lambda, currentScope)

    currentScope = lambdaScope
    visitChildren(lambda, Unit) // Visit parameters and then body
    currentScope = currentScope.parent

    scopes[lambda] = lambdaScope
  }

  override fun visitParameter(parameter: ExParameter<T>) {
    val resolution = currentScope.define(parameter.symbol)
    resolutions[parameter.symbol] = resolution
  }

  override fun visitLiteral(literal: ExLiteral<T, *>) {
    // Nothing to do.
  }

  override fun visitSymbol(symbol: ExSymbol<T>) {
    val res = currentScope.resolve(symbol)
    resolutions[symbol] = res
    if (res is Scope.Resolution.Local) {
      resolutionsInBoundValue.add(res.symbol)
    }
  }
}
