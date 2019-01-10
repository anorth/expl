package org.explang.syntax

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
    "-" to listOf(Operator(Type.DOUBLE, Type.DOUBLE)),
    "+" to listOf(Operator(Type.DOUBLE, Type.DOUBLE)))
)

val BINARY_OPERATORS = Operators(mapOf(
    "==" to listOf(Operator(Type.BOOL, Type.BOOL), Operator(Type.BOOL, Type.DOUBLE)),
    "<>" to listOf(Operator(Type.BOOL, Type.BOOL), Operator(Type.BOOL, Type.DOUBLE)),

    "<" to listOf(Operator(Type.BOOL, Type.DOUBLE)),
    "<=" to listOf(Operator(Type.BOOL, Type.DOUBLE)),
    ">=" to listOf(Operator(Type.BOOL, Type.DOUBLE)),
    ">" to listOf(Operator(Type.BOOL, Type.DOUBLE)),

    "+" to listOf(Operator(Type.DOUBLE, Type.DOUBLE)),
    "-" to listOf(Operator(Type.DOUBLE, Type.DOUBLE)),
    "*" to listOf(Operator(Type.DOUBLE, Type.DOUBLE)),
    "/" to listOf(Operator(Type.DOUBLE, Type.DOUBLE)),
    "^" to listOf(Operator(Type.DOUBLE, Type.DOUBLE)))
)
