package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.nodes.ExpressionNode;
import org.explang.truffle.nodes.ExpressionRootNode;

/**
 * Abstract base class for all built-in functions implementations.
 * <p>
 * Built-ins are expected to have no child nodes, but read their arguments directly from
 * the call frame.
 */
public abstract class BuiltInNode<T> extends ExpressionNode<T> {
  /**
   * Creates an function node for a built-in.
   */
  public static ExplFunction createBuiltin(BuiltInNode<?> builtin) {
    RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(
        new ExpressionRootNode(builtin, new FrameDescriptor())
    );
    return new ExplFunction(callTarget);
  }

  private final String name;

  BuiltInNode(String name) {
    this.name = name;
  }

  /** The name to which this built-in should be bound. */
  public String name() { return name; }

  @Override
  public String toString() { return "#" + name; }
}
