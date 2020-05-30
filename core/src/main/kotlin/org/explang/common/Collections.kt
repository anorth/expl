package org.explang.common

/** Returns the singleton element if the collection has exactly one. */
fun <T> Collection<T>.singleOrNull() = if (this.size == 1) this.first() else null

@Suppress("UNCHECKED_CAST")
fun <T, U> Array<T>.mapArr(klass: Class<U>, f: (T) -> U): Array<U> {
  val mapped = java.lang.reflect.Array.newInstance(klass, this.size) as Array<U?>
  this.forEachIndexed{ i, it -> mapped[i] = f(it)}
  return mapped as Array<U>
}