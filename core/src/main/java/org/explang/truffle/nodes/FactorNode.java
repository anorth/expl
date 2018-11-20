package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.truffle.Type;

/**
 * Strongly typed exponentiation node.
 */
public final class FactorNode {
  public static ExpressionNode expDouble(ExpressionNode left,
      ExpressionNode right) {
    return new BinaryNode(Type.DOUBLE, left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return Math.pow( left.executeDouble(frame), right.executeDouble(frame));
      }

      @Override
      public String toString() { return "^(" + this.left + "," + this.right + ")"; }
    };
  }
}
