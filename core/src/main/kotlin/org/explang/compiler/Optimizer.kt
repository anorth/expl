package org.explang.compiler

import org.explang.intermediate.*

class Optimizer(private val resolver: LookupResolver, private val env: CompilationEnvironment) {
  fun optimize(tree: ITree): ITree {
    val opt = tree.accept(Inliner(resolver, env))
    return opt
  }
}

private class Inliner(
    private val resolver: LookupResolver,
    private val env: CompilationEnvironment
) : TreeTransformer(resolver) {
  override fun visitSymbol(symbol: ISymbol): ITree {
    val resolution = resolver.resolve(symbol)
    return when (resolution) {
      is Scope.Resolution.Environment -> {
        // Statically resolve the lookup into the environment.
        env.builtin(symbol.name, symbol.type, symbol.syntax)
      }
//      is Scope.Resolution.Argument -> TODO()
//      is Scope.Resolution.Local -> TODO()
//      is Scope.Resolution.Closure -> TODO()
//      is Scope.Resolution.Unresolved -> TODO()
      else -> {
        symbol
      }
    }
  }
}

private open class TreeTransformer(private val resolver: LookupResolver) : ITree.Visitor<ITree> {
  override fun visitCall(call: ICall): ITree {
    return call.with(call.callee.accept(this), call.args.map { it.accept(this) })
  }

  override fun visitIf(iff: IIf): ITree {
    return iff.with(iff.test.accept(this), iff.left.accept(this), iff.right.accept(this))
  }

  override fun visitLet(let: ILet): ITree {
    return let.with(let.bindings.map(this::visitBinding), let.bound.accept(this))
  }

  override fun visitBinding(binding: IBinding): IBinding {
    // Binding LHS symbol is not traversed, retains identity.
    return binding.with(binding.symbol, binding.value.accept(this))
  }

  override fun visitLambda(lambda: ILambda): ITree {
    val new = lambda.with(lambda.parameters.map(this::visitParameter), lambda.returnType, lambda.body.accept(this))
    resolver.rename(lambda, new)
    return new
  }

  // Parameter symbol is not traversed, retains identity.
  override fun visitParameter(parameter: IParameter) = parameter

  override fun visitLiteral(literal: ILiteral<*>): ITree = literal

  override fun visitSymbol(symbol: ISymbol): ITree = symbol

  override fun visitBuiltin(builtin: IBuiltin<*>): ITree = builtin

  override fun visitNull(n: INull): ITree = n
}