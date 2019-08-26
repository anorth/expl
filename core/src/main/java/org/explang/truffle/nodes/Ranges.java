package org.explang.truffle.nodes;

import javax.annotation.Nullable;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.LongRangeValue;
import org.explang.syntax.Type;

/**
 * Operator nodes for ranges
 */
public final class Ranges {
  public static ExpressionNode constructor(@Nullable ExpressionNode first,
      @Nullable ExpressionNode last, @Nullable ExpressionNode step) {
    return new BaseNode(Type.range(Type.LONG)) {
      @Override
      public LongRangeValue executeObject(VirtualFrame frame) {
        Long firstv = null, lastv = null, stepv = null;
        if (first != null) firstv = first.executeLong(frame);
        if (last != null) lastv = last.executeLong(frame);
        if (step != null) stepv = step.executeLong(frame);
        return LongRangeValue.Companion.of(firstv, lastv, stepv);
      }
    };
  }
}
