package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.array.ArrayValue;
import org.explang.syntax.RuntimeTypeError;
import org.explang.syntax.Type;
import org.explang.truffle.ExplFunction;

/**
 * A symbol to be resolved at runtime in the executing frame.
 */
@NodeInfo(shortName = "Symbol")
public final class SymbolNode extends ExpressionNode {
  private final FrameSlot value;

  public SymbolNode(Type t, FrameSlot value) {
    super(t);
    this.value = value;
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    assertType(Type.BOOL);
    try {
      return frame.getBoolean(value);
    } catch (FrameSlotTypeException e) {
      throw fail(e);
    }
  }
  @Override
  public long executeLong(VirtualFrame frame) {
    assertType(Type.LONG);
    try {
      return frame.getLong(value);
    } catch (FrameSlotTypeException e) {
      throw fail(e);
    }
  }
  @Override
  public double executeDouble(VirtualFrame frame) {
    assertType(Type.DOUBLE);
    try {
      return frame.getDouble(value);
    } catch (FrameSlotTypeException e) {
      throw fail(e);
    }
  }
  @Override
  public ArrayValue executeArray(VirtualFrame frame) {
    assertTypeIsArray();
    try {
      return (ArrayValue) frame.getObject(value);
    } catch (FrameSlotTypeException | ClassCastException e) {
      throw fail(e);
    }
  }
  @Override
  public ExplFunction executeFunction(VirtualFrame frame) {
    assertTypeIsFunction();
    try {
      return (ExplFunction) frame.getObject(value);
    } catch (FrameSlotTypeException | ClassCastException e) {
      throw fail(e);
    }
  }

  @Override
  public String toString() { return value.getIdentifier().toString(); }

  private RuntimeTypeError fail(Throwable cause) {
      throw new RuntimeTypeError(
          String.format("Failed to find %s of type %s", value.getIdentifier(), type()), cause);
  }
}
