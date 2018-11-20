package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

@NodeInfo(shortName = "Double")
public final class LiteralDoubleNode extends ExpressionNode {
  private final double value;

  public LiteralDoubleNode(double value) {
    this.value = value;
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    return value;
  }

  @Override
  public String toString() {
    return Double.toString(value);
  }
}
