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

    // The function object holds the closure frame, if any, which is passed here to the
    // callee as the first argument.
    ExplFunction function = this.functionNode.executeFunction(virtualFrame);
    Object[] argValues = new Object[this.argNodes.length + 1];
    argValues[0] = function.closure().orElse(null);
    for (int i = 0; i < this.argNodes.length; i++) {
      // I'm not sure whether Truffle avoids boxing primitives passed as arguments.
      argValues[i + 1] = this.argNodes[i].executeDeclaredType(virtualFrame);
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
