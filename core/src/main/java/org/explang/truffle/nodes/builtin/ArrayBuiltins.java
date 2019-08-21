package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import org.explang.array.ArrayValue;
import org.explang.array.ArrayValueKt;
import org.explang.array.DoubleArrayValue;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.nodes.ArgReadNode;

import static org.explang.syntax.Type.BOOL;
import static org.explang.syntax.Type.DOUBLE;
import static org.explang.syntax.Type.LONG;
import static org.explang.syntax.Type.function;
import static org.explang.syntax.Type.slice;

@SuppressWarnings("unused") // Installed via reflection
public final class ArrayBuiltins {
  /** Builds a 1-d array of zeros */
  public static BuiltInNode zeros() {
    return new BuiltInNode("zeros", slice(DOUBLE), LONG) {
      // TODO: add element type as parameter for double/long.
      @Override
      public ArrayValue executeObject(VirtualFrame frame) {
        long size = ArgReadNode.readLong(frame, 0);
        if (size > Integer.MAX_VALUE) { // The practical limit is much smaller
          throw new RuntimeException("Array size too big: " + size);
        }
        return new DoubleArrayValue(new double[(int)size]); // FIXME slice
      }
    };
  }

  public static BuiltInNode filter() {
    // 1-d arrays only
    return new BuiltInNode("filter", slice(DOUBLE), slice(DOUBLE), function(BOOL, DOUBLE)) {
      @Override
      public ArrayValue executeObject(VirtualFrame frame) {
        DoubleArrayValue arr = ArgReadNode.readObject(frame, 0, DoubleArrayValue.class);
        ExplFunction f = ArgReadNode.readFunction(frame, 1);

        Call1 caller = new Call1(f);
        return arr.filter(caller::callBoolean);
      }
    };
  }

  public static BuiltInNode map() {
    return new BuiltInNode("map", slice(DOUBLE), slice(DOUBLE), function(DOUBLE, DOUBLE)) {
      @Override
      public ArrayValue executeObject(VirtualFrame frame) {
        DoubleArrayValue arr = ArgReadNode.readObject(frame, 0, DoubleArrayValue.class);
        ExplFunction f = ArgReadNode.readFunction(frame, 1);

        // Actually doing the function call here will be slower than necessary. We want to inline
        // this at compile time. That might require special effort since map is a built-in,
        // where more general inlining strategies can't help.
        Call1 caller = new Call1(f);
        return ArrayValueKt.mapToDouble(arr, caller::callDouble);
      }
    };
  }

  public static BuiltInNode fold() {
    // 1-d arrays only
    return new BuiltInNode("fold", DOUBLE, slice(DOUBLE), DOUBLE, function(DOUBLE, DOUBLE, DOUBLE)) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        DoubleArrayValue arr = ArgReadNode.readObject(frame, 0, DoubleArrayValue.class);
        double init = ArgReadNode.readDouble(frame, 1);
        ExplFunction f = ArgReadNode.readFunction(frame, 2);

        Call2 caller = new Call2(f);
        return arr.fold(init, caller::callDouble);
      }
    };
  }

  public static BuiltInNode reduce() {
    return new BuiltInNode("reduce", DOUBLE, slice(DOUBLE), function(DOUBLE, DOUBLE, DOUBLE)) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        DoubleArrayValue arr = ArgReadNode.readObject(frame, 0, DoubleArrayValue.class);
        ExplFunction f = ArgReadNode.readFunction(frame, 1);

        Call2 caller = new Call2(f);
        return arr.reduce(caller::callDouble);
      }
    };
  }

  /** Wraps a call to a unary function */
  private static class Call1 {
    private final DirectCallNode node;
    private final Object[] args = new Object[2];

    Call1(ExplFunction fn) {
      this.node = Truffle.getRuntime().createDirectCallNode(fn.callTarget());
      args[0] = fn.closure().orElse(null);
    }

    boolean callBoolean(Object arg) {
      args[1] = arg;
      return (boolean) node.call(args);
    }
    double callDouble(Object arg) {
      args[1] = arg;
      return (double) node.call(args);
    }
  }

  private static class Call2 {
    private final DirectCallNode node;
    private final Object[] args = new Object[3];

    Call2(ExplFunction fn) {
      this.node = Truffle.getRuntime().createDirectCallNode(fn.callTarget());
      args[0] = fn.closure().orElse(null);
    }

    boolean callBoolean(Object a1, Object a2) {
      args[1] = a1;
      args[2] = a2;
      return (boolean) node.call(args);
    }
    double callDouble(Object a1, Object a2) {
      args[1] = a1;
      args[2] = a2;
      return (double) node.call(args);
    }
  }
}
