package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.truffle.nodes.ArgReadNode;

import static org.explang.syntax.Type.DOUBLE;

@SuppressWarnings("unused") // Installed via reflection
public final class MathBuiltins {
  public static BuiltInNode sqrt() {
    return new BuiltInNode("sqrt", DOUBLE, DOUBLE) {
      @Override
      public double executeDouble(VirtualFrame frame) {
        return Math.sqrt(ArgReadNode.readDouble(frame, 0));
      }
    };
  }
}
