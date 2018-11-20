package org.explang.truffle.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import org.explang.truffle.ExplFunction;
import org.explang.truffle.ExplSymbol;
import org.explang.truffle.RuntimeTypeError;
import org.explang.truffle.Types;

@TypeSystemReference(Types.class)
@NodeInfo(language = "Expl", description = "Abstract base node for all expressions")
public abstract class ExpressionNode extends Node {


  public Object executeObject(VirtualFrame frame, Class<Object> clazz) { throw typeError(Object.class); }
  public double executeDouble(VirtualFrame frame) { throw typeError(double.class); }
  public ExplSymbol executeSymbol(VirtualFrame frame) { throw typeError(ExplSymbol.class); }
  public ExplFunction executeFunction(VirtualFrame frame) { throw typeError(ExplFunction.class); }

  private RuntimeTypeError typeError(Class<?> t) {
    throw new RuntimeTypeError("Node " + this + " isn't of type " + t);
  }
}
