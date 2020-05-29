package org.explang.interpreter

import org.explang.array.ArrayValue
import org.explang.array.DoubleArrayValue
import org.explang.array.LongRangeValue
import org.explang.array.RangeValue
import org.explang.array.fold
import org.explang.array.mapToDouble
import org.explang.array.mapToLong
import org.explang.array.reduce
import org.explang.syntax.FuncType
import org.explang.syntax.Type.Companion.BOOL
import org.explang.syntax.Type.Companion.DOUBLE
import org.explang.syntax.Type.Companion.LONG
import org.explang.syntax.Type.Companion.array
import org.explang.syntax.Type.Companion.function
import org.explang.syntax.Type.Companion.range
import kotlin.math.sign
import kotlin.math.sqrt

abstract class BuiltinFunction(
    val name: String,
    val funcType: FuncType,
    val body: (List<EvalResult>, CallContext) -> Any) : Callable {
  override fun call(ctx: CallContext, args: List<EvalResult>) = EvalResult(body(args, ctx))
}

@Suppress("UNCHECKED_CAST")
val BUILTINS = listOf(
    // Math
    make("sqrt", function(DOUBLE, DOUBLE)) { args, ctx -> sqrt(args[0].value as Double) },
    make("sign", function(DOUBLE, DOUBLE)) { args, ctx -> (args[0].value as Double).sign },
    make("positive", function(BOOL, DOUBLE)) { args, ctx -> (args[0].value as Double) > 0.0 },

    // Arrays
    make("zeros", function(array(DOUBLE), LONG)) { args, ctx ->
      DoubleArrayValue.of(*DoubleArray((args[0].value as Long).toInt()))
    },
    make("filter", function(array(DOUBLE), array(DOUBLE), function(BOOL, DOUBLE))) { args, ctx ->
      val r = args[0].value as ArrayValue<Double>
      val f = args[1].value as Callable
      r.filter { v -> f.call(ctx, listOf(EvalResult(v))).value as Boolean }
    },
    make("map", function(array(DOUBLE), array(DOUBLE), function(DOUBLE, DOUBLE))) { args, ctx ->
      val r = args[0].value as ArrayValue<Double>
      val f = args[1].value as Callable
      mapToDouble(r) { v -> f.call(ctx, listOf(EvalResult(v))).value as Double }
    },
    make("fold", function(DOUBLE, array(DOUBLE), DOUBLE, function(DOUBLE, DOUBLE, DOUBLE))) { args, ctx ->
      val r = args[0].value as ArrayValue<Double>
      val init = args[1].value as Double
      val f = args[2].value as Callable
      fold(r, init) { acc, v -> f.call(ctx, listOf(EvalResult(acc), EvalResult(v))).value as Double }
    },
    make("reduce", function(DOUBLE, array(DOUBLE), function(DOUBLE, DOUBLE, DOUBLE))) { args, ctx ->
      val r = args[0].value as ArrayValue<Double>
      val f = args[1].value as Callable
      reduce(r) { acc, v -> f.call(ctx, listOf(EvalResult(acc), EvalResult(v))).value as Double }
    },

    // Range
    make("range", function(range(LONG), LONG, LONG, LONG)) { args, ctx ->
      val first = args[0].value as Long
      val last = args[1].value as Long
      val step = args[2].value as Long
      LongRangeValue(first, last, step)
    },
    make("mapr", function(array(LONG), range(LONG), function(LONG, LONG))) { args, ctx ->
      val r = args[0].value as RangeValue<Long>
      val f = args[1].value as Callable
      mapToLong(r) { v -> f.call(ctx, listOf(EvalResult(v))).value as Long }
    },
    make("foldr", function(LONG, array(LONG), LONG, function(LONG, LONG, LONG))) { args, ctx ->
      val r = args[0].value as RangeValue<Long>
      val init = args[1].value as Long
      val f = args[2].value as Callable
      fold(r, init) { acc, v -> f.call(ctx, listOf(EvalResult(acc), EvalResult(v))).value as Long }
    },
    make("reducer", function(LONG, array(LONG), function(LONG, LONG, LONG))) { args, ctx ->
      val r = args[0].value as RangeValue<Long>
      val f = args[1].value as Callable
      reduce(r) { acc, v -> f.call(ctx, listOf(EvalResult(acc), EvalResult(v))).value as Long }
    }
)

fun make(name: String, type: FuncType, body: (List<EvalResult>, CallContext) -> Any): BuiltinFunction {
  return object : BuiltinFunction(name, type, { args, ctx -> body(args, ctx) }) {}
}