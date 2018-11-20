package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

public class ExpressionRootNode extends RootNode {
  @Child private ExpressionNode body;

  public ExpressionRootNode(ExpressionNode body, FrameDescriptor frameDescriptor) {
    super(null, frameDescriptor);
    this.body = body;
  }

  @Override
  public Object execute(VirtualFrame frame) {
    return body.executeDouble(frame);
  }

  @Override
  public String toString() { return this.body.toString(); }

  public ExpressionNode getBody() {
    return body;
  }
}
