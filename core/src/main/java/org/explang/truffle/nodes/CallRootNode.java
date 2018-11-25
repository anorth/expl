package org.explang.truffle.nodes;

import javax.annotation.Nullable;

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
  // Closure in which the function evaluates. This is set at runtime before the node is
  // executed.
  private @Nullable MaterializedFrame closure = null;

  /**
   * @param body the expression body node, the result of which forms the result of this node
   * @param frameDescriptor descriptor of the frame for the evaluation
   */
  public CallRootNode(ExpressionNode body, FrameDescriptor frameDescriptor, Discloser discloser) {
    super(null, frameDescriptor);
    this.body = body;
    this.discloser = discloser;
  }

  /** Sets the call's closure. Call this immediately before execution. */
  void setClosure(MaterializedFrame closure) {
    this.closure = closure;
  }

  /** Called by the truffle framework to begin execution. */
  @Override
  public Object execute(VirtualFrame frame) {
    // Copy values from closure frame into the call frame.
    discloser.disclose(closure, frame);
    return body.executeDouble(frame); // FIXME choose correct type
  }

  @Override
  public String toString() { return this.body.toString(); }
}
