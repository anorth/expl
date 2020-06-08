package org.explang.interpreter

import org.explang.array.ArrayValue
import org.explang.array.BooleanArrayValue
import org.explang.array.DoubleArrayValue
import org.explang.array.DoubleRangeValue
import org.explang.array.LongArrayValue
import org.explang.array.LongRangeValue
import org.explang.array.ObjectArrayValue
import org.explang.array.RangeValue
import org.explang.syntax.FuncType
import org.explang.syntax.Type
import org.explang.syntax.Type.Companion.BOOL
import org.explang.syntax.Type.Companion.DOUBLE
import org.explang.syntax.Type.Companion.LONG
import org.explang.syntax.Type.Companion.NIL
import org.explang.syntax.Type.Companion.array
import org.explang.syntax.Type.Companion.function
import org.explang.syntax.Type.Companion.range
import org.explang.syntax.TypeParameter
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

interface Builtin {
  val name: String
  fun types(): Iterable<Type>
  fun forType(t: FuncType): Callable
}

// A builtin with one or more concretely-typed overloads.
class SimpleBuiltin(
    override val name: String,
    private val impls: Map<Type, Callable>
) : Builtin {
  override fun types() = impls.keys
  override fun forType(t: FuncType) = impls.getValue(t)
}

// A builtin with one or more possibly-parameterizes overloads.
class GenericBuiltin(
    override val name: String,
    private val impls: List<Pair<Type, Callable>>
) : Builtin {
  override fun types() = impls.map(Pair<Type, Callable>::first)
  override fun forType(t: FuncType) = impls.first { it.first.unify(t) }.second
}

typealias E = EvalResult

