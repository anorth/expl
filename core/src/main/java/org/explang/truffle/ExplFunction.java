package org.explang.truffle;

import com.oracle.truffle.api.RootCallTarget;

/**
 * Type object for a function.
 */
public class ExplFunction {
  public final Type type;
  public final RootCallTarget callTarget;

  public ExplFunction(Type type, RootCallTarget callTarget) {
    assert type.isFunction() : "Expected a function type, got" + type;
    this.type = type;
    this.callTarget = callTarget;
  }

  @Override
  public String toString() {
    return type.toString();
  }
}
