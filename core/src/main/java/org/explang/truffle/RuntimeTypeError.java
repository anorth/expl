package org.explang.truffle;

/**
 * Indicates a type error (that should have been prevented at compile time).
 */
public final class RuntimeTypeError extends RuntimeException {
  public RuntimeTypeError(String message) {
    super(message);
  }
}
