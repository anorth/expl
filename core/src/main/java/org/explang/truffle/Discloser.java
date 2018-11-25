package org.explang.truffle;

import javax.annotation.Nullable;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Copies values from a closure frame to the call frame at the top of a function invocation.
 */
public final class Discloser {
  public static final Discloser EMPTY = new Discloser(new SlotBinding[]{});

  private final SlotBinding[] bindings;

  /**
   * @param bindings closure references and corresponding callee frame slots
   */
  public Discloser(SlotBinding[] bindings) {
    this.bindings = bindings;
  }

  /**
   * Copies values from the closure frame to the call frame.
   */
  public void disclose(@Nullable MaterializedFrame closure, VirtualFrame frame) {
    if (closure != null) {
      assert bindings.length > 0: "Closure with no disclosure bindings";
      for (SlotBinding b : bindings) {
        b.copy(closure, frame);
      }
    } else {
      assert bindings.length == 0: "Disclosure bindings with no closure";
    }
  }
}
