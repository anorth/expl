package org.explang.truffle;

import com.oracle.truffle.api.RootCallTarget;

/**
 * Type object for a function.
 */
public class ExplFunction {
  public final Type type;
  public final RootCallTarget callTarget;

  public ExplFunction(Type type, RootCallTarget callTarget) {
    this.type = type;
    this.callTarget = callTarget;
  }
}
