package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.syntax.Type;

/**
 * Operator nodes for long arguments.
 */
public final class Longs {
  public static ExpressionNode literal(long v) {
    return new BaseNode(Type.LONG) {
      @Override
      public long executeLong(VirtualFrame frame) {
        return v;
      }
      @Override
      public String toString() {
        return Long.toString(v);
      }
    };
  }

  // Arithmetic
  public static ExpressionNode negate(ExpressionNode child) {
    return new BaseNode(Type.LONG) {
      @Override
      public long executeLong(VirtualFrame frame) {
        return -child.executeLong(frame);
      }

      @Override
      public String toString() { return "-(" + child + ")"; }

    };
  }
  public static ExpressionNode exp(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.LONG, "^", left, right) {
      @Override
      public long executeLong(VirtualFrame frame) {
        return (long) Math.pow(left.executeLong(frame), right.executeLong(frame));
      }
    };
  }
  public static ExpressionNode mul(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.LONG, "*", left, right) {
      @Override
      public long executeLong(VirtualFrame frame) {
        return left.executeLong(frame) * right.executeLong(frame);
      }
    };
  }
  public static ExpressionNode div(ExpressionNode left,
      ExpressionNode right) {
    return new BinOpNode(Type.LONG, "/", left, right) {
      @Override
      public long executeLong(VirtualFrame frame) {
        return left.executeLong(frame) / right.executeLong(frame);
      }
    };
  }
  public static ExpressionNode add(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.LONG, "+", left, right) {
      @Override
      public long executeLong(VirtualFrame frame) {
        return left.executeLong(frame) + right.executeLong(frame);
      }
    };
  }
  public static ExpressionNode sub(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.LONG, "-", left, right) {
      @Override
      public long executeLong(VirtualFrame frame) {
        return left.executeLong(frame) - right.executeLong(frame);
      }
    };
  }

  // Comparison
  public static ExpressionNode lt(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.BOOL, "<", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeLong(frame) < right.executeLong(frame);
      }
    };
  }
  public static ExpressionNode le(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.BOOL, "<=", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeLong(frame) <= right.executeLong(frame);
      }
    };
  }
  public static ExpressionNode gt(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.BOOL, ">", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeLong(frame) > right.executeLong(frame);
      }
    };
  }
  public static ExpressionNode ge(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.BOOL, ">=", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeLong(frame) >= right.executeLong(frame);
      }
    };
  }
  public static ExpressionNode eq(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.BOOL, "==", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeLong(frame) == right.executeLong(frame);
      }
    };
  }
  public static ExpressionNode ne(ExpressionNode left, ExpressionNode right) {
    return new BinOpNode(Type.BOOL, "<>", left, right) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return left.executeLong(frame) != right.executeLong(frame);
      }
    };
  }
}
