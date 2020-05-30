package org.explang.compiler

import org.explang.common.mapArr
import org.explang.intermediate.*

class Optimizer(private val resolver: LookupResolver, private val env: CompilationEnvironment) {
  fun optimize(tree: ITree): ITree {
    val opt = tree.accept(SymbolResolver(resolver, env))
    return opt
  }
}

private class SymbolResolver(
    private val resolver: LookupResolver,
    private val env: CompilationEnvironment
) : TreeTransformer(resolver) {
  override fun visitSymbol(symbol: ISymbol): ITree {
    val resolution = resolver.resolve(symbol)
    // Statically resolve all symbols.
    return when (resolution) {
      is Scope.Resolution.Argument -> IArgRead(symbol.syntax, symbol.type, symbol.name, resolution.index)
      is Scope.Resolution.Environment -> env.builtin(symbol.name, symbol.type, symbol.syntax)
      is Scope.Resolution.Local -> ILocalRead(symbol.syntax, symbol.type, symbol.name)
      is Scope.Resolution.Closure -> IClosureRead(symbol.syntax, symbol.type, symbol.name)
      is Scope.Resolution.Unresolved -> throw CompileError("Unresolved symbol $symbol", symbol)
    }
  }
}

private open class TreeTransformer(private val resolver: LookupResolver) : ITree.Visitor<ITree> {
  override fun visitCall(call: ICall): ITree {
    return call.with(call.callee.accept(this), call.args.mapArr(ITree::class.java) { it.accept(this) })
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
  override fun visitArgRead(read: IArgRead): ITree = read
  override fun visitLocalRead(read: ILocalRead): ITree = read
  override fun visitClosureRead(read: IClosureRead): ITree = read
  override fun visitBuiltin(builtin: IBuiltin<*>): ITree = builtin
  override fun visitNull(n: INull): ITree = n
}