package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.nodes.ExpressionNode;

/**
 * Node for a static (compile-time) reference.
 */
public final class StaticBoundNode<T> extends ExpressionNode {
  /** Builds a node statically resolving to a built-in function */
  public static ExpressionNode builtIn(BuiltInNode<?> builtin) {
    return function(BuiltInNode.createBuiltin(builtin));
  }

  /** Builds a node statically resolving to a function */
  public static ExpressionNode function(ExplFunction f) {
    return new ExpressionNode() {
      @Override
      public ExplFunction executeFunction(VirtualFrame frame) {
        return f;
      }

      @Override
      public String toString() {
        return f.callTarget.toString();
      }
    };
  }
}
