package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.AbstractArray;

public final class ArrayNodes {
  public static ExpressionNode literal(AbstractArray v) {
    return new BaseNode(v.type()) {
      @Override
      public AbstractArray executeArray(VirtualFrame frame) {
        return v;
      }

      @Override
      public String toString() {
        return v.toString();
      }
    };
  }
}
