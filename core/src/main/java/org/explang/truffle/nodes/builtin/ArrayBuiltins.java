package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.ArrayValue;
import org.explang.array.DoubleArrayValue;
import org.explang.truffle.nodes.ArgReadNode;

import static org.explang.syntax.Type.DOUBLE;
import static org.explang.syntax.Type.LONG;
import static org.explang.syntax.Type.array;

public final class ArrayBuiltins {
  /** Builds a 1-d array of zeros */
  public static BuiltInNode zeros() {
    return new BuiltInNode("zeros", array(DOUBLE, 1), LONG) {
      // TODO: add element type as parameter for double/long.
      // TODO: use guest language varargs or a record to specify more than one dimension
      @Override
      public ArrayValue executeArray(VirtualFrame frame) {
        long size = ArgReadNode.readLong(frame, 0);
        if (size > Integer.MAX_VALUE) { // The practical limit is much smaller
          throw new RuntimeException("Array size too big: " + size);
        }
        return new DoubleArrayValue(new double[(int)size]);
      }
    };
  }

  public static BuiltInNode sum() {
    return new BuiltInNode("sum", DOUBLE, array(DOUBLE, 1)) {
      // TODO: relax type to array of unknown dimension
      @Override
      public double executeDouble(VirtualFrame frame) {
        DoubleArrayValue arr = (DoubleArrayValue) ArgReadNode.readArray(frame, 0);
        return arr.sum();
      }
    };
  }
}
