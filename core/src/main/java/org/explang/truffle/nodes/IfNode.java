package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.AbstractArray;
import org.explang.syntax.Type;
import org.explang.truffle.ExplFunction;

public final class IfNode extends ExpressionNode {
  @Child private ExpressionNode test;
  @Child private ExpressionNode left;
  @Child private ExpressionNode right;

  public IfNode(ExpressionNode test, ExpressionNode left, ExpressionNode right) {
    super(left.type());
    test.assertType(Type.BOOL);
    assert left.type() == right.type() :
        "Mismatched types in test: " + left.type() + ", " + right.type();
    this.test = test;
    this.left = left;
    this.right = right;
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    return test.executeBoolean(frame) ? left.executeBoolean(frame) : right.executeBoolean(frame);
  }
  @Override
  public double executeDouble(VirtualFrame frame) {
    return test.executeBoolean(frame) ? left.executeDouble(frame) : right.executeDouble(frame);
  }
  @Override
  public ExplFunction executeFunction(VirtualFrame frame) {
    return test.executeBoolean(frame) ? left.executeFunction(frame) : right.executeFunction(frame);
  }
  @Override
  public AbstractArray executeArray(VirtualFrame frame) {
    return test.executeBoolean(frame) ? left.executeArray(frame) : right.executeArray(frame);
  }

  @Override
  public String toString() {
    return "if(" + test.toString() + "," + left.toString() + "," + right.toString() + ")";
  }
}
