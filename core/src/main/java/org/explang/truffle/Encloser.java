package org.explang.truffle;

import java.util.Optional;

import com.oracle.truffle.api.frame.FrameDescriptor;
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
   * Allocates a materialized frame for the closure and copies enclosed values to it.
   */
  public Optional<MaterializedFrame> enclose(VirtualFrame frame) {
    if (bindings.length > 0) {
      MaterializedFrame closure = getRuntime().createMaterializedFrame(NO_ARGS, descriptor);
      for (FrameBinding b : bindings) {
        b.copy(frame, closure);
      }
      return Optional.of(closure);
    } else {
      return Optional.empty();
    }
  }
}
