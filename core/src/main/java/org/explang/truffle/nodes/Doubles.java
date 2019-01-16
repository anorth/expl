package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.syntax.Type;

/**
 * Operator nodes for double arguments.
 */
public final class Doubles {
  public static ExpressionNode literal(double v) {
    return new BaseNode(Type.DOUBLE) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return v;
      }
      @Override
      public String toString() {
        return Double.toString(v);
      }
    };
  }

  // Arithmetic
  public static ExpressionNode negate(ExpressionNode child) {
    return new BaseNode(Type.DOUBLE) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return -child.executeDouble(frame);
      }

      @Override
      public String toString() { return "-(" + child + ")"; }

    };
  }
  public static ExpressionNode exp(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.DOUBLE, "^", left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return Math.pow(left.executeDouble(frame), right.executeDouble(frame));
      }
    };
  }
  public static ExpressionNode mul(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.DOUBLE, "*", left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return left.executeDouble(frame) * right.executeDouble(frame);
      }
    };
  }
  public static ExpressionNode div(ExpressionNode left,
      ExpressionNode right) {
    return new BinOpNode(Type.DOUBLE, "/", left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return left.executeDouble(frame) / right.executeDouble(frame);
      }
    };
  }
  public static ExpressionNode add(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.DOUBLE, "+", left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return left.executeDouble(frame) + right.executeDouble(frame);
      }
    };
  }
  public static ExpressionNode sub(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.DOUBLE, "-", left, right) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return left.executeDouble(frame) - right.executeDouble(frame);
      }
    };
  }

  // Comparison
  public static ExpressionNode lt(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.BOOL, "<", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeDouble(frame) < right.executeDouble(frame);
      }
    };
  }
  public static ExpressionNode le(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.BOOL, "<=", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeDouble(frame) <= right.executeDouble(frame);
      }
    };
  }
  public static ExpressionNode gt(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.BOOL, ">", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeDouble(frame) > right.executeDouble(frame);
      }
    };
  }
  public static ExpressionNode ge(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.BOOL, ">=", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeDouble(frame) >= right.executeDouble(frame);
      }
    };
  }
  public static ExpressionNode eq(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.BOOL, "==", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeDouble(frame) == right.executeDouble(frame);
      }
    };
  }
  public static ExpressionNode ne(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.BOOL, "<>", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeDouble(frame) != right.executeDouble(frame);
      }
    };
  }
}
