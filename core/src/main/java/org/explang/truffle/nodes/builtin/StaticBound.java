package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.nodes.BaseNode;
import org.explang.truffle.nodes.ExpressionNode;

/**
 * Nodes for static (compile-time) references.
 */
public final class StaticBound {
  /** Builds a node statically resolving to a built-in function */
  public static ExpressionNode builtIn(BuiltInNode builtin) {
    return function(BuiltInNode.createBuiltin(builtin));
  }

  /** Builds a node statically resolving to a function */
  public static ExpressionNode function(ExplFunction f) {
    return new BaseNode(f.type()) {
      @Override
      public ExplFunction executeFunction(VirtualFrame frame) {
        return f;
      }

      @Override
      public String toString() {
        return f.callTarget().toString();
      }
    };
  }
}
