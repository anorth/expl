package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.truffle.Type;

/**
 * Strongly typed multiplication nodes.
 */
public final class ProductNode {
  public static ExpressionNode mulDouble(ExpressionNode left, ExpressionNode right) {
    return new BinaryNode(Type.DOUBLE, left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return left.executeDouble(frame) * right.executeDouble(frame);
      }

      @Override
      public String toString() { return "*(" + this.left + "," + this.right + ")"; }
    };
  }

  public static ExpressionNode divDouble(ExpressionNode left,
      ExpressionNode right) {
    return new BinaryNode(Type.DOUBLE, left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return left.executeDouble(frame) / right.executeDouble(frame);
      }

      @Override
      public String toString() { return "/(" + this.left + "," + this.right + ")"; }
    };
  }
}
