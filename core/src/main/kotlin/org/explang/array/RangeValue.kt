package org.explang.array

import kotlin.math.sign

/**
 * A possibly-open-ended range of numbers.
 */
interface RangeValue<T> : Iterable<T> {
  val first: T?
  val last: T?
  val step: T
}

data class LongRangeValue(
    override val first: Long? = null,
    override val last: Long? = null,
    // Defaults to -1 if last < first, else 1.
    override val step: Long
) : RangeValue<Long> {
  companion object {
    fun of(first: Long? = null, last: Long? = null, step: Long? = null) =
        LongRangeValue(first, last, step ?: impliedStep(first, last))
  }

  init {
    check(step != 0L) { "Slice step must be non-zero" }
    check(step != Long.MIN_VALUE) { "Slice step must be greater than ${Long.MIN_VALUE}" }
  }

  fun reversed() = LongRangeValue(last, first, -step)

  override fun iterator(): Iterator<Long> {
    check(first != null && last != null) { "Can't iterate open-ended range $this" }
    return object : Iterator<Long> {
      var next: Long = first
      override fun hasNext(): Boolean {
        val sign = (last - next).sign
        return sign == 0 || sign == step.sign
      }

      override fun next(): Long {
        val it = next
        next += step
        return it
      }
    }
  }

  override fun toString() = rangeToString(first, last, step, impliedStep(first, last))
}

data class DoubleRangeValue(
    override val first: Double? = null,
    override val last: Double? = null,
    // Defaults to -1.0 if last < first, else 1.0.
    override val step: Double
) : RangeValue<Double> {
  companion object {
    fun of(first: Double? = null, last: Double? = null, step: Double? = null) =
        DoubleRangeValue(first, last, step ?: impliedStep(first, last))
  }

  init {
    check(step != 0.0) { "Slice step must be non-zero" }
    check(step != Double.MIN_VALUE) { "Slice step must be greater than ${Double.MIN_VALUE}" }
  }

  fun reversed() = DoubleRangeValue(last, first, -step)

  override fun iterator(): Iterator<Double> {
    check(first != null && last != null) { "Can't iterate open-ended range $this" }
    return object : Iterator<Double> {
      var next: Double = first
      override fun hasNext(): Boolean {
        val sign = (last - next).sign
        return sign == 0.0 || sign == step.sign
      }

      override fun next(): Double {
        val it = next
        next += step
        return it
      }
    }
  }

  override fun toString() = rangeToString(first, last, step, impliedStep(first, last))
}

// Not Kotlin extensions so as to be visible from Java.
inline fun <T, R> fold(range: RangeValue<T>, initial: R, operation: (R, T) -> R): R =
    range.fold(initial, operation)

inline fun <T> reduce(range: RangeValue<T>, acc: (T, T) -> T): T =
    range.reduce(acc)

inline fun <T> mapToLong(range: RangeValue<T>, mapper: (T) -> Long): ArrayValue<Long> {
  val mapped = mutableListOf<Long>()
  for (v in range) {
    mapped.add(mapper(v))
  }
  return LongArrayValue.of(*mapped.toLongArray())
}

private fun impliedStep(first: Long?, last: Long?) =
    if ((last ?: Long.MAX_VALUE) < (first ?: Long.MIN_VALUE)) -1L else 1L

private fun impliedStep(first: Double?, last: Double?) =
    if ((last ?: Double.MAX_VALUE) < (first ?: Double.MIN_VALUE)) -1.0 else 1.0

private fun <T> rangeToString(first: T?, last: T?, step: T, impliedStep: T): String {
  val b = StringBuilder()
  b.append(first?.toString() ?: "*")
  b.append(":")
  b.append(last?.toString() ?: "*")
  if (step != impliedStep) {
    b.append(":")
    b.append(step)
  }
  return b.toString()
}
