package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Strongly typed multiplication nodes.
 */
public final class ProductNode {
  public static ExpressionNode<Double> mulDouble(ExpressionNode<Double> left,
      ExpressionNode<Double> right) {
    return new BinaryNode<Double>(left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return left.executeDouble(frame) * right.executeDouble(frame);
      }
    };
  }

  public static ExpressionNode<Double> divDouble(ExpressionNode<Double> left,
      ExpressionNode<Double> right) {
    return new BinaryNode<Double>(left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return left.executeDouble(frame) / right.executeDouble(frame);
      }
    };
  }
}
