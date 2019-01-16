package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.truffle.ExplFunction;

/**
 * Binds a value to a slot in the current frame.
 */
@NodeInfo(shortName = "Bind")
public final class BindingNode extends ExpressionNode {
  public final FrameSlot slot;
  @Child private ExpressionNode valueNode;

  /**
   * @param slot the target slot to be bound
   * @param valueNode provides the bound value
   */
  public BindingNode(FrameSlot slot, ExpressionNode valueNode) {
    super(valueNode.type());
    this.slot = slot;
    this.valueNode = valueNode;
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    boolean v = this.valueNode.executeBoolean(frame);
    frame.setBoolean(slot, v);
    return v;

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
    // Capture recursive reference in the closure after setting the function symbol in the frame
    f.capture(frame, slot);
    return f;
  }

  @Override
  public String toString() { return "(" + slot.getIdentifier() + "=" + valueNode + ")"; }
}
