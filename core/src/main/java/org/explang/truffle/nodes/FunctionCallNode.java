package org.explang.truffle.nodes;

import java.util.StringJoiner;
import javax.annotation.Nullable;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.syntax.FuncType;
import org.explang.syntax.Type;
import org.explang.truffle.ExplFunction;

@NodeInfo(shortName = "Call")
public class FunctionCallNode extends ExpressionNode {
  @Child private ExpressionNode functionNode;
  @Children private final ExpressionNode[] argNodes;
  // Using a DirectCallNode might be faster here, allowing more Truffle optimizations,
  // especially if we can resolve the function reference at compile time.
  // See http://cesquivias.github.io/blog/2015/01/08/writing-a-language-in-truffle-part-3-making-my-language-much-faster/#making-function-calls-faster
  @Child private IndirectCallNode callNode;

  public FunctionCallNode(ExpressionNode functionNode, ExpressionNode[] argNodes) {
    super(functionNode.type().asFunc().result());
    assert argsMatch(functionNode.type().asFunc(), argNodes);
    this.functionNode = functionNode;
    this.argNodes = argNodes;
    this.callNode = Truffle.getRuntime().createIndirectCallNode();
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    assertType(Type.BOOL);
    return (boolean) execute(frame);
  }
  @Override
  public long executeLong(VirtualFrame frame) {
    assertType(Type.LONG);
    return (long) execute(frame);
  }
  @Override
  public double executeDouble(VirtualFrame frame) {
    assertType(Type.DOUBLE);
    return (double) execute(frame);
  }
  @Override
  public Object executeObject(VirtualFrame frame) {
    assertTypeIsObject();
    return execute(frame);
  }
  @Override
  public ExplFunction executeFunction(VirtualFrame frame) {
    assertTypeIsFunction();
    return (ExplFunction) execute(frame);
  }

  private Object execute(VirtualFrame frame) {
    ExplFunction function = this.functionNode.executeFunction(frame);
    Object[] argValues = buildArgs(frame, function.closure().orElse(null));
    return this.callNode.call(function.callTarget(), argValues);
  }

  @ExplodeLoop
  private Object[] buildArgs(VirtualFrame virtualFrame, @Nullable MaterializedFrame closure) {
    CompilerAsserts.compilationConstant(this.argNodes.length);
    // The closure frame, if any, is passed to the callee as the first argument.
    Object[] argValues = new Object[this.argNodes.length + 1];
    argValues[0] = closure;
    for (int i = 0; i < this.argNodes.length; i++) {
      // I'm not sure whether Truffle avoids boxing primitives passed as arguments.
      argValues[i + 1] = this.argNodes[i].executeDeclaredType(virtualFrame);
    }
    return argValues;
  }

  private boolean argsMatch(FuncType type, ExpressionNode[] argNodes) {
    Type[] paramTypes = type.parameters();
    assert (paramTypes.length == argNodes.length) :
        "Mismatched arguments, expected " + paramTypes.length + " got " + argNodes.length + " args";
    for (int i = 0; i < paramTypes.length; i++) {
      assert (argNodes[i].type().satisfies(paramTypes[i])) :
          "Argument " + i + " with type " + argNodes[i].type() + " doesn't satisfy " +
              paramTypes[i];
    }
    return true;
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
