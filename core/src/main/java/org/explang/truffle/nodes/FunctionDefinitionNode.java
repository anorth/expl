package org.explang.truffle.nodes;

import java.util.StringJoiner;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.syntax.Func;
import org.explang.syntax.Type;
import org.explang.truffle.ExplFunction;

@NodeInfo(shortName = "Function")
public final class FunctionDefinitionNode extends BaseNode {
  private final ExplFunction function;

  /**
   * Function bodies can close over non-local values. The references are evaluated at the time
   * the function is defined. The value is closed over, not the reference.
   * <p>
   * The function will contain descriptor scope resolution nodes which resolve in the descriptor scope
   * attached to the function.
   */
  public FunctionDefinitionNode(ExplFunction function) {
    super(function.type());
    this.function = function;
  }

  @Override
  public Func type() {
    return function.type();
  }

  @Override
  public ExplFunction executeFunction(VirtualFrame frame) {
    ExplFunction f = this.function.copy();
    // Note: for a function with a recursive reference to itself, this will initially capture
    // a null for that slot. The binding node must additionally capture the function reference after
    // setting it.
    f.capture(frame);
    return f;
  }

  @Override
  public String toString() {
    StringJoiner args = new StringJoiner(",");
    for (Type argType : type().parameters()) {
      args.add(argType.name());
    }
    return "(" + args + ")->" + function.callTarget().getRootNode().toString();
  }
}
