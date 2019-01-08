package org.explang.truffle.compiler

import org.explang.syntax.BINARY_OPERATORS
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
import org.explang.syntax.UNARY_OPERATORS

/**
 * Infers types for all nodes in the syntax tree.
 *
 * Types are first assigned to literals in leaves of the AST, then propagated up. Nodes with
 * children then propagate types inferred from siblings back down the tree.
 *
 * This doesn't yet support any kind of polymorphism or type parameters.
 */
class TypeChecker(
    private val resolver: SymbolResolver
) : ExTree.Visitor<Analyzer.Tag, Unit> {

  interface SymbolResolver {
    fun resolve(symbol: ExSymbol<*>): Scope.Resolution
  }

  data class Result(
      val bindings: Map<Scope.Resolution, Type>
  )

  companion object {
    fun computeTypes(tree: ExTree<Analyzer.Tag>, resolver: SymbolResolver): Result {
      val checker = TypeChecker(resolver)
      checker.visit(tree)
      return Result(checker.bindings)
    }
  }

  // Types inferred for resolved symbols.
  // This is used to propagate types to each symbol occurrence sharing the same resolution.
  private val bindings = mutableMapOf<Scope.Resolution, Type>()

  // For each visitXxx method
  // - if the node type is NONE, attempt to infer it from children
  // - if the node type is not NONE, push it down to any untyped children

  override fun visitCall(call: ExCall<Analyzer.Tag>) {
    TODO("not implemented")
  }

  override fun visitUnaryOp(unop: ExUnaryOp<Analyzer.Tag>) {
    if (unop.typeTag != Type.NONE) {
      // Propagate inferred type to operand.
      val ops = UNARY_OPERATORS.withResultType(unop.operator, unop.typeTag)
      ops.singleOrNull()?.let { unop.operand.typeTag = it.operandType }
    }
    visit(unop.operand)
    if (unop.operand.typeTag != Type.NONE) {
      // Propagate operand type to operator.
      val ops = UNARY_OPERATORS.withOperandType(unop.operator, unop.operand.typeTag)
      ops.singleOrNull()?.let { unop.typeTag = it.resultType }
    }
  }

  override fun visitBinaryOp(binop: ExBinaryOp<Analyzer.Tag>) {
    if (binop.typeTag != Type.NONE) {
      // Propagate inferred type to operands, if uniquely determined
      val ops = BINARY_OPERATORS.withResultType(binop.operator, binop.typeTag)
      ops.singleOrNull()?.let {
        binop.left.typeTag = it.operandType
        binop.right.typeTag = it.operandType
      }
    }
    visitChildren(binop, Unit)

    // Propagate type between operands (binops all have same-typed operands)
    matchTypes(binop, binop.left, binop.right)

    // Propagate operator result type upward
    if (binop.typeTag == Type.NONE) {
      val ops = BINARY_OPERATORS.withOperandType(binop.operator, binop.left.typeTag)
      ops.singleOrNull()?.let {
        binop.typeTag = it.resultType
      }
    }
  }

  override fun visitIf(iff: ExIf<Analyzer.Tag>) {
    // Push boolean type down to test
    iff.test.typeTag = Type.BOOL
    if (iff.typeTag != Type.NONE) {
      // Push inferred type to both branches
      iff.left.typeTag = iff.typeTag
      iff.right.typeTag = iff.typeTag
    }
    visitChildren(iff, Unit)

    // Propagate type between branches.
    matchTypes(iff, iff.left, iff.right)

    // Propagate branch type upwards
    if (iff.typeTag == Type.NONE) {
      iff.typeTag = iff.left.typeTag
    }
  }

  override fun visitLet(let: ExLet<Analyzer.Tag>) {
    if (let.typeTag != Type.NONE) {
      // Push inferred type down to bound expression
      let.bound.typeTag = let.typeTag
    }
    val resolutions = let.bindings.map { resolver.resolve(it.symbol) }
    val unTyped = resolutions.filter { it !in bindings }.toMutableSet()
    while (unTyped.isNotEmpty()) {
      // Visit binding resolutions and then bound expression, each cycle of of which could resolve
      // another symbol's type until reaching a fixed point.
      visitChildren(let, Unit) // Visits bindings and then bound expression
      unTyped.removeIf { it in bindings }
      // FIXME: prevent infinite loop here
    }
    // Propagate bound expression type upwards
    if (let.typeTag == Type.NONE) {
      let.typeTag = let.bound.typeTag
    }
  }

  override fun visitBinding(binding: ExBinding<Analyzer.Tag>) {
    // The binding expression remains untyped, but the bound symbol may have a type pushed
    // down or inferred up.
    resolver.resolve(binding.symbol).let { resolution ->
      val resolvedType = bindings[resolution]
      if (resolvedType != null) {
        binding.value.typeTag = resolvedType
      }
    }

    binding.value.accept(this)

    val valueType = binding.value.typeTag
    // Propagate value type to the symbol resolution
    if (valueType != Type.NONE) {
      val resolution = resolver.resolve(binding.symbol)
      if (resolution !in bindings) {
        bindings[resolution] = valueType
      } else {
        check(binding, bindings[resolution] == valueType) {
          "Inconsistent types for symbol ${binding.symbol}: ${bindings[resolution]}, $valueType"
        }
      }
    }
  }

  override fun visitLambda(lambda: ExLambda<Analyzer.Tag>) {
    TODO("not implemented")
//    lambda.tag.type =
//        Type.function(lambda.body.tag.type, *lambda.parameters.map { it.tag.type }.toTypedArray())

  }

  override fun visitLiteral(literal: ExLiteral<Analyzer.Tag, *>) {
    val actual = when (literal.type) {
      Boolean::class.java -> Type.BOOL
      Double::class.java -> Type.DOUBLE
      else -> throw CompileError("Unrecognized literal type ${literal.type}", literal)
    }

    if (literal.tag.type == Type.NONE) {
      literal.typeTag = actual
    } else if (literal.typeTag != actual) {
      // An incompatible type was pushed down
      throw CompileError("Unexpected literal type $actual, expected ${literal.typeTag}", literal)
    }
    assert(literal.tag.type != Type.NONE)
  }

  override fun visitSymbol(symbol: ExSymbol<Analyzer.Tag>) {
    val pushedType = symbol.typeTag
    val resolution = resolver.resolve(symbol)
    if (pushedType == Type.NONE) {
      // Read type from symbol resolution if known
      bindings[resolution]?.let { resolvedType ->
        symbol.typeTag = resolvedType
      }
    } else {
      // Push type onto symbol resolution
      if (resolution !in bindings) {
        bindings[resolution] = pushedType
      } else {
        check(symbol, bindings[resolution] == pushedType) {
          "Conflicting types for $symbol: ${bindings[resolution]}, $pushedType"
        }
      }
    }
  }

  ///// Private implementation /////

  // Enforces two trees have the same type, propagating from one to the other if only
  // one is set.
  private fun matchTypes(parent: ExTree<Analyzer.Tag>, left: ExTree<Analyzer.Tag>,
      right: ExTree<Analyzer.Tag>) {
    if (left.typeTag != right.typeTag) {
      when {
        left.typeTag == Type.NONE -> {
          left.typeTag = right.typeTag
          visit(left)
        }
        right.typeTag == Type.NONE -> {
          right.typeTag = left.typeTag
          visit(right)
        }
        else -> throw CompileError("Incompatible types: ${left.typeTag}, ${right.typeTag}", parent)
      }
    }
    assert(left.typeTag == right.typeTag)
  }
}

private var ExTree<Analyzer.Tag>.typeTag: Type
  get() = tag.type
  set(value) {
    if (tag.type == Type.NONE) {
      tag.type = value
    } else {
      check(this, tag.type == value) {
        "Conflicting types for $this: ${tag.type}, $value"
      }
    }
  }

/** Returns the singleton element if the iterable has exactly one. */
private fun <T> Collection<T>.singleOrNull() = if (this.size == 1) this.first() else null
