package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.syntax.FuncType;
import org.explang.syntax.Type;
import org.explang.truffle.ExplFunction;

//@TypeSystemReference(TruffleTypes.class)
@NodeInfo(language = "Expl", description = "Abstract base node for all expressions")
public abstract class ExpressionNode extends Node {
  private final Type type;

  protected ExpressionNode(Type type) { this.type = type; }

  /**
   * Returns the type provided at construction.
   *
   * Override this to return a type that wasn't known at construction.
   */
  public Type type() { return type; }

  public abstract boolean executeBoolean(VirtualFrame frame);
  public abstract double executeDouble(VirtualFrame frame);
  public abstract ExplFunction executeFunction(VirtualFrame frame);

  /**
   * Executes a node according to its declared type.
   */
  public final Object executeDeclaredType(VirtualFrame frame) {
    if (type() == Type.DOUBLE) {
      return executeDouble(frame);
    } else if (type() == Type.BOOL) {
      return executeBoolean(frame);
    } else {
      assert type() instanceof FuncType : "Unexpected type " + type();
      return executeFunction(frame);
    }
  }

  // Run-time type assertions (which should never fail if static type checks are sufficient).
  protected void assertType(Type expected) {
    assert type().equals(expected) :
        String.format("Expecting type %s but %s is %s", expected, this, type());
  }

  protected void assertTypeIsFunction() {
    assert type() instanceof FuncType :
        String.format("Expecting a function type but %s is %s", this, type());
  }
}
