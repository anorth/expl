package org.explang.truffle.nodes;

import java.util.Optional;
import java.util.StringJoiner;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.truffle.Encloser;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.Type;

@NodeInfo(shortName = "Function")
public final class FunctionDefinitionNode extends ExpressionNode {
  // The function value.
  private final ExplFunction function;
  // Captures the closure frame.
  private Encloser encloser;

  /**
   * Function bodies can close over non-local values. The references are evaluated at the time
   * the function is defined. The value is closed over, not the reference.
   * <p>
   * The function will contain descriptor scope resolution nodes which resolve in the descriptor scope
   * attached to the function.
   */
  public FunctionDefinitionNode(ExplFunction function, Encloser encloser) {
    super(function.type());
    this.function = function;
    this.encloser = encloser;
  }

  @Override
  public ExplFunction executeFunction(VirtualFrame frame) {
    Optional<MaterializedFrame> closure = this.encloser.enclose(frame);
    return closure.map(this.function::withClosure).orElse(this.function);
  }

  @Override
  public String toString() {
    StringJoiner args = new StringJoiner(",");
    for (Type argType : type().arguments()) {
      args.add(argType.name());
    }
    return "(" + args + ")->" + function.callTarget().getRootNode().toString();
  }
}
