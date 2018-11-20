package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Unary negation node.
 */
public final class NegationNode extends ExpressionNode {
  @Child private ExpressionNode child;

  public NegationNode(ExpressionNode child) {
    super(child.type);
    this.child = child;
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    return -child.executeDouble(frame);
  }

  @Override
  public String toString() { return "-(" + this.child + ")"; }
}
