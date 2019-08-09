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
  public static ExpressionNode arrays(ArrayArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public ArrayArrayValue executeObject(VirtualFrame frame) { return v;}
      @Override
      public String toString() { return v.toString(); }
    };
  }
  public static ExpressionNode functions(FunctionArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public FunctionArrayValue executeObject(VirtualFrame frame) { return v;}
      @Override
      public String toString() { return v.toString(); }
    };
  }
}
