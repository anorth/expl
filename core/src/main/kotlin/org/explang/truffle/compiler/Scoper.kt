package org.explang.truffle.compiler

import org.explang.syntax.ExBinaryOp
import org.explang.syntax.ExBinding
import org.explang.syntax.ExCall
import org.explang.syntax.ExIf
import org.explang.syntax.ExLambda
import org.explang.syntax.ExLet
import org.explang.syntax.ExLiteral
import org.explang.syntax.ExParameter
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree
import org.explang.syntax.ExUnaryOp

/**
 * AST visitor which resolves symbols to scopes in which they are defined.
 *
 * @param <T> type of the AST tag (opaque)
 */
class Scoper<T>(rootScope: RootScope) : ExTree.Visitor<T, Unit> {
  companion object {
    /** Computes scopes and symbol resolutions for a tree. */
    fun <T> buildResolver(tree: ExTree<T>): Resolver {
      val rootScope = RootScope(tree)
      val scoped = Scoper<T>(rootScope)
      tree.accept(scoped)
      assert(scoped.currentScope == rootScope) { "Scope visitor corrupt" }

      val captured = computeCapturedSymbols(scoped.resolutions.values)
      return LookupResolver(scoped.resolutions, captured)
    }

    private fun computeCapturedSymbols(
        resolutions: Iterable<Scope.Resolution>): Map<ExLambda<*>, Set<Scope.Resolution>> {
      val captured = mutableMapOf<ExLambda<*>, MutableSet<Scope.Resolution>>()
      resolutions.forEach {
        var res = it
        while (res is Scope.Resolution.Closure) {
          captured.getOrPut(res.scope.tree) { mutableSetOf() }.add(res.capture)
          res = res.capture
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

  override fun visitCall(call: ExCall<T>) {
    visitChildren(call, Unit)
  }

  override fun visitUnaryOp(unop: ExUnaryOp<T>) {
    visitChildren(unop, Unit)
  }

  override fun visitBinaryOp(binop: ExBinaryOp<T>) {
    visitChildren(binop, Unit)
  }

  override fun visitIf(iff: ExIf<T>) {
    visitChildren(iff, Unit)
  }

  override fun visitLet(let: ExLet<T>) {
    val scope = BindingScope(let, currentScope)
    scopes[let] = scope
    currentScope = scope
    visitChildren(let, Unit) // Visits all the bindings and then the bound expression last
    currentScope = currentScope.parent
  }

  override fun visitBinding(binding: ExBinding<T>) {
    assert(currentScope is BindingScope) { "Encountered binding without enclosing binding scope" }
    // Define the binding before visiting the value, thus supporting recursive resolution.
    val resolution = currentScope.define(binding.symbol)
    resolutions[binding.symbol] = resolution // Resolve the bound symbol to "itself"
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
  }
}
