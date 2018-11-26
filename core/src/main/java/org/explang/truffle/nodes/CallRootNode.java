package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import org.explang.truffle.Discloser;

/**
 * A "root" node which executes directly under a new runtime call frame.
 * <p>
 * This node functions as a call target, including as the anonymous program entry point.
 */
public class CallRootNode extends RootNode {
  // The function body to evaluate.
  @Child private ExpressionNode body;
  // Binds closure values to the call frame.
  private final Discloser discloser;

  /**
   * @param body the expression body node, the result of which forms the result of this node
   * @param frameDescriptor descriptor of the frame for the evaluation
   * @param discloser copies captured any values into the call frame before the body is executed
   */
  public CallRootNode(ExpressionNode body, FrameDescriptor frameDescriptor, Discloser discloser) {
    super(null, frameDescriptor);
    this.body = body;
    this.discloser = discloser;
  }

  /** Called by the truffle framework to begin execution. */
  @Override
  public Object execute(VirtualFrame frame) {
    // Copy values from closure frame into the call frame.
    MaterializedFrame closure = (MaterializedFrame) frame.getArguments()[0];
    discloser.disclose(closure, frame);
    return body.executeDeclaredType(frame);
  }

  @Override
  public String toString() { return this.body.toString(); }
}
