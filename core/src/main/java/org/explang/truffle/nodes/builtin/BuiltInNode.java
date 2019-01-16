package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import org.explang.syntax.FuncType;
import org.explang.syntax.Type;
import org.explang.truffle.Discloser;
import org.explang.truffle.Encloser;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.nodes.BaseNode;
import org.explang.truffle.nodes.CallRootNode;

/**
 * Abstract base class for all built-in function implementations.
 * <p>
 * Built-ins are expected to have no child nodes, but read their arguments directly from
 * the call frame.
 */
public abstract class BuiltInNode extends BaseNode {
  /** Creates an function node for a built-in. */
  public static ExplFunction createBuiltin(BuiltInNode builtin) {
    RootCallTarget callTarget = Truffle.getRuntime().createCallTarget(
        new CallRootNode(builtin, new FrameDescriptor(), Discloser.EMPTY)
    );
    return new ExplFunction(builtin.funcType, callTarget, Encloser.EMPTY);
  }

  /** The name to which this built-in should be bound. */
  public final String name;
  /** The type of the function wrapping this built-in implementation */
  public final FuncType funcType;

  /**
   * @param name the name of this built-in
   * @param result the type of the result of this built-in
   */
  BuiltInNode(String name, Type result, Type... parameters) {
    // The type of the implementation is the "return type" of the function call (c.f. operators)
    super(result);
    this.name = name;
    this.funcType = Type.function(result, parameters);
  }

  @Override
  public String toString() { return "#" + name; }
}