val OPERATORS = listOf(
    // Comparison operators
    SimpleBuiltin("==", mapOf(
        function(BOOL, BOOL, BOOL) to { _, args -> E(args[0].value as Boolean == args[1].value as Boolean) },
        function(BOOL, LONG, LONG) to { _, args -> E(args[0].value as Long == args[1].value as Long) },
        function(BOOL, DOUBLE, DOUBLE) to { _, args -> E(args[0].value as Double == args[1].value as Double) }
    )),
    SimpleBuiltin("<>", mapOf(
        function(BOOL, BOOL, BOOL) to { _, args -> E(args[0].value as Boolean != args[1].value as Boolean) },
        function(BOOL, LONG, LONG) to { _, args -> E(args[0].value as Long != args[1].value as Long) },
        function(BOOL, DOUBLE, DOUBLE) to { _, args -> E(args[0].value as Double != args[1].value as Double) }
    )),
    SimpleBuiltin("<", mapOf(
        function(BOOL, LONG, LONG) to { _, args -> E((args[0].value as Long) < args[1].value as Long) },
        function(BOOL, DOUBLE, DOUBLE) to { _, args -> E((args[0].value as Double) < args[1].value as Double) }
    )),
    SimpleBuiltin("<=", mapOf(
        function(BOOL, LONG, LONG) to { _, args -> E((args[0].value as Long) <= args[1].value as Long) },
        function(BOOL, DOUBLE, DOUBLE) to { _, args -> E((args[0].value as Double) <= args[1].value as Double) }
    )),
    SimpleBuiltin(">", mapOf(
        function(BOOL, LONG, LONG) to { _, args -> E((args[0].value as Long) > args[1].value as Long) },
        function(BOOL, DOUBLE, DOUBLE) to { _, args -> E((args[0].value as Double) > args[1].value as Double) }
    )),
    SimpleBuiltin(">=", mapOf(
        function(BOOL, LONG, LONG) to { _, args -> E((args[0].value as Long) >= args[1].value as Long) },
        function(BOOL, DOUBLE, DOUBLE) to { _, args -> E((args[0].value as Double) >= args[1].value as Double) }
    )),

    // Arithmetic operators
    SimpleBuiltin("^", mapOf(
        function(LONG, LONG, LONG) to { _, args ->
          E((args[0].value as Long).toBigInteger().pow(Math.toIntExact(args[1].value as Long)).longValueExact())
        },
        function(DOUBLE, DOUBLE, DOUBLE) to { _, args -> E((args[0].value as Double).pow(args[1].value as Double)) }
    )),
    SimpleBuiltin("*", mapOf(
        function(LONG, LONG, LONG) to { _, args -> E(args[0].value as Long * args[1].value as Long) },
        function(DOUBLE, DOUBLE, DOUBLE) to { _, args -> E(args[0].value as Double * args[1].value as Double) }
    )),
    SimpleBuiltin("/", mapOf(
        function(LONG, LONG, LONG) to { _, args -> E(args[0].value as Long / args[1].value as Long) },
        function(DOUBLE, DOUBLE, DOUBLE) to { _, args -> E(args[0].value as Double / args[1].value as Double) }
    )),
    SimpleBuiltin("+", mapOf(
        function(LONG, LONG, LONG) to { _, args -> E(args[0].value as Long + args[1].value as Long) },
        function(DOUBLE, DOUBLE, DOUBLE) to { _, args -> E(args[0].value as Double + args[1].value as Double) }
    )),
    SimpleBuiltin("-", mapOf(
        function(LONG, LONG) to { _, args -> E(-(args[0].value as Long)) },
        function(DOUBLE, DOUBLE) to { _, args -> E(-(args[0].value as Double)) },
        function(LONG, LONG, LONG) to { _, args -> E(args[0].value as Long - args[1].value as Long) },
        function(DOUBLE, DOUBLE, DOUBLE) to { _, args -> E(args[0].value as Double - args[1].value as Double) }
    )),

    // Boolean arithmetic
    SimpleBuiltin("not", mapOf(function(BOOL, BOOL) to { _, args -> E(!(args[0].value as Boolean)) })),
    SimpleBuiltin("and", mapOf(
        function(BOOL, BOOL, BOOL) to { _, args -> E(args[0].value as Boolean and args[1].value as Boolean) })),
    SimpleBuiltin("or", mapOf(
        function(BOOL, BOOL, BOOL) to { _, args -> E(args[0].value as Boolean or args[1].value as Boolean) })),
    SimpleBuiltin("xor", mapOf(
        function(BOOL, BOOL, BOOL) to { _, args -> E(args[0].value as Boolean xor args[1].value as Boolean) })),

    // Indexing operators
    GenericBuiltin("[]", listOf(
        // Index
        TypeParameter("a").let { function(it, array(it), LONG) } to { _, args ->
          E((args[0].value as ArrayValue<*>)[Math.toIntExact(args[1].value as Long)]!!)
        },
        // Slice
        TypeParameter("a").let { function(array(it), array(it), range(LONG)) } to { _, args ->
          E((args[0].value as ArrayValue<*>).slice(args[1].value as LongRangeValue))
        }
    )),

    // Range constructors
    SimpleBuiltin(":", mapOf(
        // TODO: Rationalize when types can be nullable
        function(range(LONG), LONG, LONG, LONG) to { _, args ->
          E(LongRangeValue.of((args[0].value as? Long), (args[1].value as? Long), (args[2].value as? Long)))
        },
        function(range(LONG), LONG, LONG, NIL) to { _, args ->
          E(LongRangeValue.of((args[0].value as? Long), (args[1].value as? Long), null))
        },
        function(range(LONG), LONG, NIL, LONG) to { _, args ->
          E(LongRangeValue.of((args[0].value as? Long), null, (args[2].value as? Long)))
        },
        function(range(LONG), NIL, LONG, LONG) to { _, args ->
          E(LongRangeValue.of(null, (args[1].value as? Long), (args[2].value as? Long)))
        },
        function(range(LONG), LONG, NIL, NIL) to { _, args ->
          E(LongRangeValue.of((args[0].value as? Long), null, null))
        },
        function(range(LONG), NIL, LONG, NIL) to { _, args ->
          E(LongRangeValue.of(null, (args[1].value as? Long), null))
        },
        function(range(LONG), NIL, NIL, LONG) to { _, args ->
          E(LongRangeValue.of(null, null, (args[2].value as? Long)))
        },
        function(range(LONG), NIL, NIL, NIL) to { _, args ->
          E(LongRangeValue.of(null, null, null))
        },
        function(range(DOUBLE), DOUBLE, DOUBLE, DOUBLE) to { _, args ->
          E(DoubleRangeValue.of((args[0].value as? Double), (args[1].value as? Double), (args[2].value as? Double)))
        },
        function(range(DOUBLE), DOUBLE, DOUBLE, NIL) to { _, args ->
          E(DoubleRangeValue.of((args[0].value as? Double), (args[1].value as? Double), null))
        },
        function(range(DOUBLE), DOUBLE, NIL, DOUBLE) to { _, args ->
          E(DoubleRangeValue.of((args[0].value as? Double), null, (args[2].value as? Double)))
        },
        function(range(DOUBLE), NIL, DOUBLE, DOUBLE) to { _, args ->
          E(DoubleRangeValue.of(null, (args[1].value as? Double), (args[2].value as? Double)))
        },
        function(range(DOUBLE), DOUBLE, NIL, NIL) to { _, args ->
          E(DoubleRangeValue.of((args[0].value as? Double), null, null))
        },
        function(range(DOUBLE), NIL, DOUBLE, NIL) to { _, args ->
          E(DoubleRangeValue.of(null, (args[1].value as? Double), null))
        },
        function(range(DOUBLE), NIL, NIL, DOUBLE) to { _, args ->
          E(DoubleRangeValue.of(null, null, (args[2].value as? Double)))
        }
    ))
)

