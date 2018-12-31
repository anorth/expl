package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.syntax.Type;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.RuntimeTypeError;

/**
 * A reference to a positional argument, resolved at runtime in the executing frame.
 *
 * The first argument slot is reserved for a closure reference, so arguments are resolved
 * one slot higher than their declared index.
 *
 * @see FunctionCallNode
 */
@NodeInfo(shortName = "Argument")
public final class ArgReadNode extends ExpressionNode {
  private final int index; // Declared parameter index
  private final String name; // For inspection

  public ArgReadNode(Type t, int index, String name) {
    super(t);
    this.index = index;
    this.name = name;
  }

  @Override
  public ExplFunction executeFunction(VirtualFrame frame) {
    assertTypeIsFunction();
    try {
      return (ExplFunction) frame.getArguments()[index + 1];
    } catch (ClassCastException | IndexOutOfBoundsException e) {
      throw fail(e);
    }
  }

  @Override
  public double executeDouble(VirtualFrame frame) {
    assertType(Type.DOUBLE);
    try {
      return (double) frame.getArguments()[index + 1];
    } catch (ClassCastException | IndexOutOfBoundsException e) {
      throw fail(e);
    }
  }

  @Override
  public String toString() {
    return name + "|" + index;
  }

  private RuntimeTypeError fail(Throwable cause) {
      throw new RuntimeTypeError(
          String.format("Failed to read argument %d (%s) of type %s", index, name, type()), cause);
  }
}
