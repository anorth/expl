package org.explang.interpreter

import org.explang.intermediate.ITree

class Function(
    val body: ITree,
    // The function closure has to be mutable in order to support setting recursive and mutually-recursive
    // values in it after the function is defined (but before it's called).
    val closure: MutableMap<String, EvalResult>) : Callable {

  override fun call(ctx: CallContext, args: List<EvalResult>): EvalResult {
    val frame = Frame(args, closure = closure)
    ctx.pushFrame(frame)
    try {
      return ctx.evaluate(body)
    } finally {
      ctx.popFrame()
    }
  }

  fun resolveClosure(resolved: Map<String, EvalResult>) {
    for ((n, v) in resolved) {
      resolvedClosedValue(n, v)
    }
  }

  private fun resolvedClosedValue(name: String, value: EvalResult) {
    val prev = closure[name]
    // Replace value only if it was set explicitly to be resolved later.
    if (prev == UNRESOLVED) {
      closure[name] = value
    }
  }
}
