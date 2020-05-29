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

  fun resolveClosure(resolved: Map<String, EvalResult>) {
    for ((n, v) in resolved) {
      resolvedClosedValue(n, v)
    }
  }

  private fun resolvedClosedValue(name: String, value: EvalResult) {
    val prev = closure[name]
    // Replace value only if it was set explicitly to be resolved later.
    if (prev == NIL) {
      closure[name] = value
    }
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

