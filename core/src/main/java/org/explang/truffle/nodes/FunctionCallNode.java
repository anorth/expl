package org.explang.truffle.nodes;

import java.util.StringJoiner;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.Type;

@NodeInfo(shortName = "Call")
public class FunctionCallNode extends ExpressionNode {
  @Child private ExpressionNode functionNode;
  @Children private final ExpressionNode[] argNodes;
  @Child private IndirectCallNode callNode; // FIXME make a direct call

  public FunctionCallNode(ExpressionNode functionNode, ExpressionNode[] argNodes) {
    super(functionNode.type.result());
    this.functionNode = functionNode;
    this.argNodes = argNodes;
    this.callNode = Truffle.getRuntime().createIndirectCallNode();
  }

  @Override
  @ExplodeLoop
  public double executeDouble(VirtualFrame virtualFrame) {
    assertType(Type.DOUBLE);
    CompilerAsserts.compilationConstant(this.argNodes.length);

    ExplFunction function = this.functionNode.executeFunction(virtualFrame);

    // The function object has the closure, if any, which is passed here to the "inside" of the
    // call as a reference on the call root node.
    // An alternative way to pass it would be as an arg, which is probably much safer, e.g.
    // with multiple threads..
    function.closure().ifPresent(c ->
        ((CallRootNode) function.callTarget().getRootNode()).setClosure(c));

    Object[] argValues = new Object[this.argNodes.length];
    for (int i = 0; i < this.argNodes.length; i++) {
      // FIXME need type information to invoke the right execute call.
      argValues[i] = this.argNodes[i].executeDouble(virtualFrame);
    }

    // FIXME handle returns of functions (i.e. higher-order fns)
    return (double) this.callNode.call(function.callTarget(), argValues);
  }

  @Override
  public String toString() {
    StringJoiner j = new StringJoiner(",", "call(", ")");
    j.add(this.functionNode.toString());
    for (ExpressionNode node : this.argNodes) {
      j.add(node.toString());
    }
    return j.toString();
  }
}
