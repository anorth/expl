package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.truffle.Type;

/**
 * Strongly typed addition nodes.
 */
public final class SumNode {
  public static ExpressionNode addDouble(ExpressionNode left,
      ExpressionNode right) {
    return new BinaryNode(Type.DOUBLE, left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return left.executeDouble(frame) + right.executeDouble(frame);
      }

      @Override
      public String toString() { return "+(" + this.left + "," + this.right + ")"; }
    };
  }

  public static ExpressionNode subDouble(ExpressionNode left,
      ExpressionNode right) {
    return new BinaryNode(Type.DOUBLE, left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return left.executeDouble(frame) - right.executeDouble(frame);
      }

      @Override public String toString() { return "-(" + this.left + "," + this.right + ")"; }
    };
  }
}
