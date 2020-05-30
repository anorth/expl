package org.explang.compiler

import org.explang.intermediate.*
import org.explang.syntax.FuncType
import org.explang.syntax.Type

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
    private val builtins: Map<String, List<Type>>
) : ITree.Visitor<Unit> {
  data class Result(
      val resolutions: Map<Scope.Resolution, Type>
  )

  companion object {
    fun computeTypes(tree: ITree, resolver: Resolver, builtins: Map<String, List<Type>>): Result {
      val checker = TypeChecker(resolver, builtins)
      tree.accept(checker)
      return Result(checker.symbolTypes)
    }
  }

  // Type inference is strictly bottom-up for now. All literals and lambda parameters must be
  // concretely typed.

  // Types inferred for resolved symbols.
  // This is used to propagate types to each symbol occurrence sharing the same resolution.
  private val symbolTypes = mutableMapOf<Scope.Resolution, Type>()

  override fun visitCall(call: ICall) {
    // Visit callee and argument expressions. The arguments must become fully typed, and the callee must at least
    // have some candidates.
    visitChildren(call)
    val callee = call.callee
    val args = call.args

    if (callee.type == Type.NONE) {
      // Hack special handling for "*" in range operators until we have nullable types or optional arguments.
      // Set all args to the same type; if none specify a type ("*:*"), use LONG.
      val nullArgType: Type = args.firstOrNull { it.type != Type.NONE }?.type ?: Type.LONG
      for (arg in args) {
        if (arg.type == Type.NONE) {
          arg.type = nullArgType
        }
      }
      val argTypes = args.map(ITree::type)

      for (candidate in callee.typeCandidates) {
        if (!candidate.isFunc()) {
          throw CompileError("Type candidate for callee $callee is ${candidate}, not a function", callee)
        }
        if (candidate.asFunc().parameters().contentEquals(argTypes.toTypedArray())) {
          callee.type = candidate
          break
        }
      }
      if (callee.type == Type.NONE) {
        throw CompileError("No type for $callee", callee)
      }
    }

    val calleeType = callee.type as? FuncType
        ?: throw CompileError("Callee $callee is ${callee.type}, not a function", callee)
    val formalParamTypes = calleeType.parameters()
    check(call, args.size == formalParamTypes.size) {
      "Expected ${formalParamTypes.size} arguments, got ${args.size}"
    }
    for (i in formalParamTypes.indices) {
      check(args[i], args[i].type.satisfies(formalParamTypes[i])) {
        "Argument $i expected ${formalParamTypes[i]}, got ${args[i].type}"
      }
    }

    call.type = calleeType.result()
  }

  override fun visitIf(iff: IIf) {
    visitChildren(iff)
    check(iff, iff.test.type == Type.BOOL) {
      "Condition has type ${iff.test.type}, should be boolean"
    }

    checkTypesMatch(iff, iff.left, iff.right)

    // Propagate branch type upwards
    iff.type = iff.left.type
  }

  override fun visitLet(let: ILet) {
    // Set types for bound lambdas carrying type annotations, which may recursively call each other or themselves.
    // Set the type for the symbol *before* visiting the bodies.
    for (binding in let.bindings) {
      if (binding.value is ILambda) {
        val retType = binding.value.returnType
        val paramTypes = binding.value.parameters.map(IParameter::type)
        if (retType != Type.NONE && paramTypes.none { it == Type.NONE }) {
          val resolution = resolver.resolve(binding.symbol)
          symbolTypes[resolution] = Type.function(retType, *paramTypes.toTypedArray())
        }
      }
    }

    visitChildren(let) // Visits bindings and then bound expression
    // Propagate bound expression type upwards
    let.type = let.bound.type
  }

  override fun visitBinding(binding: IBinding) {
    val resolution = resolver.resolve(binding.symbol)
    binding.value.accept(this)
    val valueType = binding.value.type
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

  override fun visitLambda(lambda: ILambda) {
    // We could populate types for enclosed symbols here,
    // including the name of the lambda for recursive calls?

    visitChildren(lambda) // Visit parameters and then body
    check(lambda, lambda.returnType == Type.NONE || lambda.body.type == lambda.returnType) {
      "Inconsistent return type for lambda, annotated ${lambda.returnType} " +
          "but returns ${lambda.body.type}"
    }

    lambda.type = Type.function(lambda.body.type,
        *lambda.parameters.map(IParameter::type).toTypedArray())
  }

  override fun visitParameter(parameter: IParameter) {
    assert(parameter.type != Type.NONE) {
      "Unexpected parameter type ${parameter.type}"
    }
    val resolution = resolver.resolve(parameter.symbol)
    assert(resolution !in symbolTypes) { "Parameter binding resolved before definition" }
    symbolTypes[resolution] = parameter.type
  }

  override fun visitLiteral(literal: ILiteral<*>) {
    val actual = when (literal.implType) {
      Boolean::class.java -> Type.BOOL
      Long::class.java -> Type.LONG
      Double::class.java -> Type.DOUBLE
      else -> throw CompileError("Unrecognized literal type ${literal.type}", literal)
    }
    literal.type = actual
  }

  override fun visitSymbol(symbol: ISymbol) {
    // Follow closure chain to a binding or argument to find the type.
    // This is a bit messy. It might be better to change the resolver's captures to include the
    // closure resolution, and set the closure symbol types with the enclosing function definition.
    val initialResolution = resolver.resolve(symbol)
    var resolution = initialResolution
    while (resolution is Scope.Resolution.Closure) {
      resolution = resolution.capture
    }
    if (resolution is Scope.Resolution.Environment) {
      val candidates = builtins[resolution.identifier]
      if (candidates != null && candidates.isNotEmpty()) {
        symbol.typeCandidates.addAll(candidates)
        if (candidates.size == 1) {
          symbol.type = candidates[0]
        }
      } else {
        throw CompileError("No type for $resolution", symbol)
      }
    } else {
      val type = symbolTypes[resolution]
      if (type != null) {
        // Set the type for the free closure resolutions.
        resolution = initialResolution
        while (resolution is Scope.Resolution.Closure) {
          symbolTypes[resolution] = type
          resolution = resolution.capture
        }
        symbol.type = type
      } else {
        throw CompileError("No type for $resolution", symbol)
      }
    }
  }

  override fun visitBuiltin(builtin: IBuiltin<*>) {}

  override fun visitNull(n: INull) {}
}

///// Private implementation /////

// Enforces two trees have the same type.
private fun checkTypesMatch(parent: ITree, left: ITree, right: ITree) {
  check(parent, left.type == right.type) {
    "Incompatible types for $parent: ${left.type}, ${right.type}"
  }
}
