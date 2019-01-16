package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import org.explang.syntax.Func;
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
    RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(
        new CallRootNode(builtin, new FrameDescriptor(), Discloser.EMPTY)
    );
    return new ExplFunction(builtin.type(), callTarget, Encloser.EMPTY);
  }

  private final String name;

  BuiltInNode(Func type, String name) {
    super(type);
    this.name = name;
  }

  @Override
  public Func type() {
    return (Func) super.type();
  }

  /** The name to which this built-in should be bound. */
  public String name() { return name; }

  @Override
  public String toString() { return "#" + name; }
}
