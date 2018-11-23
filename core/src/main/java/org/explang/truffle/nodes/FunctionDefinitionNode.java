package org.explang.truffle.nodes;

import java.util.StringJoiner;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.Type;

@NodeInfo(shortName = "Function")
public final class FunctionDefinitionNode extends ExpressionNode {
  public final ExplFunction function;

  public FunctionDefinitionNode(ExplFunction function) {
    super(function.type);
    this.function = function;
  }

  @Override
  public ExplFunction executeFunction(VirtualFrame frame) {
    return this.function;
  }

  @Override
  public String toString() {
    StringJoiner args = new StringJoiner(",");
    for (Type argType : type.arguments()) {
      args.add(argType.name());
    }
    return "(" + args + ")->" + function.callTarget.getRootNode().toString();
  }
}
