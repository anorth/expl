package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.ArrayValue;
import org.explang.syntax.Type;
import org.explang.truffle.ExplFunction;

/**
 * A node which can't be executed for any type. This class is intended to be subclassed by
 * simple node types which override only one execution method.
 */
public abstract class BaseNode extends ExpressionNode {
  public BaseNode(Type type) {
    super(type);
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    throw new AssertionError(String.format("%s has no implementation for boolean", this));
  }
  @Override
  public long executeLong(VirtualFrame frame) {
    throw new AssertionError(String.format("%s has no implementation for long", this));
  }
  @Override
  public double executeDouble(VirtualFrame frame) {
    throw new AssertionError(String.format("%s has no implementation for double", this));
  }
  @Override
  public ExplFunction executeFunction(VirtualFrame frame) {
    throw new AssertionError(String.format("%s has no implementation for function", this));
  }
  @Override
  public ArrayValue executeArray(VirtualFrame frame) {
    throw new AssertionError(String.format("%s has no implementation for array", this));
  }
}