@Suppress("UNCHECKED_CAST")
val BUILTINS = listOf(
    // Array constructors
    SimpleBuiltin("zeros", mapOf(function(array(DOUBLE), LONG) to { _, args ->
      E(DoubleArrayValue.of(*DoubleArray((args[0].value as Long).toInt())))
    })),

    SimpleBuiltin("range", mapOf(
        function(range(LONG), LONG, LONG, LONG) to { _, args ->
          E(LongRangeValue.of((args[0].value as? Long), (args[1].value as? Long), (args[2].value as? Long)))
        },
        function(range(DOUBLE), DOUBLE, DOUBLE, DOUBLE) to { _, args ->
          E(DoubleRangeValue.of((args[0].value as? Double), (args[1].value as? Double), (args[2].value as? Double)))
        }
    )),

    // Math builtins
    SimpleBuiltin("sign", mapOf(
        function(LONG, LONG) to { _, args -> E((args[0].value as Long).sign) },
        function(DOUBLE, DOUBLE) to { _, args -> E((args[0].value as Double).sign) }
    )),
    SimpleBuiltin("sqrt", mapOf(
        function(DOUBLE, LONG) to { _, args -> E(sqrt((args[0].value as Long).toDouble())) },
        function(DOUBLE, DOUBLE) to { _, args -> E(sqrt(args[0].value as Double)) }
    )),
    SimpleBuiltin("isNaN", mapOf( function(BOOL, DOUBLE) to { _, args -> E((args[0].value as Double).isNaN()) })),

    // Sequence functions
    GenericBuiltin("map", listOf(
        // Array
        // The order of these matters, which is a bit nasty.
        TypeParameter("a").let { a -> function(array(BOOL), array(a), function(BOOL, a)) } to { ctx, args ->
          val r = args[0].value as ArrayValue<out Any>
          val f = args[1].value as Callable
          E(mapToBool(r) { v -> f(ctx, arrayOf(EvalResult(v))).value as Boolean })
        },
        TypeParameter("a").let { a -> function(array(LONG), array(a), function(LONG, a)) } to { ctx, args ->
          val r = args[0].value as ArrayValue<out Any>
          val f = args[1].value as Callable
          E(mapToLong(r) { v -> f(ctx, arrayOf(EvalResult(v))).value as Long })
        },
        TypeParameter("a").let { a -> function(array(DOUBLE), array(a), function(DOUBLE, a)) } to { ctx, args ->
          val r = args[0].value as ArrayValue<out Any>
          val f = args[1].value as Callable
          E(mapToDouble(r) { v -> f(ctx, arrayOf(EvalResult(v))).value as Double })
        },
        TypeParameter("a").let { a -> TypeParameter("b").let { b -> function(array(b), array(a), function(b, a)) } } to
            { ctx, args ->
              val r = args[0].value as ArrayValue<out Any>
              val f = args[1].value as Callable
              E(mapToObject(r) { v -> f(ctx, arrayOf(EvalResult(v))).value as Boolean })
            },
        // Range
        TypeParameter("a").let { a -> function(array(BOOL), range(a), function(BOOL, a)) } to { ctx, args ->
          val r = args[0].value as RangeValue<out Any>
          val f = args[1].value as Callable
          E(mapToBool(r) { v -> f(ctx, arrayOf(EvalResult(v))).value as Boolean })
        },
        TypeParameter("a").let { a -> function(array(LONG), range(a), function(LONG, a)) } to { ctx, args ->
          val r = args[0].value as RangeValue<out Any>
          val f = args[1].value as Callable
          E(mapToLong(r) { v -> f(ctx, arrayOf(EvalResult(v))).value as Long })
        },
        TypeParameter("a").let { a -> function(array(DOUBLE), range(a), function(DOUBLE, a)) } to { ctx, args ->
          val r = args[0].value as RangeValue<out Any>
          val f = args[1].value as Callable
          E(mapToDouble(r) { v -> f(ctx, arrayOf(EvalResult(v))).value as Double })
        },
        TypeParameter("a").let { a -> TypeParameter("b").let { b -> function(array(b), range(a), function(b, a)) } } to
            { ctx, args ->
              val r = args[0].value as RangeValue<out Any>
              val f = args[1].value as Callable
              E(mapToObject(r) { v -> f(ctx, arrayOf(EvalResult(v))).value as Boolean })
            }
    )),
    GenericBuiltin("fold", listOf(
        TypeParameter("a").let { a -> TypeParameter("b").let { b -> function(b, array(a), b, function(b, b, a)) } } to
            { ctx, args -> E(fold(ctx, args)) },
        TypeParameter("a").let { a -> TypeParameter("b").let { b -> function(b, range(a), b, function(b, b, a)) } } to
            { ctx, args -> E(fold(ctx, args)) }
    )),
    GenericBuiltin("reduce", listOf(
        TypeParameter("a").let { a -> TypeParameter("b").let { b -> function(b, array(a), function(b, b, a)) } } to
            { ctx, args -> E(reduce(ctx, args)) },
        TypeParameter("a").let { a -> TypeParameter("b").let { b -> function(b, range(a), function(b, b, a)) } } to
            { ctx, args -> E(reduce(ctx, args)) }
    )),
    GenericBuiltin("filter", listOf(
        // Array
        TypeParameter("a").let { a -> function(array(a), array(a), function(BOOL, a)) } to { ctx, args ->
          val r = args[0].value as ArrayValue<out Any>
          val f = args[1].value as Callable
          E(r.filter { v -> f(ctx, arrayOf(E(v))).value as Boolean })
        },
        TypeParameter("a").let { a -> function(array(a), range(a), function(BOOL, a)) } to { ctx, args ->
          val r = args[0].value as RangeValue<out Any>
          val f = args[1].value as Callable
          E(r.filter { v -> f(ctx, arrayOf(E(v))).value as Boolean })
        }
    ))
)

