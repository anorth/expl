package org.explang.truffle.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.truffle.RuntimeTypeError;
import org.explang.truffle.Types;

/**
 * @param <T> result type of the node
 */
@TypeSystemReference(Types.class)
@NodeInfo(language = "Expl", description = "Abstract base node for all expressions")
public abstract class ExpressionNode<T> extends Node {
  public T executeObject(VirtualFrame frame) { throw typeError(Object.class); }
  public double executeDouble(VirtualFrame frame) { throw typeError(double.class); }

  private RuntimeTypeError typeError(Class<?> t) {
    throw new RuntimeTypeError("Node " + this + " isn't of type " + t);
  }
}
