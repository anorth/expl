package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.truffle.ExplFunction;

public final class FunctionNodes {
  public static ExpressionNode literal(ExplFunction v) {
    return new BaseNode(v.type()) {
      @Override
      public ExplFunction executeFunction(VirtualFrame frame) {
        return v;
      }

      @Override
      public String toString() {
        return v.toString();
      }
    };
  }
}
