package org.explang.truffle.nodes;

import javax.annotation.Nullable;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.SlicerValue;
import org.explang.syntax.Type;

/**
 * Operator nodes for ranges
 */
public final class Ranges {
  public static ExpressionNode constructor(@Nullable ExpressionNode first,
      @Nullable ExpressionNode last, @Nullable ExpressionNode step) {
    return new BaseNode(Type.range(Type.LONG)) {
      @Override
      public SlicerValue executeObject(VirtualFrame frame) {
        Integer firstv = null, lastv = null, stepv = null;
        if (first != null) firstv = Math.toIntExact(first.executeLong(frame));
        if (last != null) lastv = Math.toIntExact(last.executeLong(frame));
        if (step != null) stepv = Math.toIntExact(step.executeLong(frame));
        return SlicerValue.Companion.of(firstv, lastv, stepv);
      }
    };
  }
}
