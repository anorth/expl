package org.explang.truffle;

import javax.annotation.Nullable;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import static com.oracle.truffle.api.Truffle.getRuntime;

/**
 * Captures values from a function definition frame to a closure frame.
 */
public final class Encloser {
  public static final Encloser EMPTY = new Encloser(new FrameDescriptor(), new FrameBinding[]{});

  private static final Object[] NO_ARGS = new Object[]{};

  private final FrameDescriptor descriptor;
  private final FrameBinding[] bindings;

  /**
   * @param descriptor descriptor for the closure frame
   * @param bindings enclosed references and corresponding closure frame slots
   */
  public Encloser(FrameDescriptor descriptor, FrameBinding[] bindings) {
    this.bindings = bindings;
    this.descriptor = descriptor;
  }

  /**
   * Allocates a materialized frame for the closure and copies enclosed values to it, if there
   * are any bindings.
   */
  public @Nullable MaterializedFrame enclose(VirtualFrame frame) {
    if (bindings.length > 0) {
      MaterializedFrame closure = makeFrame();
      for (FrameBinding b : bindings) {
        b.copy(frame, closure);
      }
      return closure;
    }
    return null;
  }

  /**
   * Copies a single value from a frame, if it is bound by this encloser.
   *
   * @param frame the frame to copy to, or null to allocate a new one if necessary
   * @param sourceSlot slot in the source frame from which to copy
   * @param closure target frame
   * @return the provided closure frame, or a newly allocated one if it was and a value was copied
   */
  public @Nullable MaterializedFrame enclose(VirtualFrame frame, FrameSlot sourceSlot,
      @Nullable MaterializedFrame closure) {
    for (FrameBinding binding : bindings) {
      if (binding instanceof FrameBinding.SlotBinding) {
        FrameBinding.SlotBinding slotBinding = (FrameBinding.SlotBinding) binding;
        if (slotBinding.sourceSlot == sourceSlot) {
          if (closure == null) {
            closure = makeFrame();
          }
          slotBinding.copy(frame, closure);
        }
      }
    }
    return closure;
  }

  private MaterializedFrame makeFrame() {
    return getRuntime().createMaterializedFrame(NO_ARGS, descriptor);
  }
}
