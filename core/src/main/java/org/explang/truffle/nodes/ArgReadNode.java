package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.array.ArrayValue;
import org.explang.syntax.RuntimeTypeError;
import org.explang.syntax.Type;
import org.explang.truffle.ExplFunction;

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
  private final String name; // For inspection only

  public static boolean readBoolean(VirtualFrame frame, int index) {
    try {
      // Are arguments to functions are necessarily boxed?
      return (boolean) frame.getArguments()[index + 1];
    } catch (ClassCastException | IndexOutOfBoundsException e) {
      throw fail(index, Type.BOOL.name(), e);
    }
  }

  public static long readLong(VirtualFrame frame, int index) {
    try {
      return (long) frame.getArguments()[index + 1];
    } catch (ClassCastException | IndexOutOfBoundsException e) {
      throw fail(index, Type.LONG.name(), e);
    }
  }

  public static double readDouble(VirtualFrame frame, int index) {
    try {
      return (double) frame.getArguments()[index + 1];
    } catch (ClassCastException | IndexOutOfBoundsException e) {
      throw fail(index, Type.DOUBLE.name(), e);
    }
  }

  public static ExplFunction readFunction(VirtualFrame frame, int index) {
    try {
      return (ExplFunction) frame.getArguments()[index + 1];
    } catch (ClassCastException | IndexOutOfBoundsException e) {
      throw fail(index, "function", e);
    }
  }

  public static ArrayValue readArray(VirtualFrame frame, int index) {
    try {
      return (ArrayValue) frame.getArguments()[index + 1];
    } catch (ClassCastException | IndexOutOfBoundsException e) {
      throw fail(index, "array", e);
    }
  }

  public ArgReadNode(Type t, int index, String name) {
    super(t);
    this.index = index;
    this.name = name;
  }

  @Override
  public boolean executeBoolean(VirtualFrame frame) {
    assertType(Type.BOOL);
    return readBoolean(frame, index);
  }
  @Override
  public long executeLong(VirtualFrame frame) {
    assertType(Type.LONG);
    return readLong(frame, index);
  }
  @Override
  public double executeDouble(VirtualFrame frame) {
    assertType(Type.DOUBLE);
    return readDouble(frame, index);
  }
  @Override
  public ExplFunction executeFunction(VirtualFrame frame) {
    assertTypeIsFunction();
    return readFunction(frame, index);
  }
  @Override
  public ArrayValue executeArray(VirtualFrame frame) {
    assertTypeIsArray();
    return readArray(frame, index);
  }

  @Override
  public String toString() {
    return name + "|" + index;
  }

  private static RuntimeTypeError fail(int index, String type, Throwable cause) {
      throw new RuntimeTypeError(
          String.format("Failed to read argument %d of type %s", index, type), cause);
  }
}
