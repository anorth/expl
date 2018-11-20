package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.truffle.Type;

@NodeInfo(shortName = "sqrt")
public final class SqrtBuiltIn extends BuiltInNode {
  public SqrtBuiltIn() { super(Type.function(Type.DOUBLE, Type.DOUBLE), "sqrt"); }

  @Override
  public double executeDouble(VirtualFrame frame) {
    // Are arguments to functions are necessarily boxed?
    Object[] args = frame.getArguments();
    return Math.sqrt((Double) args[0]);
  }
}
