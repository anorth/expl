package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Strongly typed addition nodes.
 */
public final class SumNode {
  public static ExpressionNode<Double> addDouble(ExpressionNode<Double> left,
      ExpressionNode<Double> right) {
    return new BinaryNode<Double>(left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return left.executeDouble(frame) + right.executeDouble(frame);
      }
    };
  }

  public static ExpressionNode<Double> subDouble(ExpressionNode<Double> left,
      ExpressionNode<Double> right) {
    return new BinaryNode<Double>(left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return left.executeDouble(frame) - right.executeDouble(frame);
      }
    };
  }
}
