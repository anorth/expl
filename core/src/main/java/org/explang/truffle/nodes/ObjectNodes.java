package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.syntax.Type;

public final class ObjectNodes {
  public static ExpressionNode obj(Object v, Type t) {
    return new BaseNode(t) {
      @Override
      public Object executeObject(VirtualFrame frame) {
        return v;
      }
      @Override
      public String toString() { return v.toString(); }
    };
  }
}
