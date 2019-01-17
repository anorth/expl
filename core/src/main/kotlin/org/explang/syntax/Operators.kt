package org.explang.syntax

import org.explang.syntax.Type.Companion.BOOL
import org.explang.syntax.Type.Companion.DOUBLE
import org.explang.syntax.Type.Companion.LONG

/**
 * Describes the types of an operator's operands and results.
 * In this representation, operators of arity greater than one must have the same type
 * for all operands.
 */
data class Operator(
    val resultType: Type,
    val operandType: Type
)

/** A collection of operators. */
class Operators(private val ops: Map<String, List<Operator>>) {
  fun withResultType(name: String, type: Type) = ops[name]!!.filter { it.resultType == type }
  fun withOperandType(name: String, type: Type) = ops[name]!!.single { it.operandType == type }
}

val UNARY_OPERATORS = Operators(mapOf(
    "-" to listOf(Operator(LONG, LONG), Operator(DOUBLE, DOUBLE)),
    "+" to listOf(Operator(LONG, LONG), Operator(DOUBLE, DOUBLE)),
    "not" to listOf(Operator(BOOL, BOOL))
))

val BINARY_OPERATORS = Operators(mapOf(
    "and" to listOf(Operator(BOOL, BOOL)),
    "or" to listOf(Operator(BOOL, BOOL)),
    "xor" to listOf(Operator(BOOL, BOOL)),

    "==" to listOf(Operator(BOOL, BOOL), Operator(BOOL, LONG), Operator(BOOL, DOUBLE)),
    "<>" to listOf(Operator(BOOL, BOOL), Operator(BOOL, LONG), Operator(BOOL, DOUBLE)),

    "<" to listOf(Operator(BOOL, DOUBLE), Operator(BOOL, LONG)),
    "<=" to listOf(Operator(BOOL, DOUBLE), Operator(BOOL, LONG)),
    ">=" to listOf(Operator(BOOL, DOUBLE), Operator(BOOL, LONG)),
    ">" to listOf(Operator(BOOL, DOUBLE), Operator(BOOL, LONG)),

    "+" to listOf(Operator(DOUBLE, DOUBLE), Operator(LONG, LONG)),
    "-" to listOf(Operator(DOUBLE, DOUBLE), Operator(LONG, LONG)),
    "*" to listOf(Operator(DOUBLE, DOUBLE), Operator(LONG, LONG)),
    "/" to listOf(Operator(DOUBLE, DOUBLE), Operator(LONG, LONG)),
    "^" to listOf(Operator(DOUBLE, DOUBLE), Operator(LONG, LONG))
))
