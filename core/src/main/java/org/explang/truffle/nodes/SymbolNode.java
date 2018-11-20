package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.RuntimeTypeError;

/**
 * A symbol to be resolved at runtime in the executing frame.
 */
@NodeInfo(shortName = "Symbol")
public final class SymbolNode extends ExpressionNode {
  private final FrameSlot value;

  public SymbolNode(FrameSlot value) {
    this.value = value;
  }

  @Override
  public ExplFunction executeFunction(VirtualFrame frame) {
    try {
      return (ExplFunction) frame.getObject(value);
    } catch (FrameSlotTypeException | ClassCastException e) {
      throw new RuntimeTypeError(e);
    }
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    try {
      return frame.getDouble(value);
    } catch (FrameSlotTypeException e) {
      throw new RuntimeTypeError(e);
    }
  }

  @Override
  public String toString() {
    return value.toString();
  }
}