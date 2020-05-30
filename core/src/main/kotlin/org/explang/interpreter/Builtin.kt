package org.explang.interpreter

import org.explang.array.*
import org.explang.syntax.FuncType
import org.explang.syntax.Type.Companion.BOOL
import org.explang.syntax.Type.Companion.DOUBLE
import org.explang.syntax.Type.Companion.LONG
import org.explang.syntax.Type.Companion.array
import org.explang.syntax.Type.Companion.function
import org.explang.syntax.Type.Companion.range
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sqrt

open class BuiltinFunction(
    val name: String,
    val type: FuncType,
    val body: (Array<EvalResult>, CallContext) -> Any) : Callable {
  override fun call(ctx: CallContext, args: Array<EvalResult>) = EvalResult(body(args, ctx))
}

val OPERATORS = listOf(
    // Unary operators
    BuiltinFunction("not", function(BOOL, BOOL)) { args, _ -> !(args[0].value as Boolean) },
    BuiltinFunction("-", function(LONG, LONG)) { args, _ -> -(args[0].value as Long) },
    BuiltinFunction("-", function(DOUBLE, DOUBLE)) { args, _ -> -(args[0].value as Double) },

    // Boolean operators
    BuiltinFunction("==",
        function(BOOL, BOOL, BOOL)) { args, _ -> args[0].value as Boolean == args[1].value as Boolean },
    BuiltinFunction("<>",
        function(BOOL, BOOL, BOOL)) { args, _ -> args[0].value as Boolean != args[1].value as Boolean },
    BuiltinFunction("and",
        function(BOOL, BOOL, BOOL)) { args, _ -> args[0].value as Boolean and args[1].value as Boolean },
    BuiltinFunction("or",
        function(BOOL, BOOL, BOOL)) { args, _ -> args[0].value as Boolean or args[1].value as Boolean },
    BuiltinFunction("xor",
        function(BOOL, BOOL, BOOL)) { args, _ -> args[0].value as Boolean xor args[1].value as Boolean },

    // Long operators
    BuiltinFunction("^", function(LONG, LONG, LONG)) { args, _ ->
      (args[0].value as Long).toBigInteger().pow(Math.toIntExact(args[1].value as Long)).longValueExact()
    },
    BuiltinFunction("*", function(LONG, LONG, LONG)) { args, _ -> args[0].value as Long * args[1].value as Long },
    BuiltinFunction("/", function(LONG, LONG, LONG)) { args, _ -> args[0].value as Long / args[1].value as Long },
    BuiltinFunction("+", function(LONG, LONG, LONG)) { args, _ -> args[0].value as Long + args[1].value as Long },
    BuiltinFunction("-", function(LONG, LONG, LONG)) { args, _ -> args[0].value as Long - args[1].value as Long },
    BuiltinFunction("<", function(BOOL, LONG, LONG)) { args, _ -> (args[0].value as Long) < args[1].value as Long },
    BuiltinFunction("<=", function(BOOL, LONG, LONG)) { args, _ -> args[0].value as Long <= args[1].value as Long },
    BuiltinFunction(">", function(BOOL, LONG, LONG)) { args, _ -> args[0].value as Long > args[1].value as Long },
    BuiltinFunction(">=", function(BOOL, LONG, LONG)) { args, _ -> args[0].value as Long >= args[1].value as Long },
    BuiltinFunction("==", function(BOOL, LONG, LONG)) { args, _ -> args[0].value as Long == args[1].value as Long },
    BuiltinFunction("<>", function(BOOL, LONG, LONG)) { args, _ -> args[0].value as Long != args[1].value as Long },

    // Double operators
    BuiltinFunction("^",
        function(DOUBLE, DOUBLE, DOUBLE)) { args, _ -> (args[0].value as Double).pow(args[1].value as Double) },
    BuiltinFunction("*",
        function(DOUBLE, DOUBLE, DOUBLE)) { args, _ -> args[0].value as Double * args[1].value as Double },
    BuiltinFunction("/",
        function(DOUBLE, DOUBLE, DOUBLE)) { args, _ -> args[0].value as Double / args[1].value as Double },
    BuiltinFunction("+",
        function(DOUBLE, DOUBLE, DOUBLE)) { args, _ -> args[0].value as Double + args[1].value as Double },
    BuiltinFunction("-",
        function(DOUBLE, DOUBLE, DOUBLE)) { args, _ -> args[0].value as Double - args[1].value as Double },
    BuiltinFunction("<",
        function(BOOL, DOUBLE, DOUBLE)) { args, _ -> (args[0].value as Double) < args[1].value as Double },
    BuiltinFunction("<=",
        function(BOOL, DOUBLE, DOUBLE)) { args, _ -> args[0].value as Double <= args[1].value as Double },
    BuiltinFunction(">",
        function(BOOL, DOUBLE, DOUBLE)) { args, _ -> args[0].value as Double > args[1].value as Double },
    BuiltinFunction(">=",
        function(BOOL, DOUBLE, DOUBLE)) { args, _ -> args[0].value as Double >= args[1].value as Double },
    BuiltinFunction("==",
        function(BOOL, DOUBLE, DOUBLE)) { args, _ -> args[0].value as Double == args[1].value as Double },
    BuiltinFunction("<>",
        function(BOOL, DOUBLE, DOUBLE)) { args, _ -> args[0].value as Double != args[1].value as Double },

    // Long array operators
    BuiltinFunction("[]", function(LONG, array(LONG), LONG)) { args, _ ->
      (args[0].value as LongArrayValue)[Math.toIntExact(args[1].value as Long)]
    },
    BuiltinFunction("[]", function(array(LONG), array(LONG), range(LONG))) { args, _ ->
      (args[0].value as LongArrayValue).slice((args[1].value as LongRangeValue))
    },

    // Double array operators
    BuiltinFunction("[]", function(DOUBLE, array(DOUBLE), LONG)) { args, _ ->
      (args[0].value as DoubleArrayValue)[Math.toIntExact(args[1].value as Long)]
    },
    BuiltinFunction("[]", function(array(DOUBLE), array(DOUBLE), range(LONG))) { args, _ ->
      (args[0].value as DoubleArrayValue).slice((args[1].value as LongRangeValue))
    },

    // Range operators
    BuiltinFunction(":", function(range(LONG), LONG, LONG, LONG)) { args, _ ->
      val first = (args[0].value as? Long)
      val last = (args[1].value as? Long)
      val step = (args[2].value as? Long)
      LongRangeValue.of(first, last, step)
    },
    BuiltinFunction(":", function(range(DOUBLE), DOUBLE, DOUBLE, DOUBLE)) { args, _ ->
      val first = (args[0].value as? Double)
      val last = (args[1].value as? Double)
      val step = (args[2].value as? Double)
      DoubleRangeValue.of(first, last, step)
    }
)

