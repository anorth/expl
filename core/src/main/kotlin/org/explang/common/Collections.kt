package org.explang.common

/** Returns the singleton element if the collection has exactly one. */
fun <T> Collection<T>.singleOrNull() = if (this.size == 1) this.first() else null
