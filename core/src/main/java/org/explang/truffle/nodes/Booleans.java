package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.truffle.Type;

/**
 * Strongly typed multiplication nodes.
 */
public final class Booleans {
    public static ExpressionNode literal(boolean v) {
    return new ExpressionNode(Type.BOOL) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return v;
      }

      @Override
      public String toString() {
        return Boolean.toString(v);
      }
    };
  }

  // Comparison
  public static ExpressionNode eq(ExpressionNode left, ExpressionNode right) {
    return new BinaryNode(Type.BOOL, "==", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeBoolean(frame) == right.executeBoolean(frame);
      }
    };
  }
  public static ExpressionNode ne(ExpressionNode left, ExpressionNode right) {
    return new BinaryNode(Type.BOOL, "<>", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeBoolean(frame) != right.executeBoolean(frame);
      }
    };
  }
}
