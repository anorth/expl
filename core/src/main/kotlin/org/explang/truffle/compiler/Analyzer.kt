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
import org.explang.truffle.Type

/**
 * Analyses a syntax tree for binding and type information.
 */
class Analyzer() {
  class Analysis(
      val rootScope: RootScope,
      // Scopes introduced by syntax elements.
      val scopes: Map<ExTree, Scope2>,
      // Maps symbols to resolutions.
      val resolutions: Map<ExSymbol, Scope2.Resolution>,
      // Maps function definitions to a collection of symbols which resolved outside the
      // function's scope, so must be captured in a closure at function definition.
      val captured: Map<ExLambda, Set<Scope2.Resolution>>
  ) {
    override fun toString(): String {
      return """Resolutions: ${resolutions.values}
        |Captures: ${captured.values}
      """.trimMargin()
    }
  }

  fun analyze(tree: ExTree): Analysis {
    val rootScope = RootScope(tree)
    val v = ScopeVisitor(rootScope)
    tree.accept(v)
    assert(v.scope() == rootScope) { "Scope visitor corrupt" }

    val captured = computeCapturedSymbols(v.resolutions.values)

    return Analysis(v.rootScope, v.scopes, v.resolutions, captured)
  }

  private fun computeCapturedSymbols(
      resolutions: Iterable<Scope2.Resolution>): Map<ExLambda, Set<Scope2.Resolution>> {
    val captured = mutableMapOf<ExLambda, MutableSet<Scope2.Resolution>>()
    resolutions.forEach {
      var res = it
      while (res is Scope2.Resolution.Closure) {
        captured.getOrPut(res.scope.tree) { mutableSetOf() }.add(res)
        res = res.capture
      }
    }
    return captured
  }
}

/**
 * Resolves symbols to scopes in which they are defined.
 */
private class ScopeVisitor(val rootScope: RootScope) : ExTree.Visitor<Unit> {
  // Scopes introduced by syntactic trees (functions and bindings)
  val scopes = mutableMapOf<ExTree, Scope2>()
  // Maps symbols to resolutions. The "same" symbol string may occur multiple times with the
  // same resolution if it occurs multiple times in the tree.
  val resolutions = mutableMapOf<ExSymbol, Scope2.Resolution>()
  private var currentScope: Scope2 = rootScope

  fun scope() = currentScope

  override fun visitCall(call: ExCall) {
    visitChildren(call, Unit)
  }

  override fun visitUnaryOp(unop: ExUnaryOp) {
    visitChildren(unop, Unit)
  }

  override fun visitBinaryOp(binop: ExBinaryOp) {
    visitChildren(binop, Unit)
  }

  override fun visitIf(iff: ExIf) {
    visitChildren(iff, Unit)
  }

  override fun visitLet(let: ExLet) {
    val scope = BindingScope(let, currentScope)
    scopes[let] = scope
    currentScope = scope
    visitChildren(let, Unit) // Visits all the bindings and then the bound expression last
    currentScope = currentScope.parent
  }

  override fun visitBinding(binding: ExBinding) {
    // Define the binding before visiting the value, thus supporting recursive resolution.
    (currentScope as BindingScope).defineBinding(Type.NONE, binding.symbol)
    binding.value.accept(this)
  }

  override fun visitLambda(lambda: ExLambda) {
    val scope = FunctionScope(lambda, currentScope)
    lambda.parameters.forEach {
      scope.defineArgument(Type.NONE, it)
    }

    scopes[lambda] = scope
    currentScope = scope
    lambda.body.accept(this)
    currentScope = currentScope.parent
  }

  override fun visitLiteral(literal: ExLiteral<*>) {
    visitChildren(literal, Unit)
  }

  override fun visitSymbol(symbol: ExSymbol) {
    val res = currentScope.resolve(symbol)
    resolutions[symbol] = res
  }
}
