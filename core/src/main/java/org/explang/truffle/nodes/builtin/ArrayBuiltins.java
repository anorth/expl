package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.ArrayValue;
import org.explang.array.ArrayValueKt;
import org.explang.array.DoubleArrayValue;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.nodes.ArgReadNode;

import static org.explang.syntax.Type.BOOL;
import static org.explang.syntax.Type.DOUBLE;
import static org.explang.syntax.Type.LONG;
import static org.explang.syntax.Type.array;
import static org.explang.syntax.Type.function;

@SuppressWarnings("unused") // Installed via reflection
public final class ArrayBuiltins {
  /** Builds a 1-d array of zeros */
  public static BuiltInNode zeros() {
    return new BuiltInNode("zeros", array(DOUBLE), LONG) {
      // TODO: add element type as parameter for double/long.
      @Override
      public ArrayValue<Double> executeObject(VirtualFrame frame) {
        long size = ArgReadNode.readLong(frame, 0);
        if (size > Integer.MAX_VALUE) { // The practical limit is much smaller
          throw new RuntimeException("Slice size too big: " + size);
        }
        return DoubleArrayValue.Companion.of(new double[(int)size]);
      }
    };
  }

  public static BuiltInNode filter() {
    // 1-d arrays only
    // TODO: parametric polymorphism
    return new BuiltInNode("filter", array(DOUBLE), array(DOUBLE), function(BOOL, DOUBLE)) {
      @Override
      @SuppressWarnings("unchecked")
      public ArrayValue<Double> executeObject(VirtualFrame frame) {
        ArrayValue<Double> s = ArgReadNode.readObject(frame, 0, ArrayValue.class);
        ExplFunction f = ArgReadNode.readFunction(frame, 1);

        Callers.Call1 caller = new Callers.Call1(f);
        return s.filter(caller::callBoolean);
      }
    };
  }

  public static BuiltInNode map() {
    return new BuiltInNode("map", array(DOUBLE), array(DOUBLE), function(DOUBLE, DOUBLE)) {
      @Override
      @SuppressWarnings("unchecked")
      public ArrayValue<Double> executeObject(VirtualFrame frame) {
        ArrayValue<Double> s = ArgReadNode.readObject(frame, 0, ArrayValue.class);
        ExplFunction f = ArgReadNode.readFunction(frame, 1);

        // Actually doing the function call here will be slower than necessary. We want to inline
        // this at compile time. That might require special effort since map is a built-in,
        // where more general inlining strategies can't help.
        Callers.Call1 caller = new Callers.Call1(f);
        return ArrayValueKt.mapToDouble(s, caller::callDouble);
      }
    };
  }

  public static BuiltInNode fold() {
    // 1-d arrays only
    return new BuiltInNode("fold", DOUBLE, array(DOUBLE), DOUBLE, function(DOUBLE, DOUBLE, DOUBLE)) {
      @Override
      @SuppressWarnings("unchecked")
      public double executeDouble(VirtualFrame frame) {
        ArrayValue<Double> s = ArgReadNode.readObject(frame, 0, ArrayValue.class);
        double init = ArgReadNode.readDouble(frame, 1);
        ExplFunction f = ArgReadNode.readFunction(frame, 2);

        Callers.Call2 caller = new Callers.Call2(f);
        return ArrayValueKt.fold(s, init, caller::callDouble);
      }
    };
  }

  public static BuiltInNode reduce() {
    return new BuiltInNode("reduce", DOUBLE, array(DOUBLE), function(DOUBLE, DOUBLE, DOUBLE)) {
      @Override
      @SuppressWarnings("unchecked")
      public double executeDouble(VirtualFrame frame) {
        ArrayValue<Double> s = ArgReadNode.readObject(frame, 0, ArrayValue.class);
        ExplFunction f = ArgReadNode.readFunction(frame, 1);

        Callers.Call2 caller = new Callers.Call2(f);
        return ArrayValueKt.reduce(s, caller::callDouble);
      }
    };
  }

}
