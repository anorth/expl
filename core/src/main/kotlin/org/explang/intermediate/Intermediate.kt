package org.explang.intermediate

import org.explang.syntax.ExTree
import org.explang.syntax.Type

/**
 * Intermediate tree representation.
 * This tree is manipulated to be appropriate for interpretation/compilation.
 *
 * Note: subtypes intentionally use identity-based equals and hashcode for efficient use
 * as container keys.
 */
sealed class ITree(
    val syntax: ExTree?,
    type: Type,
    val typeCandidates: MutableList<Type>
) {
  /** A visitor for trees. */
  interface Visitor<V> {
    fun visitCall(call: ICall): V
    fun visitIf(iff: IIf): V
    fun visitLet(let: ILet): V
    fun visitBinding(binding: IBinding): V
    fun visitLambda(lambda: ILambda): V
    fun visitParameter(parameter: IParameter): V
    fun visitLiteral(literal: ILiteral<*>): V
    fun visitSymbol(symbol: ISymbol): V
    fun visitBuiltin(builtin: IBuiltin<*>): V
    fun visitNull(n: INull): V

    // Visits a tree's children, ignoring any return value
    fun visitChildren(tree: ITree) {
      for (child in tree.children()) {
        child.accept(this)
      }
    }
  }

  var type: Type = Type.NONE
    set(value) {
      if (field == Type.NONE) {
        field = value
      } else {
        org.explang.compiler.check(this, field == value) {
          "Conflicting types for $this: $field, $value"
        }
      }
    }

  init {
    this.type = type
  }

  abstract fun children(): Iterable<ITree>

  abstract fun <V> accept(v: Visitor<V>): V
}

class ICall(
    syntax: ExTree?,
    val callee: ITree,
    val args: List<ITree>,
    type: Type
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf(callee) + args
  override fun <V> accept(v: Visitor<V>) = v.visitCall(this)
  override fun toString() = "call($callee, ${args.joinToString(",")})"
  fun with(callee: ITree, args: List<ITree>) = ICall(syntax, callee, args, type)
}

class IIf(
    syntax: ExTree?,
    val test: ITree,
    val left: ITree,
    val right: ITree,
    type: Type
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf(test, left, right)
  override fun <V> accept(v: Visitor<V>) = v.visitIf(this)
  override fun toString() = "if($test, $left, $right)"
  fun with(test: ITree, left: ITree, right: ITree) = IIf(syntax, test, left, right, type)
}

class ILet(
    syntax: ExTree?,
    val bindings: List<IBinding>,
    val bound: ITree,
    type: Type
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = bindings + listOf(bound)
  override fun <V> accept(v: Visitor<V>) = v.visitLet(this)
  override fun toString() = "let(${bindings.joinToString(",")}; $bound)"
  fun with(bindings: List<IBinding>, bound: ITree) = ILet(syntax, bindings, bound, type)
}

class IBinding(
    syntax: ExTree?,
    val symbol: ISymbol,
    val value: ITree,
    type: Type
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf(symbol, value)
  override fun <V> accept(v: Visitor<V>) = v.visitBinding(this)
  override fun toString() = "$symbol = $value"
  fun with(symbol: ISymbol, value: ITree) = IBinding(syntax, symbol, value, type)
}

class ILambda(
    syntax: ExTree?,
    val parameters: List<IParameter>,
    val body: ITree,
    val returnType: Type,
    type: Type
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = parameters + listOf(body)
  override fun <V> accept(v: Visitor<V>) = v.visitLambda(this)
  override fun toString() = "(${parameters.joinToString(",")} -> $body)"
  fun with(parameters: List<IParameter>, annotation: Type, body: ITree) =
      ILambda(syntax, parameters, body, annotation, type)
}

class IParameter(
    syntax: ExTree?,
    val symbol: ISymbol,
    type: Type
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf(symbol)
  override fun <V> accept(v: Visitor<V>): V = v.visitParameter(this)
  override fun toString() = "$symbol" + if (type != Type.NONE) ":$type" else ""
}

class ILiteral<L : Any>(
    syntax: ExTree?,
    val implType: Class<L>,
    val value: L,
    type: Type
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf<ITree>()
  override fun <V> accept(v: Visitor<V>) = v.visitLiteral(this)
  override fun toString() = "$value"
}

class ISymbol(
    syntax: ExTree?,
    val name: String,
    type: Type
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf<ITree>()
  override fun <V> accept(v: Visitor<V>) = v.visitSymbol(this)
  override fun toString() = "#$name"
}

open class IBuiltin<T: Any>(
    syntax: ExTree?,
    val name: String,
    val value: T,
    type: Type
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf<ITree>()
  override fun <V> accept(v: Visitor<V>) = v.visitBuiltin(this)
}

class INull(syntax: ExTree?, type: Type) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf<ITree>()
  override fun <V> accept(v: Visitor<V>) = v.visitNull(this)
}