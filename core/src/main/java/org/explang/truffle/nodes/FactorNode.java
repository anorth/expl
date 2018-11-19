package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Strongly typed exponentiation node.
 */
public final class FactorNode {
  public static ExpressionNode<Double> expDouble(ExpressionNode<Double> left,
      ExpressionNode<Double> right) {
    return new BinaryNode<Double>(left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return Math.pow( left.executeDouble(frame), right.executeDouble(frame));
      }
    };
  }
}