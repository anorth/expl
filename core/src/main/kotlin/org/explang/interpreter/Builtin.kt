package org.explang.interpreter

import org.explang.array.ArrayValue
import org.explang.array.BooleanArrayValue
import org.explang.array.DoubleArrayValue
import org.explang.array.DoubleRangeValue
import org.explang.array.LongArrayValue
import org.explang.array.LongRangeValue
import org.explang.syntax.ArrayType
import org.explang.syntax.FuncType
import org.explang.syntax.PrimType
import org.explang.syntax.RangeType
import org.explang.syntax.Type
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

    // Range
    BuiltinFunction("range", function(range(LONG), LONG, LONG, LONG)) { args, _ ->
      LongRangeValue(args[0].value as Long, args[1].value as Long, args[2].value as Long)
    }
) +
    mapFns(::ArrayType) + mapFns(::RangeType) +
    foldFns(::ArrayType) + foldFns(::RangeType) +
    reduceFns(::ArrayType) + reduceFns(::RangeType)

// Returns a list of map functions for each primitive result type for a sequence type.
// TODO: this is temporary, hack polymorphism. The output type needs to be arbitrary, including arrays, functions,
// records etc. The full set can't be known statically. We need a builtin factory or similar at compile time.
private fun mapFns(seqTypeCtor: (elType: Type) -> Type): List<BuiltinFunction> {
  val fns = mutableListOf<BuiltinFunction>()
  for (inElType in PrimType.all()) {
    fns.add(BuiltinFunction("map", function(array(BOOL), seqTypeCtor(inElType), function(BOOL, inElType))) {
      args, ctx -> map<Boolean>(args, ctx, BOOL) })
    fns.add(BuiltinFunction("map", function(array(LONG), seqTypeCtor(inElType), function(LONG, inElType))) {
      args, ctx -> map<Long>(args, ctx, LONG) })
    fns.add(BuiltinFunction("map", function(array(DOUBLE), seqTypeCtor(inElType), function(DOUBLE, inElType))) {
      args, ctx -> map<Double>(args, ctx, DOUBLE) })
  }
  return fns
}

// Returns a list of map functions for each primitive result type for a sequence type.
private fun foldFns(seqTypeCtor: (elType: Type) -> Type): List<BuiltinFunction> {
  val fns = mutableListOf<BuiltinFunction>()
  for (inElType in PrimType.all()) {
    for (outElType in PrimType.all()) {
      fns.add(BuiltinFunction("fold", function(outElType, seqTypeCtor(DOUBLE), outElType, function(outElType, outElType, inElType)), ::fold))
    }
  }
  return fns
}

// Returns a list of map functions for each primitive result type for a sequence type.
private fun reduceFns(seqTypeCtor: (elType: Type) -> Type): List<BuiltinFunction> {
  val fns = mutableListOf<BuiltinFunction>()
  for (inElType in PrimType.all()) {
    for (outElType in PrimType.all()) {
      fns.add(BuiltinFunction("reduce", function(outElType, seqTypeCtor(inElType), function(outElType, outElType, inElType)), ::reduce))
    }
  }
  return fns
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> map(args: Array<EvalResult>, ctx: CallContext, outType: Type): ArrayValue<*> {
  val r = args[0].value as Iterable<T>
  val f = args[1].value as Callable
  return when (outType) {
    BOOL -> mapToBool(r) { v: T -> f.call(ctx, arrayOf(EvalResult(v))).value as Boolean }
    LONG -> mapToLong(r) { v: T -> f.call(ctx, arrayOf(EvalResult(v))).value as Long }
    DOUBLE -> mapToDouble(r) { v: T -> f.call(ctx, arrayOf(EvalResult(v))).value as Double }
    else -> throw EvalError("Can't map to $outType", null)
  }
}

private fun <T> mapToBool(source: Iterable<T>, mapper: (T) -> Boolean): ArrayValue<Boolean> {
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

@Suppress("UNCHECKED_CAST")
private fun <T : Any> fold(args: Array<EvalResult>, ctx: CallContext): T {
  val r = args[0].value as Iterable<T>
  val init = args[1].value as T
  val f = args[2].value as Callable
  return r.fold(init) { acc, v -> f.call(ctx, arrayOf(EvalResult(acc), EvalResult(v))).value as T }
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> reduce(args: Array<EvalResult>, ctx: CallContext): T {
  val r = args[0].value as Iterable<T>
  val f = args[1].value as Callable
  return r.reduce { acc, v -> f.call(ctx, arrayOf(EvalResult(acc), EvalResult(v))).value as T }
}
