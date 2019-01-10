package org.explang.truffle;

import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotTypeException;

/**
 * A mapping between two frame slots or arguments (usually in different frames).
 */
public interface FrameBinding {
  /** Copies the value from the source frame reference to the target frame reference. */
  void copy(Frame sourceFrame, Frame targetFrame);

  class SlotBinding implements FrameBinding {
    private final FrameSlot sourceSlot;
    private final FrameSlot targetSlot;

    public SlotBinding(FrameSlot sourceSlot, FrameSlot targetSlot) {
      this.sourceSlot = sourceSlot;
      this.targetSlot = targetSlot;
    }

    @Override
    public void copy(Frame sourceFrame, Frame targetFrame) {
      assert sourceFrame.getFrameDescriptor().getFrameSlotKind(sourceSlot) ==
          targetFrame.getFrameDescriptor().getFrameSlotKind(targetSlot) :
          "Mismatched slot binding types, source " + sourceSlot + ", target " + targetSlot;
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

  class ArgumentBinding implements FrameBinding {
    private final int sourceIndex;
    private final FrameSlot targetSlot;

    public ArgumentBinding(int sourceIndex, FrameSlot targetSlot) {
      this.sourceIndex = sourceIndex;
      this.targetSlot = targetSlot;
    }

    @Override
    public void copy(Frame sourceFrame, Frame targetFrame) {
      switch (targetFrame.getFrameDescriptor().getFrameSlotKind(targetSlot)) {
        case Object:
          targetFrame.setObject(targetSlot, sourceFrame.getArguments()[sourceIndex]);
          break;
        case Double:
          targetFrame.setDouble(targetSlot, (Double) sourceFrame.getArguments()[sourceIndex]);
          break;
        case Illegal:
        case Long:
        case Int:
        case Float:
        case Boolean:
        case Byte:
          throw new RuntimeException("Not implemented");
      }
    }
  }
}
