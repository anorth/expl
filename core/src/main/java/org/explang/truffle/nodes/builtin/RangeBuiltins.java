package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.ArrayValue;
import org.explang.array.LongRangeValue;
import org.explang.array.RangeValue;
import org.explang.array.RangeValueKt;
import org.explang.syntax.Type;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.nodes.ArgReadNode;

import static org.explang.syntax.Type.LONG;
import static org.explang.syntax.Type.array;
import static org.explang.syntax.Type.function;

@SuppressWarnings("unused") // Installed via reflection
public final class RangeBuiltins {
  /** Builds a 1-d array of zeros */
  public static BuiltInNode range() {
    // TODO: add element type as parameter for double/long.
    return new BuiltInNode("range", Type.range(LONG), LONG, LONG, LONG) {
      @Override
      public LongRangeValue executeObject(VirtualFrame frame) {
        long first = ArgReadNode.readLong(frame, 0);
        long last = ArgReadNode.readLong(frame, 1);
        long step = ArgReadNode.readLong(frame, 2);
        return new LongRangeValue(first, last, step);
      }
    };
  }

  public static BuiltInNode map() {
    return new BuiltInNode("mapr", array(LONG), Type.range(LONG), function(LONG, LONG)) {
      @Override
      @SuppressWarnings("unchecked")
      public ArrayValue<Long> executeObject(VirtualFrame frame) {
        RangeValue<Long> s = ArgReadNode.readObject(frame, 0, RangeValue.class);
        ExplFunction f = ArgReadNode.readFunction(frame, 1);

        // Actually doing the function call here will be slower than necessary. We want to inline
        // this at compile time. That might require special effort since map is a built-in,
        // where more general inlining strategies can't help.
        Callers.Call1 caller = new Callers.Call1(f);
        return RangeValueKt.mapToLong(s, caller::callLong);
      }
    };
  }

  public static BuiltInNode fold() {
    // 1-d arrays only
    return new BuiltInNode("foldr", LONG, Type.range(LONG), LONG, function(LONG, LONG, LONG)) {
      @Override
      @SuppressWarnings("unchecked")
      public long executeLong(VirtualFrame frame) {
        RangeValue<Long> s = ArgReadNode.readObject(frame, 0, RangeValue.class);
        long init = ArgReadNode.readLong(frame, 1);
        ExplFunction f = ArgReadNode.readFunction(frame, 2);

        Callers.Call2 caller = new Callers.Call2(f);
        return RangeValueKt.fold(s, init, caller::callLong);
      }
    };
  }

  public static BuiltInNode reduce() {
    return new BuiltInNode("reducer", LONG, Type.range(LONG), function(LONG, LONG, LONG)) {
      @Override
      @SuppressWarnings("unchecked")
      public long executeLong(VirtualFrame frame) {
        RangeValue<Long> s = ArgReadNode.readObject(frame, 0, RangeValue.class);
        ExplFunction f = ArgReadNode.readFunction(frame, 1);

        Callers.Call2 caller = new Callers.Call2(f);
        return RangeValueKt.reduce(s, caller::callLong);
      }
    };
  }
}
