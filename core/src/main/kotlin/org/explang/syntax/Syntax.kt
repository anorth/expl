package org.explang.syntax

/**
 * Pure syntax tree representation with a generic tag object for metadata. Nodes are immutable.
 * This corresponds closely to the grammar (de-sugared).
 *
 * No explicit type information, scoping or reference to compilation outputs, but these may be stored
 * in the tag (which might be internally mutable).
 *
 * Note: subtypes intentionally use identity-based equals and hashcode for efficient use
 * as container keys.
 */
sealed class ExTree {
  /**
   * A visitor for trees.
   *
   * @param <V> type of the value returned by each visit method
   */
  interface Visitor<V> {
    fun visitCall(call: ExCall): V
    fun visitIndex(index: ExIndex): V
    fun visitUnaryOp(op: ExUnaryOp): V
    fun visitBinaryOp(op: ExBinaryOp): V
    fun visitRangeOp(op: ExRangeOp): V
    fun visitIf(iff: ExIf): V
    fun visitLet(let: ExLet): V
    fun visitBinding(binding: ExBinding): V
    fun visitLambda(lambda: ExLambda): V
    fun visitParameter(parameter: ExParameter): V
    fun visitLiteral(literal: ExLiteral<*>): V
    fun visitSymbol(symbol: ExSymbol): V
  }

  // Range of tokens which this syntax object represents
  abstract val tokenRange: IntRange

  abstract fun children(): Iterable<ExTree>

  abstract fun <V> accept(v: Visitor<V>): V
}

class ExCall(
    override val tokenRange: IntRange,
    val callee: ExTree,
    val args: List<ExTree>
) : ExTree() {
  override fun children() = listOf(callee) + args
  override fun <V> accept(v: Visitor<V>) = v.visitCall(this)
  override fun toString() = "call($callee, ${args.joinToString(",")})"
}

class ExIndex(
    override val tokenRange: IntRange,
    val indexee: ExTree,
    val indexer: ExTree
) : ExTree() {
  override fun children() = listOf(indexee, indexer)
  override fun <V> accept(v: Visitor<V>) = v.visitIndex(this)
  override fun toString() = "index($indexee, $indexer)"
}

class ExUnaryOp(
    override val tokenRange: IntRange,
    val operator: String,
    val operand: ExTree
) : ExTree() {
  override fun children() = listOf(operand)
  override fun <V> accept(v: Visitor<V>) = v.visitUnaryOp(this)
  override fun toString() = "$operator($operand)"
}

class ExBinaryOp(
    override val tokenRange: IntRange,
    val operator: String,
    val left: ExTree,
    val right: ExTree
) : ExTree() {
  override fun children() = listOf(left, right)
  override fun <V> accept(v: Visitor<V>) = v.visitBinaryOp(this)
  override fun toString() = "$operator($left, $right)"
}

class ExRangeOp(
    override val tokenRange: IntRange,
    val first: ExTree?,
    val last: ExTree?,
    val step: ExTree?
) : ExTree() {
  override fun children() = listOfNotNull(first, last, step)
  override fun <V> accept(v: Visitor<V>) = v.visitRangeOp(this)
  override fun toString() = "range($first, $last, $step)"
}

class ExIf(
    override val tokenRange: IntRange,
    val test: ExTree,
    val left: ExTree,
    val right: ExTree
) : ExTree() {
  override fun children() = listOf(test, left, right)
  override fun <V> accept(v: Visitor<V>) = v.visitIf(this)
  override fun toString() = "if($test, $left, $right)"
}

class ExLet(
    override val tokenRange: IntRange,
    val bindings: List<ExBinding>,
    val bound: ExTree
): ExTree() {
  override fun children() = bindings + listOf(bound)
  override fun <V> accept(v: Visitor<V>) = v.visitLet(this)
  override fun toString() = "let(${bindings.joinToString(",")}; $bound)"
}

class ExBinding(
    override val tokenRange: IntRange,
    val symbol: ExSymbol,
    val value: ExTree
): ExTree() {
  override fun children() = listOf(symbol, value)
  override fun <V> accept(v: Visitor<V>) = v.visitBinding(this)
  override fun toString() = "$symbol = $value"
}

class ExLambda(
    override val tokenRange: IntRange,
    val parameters: List<ExParameter>,
    val annotation: Type,
    val body: ExTree
): ExTree() {
  override fun children() = parameters + listOf(body)
  override fun <V> accept(v: Visitor<V>) = v.visitLambda(this)
  override fun toString() = "(${parameters.joinToString(",")} -> $body)"
}

class ExParameter(
  override val tokenRange: IntRange,
  val symbol: ExSymbol,
  val annotation: Type
): ExTree() {
  override fun children() = listOf(symbol)
  override fun <V> accept(v: Visitor<V>): V = v.visitParameter(this)
  override fun toString() = "$symbol:$annotation"
}

class ExLiteral<L: Any>(
    override val tokenRange: IntRange,
    val type: Class<L>,
    val value: L
) : ExTree() {
  override fun children() = listOf<ExTree>()
  override fun <V> accept(v: Visitor<V>) = v.visitLiteral(this)
  override fun toString() = "$value"
}

class ExSymbol(
    override val tokenRange: IntRange,
    val name: String
) : ExTree() {
  override fun children() = listOf<ExTree>()
  override fun <V> accept(v: Visitor<V>) = v.visitSymbol(this)
  override fun toString() = "#$name"
}
