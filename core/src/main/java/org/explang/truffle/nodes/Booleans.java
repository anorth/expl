package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.syntax.Type;

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

  // Unary
  public static ExpressionNode invert(ExpressionNode child) {
    return new ExpressionNode(Type.BOOL) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return !child.executeBoolean(frame);
      }
      @Override
      public String toString() { return "not(" + child + ")"; }

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

  // Conjunction
  public static ExpressionNode and(ExpressionNode left, ExpressionNode right) {
    return new BinaryNode(Type.BOOL, "and", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeBoolean(frame) && right.executeBoolean(frame);
      }
    };
  }
  public static ExpressionNode or(ExpressionNode left, ExpressionNode right) {
    return new BinaryNode(Type.BOOL, "or", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeBoolean(frame) || right.executeBoolean(frame);
      }
    };
  }
  public static ExpressionNode xor(ExpressionNode left, ExpressionNode right) {
    return new BinaryNode(Type.BOOL, "xor", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        boolean l = left.executeBoolean(frame);
        boolean r = right.executeBoolean(frame);
        return (l || r) && !(l && r);
      }
    };
  }
}
