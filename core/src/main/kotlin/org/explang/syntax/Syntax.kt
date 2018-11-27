package org.explang.syntax

/**
 * Pure syntax tree representation, with no type information, scoping or reference to Graal/Truffle.
 * This corresponds closely to the grammar, de-sugared.
 *
 * Note: instances intentionally do not override equals or hashcode so that instances used as
 * container keys use fast identity-based implementations.
 */
sealed class ExTree {
  // Range of tokens which this syntax object represents
  abstract val range: IntRange
}

class ExCall(
    override val range: IntRange,
    val callee: ExTree,
    val args: List<ExTree>
) : ExTree() {
  override fun toString() = "call($callee, ${args.joinToString(",")})"
}

class ExUnaryOp(
    override val range: IntRange,
    val operator: String,
    val operand: ExTree
) : ExTree() {
  override fun toString() = "$operator($operand)"
}

class ExBinaryOp(
    override val range: IntRange,
    val operator: String,
    val left: ExTree,
    val right: ExTree
) : ExTree() {
  override fun toString() = "$operator($left, $right)"
}

class ExIf(
    override val range: IntRange,
    val test: ExTree,
    val left: ExTree,
    val right: ExTree
) : ExTree() {
  override fun toString() = "if($test, $left, $right)"
}

class ExLet(
    override val range: IntRange,
    val bindings: List<ExBinding>,
    val bound: ExTree
): ExTree() {
  override fun toString() = "let(${bindings.joinToString(",")}; $bound)"
}

class ExBinding(
    override val range: IntRange,
    val symbol: ExSymbol,
    val value: ExTree
): ExTree() {
  override fun toString() = "$symbol = $value)"
}

class ExLambda(
    override val range: IntRange,
    val parameters: List<ExSymbol>,
    val body: ExTree
): ExTree() {
  override fun toString() = "(${parameters.joinToString(",")} -> $body)"
}

class ExLiteral<T>(
    override val range: IntRange,
    val type: Class<T>,
    val value: T
) : ExTree() {
  override fun toString() = "$value"
}

class ExSymbol(
    override val range: IntRange,
    val name: String
) : ExTree() {
  override fun toString() = "#$name"
}
