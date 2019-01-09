package org.explang.syntax;

import java.util.Arrays;
import java.util.Objects;
import java.util.StringJoiner;

import org.jetbrains.annotations.Nullable;

/**
 * Describes a guest language type.
 *
 * Primitive types compare equal on identity. Function types compare equal on argument and
 * result types.
 */
public final class Type {
  public static final Type NONE = new Type("none", null, null);
  public static final Type BOOL = new Type("boolean", null, null);
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
  private final @Nullable Type[] parameters; // null for non-functions

  private Type(String name, @Nullable Type result, @Nullable Type[] parameters) {
    this.name = name;
    this.result = result;
    this.parameters = parameters;
  }

  public String name() { return name; }
  public Type result() { return result; }
  public Type[] parameters() { return parameters; }

  public boolean isFunction() {
    return parameters != null;
  }

  @Override
  public String toString() { return name; }

  @Override
  public boolean equals(Object o) {
    if (this.isFunction()) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Type type = (Type) o;
      return Objects.equals(result, type.result) &&
          Arrays.equals(parameters, type.parameters);

    } else {
      return this == o;
    }
  }

  @Override
  public int hashCode() {
    if (this.isFunction()) {
      return Objects.hash(result, parameters);
    } else {
      return super.hashCode();
    }
  }
}
