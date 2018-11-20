package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.Type;

//@TypeSystemReference(TruffleTypes.class)
@NodeInfo(language = "Expl", description = "Abstract base node for all expressions")
public abstract class ExpressionNode extends Node {
  public final Type type;

  protected ExpressionNode(Type type) { this.type = type; }

  public double executeDouble(VirtualFrame frame) {
    checkType(Type.DOUBLE);
    throw new AssertionError("No implementation for double");
  }
  public ExplFunction executeFunction(VirtualFrame frame) {
    throw new AssertionError("No implementation for function");
  }

  protected void checkType(Type expected) {
    assert type.equals(expected):
        String.format("Expecting type %s but %s is %s", expected, this, type);
  }

  protected void checkTypeIsFunction() {
    assert type.isFunction() :
        String.format("Expecting a function type but %s is %s", this, type);
  }
}
