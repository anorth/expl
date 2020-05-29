package org.explang.interpreter

import org.explang.analysis.Analyzer
import org.explang.analysis.Scope
import org.explang.array.ArrayValue
import org.explang.array.LongRangeValue
import org.explang.syntax.*
import kotlin.math.pow

data class EvalResult(val value: Any)

val NIL = EvalResult(object {
  override fun toString() = "NIL"
})

class EvalError(msg: String, val tree: ExTree<Analyzer.Tag>?) : Exception(msg)

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
  fun evaluate(tree: ExTree<Analyzer.Tag>, env: Environment): EvalResult {
    val envTypes = env.types()
    val analyzer = Analyzer()
    val analysis = analyzer.analyze(tree, envTypes)
    if (printAnalysis) {
      println("*Analysis*")
      println(analysis)
    }

    return DirectInterpreter(analysis, env).visit(tree)
  }
}

private class DirectInterpreter(val analysis: Analyzer.Analysis, val env: Environment) :
    ExTree.Visitor<Analyzer.Tag, EvalResult>, CallContext {

  private val resolver = analysis.resolver
  private val stack = mutableListOf(Frame())

  override fun visitCall(call: ExCall<Analyzer.Tag>): EvalResult {
    val callee = call.callee.accept(this).value as Callable
    val args = call.args.map { it.accept(this) }
    return callee.call(this, args)
  }

  override fun visitIndex(index: ExIndex<Analyzer.Tag>): EvalResult {
    val indexee = index.indexee.accept(this).value as ArrayValue<*>
    val indexer = index.indexer.accept(this).value
    val idxType = index.indexer.tag.type
    return when {
      idxType.satisfies(Type.LONG) -> EvalResult(indexee.get(Math.toIntExact(indexer as Long))!!)
      idxType.satisfies(Type.range(Type.LONG)) -> EvalResult(indexee.slice(indexer as LongRangeValue))
      else -> throw EvalError("Can't index $indexee with $indexer", index)
    }
  }

  override fun visitUnaryOp(op: ExUnaryOp<Analyzer.Tag>): EvalResult {
    val left = op.operand.accept(this)
    val operator = UNOPS[op.operand.tag.type]!![op.operator]
    return if (operator != null)
      operator(left)
    else throw EvalError("unknown unup ${op.operator} for type ${op.operand.tag.type}", op)
  }

  override fun visitBinaryOp(op: ExBinaryOp<Analyzer.Tag>): EvalResult {
    val left = op.left.accept(this)
    val right = op.right.accept(this)
    val operator = when (op.left.tag.type) {
      PrimType.BOOL -> BOOLOPS[op.operator]
      PrimType.LONG -> LONGOPS[op.operator]
      PrimType.DOUBLE -> DOUBLEOPS[op.operator]
      else -> throw EvalError("unknown binop type", op)
    }
    return if (operator != null)
      operator(left, right)
    else throw EvalError("unknown operator ${op.operator} for type ${op.left.tag.type}", op)
  }

  override fun visitRangeOp(op: ExRangeOp<Analyzer.Tag>): EvalResult {
    val first = op.first?.accept(this)?.value as Long?
    val last = op.last?.accept(this)?.value as Long?
    val step = op.step?.accept(this)?.value as Long?
    return EvalResult(LongRangeValue.of(first, last, step))
  }

  override fun visitIf(iff: ExIf<Analyzer.Tag>): EvalResult {
    val test = iff.test.accept(this)
    // Short-circuit evaluation, evaluates on the the relevant branch.
    return if (test.value as Boolean) {
      iff.left.accept(this)
    } else {
      iff.right.accept(this)
    }
  }

  override fun visitLet(let: ExLet<Analyzer.Tag>): EvalResult {
    val top = stack.last()
    // Copy the frame to add new local bindings, lexical scoping.
    stack[stack.lastIndex] = top.copy()

    // Add symbols for functions to frame before visiting bound value (for [mutual] recursion).
    for (binding in let.bindings) {
      if (binding.value.tag.type.isFunc())
        stack.last().setLocal(binding.symbol.name, NIL)
    }

    // Visit bound values, and remember the functions so they can be set in closures after all are resolved.
    val functions = mutableMapOf<String, EvalResult>()
    for (binding in let.bindings) {
      val r = binding.accept(this)
      // The second part of the test is needed to distinguish builtins from functions.
      // Would not be necessary if they shared an interface (and the builtins could ignore the call).
      if (binding.value.tag.type.isFunc() && r.value is Function) {
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

  override fun visitBinding(binding: ExBinding<Analyzer.Tag>): EvalResult {
    // Note: the symbol node is not visited.
    val value = binding.value.accept(this)
    stack.last().setLocal(binding.symbol.name, value)
    return value
  }

  override fun visitLambda(lambda: ExLambda<Analyzer.Tag>): EvalResult {
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

  override fun visitParameter(parameter: ExParameter<Analyzer.Tag>): EvalResult {
    throw EvalError("not used", parameter)
  }

  override fun visitLiteral(literal: ExLiteral<Analyzer.Tag, *>): EvalResult {
    return EvalResult(literal.value!!)
  }

  override fun visitSymbol(symbol: ExSymbol<Analyzer.Tag>): EvalResult {
    val frame = stack.last()
    val resolution = resolver.resolve(symbol)
    val id = resolution.identifier
    return when (resolution) {
      is Scope.Resolution.Argument -> frame.getArg(resolution.index)
      is Scope.Resolution.Local -> frame.getLocal(id)
      is Scope.Resolution.Closure -> frame.getClosure(id)
      is Scope.Resolution.Environment -> EvalResult(env.getBuiltin(id))
      is Scope.Resolution.Unresolved ->
        throw EvalError("Unbound symbol ${resolution.symbol}", symbol)
    }
  }

  ///// EvalContext implementation /////

  override fun evaluate(tree: ExTree<Analyzer.Tag>): EvalResult {
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

private val UNOPS = mapOf(
    Type.BOOL to mapOf(
        "not" to { a: EvalResult -> EvalResult(!(a.value as Boolean)) }
    ),
    Type.LONG to mapOf(
        "-" to { a: EvalResult -> EvalResult(-(a.value as Long)) }
    ),
    Type.DOUBLE to mapOf(
        "-" to { a: EvalResult -> EvalResult(-(a.value as Double)) }
    )
)

private val BOOLOPS = mapOf(
    "==" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Boolean == b.value as Boolean) },
    "<>" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Boolean != b.value as Boolean) },
    "and" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Boolean && b.value as Boolean) },
    "or" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Boolean || b.value as Boolean) },
    "xor" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Boolean xor b.value as Boolean) }
)

