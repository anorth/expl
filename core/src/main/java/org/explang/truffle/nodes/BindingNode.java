package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.truffle.ExplFunction;

/**
 * A node that binds a valueNode to a slot in the current frame.
 */
public final class BindingNode extends ExpressionNode {
  private final FrameSlot slot;
  @Child private ExpressionNode valueNode;

  public BindingNode(FrameSlot slot, ExpressionNode valueNode) {
    super(valueNode.type);
    this.slot = slot;
    this.valueNode = valueNode;
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    double v = this.valueNode.executeDouble(frame);
    frame.setDouble(slot, v);
    return v;
  }

  @Override
  public ExplFunction executeFunction(VirtualFrame frame) {
    ExplFunction f = this.valueNode.executeFunction(frame);
    frame.setObject(slot, f);
    return f;
  }

  @Override
  public String toString() { return "(" + slot.getIdentifier() + "=" + valueNode + ")"; }
}
