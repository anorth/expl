package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "sqrt")
public final class SqrtBuiltIn extends BuiltInNode<Double> {
  public SqrtBuiltIn() { super("sqrt"); }

  @Override
  public double executeDouble(VirtualFrame frame) {
    // Are arguments to functions are necessarily boxed?
    Object[] args = frame.getArguments();
    return Math.sqrt((Double) args[0]);
  }
}
