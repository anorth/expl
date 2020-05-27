package org.explang.interpreter

import org.explang.analysis.Analyzer
import org.explang.syntax.ExTree

interface EvalContext : ExTree.Visitor<Analyzer.Tag, EvalResult> {
  fun call(callee: Callable, args: List<EvalResult>): EvalResult
}

interface Callable {
  fun closure(): Map<String, EvalResult>
  operator fun invoke(frame: Frame, ctx: EvalContext): EvalResult
}

class Function(
    val body: ExTree<Analyzer.Tag>,
    // The function closure has to be mutable in order to support setting recursive and mutually-recursive
    // values in it after the function is defined (but before it's called).
    val closure: MutableMap<String, EvalResult>) : Callable {
  override fun closure() = closure

  override fun invoke(frame: Frame, ctx: EvalContext): EvalResult {
    return body.accept(ctx)
  }

  fun resolveClosure(name: String, value: EvalResult) {
    val prev = closure[name]
    check(prev == null || prev == NIL) { "Name $name is not NIL in $closure" }
    closure[name] = value
  }
}

class Frame(
    val args: List<EvalResult> = listOf(),
    val locals: MutableMap<String, EvalResult> = mutableMapOf(),
    val closure: Map<String, EvalResult> = mapOf()
) {
  fun getArg(i: Int) = args[i]
  fun getLocal(k: String) = locals[k]!!
  fun getClosure(k: String) = closure[k]!!

  fun setLocal(k: String, v: EvalResult) {
    locals[k] = v
  }

  fun copy() = Frame(args, locals.toMutableMap(), closure)
}

