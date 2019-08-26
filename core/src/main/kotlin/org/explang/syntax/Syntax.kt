package org.explang.syntax

/**
} * Pure syntax tree representation with a generic tag object for metadata. Nodes are immutable.
 * This corresponds closely to the grammar (de-sugared).
 *
 * No explicit type information, scoping or reference to Graal/Truffle, but these may be stored
 * in the tag (which might be internally mutable).
 *
 * Note: subtypes intentionally use identity-based equals and hashcode for efficient use
 * as container keys.
 *
 * @param <T> type of tag object attached to each node
 */
sealed class ExTree<T> {
  /**
   * A visitor for trees.
   *
   * @param <V> type of the value returned by each visit method
   */
  interface Visitor<T, V> {
    fun visitCall(call: ExCall<T>): V
    fun visitIndex(index: ExIndex<T>): V
    fun visitUnaryOp(op: ExUnaryOp<T>): V
    fun visitBinaryOp(op: ExBinaryOp<T>): V
    fun visitRangeOp(op: ExRangeOp<T>): V
    fun visitIf(iff: ExIf<T>): V
    fun visitLet(let: ExLet<T>): V
    fun visitBinding(binding: ExBinding<T>): V
    fun visitLambda(lambda: ExLambda<T>): V
    fun visitParameter(parameter: ExParameter<T>): V
    fun visitLiteral(literal: ExLiteral<T, *>): V
    fun visitSymbol(symbol: ExSymbol<T>): V

    fun visit(tree: ExTree<T>) = tree.accept(this)

    fun visitChildren(tree: ExTree<T>, initial: V, agg: (V, V) -> V = { _, next -> next }): V {
      var res = initial
      tree.children.forEach {
        res = agg(res, it.accept(this))
      }
      return res
    }
  }

  // Range of tokens which this syntax object represents
  abstract val tokenRange: IntRange
  // Opaque tag data
  abstract val tag: T

  abstract val children: Iterable<ExTree<T>>

  abstract fun <V> accept(v: Visitor<T, V>): V
}

class ExCall<T>(
    override val tokenRange: IntRange,
    override val tag: T,
    val callee: ExTree<T>,
    val args: List<ExTree<T>>
) : ExTree<T>() {
  override val children get() = listOf(callee) + args
  override fun <V> accept(v: Visitor<T, V>) = v.visitCall(this)
  override fun toString() = "call($callee, ${args.joinToString(",")})"
}

class ExIndex<T>(
    override val tokenRange: IntRange,
    override val tag: T,
    val indexee: ExTree<T>,
    val indexer: ExTree<T>
) : ExTree<T>() {
  override val children get() = listOf(indexee, indexer)
  override fun <V> accept(v: Visitor<T, V>) = v.visitIndex(this)
  override fun toString() = "index($indexee, $indexer)"
}

class ExUnaryOp<T>(
    override val tokenRange: IntRange,
    override val tag: T,
    val operator: String,
    val operand: ExTree<T>
) : ExTree<T>() {
  override val children get() = listOf(operand)
  override fun <V> accept(v: Visitor<T, V>) = v.visitUnaryOp(this)
  override fun toString() = "$operator($operand)"
}

class ExBinaryOp<T>(
    override val tokenRange: IntRange,
    override val tag: T,
    val operator: String,
    val left: ExTree<T>,
    val right: ExTree<T>
) : ExTree<T>() {
  override val children get() = listOf(left, right)
  override fun <V> accept(v: Visitor<T, V>) = v.visitBinaryOp(this)
  override fun toString() = "$operator($left, $right)"
}

class ExRangeOp<T>(
    override val tokenRange: IntRange,
    override val tag: T,
    val first: ExTree<T>?,
    val last: ExTree<T>?,
    val step: ExTree<T>?
) : ExTree<T>() {
  override val children get() = listOfNotNull(first, last, step)
  override fun <V> accept(v: Visitor<T, V>) = v.visitRangeOp(this)
  override fun toString() = "range($first, $last, $step)"
}

class ExIf<T>(
    override val tokenRange: IntRange,
    override val tag: T,
    val test: ExTree<T>,
    val left: ExTree<T>,
    val right: ExTree<T>
) : ExTree<T>() {
  override val children get() = listOf(test, left, right)
  override fun <V> accept(v: Visitor<T, V>) = v.visitIf(this)
  override fun toString() = "if($test, $left, $right)"
}

class ExLet<T>(
    override val tokenRange: IntRange,
    override val tag: T,
    val bindings: List<ExBinding<T>>,
    val bound: ExTree<T>
): ExTree<T>() {
  override val children get() = bindings + listOf(bound)
  override fun <V> accept(v: Visitor<T, V>) = v.visitLet(this)
  override fun toString() = "let(${bindings.joinToString(",")}; $bound)"
}

class ExBinding<T>(
    override val tokenRange: IntRange,
    override val tag: T,
    val symbol: ExSymbol<T>,
    val value: ExTree<T>
): ExTree<T>() {
  override val children get() = listOf(symbol, value)
  override fun <V> accept(v: Visitor<T, V>) = v.visitBinding(this)
  override fun toString() = "$symbol = $value"
}

class ExLambda<T>(
    override val tokenRange: IntRange,
    override val tag: T,
    val parameters: List<ExParameter<T>>,
    val annotation: Type,
    val body: ExTree<T>
): ExTree<T>() {
  override val children get() = parameters + listOf(body)
  override fun <V> accept(v: Visitor<T, V>) = v.visitLambda(this)
  override fun toString() = "(${parameters.joinToString(",")} -> $body)"
}

class ExParameter<T>(
  override val tokenRange: IntRange,
  override val tag: T,
  val symbol: ExSymbol<T>,
  val annotation: Type
): ExTree<T>() {
  override val children = listOf(symbol)
  override fun <V> accept(v: Visitor<T, V>): V = v.visitParameter(this)
  override fun toString() = "$symbol:$annotation"
}

class ExLiteral<T, L>(
    override val tokenRange: IntRange,
    override val tag: T,
    val type: Class<L>,
    val value: L
) : ExTree<T>() {
  override val children get() = listOf<ExTree<T>>()
  override fun <V> accept(v: Visitor<T, V>) = v.visitLiteral(this)
  override fun toString() = "$value"
}

class ExSymbol<T>(
    override val tokenRange: IntRange,
    override val tag: T,
    val name: String
) : ExTree<T>() {
  override val children get() = listOf<ExTree<T>>()
  override fun <V> accept(v: Visitor<T, V>) = v.visitSymbol(this)
  override fun toString() = "#$name"
}
