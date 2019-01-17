package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.AbstractArray;
import org.explang.array.ArrayOfDouble;
import org.explang.truffle.nodes.ArgReadNode;

import static org.explang.syntax.Type.DOUBLE;
import static org.explang.syntax.Type.array;

public final class ArrayBuiltins {
  /** Builds a 1-d array of zeros */
  public static BuiltInNode zeros() {
    return new BuiltInNode("zeros", array(DOUBLE, 1), DOUBLE/*FIXME int*/) {
      // TODO: use guest language varargs or a record to specify more than one dimension
      @Override
      public AbstractArray executeArray(VirtualFrame frame) {
        int dim1 = (int) ArgReadNode.readDouble(frame, 0);
        return ArrayOfDouble.zeros(dim1);
      }
    };
  }

  public static BuiltInNode sum() {
    return new BuiltInNode("sum", DOUBLE, array(DOUBLE, 1)) {
      // TODO: relax type to array of unknown dimension
      // TODO: add optional arguments to select dimensions
      @Override
      public double executeDouble(VirtualFrame frame) {
        ArrayOfDouble arr = (ArrayOfDouble) ArgReadNode.readArray(frame, 0);
        return arr.sum();
      }
    };
  }
}
