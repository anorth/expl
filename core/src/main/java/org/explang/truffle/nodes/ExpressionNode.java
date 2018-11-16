package org.explang.truffle.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import org.explang.truffle.Types;
import org.explang.truffle.TypesGen;

@TypeSystemReference(Types.class)
@NodeInfo(description = "Abstract base node for all expressions")
public abstract class ExpressionNode extends Node {
  public abstract Object executeGeneric(VirtualFrame frame);

  public double executeInt(VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectInteger(executeGeneric(frame));
  }

  public double executeDouble(VirtualFrame frame) throws UnexpectedResultException {
    return TypesGen.expectDouble(executeGeneric(frame));
  }
}
