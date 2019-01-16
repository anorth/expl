package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.truffle.nodes.ArgReadNode;

import static org.explang.syntax.Type.DOUBLE;

@NodeInfo(shortName = "sqrt")
public final class SqrtBuiltIn extends BuiltInNode {
  public SqrtBuiltIn() { super("sqrt", DOUBLE, DOUBLE); }

  @Override
  public double executeDouble(VirtualFrame frame) {
    return Math.sqrt(ArgReadNode.readDouble(frame, 0));
  }
}
