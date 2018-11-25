package org.explang.truffle;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;

/**
 * A mapping between two frame slots (usually in different frames).
 */
public class SlotBinding {
  public final FrameSlot sourceSlot;
  public final FrameSlot targetSlot;

  public SlotBinding(FrameSlot sourceSlot, FrameSlot targetSlot) {
    this.sourceSlot = sourceSlot;
    this.targetSlot = targetSlot;
  }

  public void copy(Frame sourceFrame, Frame targetFrame) {
    assert sourceFrame.getFrameDescriptor().getFrameSlotKind(sourceSlot) ==
        targetFrame.getFrameDescriptor().getFrameSlotKind(targetSlot);
    try {
      switch (sourceFrame.getFrameDescriptor().getFrameSlotKind(sourceSlot)) {
        case Object:
          targetFrame.setObject(targetSlot, sourceFrame.getObject(sourceSlot));
          break;
        case Double:
          targetFrame.setDouble(targetSlot, sourceFrame.getDouble(sourceSlot));
          break;
        case Illegal:
        case Long:
        case Int:
        case Float:
        case Boolean:
        case Byte:
          throw new RuntimeException("Not implemented");
      }
    } catch (FrameSlotTypeException e) {
      throw new RuntimeException(e);
    }

  }
}
