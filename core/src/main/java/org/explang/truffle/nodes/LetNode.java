package org.explang.truffle.nodes;

import java.util.StringJoiner;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.truffle.ExplFunction;

/**
 * A new scope introducing some bindings for a dependent expression.
 */
@NodeInfo(shortName = "Let")
public final class LetNode extends ExpressionNode {
  @Children private BindingNode[] bindings;
  @Child private ExpressionNode expression;

  public LetNode(BindingNode[] bindings, ExpressionNode expression) {
    super(expression.type());
    this.bindings = bindings;
    this.expression = expression;
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    executeBindings(frame);
    return expression.executeDouble(frame);
  }

  @Override
  public ExplFunction executeFunction(VirtualFrame frame) {
    executeBindings(frame);
    return expression.executeFunction(frame);
  }

  private void executeBindings(VirtualFrame frame) {
    for (BindingNode binding : bindings) {
      // Execute for the side-effect on the frame.
      binding.executeDeclaredType(frame);
    }
  }

  @Override
  public String toString() {
    StringJoiner j = new StringJoiner(",");
    for (BindingNode binding : bindings) {
      j.add(binding.toString());
    }

    return "let(" +j + "," + expression + ")";
  }
}
