package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.SliceValue;
import org.explang.array.SlicerValue;
import org.explang.truffle.ExplFunction;

/**
 * Operator nodes for slices.
 */
public final class Slices {
  public static ExpressionNode index(ExpressionNode indexee, ExpressionNode indexer) {
    return new BaseNode(indexee.type().asSlice().element()) {
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
        SliceValue<?> target = (SliceValue<?>) indexee.executeObject(frame);
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
      public SliceValue<?> executeObject(VirtualFrame frame) {
        SliceValue<?> target = (SliceValue<?>) indexee.executeObject(frame);
        SlicerValue slicer = (SlicerValue) indexer.executeObject(frame);
        return target.slice(slicer);
      }
    };
  }
}