private fun <T: Any> mapToBool(source: Iterable<T>, mapper: (T) -> Boolean): ArrayValue<Boolean> {
  // TODO: pre-allocate array of the right size when the source size is known.
  val mapped = mutableListOf<Boolean>()
  for (v in source) {
    mapped.add(mapper(v))
  }
  return BooleanArrayValue.of(*mapped.toBooleanArray())
}

private fun <T> mapToLong(source: Iterable<T>, mapper: (T) -> Long): ArrayValue<Long> {
  // TODO: pre-allocate array of the right size when the source size is known.
  val mapped = mutableListOf<Long>()
  for (v in source) {
    mapped.add(mapper(v))
  }
  return LongArrayValue.of(*mapped.toLongArray())
}

private fun <T> mapToDouble(source: Iterable<T>, mapper: (T) -> Double): ArrayValue<Double> {
  // TODO: pre-allocate array of the right size when the source size is known.
  val mapped = mutableListOf<Double>()
  for (v in source) {
    mapped.add(mapper(v))
  }
  return DoubleArrayValue.of(*mapped.toDoubleArray())
}

private fun <T : Any> mapToObject(source: Iterable<T>, mapper: (T) -> Any): ArrayValue<out Any> {
  // TODO: pre-allocate array of the right size when the source size is known.
  val mapped = mutableListOf<Any>()
  for (v in source) {
    mapped.add(mapper(v))
  }
  return ObjectArrayValue.of(mapped.toTypedArray())
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> fold(ctx: CallContext, args: Array<EvalResult>): T {
  val r = args[0].value as Iterable<T>
  val init = args[1].value as T
  val f = args[2].value as Callable
  return r.fold(init) { acc, v -> f(ctx, arrayOf(EvalResult(acc), EvalResult(v))).value as T }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> reduce(ctx: CallContext, args: Array<EvalResult>): T {
  val r = args[0].value as Iterable<T>
  val f = args[1].value as Callable
  return r.reduce { acc, v -> f(ctx, arrayOf(EvalResult(acc), EvalResult(v))).value as T }
}
