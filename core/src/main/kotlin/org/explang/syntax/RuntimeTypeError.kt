package org.explang.syntax

/**
 * Indicates a type error (that should have been prevented at compile time).
 */
class RuntimeTypeError : RuntimeException {
  constructor(message: String) : super(message) {}
  constructor(cause: Throwable) : super(cause) {}
  constructor(message: String, cause: Throwable) : super(message, cause) {}
}
