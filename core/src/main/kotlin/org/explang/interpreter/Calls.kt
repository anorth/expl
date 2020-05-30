package org.explang.interpreter

import org.explang.intermediate.ITree

interface CallContext {
  fun evaluate(tree: ITree): EvalResult
  fun pushFrame(frame: Frame)
  fun popFrame()
}

interface Callable {
  fun call(ctx: CallContext, args: List<EvalResult>): EvalResult
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