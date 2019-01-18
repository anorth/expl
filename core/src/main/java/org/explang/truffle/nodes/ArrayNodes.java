package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.ArrayArrayValue;
import org.explang.array.BooleanArrayValue;
import org.explang.array.DoubleArrayValue;
import org.explang.array.FunctionArrayValue;
import org.explang.array.LongArrayValue;

public final class ArrayNodes {
  public static ExpressionNode booleans(BooleanArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public BooleanArrayValue executeArray(VirtualFrame frame) {
        return v;
      }
      @Override
      public String toString() { return v.toString(); }
    };
  }
  public static ExpressionNode longs(LongArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public LongArrayValue executeArray(VirtualFrame frame) { return v; }
      @Override
      public String toString() { return v.toString(); }
    };
  }
  public static ExpressionNode doubles(DoubleArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public DoubleArrayValue executeArray(VirtualFrame frame) { return v;}
      @Override
      public String toString() { return v.toString(); }
    };
  }
  public static ExpressionNode arrays(ArrayArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public ArrayArrayValue executeArray(VirtualFrame frame) { return v;}
      @Override
      public String toString() { return v.toString(); }
    };
  }
  public static ExpressionNode functions(FunctionArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public FunctionArrayValue executeArray(VirtualFrame frame) { return v;}
      @Override
      public String toString() { return v.toString(); }
    };
  }
}
