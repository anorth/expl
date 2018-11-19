package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;

/**
 * Unary negation node.
 */
public final class NegationNode<T> extends ExpressionNode<T> {
  @Child private ExpressionNode<T> child;

  public NegationNode(ExpressionNode<T> child) {
    this.child = child;
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    return -child.executeDouble(frame);
  }
}
