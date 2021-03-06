package org.explang.interpreter

import org.explang.common.mapArr
import org.explang.compiler.*
import org.explang.intermediate.*
import org.explang.syntax.ExTree
import org.explang.syntax.FuncType

data class EvalResult(val value: Any)

val UNRESOLVED = EvalResult(object {
  override fun toString() = "UNRESOLVED"
})
val NULL = EvalResult(object {
  override fun toString() = "NULL"
})

class EvalError(msg: String, val tree: ITree?) : Exception(msg)

class Interpreter(
    private val printAnalysis: Boolean = false
) {

  /**
   * Interprets a syntax tree directly.
   *
   * @param tree the tree to interpret
   * @param env environment symbols, including built-ins and external data
   */
  @Throws(EvalError::class)
  fun evaluate(tree: ExTree, env: Environment): EvalResult {
    val compilation = compile(tree, env)
    return evaluate(compilation, env)
  }

  fun compile(tree: ExTree, env: Environment): Compiler.CompilationResult {
    val translator = SyntaxTranslator()
    val intermediate = translator.translate(tree)
    val compiler = Compiler()
    val compilation = compiler.compile(intermediate, env)
    if (printAnalysis) {
      println("*Analysis*")
      println(compilation)
    }
    return compilation
  }

  fun evaluate(compilation: Compiler.CompilationResult, env: Environment): EvalResult {
    return compilation.tree.accept(DirectInterpreter(compilation.resolver, env))
  }
}

private class DirectInterpreter(val resolver: Resolver, val env: Environment) :
    ITree.Visitor<EvalResult>, CallContext {

  private val stack = mutableListOf(Frame())

  @Suppress("UNCHECKED_CAST")
  override fun visitCall(call: ICall): EvalResult {
    val callee = call.callee.accept(this).value as Callable
    val args = call.args.mapArr(EvalResult::class) { it.accept(this) }
    return callee.invoke(this, args)
  }

  override fun visitIf(iff: IIf): EvalResult {
    val test = iff.test.accept(this)
    // Short-circuit evaluation, evaluates on the the relevant branch.
    return if (test.value as Boolean) {
      iff.left.accept(this)
    } else {
      iff.right.accept(this)
    }
  }

  override fun visitLet(let: ILet): EvalResult {
    val top = stack.last()
    // Copy the frame to add new local bindings, lexical scoping.
    stack[stack.lastIndex] = top.copy()

    // Add symbols for functions to frame before visiting bound value (for [mutual] recursion).
    for (binding in let.bindings) {
      if (binding.value.type is FuncType)
        stack.last().setLocal(binding.symbol.name, UNRESOLVED)
    }

    // Visit bound values, and remember the functions so they can be set in closures after all are resolved.
    val functions = mutableMapOf<String, EvalResult>()
    for (binding in let.bindings) {
      val r = binding.accept(this)
      // The second part of the test is needed to distinguish builtins from functions.
      // Would not be necessary if they shared an interface (and the builtins could ignore the call).
      if (binding.value.type is FuncType && r.value is Function) {
        functions[binding.symbol.name] = r
      }
    }

    // Set function references in closures for recursive reference.
    for ((_, v) in functions) {
      (v.value as Function).resolveClosure(functions)
    }

    try {
      return let.bound.accept(this)
    } finally {
      // Restore the original frame.
      stack[stack.lastIndex] = top
    }
  }

  override fun visitBinding(binding: IBinding): EvalResult {
    // Note: the symbol node is not visited.
    val value = binding.value.accept(this)
    stack.last().setLocal(binding.symbol.name, value)
    return value
  }

  override fun visitLambda(lambda: ILambda): EvalResult {
    // Function bodies can capture non-local values. The references are evaluated at the time
    // the function is defined. The value is closed over, not the reference.
    // At call time, the values are copied from the closure into the callee frame.
    val captured = resolver.captured(lambda)
    val frame = stack.last()
    val closure = mutableMapOf<String, EvalResult>()
    captured.forEach {
      when (it) {
        is Scope.Resolution.Local -> closure[it.identifier] = frame.getLocal(it.identifier)
        is Scope.Resolution.Closure -> closure[it.identifier] = frame.getClosure(it.identifier)
        is Scope.Resolution.Argument -> closure[it.identifier] = frame.getArg(it.index)
        is Scope.Resolution.Unresolved ->
          throw EvalError("Unbound capture ${it.symbol}", lambda)
        is Scope.Resolution.Environment ->
          throw EvalError("Capture ${it.symbol} is a builtin", lambda)
      }
    }
    return EvalResult(Function(lambda.body, closure))
  }

  override fun visitParameter(parameter: IParameter): EvalResult {
    throw EvalError("not used", parameter)
  }

  override fun visitLiteral(literal: ILiteral<*>): EvalResult {
    return EvalResult(literal.value)
  }

  override fun visitSymbol(symbol: ISymbol): EvalResult {
    // Symbols should all be resolved to args, locals, closure or builtins.
    throw EvalError("Unexpected symbol $symbol", symbol)
  }

  override fun visitArgRead(read: IArgRead): EvalResult {
    return stack.last().getArg(read.index)
  }

  override fun visitLocalRead(read: ILocalRead): EvalResult {
    return stack.last().getLocal(read.name)
  }

  override fun visitClosureRead(read: IClosureRead): EvalResult {
    return stack.last().getClosure(read.name)
  }

  override fun visitBuiltin(builtin: IBuiltin<*>): EvalResult {
    return EvalResult(builtin.value)
  }

  override fun visitNil(n: INil) = NULL

  ///// EvalContext implementation /////

  override fun evaluate(tree: ITree): EvalResult {
    return tree.accept(this)
  }

  override fun pushFrame(frame: Frame) {
    stack.add(frame)
  }

  override fun popFrame() {
    if (stack.isEmpty()) {
      throw EvalError("stack is empty", null)
    }
    stack.removeAt(stack.lastIndex)
  }
}