package org.explang.intermediate

import org.explang.compiler.CompileError
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
    fun visitArgRead(read: IArgRead): V
    fun visitLocalRead(read: ILocalRead): V
    fun visitClosureRead(read: IClosureRead): V
    fun visitBuiltin(builtin: IBuiltin<*>): V
    fun visitNil(n: INil): V

    // Visits a tree's children, ignoring any return value
    fun visitChildren(tree: ITree) {
      for (child in tree.children()) {
        child.accept(this)
      }
    }
  }

  private var _type = type

  var type: Type
    get() = _type
    set(value) {
      if (_type == Type.NONE) {
        _type = value
      } else if (_type != value) {
        throw CompileError("Conflicting types for $this: $_type, $value", this)
      }
    }

  fun replaceType(t: Type) {
    _type = t
  }

  abstract fun children(): Iterable<ITree>

  abstract fun <V> accept(v: Visitor<V>): V
}

class ICall(
    syntax: ExTree?,
    type: Type,
    val callee: ITree,
    val args: Array<ITree>
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf(callee) + args
  override fun <V> accept(v: Visitor<V>) = v.visitCall(this)
  override fun toString() = "call($callee, ${args.joinToString(",")})"
  fun with(callee: ITree, args: Array<ITree>) = ICall(syntax, type, callee, args)
}

class IIf(
    syntax: ExTree?,
    type: Type,
    val test: ITree,
    val left: ITree,
    val right: ITree
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf(test, left, right)
  override fun <V> accept(v: Visitor<V>) = v.visitIf(this)
  override fun toString() = "if($test, $left, $right)"
  fun with(test: ITree, left: ITree, right: ITree) = IIf(syntax, type, test, left, right)
}

class ILet(
    syntax: ExTree?,
    type: Type,
    val bindings: List<IBinding>,
    val bound: ITree
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = bindings + listOf(bound)
  override fun <V> accept(v: Visitor<V>) = v.visitLet(this)
  override fun toString() = "let(${bindings.joinToString(",")}; $bound)"
  fun with(bindings: List<IBinding>, bound: ITree) = ILet(syntax, type, bindings, bound)
}

class IBinding(
    syntax: ExTree?,
    type: Type,
    val symbol: ISymbol,
    val value: ITree
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf(symbol, value)
  override fun <V> accept(v: Visitor<V>) = v.visitBinding(this)
  override fun toString() = "$symbol = $value"
  fun with(symbol: ISymbol, value: ITree) = IBinding(syntax, type, symbol, value)
}

class ILambda(
    syntax: ExTree?,
    type: Type,
    val parameters: List<IParameter>,
    val body: ITree,
    val returnType: Type
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = parameters + listOf(body)
  override fun <V> accept(v: Visitor<V>) = v.visitLambda(this)
  override fun toString() = "(${parameters.joinToString(",")} -> $body)"
  fun with(parameters: List<IParameter>, annotation: Type, body: ITree) =
      ILambda(syntax, type, parameters, body, annotation)
}

class IParameter(
    syntax: ExTree?,
    type: Type,
    val symbol: ISymbol
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf(symbol)
  override fun <V> accept(v: Visitor<V>): V = v.visitParameter(this)
  override fun toString() = "$symbol" + if (type != Type.NONE) ":$type" else ""
}

class ILiteral<L : Any>(
    syntax: ExTree?,
    type: Type,
    val implType: Class<L>,
    val value: L
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf<ITree>()
  override fun <V> accept(v: Visitor<V>) = v.visitLiteral(this)
  override fun toString() = "$value"
}

class ISymbol(
    syntax: ExTree?,
    type: Type,
    val name: String
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf<ITree>()
  override fun <V> accept(v: Visitor<V>) = v.visitSymbol(this)
  override fun toString() = "#$name"
}

class IArgRead(
    syntax: ExTree?,
    type: Type,
    val name: String,
    val index: Int
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf<ITree>()
  override fun <V> accept(v: Visitor<V>) = v.visitArgRead(this)
}

class ILocalRead(
    syntax: ExTree?,
    type: Type,
    val name: String
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf<ITree>()
  override fun <V> accept(v: Visitor<V>) = v.visitLocalRead(this)
}

class IClosureRead(
    syntax: ExTree?,
    type: Type,
    val name: String
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf<ITree>()
  override fun <V> accept(v: Visitor<V>) = v.visitClosureRead(this)
}

class IBuiltin<T : Any>(
    syntax: ExTree?,
    type: Type,
    val name: String,
    val value: T
) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf<ITree>()
  override fun <V> accept(v: Visitor<V>) = v.visitBuiltin(this)
}

class INil(syntax: ExTree?, type: Type) : ITree(syntax, type, mutableListOf()) {
  override fun children() = listOf<ITree>()
  override fun <V> accept(v: Visitor<V>) = v.visitNil(this)
  override fun toString() = "nil"
}