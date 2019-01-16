package org.explang.truffle.compiler

import org.explang.syntax.BINARY_OPERATORS
import org.explang.syntax.ExBinaryOp
import org.explang.syntax.ExBinding
import org.explang.syntax.ExCall
import org.explang.syntax.ExIf
import org.explang.syntax.ExLambda
import org.explang.syntax.ExLet
import org.explang.syntax.ExLiteral
import org.explang.syntax.ExParameter
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree
import org.explang.syntax.ExUnaryOp
import org.explang.syntax.FuncType
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
    private val resolver: Resolver,
    private val builtins: Map<String, Type>
) : ExTree.Visitor<Analyzer.Tag, Unit> {

  data class Result(
      val resolutions: Map<Scope.Resolution, Type>
  )

  companion object {
    fun computeTypes(tree: ExTree<Analyzer.Tag>, resolver: Resolver,
        builtins: Map<String, Type>): Result {
      val checker = TypeChecker(resolver, builtins)
      checker.visit(tree)
      return Result(checker.symbolTypes)
    }
  }

  // Type inference is strictly bottom-up for now. All literals and lambda parameters must be
  // concretely typed.

  // Types inferred for resolved symbols.
  // This is used to propagate types to each symbol occurrence sharing the same resolution.
  private val symbolTypes = mutableMapOf<Scope.Resolution, Type>()

  override fun visitCall(call: ExCall<Analyzer.Tag>) {
    // Visit callee and argument expressions, which must all become fully typed.
    visitChildren(call, Unit)

    val callee = call.callee
    val args = call.args
    val calleeType = callee.typeTag as? FuncType
        ?: throw CompileError("Callee $callee is not a function", callee)
    val formalParamTypes = calleeType.parameters()
    check(call, args.size == formalParamTypes.size) {
      "Expected ${formalParamTypes.size} arguments, got ${args.size}"
    }
    for (i in formalParamTypes.indices) {
      check(args[i], args[i].typeTag == formalParamTypes[i]) {
        "Argument $i expected ${formalParamTypes[i]}, got ${args[i]}"
      }
    }

    call.typeTag = calleeType.result()
  }

  override fun visitUnaryOp(unop: ExUnaryOp<Analyzer.Tag>) {
    visit(unop.operand)
    assert(unop.operand.typeTag != Type.NONE) { "Untyped operand" }
    val op = UNARY_OPERATORS.withOperandType(unop.operator, unop.operand.typeTag)
    unop.typeTag = op.resultType
  }

  override fun visitBinaryOp(binop: ExBinaryOp<Analyzer.Tag>) {
    visitChildren(binop, Unit)

    // All binops have similarly-typed operands
    checkTypesMatch(binop, binop.left, binop.right)

    // Propagate operator result type upward
    val op = BINARY_OPERATORS.withOperandType(binop.operator, binop.left.typeTag)
    binop.typeTag = op.resultType
  }

  override fun visitIf(iff: ExIf<Analyzer.Tag>) {
    visitChildren(iff, Unit)
    check(iff, iff.test.typeTag == Type.BOOL) {
      "Condition has type ${iff.test.typeTag}, should be boolean"
    }

    checkTypesMatch(iff, iff.left, iff.right)

    // Propagate branch type upwards
    iff.typeTag = iff.left.typeTag
  }

  override fun visitLet(let: ExLet<Analyzer.Tag>) {
    visitChildren(let, Unit) // Visits bindings and then bound expression
    // Propagate bound expression type upwards
    let.typeTag = let.bound.typeTag
  }

  override fun visitBinding(binding: ExBinding<Analyzer.Tag>) {
    val resolution = resolver.resolve(binding.symbol)
    // Special case for bound lambdas carrying type annotations, which may recursively call
    // themselves. Set the type for the symbol *before* visiting the body.
    if (binding.value is ExLambda && binding.value.annotation != Type.NONE) {
      symbolTypes[resolution] = binding.value.type()
    }

    visit(binding.value)
    val valueType = binding.value.typeTag
    assert(valueType != Type.NONE) { "No type for bound expression" }

    // The binding expression remains untyped, but the symbol resolution gets a type.
    // Propagate value type to the symbol resolution
    if (resolution !in symbolTypes) {
      symbolTypes[resolution] = valueType
    } else {
      check(binding, symbolTypes[resolution] == valueType) {
        "Inconsistent types for symbol ${binding.symbol}: ${symbolTypes[resolution]}, $valueType"
      }
    }
  }

  override fun visitLambda(lambda: ExLambda<Analyzer.Tag>) {
    // We could populate types for enclosed symbols here,
    // including the name of the lambda for recursive calls?

    visitChildren(lambda, Unit) // Visit parameters and then body
    check(lambda, lambda.annotation == Type.NONE || lambda.body.typeTag == lambda.annotation) {
      "Inconsistent return type for lambda, annotated ${lambda.annotation} " +
          "but returns ${lambda.body.typeTag}"
    }

    lambda.typeTag =  Type.function(lambda.body.typeTag,
      *lambda.parameters.map(ExParameter<*>::annotation).toTypedArray())
  }

  override fun visitParameter(parameter: ExParameter<Analyzer.Tag>) {
    assert(parameter.annotation != Type.NONE) {
      "Unexpected parameter annotation ${parameter.annotation}"
    }
    parameter.typeTag = parameter.annotation
    val resolution = resolver.resolve(parameter.symbol)
    assert(resolution !in symbolTypes) { "Parameter binding resolved before definition" }
    symbolTypes[resolution] = parameter.annotation
  }

  override fun visitLiteral(literal: ExLiteral<Analyzer.Tag, *>) {
    val actual = when (literal.type) {
      Boolean::class.java -> Type.BOOL
      Double::class.java -> Type.DOUBLE
      else -> throw CompileError("Unrecognized literal type ${literal.type}", literal)
    }
    literal.typeTag = actual
  }

  override fun visitSymbol(symbol: ExSymbol<Analyzer.Tag>) {
    // Follow closure chain to a binding or argument to find the type.
    // This is a bit messy. It might be better to change the resolver's captures to include the
    // closure resolution, and set the closure symbol types with the enclosing function definition.
    val initialResolution = resolver.resolve(symbol)
    var resolution = initialResolution
    while (resolution is Scope.Resolution.Closure) {
      resolution = resolution.capture
    }
    val type = if (resolution is Scope.Resolution.BuiltIn) {
      builtins[resolution.identifier]
    } else {
      symbolTypes[resolution]
    }
    if (type != null) {
      // Set the type for the free closure resolutions
      resolution = initialResolution
      while (resolution is Scope.Resolution.Closure) {
        symbolTypes[resolution] = type
        resolution = resolution.capture
      }
      symbol.typeTag = type
    } else {
      throw CompileError("No type for $resolution", symbol)
    }
  }

  ///// Private implementation /////

  // Enforces two trees have the same type.
  private fun checkTypesMatch(parent: ExTree<Analyzer.Tag>, left: ExTree<Analyzer.Tag>,
      right: ExTree<Analyzer.Tag>) {
    check(parent, left.typeTag == right.typeTag) {
      "Incompatible types for $parent: ${left.typeTag}, ${right.typeTag}"
    }
  }
}

private fun ExLambda<*>.type(): Type {
  return Type.function(annotation,
      *parameters.map(ExParameter<*>::annotation).toTypedArray())
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
