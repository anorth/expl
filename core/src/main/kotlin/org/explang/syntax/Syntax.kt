package org.explang.syntax

/**
 * Pure syntax tree representation, with no type information, scoping or reference to Graal/Truffle.
 * This corresponds closely to the grammar, de-sugared.
 *
 * Note: instances intentionally do not override equals or hashcode so that instances used as
 * container keys use fast identity-based implementations.
 */
sealed class ExTree {
  interface Visitor<T> {
    fun visitCall(call: ExCall): T
    fun visitUnaryOp(unop: ExUnaryOp): T
    fun visitBinaryOp(binop: ExBinaryOp): T
    fun visitIf(iff: ExIf): T
    fun visitLet(let: ExLet): T
    fun visitBinding(binding: ExBinding): T
    fun visitLambda(lambda: ExLambda): T
    fun visitLiteral(literal: ExLiteral<*>): T
    fun visitSymbol(symbol: ExSymbol): T

    fun visit(tree: ExTree) = tree.accept(this)

    fun visitChildren(tree: ExTree, initial: T, agg: (T, T) -> T = { _, next -> next }): T {
      var res = initial
      tree.children.forEach {
        res = agg(res, it.accept(this))
      }
      return res
    }
  }

  // Range of tokens which this syntax object represents
  abstract val tokenRange: IntRange

  abstract val children: Iterable<ExTree>

  abstract fun <T> accept(v: Visitor<T>): T
}

class ExCall(
    override val tokenRange: IntRange,
    val callee: ExTree,
    val args: List<ExTree>
) : ExTree() {
  override val children get() = listOf(callee) + args
  override fun <T> accept(v: Visitor<T>) = v.visitCall(this)
  override fun toString() = "call($callee, ${args.joinToString(",")})"
}

class ExUnaryOp(
    override val tokenRange: IntRange,
    val operator: String,
    val operand: ExTree
) : ExTree() {
  override val children get() = listOf(operand)
  override fun <T> accept(v: Visitor<T>) = v.visitUnaryOp(this)
  override fun toString() = "$operator($operand)"
}

class ExBinaryOp(
    override val tokenRange: IntRange,
    val operator: String,
    val left: ExTree,
    val right: ExTree
) : ExTree() {
  override val children get() = listOf(left, right)
  override fun <T> accept(v: Visitor<T>) = v.visitBinaryOp(this)
  override fun toString() = "$operator($left, $right)"
}

class ExIf(
    override val tokenRange: IntRange,
    val test: ExTree,
    val left: ExTree,
    val right: ExTree
) : ExTree() {
  override val children get() = listOf(test, left, right)
  override fun <T> accept(v: Visitor<T>) = v.visitIf(this)
  override fun toString() = "if($test, $left, $right)"
}

class ExLet(
    override val tokenRange: IntRange,
    val bindings: List<ExBinding>,
    val bound: ExTree
): ExTree() {
  override val children get() = bindings + listOf(bound)
  override fun <T> accept(v: Visitor<T>) = v.visitLet(this)
  override fun toString() = "let(${bindings.joinToString(",")}; $bound)"
}

class ExBinding(
    override val tokenRange: IntRange,
    val symbol: ExSymbol,
    val value: ExTree
): ExTree() {
  override val children get() = listOf(symbol, value)
  override fun <T> accept(v: Visitor<T>) = v.visitBinding(this)
  override fun toString() = "$symbol = $value"
}

class ExLambda(
    override val tokenRange: IntRange,
    val parameters: List<ExSymbol>,
    val body: ExTree
): ExTree() {
  override val children get() = parameters + listOf(body)
  override fun <T> accept(v: Visitor<T>) = v.visitLambda(this)
  override fun toString() = "(${parameters.joinToString(",")} -> $body)"
}

class ExLiteral<T>(
    override val tokenRange: IntRange,
    val type: Class<T>,
    val value: T
) : ExTree() {
  override val children get() = listOf<ExTree>()
  override fun <T> accept(v: Visitor<T>) = v.visitLiteral(this)
  override fun toString() = "$value"
}

class ExSymbol(
    override val tokenRange: IntRange,
    val name: String
) : ExTree() {
  override val children get() = listOf<ExTree>()
  override fun <T> accept(v: Visitor<T>) = v.visitSymbol(this)
  override fun toString() = "#$name"
}
