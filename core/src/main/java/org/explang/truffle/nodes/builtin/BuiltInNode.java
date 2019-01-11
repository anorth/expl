package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import org.explang.syntax.Type;
import org.explang.truffle.Discloser;
import org.explang.truffle.Encloser;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.nodes.CallRootNode;
import org.explang.truffle.nodes.ExpressionNode;

/**
 * Abstract base class for all built-in functions implementations.
 * <p>
 * Built-ins are expected to have no child nodes, but read their arguments directly from
 * the call frame.
 */
public abstract class BuiltInNode extends ExpressionNode {
  /**
   * Creates an function node for a built-in.
   */
  public static ExplFunction createBuiltin(BuiltInNode builtin) {
    assert builtin.type().isFunction():
        String.format("Expected built-in to have function type but got %s", builtin.type());
    RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(
        new CallRootNode(builtin, new FrameDescriptor(), Discloser.EMPTY)
    );
    return new ExplFunction(builtin.type(), callTarget, Encloser.EMPTY);
  }

  private final String name;

  BuiltInNode(Type t, String name) {
    super(t);
    this.name = name;
  }

  /** The name to which this built-in should be bound. */
  public String name() { return name; }

  @Override
  public String toString() { return "#" + name; }
}
