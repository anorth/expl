package org.explang.truffle;

import com.oracle.truffle.api.RootCallTarget;

/**
 * Type object for a function.
 *
 * TODO: add argument and return type objects
 */
public class ExplFunction {
  public final RootCallTarget callTarget;

  public ExplFunction(RootCallTarget callTarget) {
    this.callTarget = callTarget;
  }
}
