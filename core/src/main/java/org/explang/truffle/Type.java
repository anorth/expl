package org.explang.truffle;

import org.jetbrains.annotations.Nullable;

/**
 * Describes a guest language type.
 */
public final class Type {
  public static final Type DOUBLE = new Type(null, null);

  public static Type function(Type result, Type... arguments) {
    return new Type(result, arguments);
  }

  /** The function result type, or *this* for grounded types. */
  private final Type result;
  private final @Nullable Type[] arguments; // null for non-functions

  private Type(@Nullable Type result, @Nullable Type[] arguments) {
    this.result = result != null ? result : this;
    this.arguments = arguments;
  }

  public Type result() { return result; }

  public boolean isFunction() {
    return arguments != null;
  }
}
