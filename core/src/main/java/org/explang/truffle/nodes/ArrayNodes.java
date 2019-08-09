package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.BooleanArrayValue;
import org.explang.array.DoubleArrayValue;
import org.explang.array.LongArrayValue;
import org.explang.array.ObjectArrayValue;

public final class ArrayNodes {
  public static ExpressionNode booleans(BooleanArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public BooleanArrayValue executeObject(VirtualFrame frame) {
        return v;
      }
      @Override
      public String toString() { return v.toString(); }
    };
  }
  public static ExpressionNode longs(LongArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public LongArrayValue executeObject(VirtualFrame frame) { return v; }
      @Override
      public String toString() { return v.toString(); }
    };
  }
  public static ExpressionNode doubles(DoubleArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public DoubleArrayValue executeObject(VirtualFrame frame) { return v;}
      @Override
      public String toString() { return v.toString(); }
    };
  }
  public static ExpressionNode objects(ObjectArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public ObjectArrayValue executeObject(VirtualFrame frame) { return v;}
      @Override
      public String toString() { return v.toString(); }
    };
  }
}
