package org.explang.truffle;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

import org.jetbrains.annotations.Nullable;

/**
 * Describes a guest language type.
 */
public final class Type {
  public static final Type NONE = new Type("none", null, null);
  public static final Type DOUBLE = new Type("double", null, null);

  public static Type function(Type result, Type... arguments) {
    StringJoiner j = new StringJoiner(",");
    for (Type arg : arguments) {
      j.add(arg.toString());
    }
    String name = "(" + j.toString() + "->" + result + ")";
    return new Type(name, result, arguments);
  }

  /** The function result type, or *this* for grounded types. */
  private final String name;
  private final Type result;
  private final @Nullable Type[] arguments; // null for non-functions

  private Type(String name, @Nullable Type result, @Nullable Type[] arguments) {
    this.name = name;
    this.result = result;
    this.arguments = arguments;
  }

  public String name() { return name; }
  public Type result() { return result; }
  public Type[] arguments() { return arguments; }

  public boolean isFunction() {
    return arguments != null;
  }

  @Override
  public String toString() { return name; }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Type type = (Type) o;
    return Objects.equals(result, type.result) &&
        Arrays.equals(arguments, type.arguments);
  }
  @Override
  public int hashCode() {
    int result1 = Objects.hash(result);
    result1 = 31 * result1 + Arrays.hashCode(arguments);
    return result1;
  }
}