private val LONGOPS = mapOf(
    "^" to { a: EvalResult, b: EvalResult ->
      EvalResult((a.value as Long).toDouble().pow((b.value as Long).toDouble()).toLong())
    },
    "*" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Long * b.value as Long) },
    "/" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Long / b.value as Long) },
    "+" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Long + b.value as Long) },
    "-" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Long - b.value as Long) },
    "<" to { a: EvalResult, b: EvalResult -> EvalResult((a.value as Long) < b.value as Long) },
    "<=" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Long <= b.value as Long) },
    ">" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Long > b.value as Long) },
    ">=" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Long >= b.value as Long) },
    "==" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Long == b.value as Long) },
    "<>" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Long != b.value as Long) }
)

private val DOUBLEOPS = mapOf(
    "^" to { a: EvalResult, b: EvalResult -> EvalResult((a.value as Double).pow((b.value as Double))) },
    "*" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Double * b.value as Double) },
    "/" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Double / b.value as Double) },
    "+" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Double + b.value as Double) },
    "-" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Double - b.value as Double) },
    "<" to { a: EvalResult, b: EvalResult -> EvalResult((a.value as Double) < b.value as Double) },
    "<=" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Double <= b.value as Double) },
    ">" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Double > b.value as Double) },
    ">=" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Double >= b.value as Double) },
    "==" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Double == b.value as Double) },
    "<>" to { a: EvalResult, b: EvalResult -> EvalResult(a.value as Double != b.value as Double) }
)
