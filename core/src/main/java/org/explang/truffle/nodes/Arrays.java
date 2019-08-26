package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.ArrayValue;
import org.explang.array.SlicerValue;
import org.explang.truffle.ExplFunction;

/**
 * Operator nodes for slices.
 */
public final class Arrays {
  public static ExpressionNode index(ExpressionNode indexee, ExpressionNode indexer) {
    return new BaseNode(indexee.type().asArray().element()) {
      @Override
      public boolean executeBoolean(VirtualFrame frame) {
        return (boolean) executeObject(frame);
      }
      @Override
      public long executeLong(VirtualFrame frame) {
        return (long) executeObject(frame);
      }
      @Override
      public double executeDouble(VirtualFrame frame) {
        return (double) executeObject(frame);
      }
      @Override
      public Object executeObject(VirtualFrame frame) {
        ArrayValue<?> target = (ArrayValue<?>) indexee.executeObject(frame);
        long idx = indexer.executeLong(frame);
        return target.get(Math.toIntExact(idx));
      }
      @Override
      public ExplFunction executeFunction(VirtualFrame frame) {
        return (ExplFunction) executeObject(frame);
      }
    };
  }

  public static ExpressionNode slice(ExpressionNode indexee, ExpressionNode indexer) {
    return new BaseNode(indexee.type()) {
      @Override
      public ArrayValue<?> executeObject(VirtualFrame frame) {
        ArrayValue<?> target = (ArrayValue<?>) indexee.executeObject(frame);
        SlicerValue slicer = (SlicerValue) indexer.executeObject(frame);
        return target.slice(slicer);
      }
    };
  }
}
