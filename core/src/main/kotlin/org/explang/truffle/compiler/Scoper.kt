package org.explang.truffle.compiler

import org.explang.syntax.ExBinaryOp
import org.explang.syntax.ExBinding
import org.explang.syntax.ExCall
import org.explang.syntax.ExIf
import org.explang.syntax.ExLambda
import org.explang.syntax.ExLet
import org.explang.syntax.ExLiteral
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree
import org.explang.syntax.ExUnaryOp
import org.explang.syntax.Type

/**
 * AST visitor which resolves symbols to scopes in which they are defined.
 *
 * @param <T> type of the AST tag (opaque)
 */
class Scoper<T>(rootScope: RootScope) : ExTree.Visitor<T, Unit> {
  /** The result of scope analysis */
  data class Result(
      val rootScope: RootScope,
      /** Scopes keyed by the syntactic tree which introduces them */
      val scopes: Map<ExTree<*>, Scope>,
      /** The resolution of each symbol occurrence in the syntactic tree. */
      val resolutions: Map<ExSymbol<*>, Scope.Resolution>,
      /** Maps function definitions to a collection of symbols which resolved outside the
       * function's scope, so must be captured in a closure at function definition.
       */
      val captured: Map<ExLambda<*>, Set<Scope.Resolution>>
  )

  companion object {
    /** Computes scopes and symbol resolutions for a tree. */
    fun <T> computeScopes(tree: ExTree<T>): Result {
      val rootScope = RootScope(tree)
      val v = Scoper<T>(rootScope)
      tree.accept(v)
      assert(v.currentScope == rootScope) { "Scope visitor corrupt" }

      val captured = computeCapturedSymbols(v.resolutions.values)
      return Result(rootScope, v.scopes, v.resolutions, captured)
    }

    private fun computeCapturedSymbols(
        resolutions: Iterable<Scope.Resolution>): Map<ExLambda<*>, Set<Scope.Resolution>> {
      val captured = mutableMapOf<ExLambda<*>, MutableSet<Scope.Resolution>>()
      resolutions.forEach {
        var res = it
        while (res is Scope.Resolution.Closure) {
          captured.getOrPut(res.scope.tree) { mutableSetOf() }.add(res)
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
    val scope = currentScope as BindingScope
    // Define the binding before visiting the value, thus supporting recursive resolution.
    // An alternative would be to have the function's name resolve in the new function scope,
    // rather than the containing binding scope.
    scope.defineBinding(binding.symbol, Type.NONE)
    binding.value.accept(this)
  }

  override fun visitLambda(lambda: ExLambda<T>) {
    val scope = FunctionScope(lambda, currentScope)
    lambda.parameters.forEach {
      scope.defineArgument(it, Type.NONE)
    }

    scopes[lambda] = scope
    currentScope = scope
    lambda.body.accept(this)
    currentScope = currentScope.parent
  }

  override fun visitLiteral(literal: ExLiteral<T, *>) {
    // Nothing to do.
  }

  override fun visitSymbol(symbol: ExSymbol<T>) {
    val res = currentScope.resolve(symbol)
    resolutions[symbol] = res
  }
}
