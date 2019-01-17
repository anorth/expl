package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.AbstractArray;
import org.explang.array.ArrayOfDouble;
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
      public AbstractArray executeArray(VirtualFrame frame) {
        long dim1 = ArgReadNode.readLong(frame, 0);
        if (dim1 > Integer.MAX_VALUE) { // The practical limit is much smaller
          throw new RuntimeException("Array size too big: " + dim1);
        }
        return ArrayOfDouble.zeros((int)dim1);
      }
    };
  }

  public static BuiltInNode sum() {
    return new BuiltInNode("sum", DOUBLE, array(DOUBLE, 1)) {
      // TODO: relax type to array of unknown dimension
      @Override
      public double executeDouble(VirtualFrame frame) {
        ArrayOfDouble arr = (ArrayOfDouble) ArgReadNode.readArray(frame, 0);
        return arr.sum();
      }
    };
  }
}
