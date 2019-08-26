package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.LongRangeValue;
import org.explang.syntax.Type;
import org.explang.truffle.nodes.ArgReadNode;

@SuppressWarnings("unused") // Installed via reflection
public final class RangeBuiltins {
  /** Builds a 1-d array of zeros */
  public static BuiltInNode range() {
    return new BuiltInNode("range", Type.range(Type.LONG), Type.LONG, Type.LONG, Type.LONG) {
      @Override
      public LongRangeValue executeObject(VirtualFrame frame) {
        long first = ArgReadNode.readLong(frame, 0);
        long last = ArgReadNode.readLong(frame, 1);
        long step = ArgReadNode.readLong(frame, 2);
        return new LongRangeValue(first, last, step);
      }
    };
  }
}