@Suppress("UNCHECKED_CAST")
val BUILTINS = listOf(
    // Math builtins
    BuiltinFunction("sqrt", function(DOUBLE, DOUBLE)) { args, _ -> sqrt(args[0].value as Double) },
    BuiltinFunction("sign", function(DOUBLE, DOUBLE)) { args, _ -> (args[0].value as Double).sign },
    BuiltinFunction("positive", function(BOOL, DOUBLE)) { args, _ -> (args[0].value as Double) > 0.0 },

    // Arrays
    BuiltinFunction("zeros", function(array(DOUBLE), LONG)) { args, _ ->
      DoubleArrayValue.of(*DoubleArray((args[0].value as Long).toInt()))
    },
    BuiltinFunction("filter", function(array(DOUBLE), array(DOUBLE), function(BOOL, DOUBLE))) { args, ctx ->
      val r = args[0].value as ArrayValue<Double>
      val f = args[1].value as Callable
      r.filter { v -> f.call(ctx, arrayOf(EvalResult(v))).value as Boolean }
    },
    BuiltinFunction("map", function(array(DOUBLE), array(DOUBLE), function(DOUBLE, DOUBLE))) { args, ctx ->
      val r = args[0].value as ArrayValue<Double>
      val f = args[1].value as Callable
      mapToDouble(r) { v -> f.call(ctx, arrayOf(EvalResult(v))).value as Double }
    },
    BuiltinFunction("fold", function(DOUBLE, array(DOUBLE), DOUBLE, function(DOUBLE, DOUBLE, DOUBLE))) { args, ctx ->
      val r = args[0].value as ArrayValue<Double>
      val init = args[1].value as Double
      val f = args[2].value as Callable
      fold(r, init) { acc, v -> f.call(ctx, arrayOf(EvalResult(acc), EvalResult(v))).value as Double }
    },
    BuiltinFunction("reduce", function(DOUBLE, array(DOUBLE), function(DOUBLE, DOUBLE, DOUBLE))) { args, ctx ->
      val r = args[0].value as ArrayValue<Double>
      val f = args[1].value as Callable
      reduce(r) { acc, v -> f.call(ctx, arrayOf(EvalResult(acc), EvalResult(v))).value as Double }
    },

    // Range
    BuiltinFunction("range", function(range(LONG), LONG, LONG, LONG)) { args, _ ->
      val first = args[0].value as Long
      val last = args[1].value as Long
      val step = args[2].value as Long
      LongRangeValue(first, last, step)
    },
    BuiltinFunction("mapr", function(array(LONG), range(LONG), function(LONG, LONG))) { args, ctx ->
      val r = args[0].value as RangeValue<Long>
      val f = args[1].value as Callable
      mapToLong(r) { v -> f.call(ctx, arrayOf(EvalResult(v))).value as Long }
    },
    BuiltinFunction("foldr", function(LONG, array(LONG), LONG, function(LONG, LONG, LONG))) { args, ctx ->
      val r = args[0].value as RangeValue<Long>
      val init = args[1].value as Long
      val f = args[2].value as Callable
      fold(r, init) { acc, v -> f.call(ctx, arrayOf(EvalResult(acc), EvalResult(v))).value as Long }
    },
    BuiltinFunction("reducer", function(LONG, array(LONG), function(LONG, LONG, LONG))) { args, ctx ->
      val r = args[0].value as RangeValue<Long>
      val f = args[1].value as Callable
      reduce(r) { acc, v -> f.call(ctx, arrayOf(EvalResult(acc), EvalResult(v))).value as Long }
    }
)