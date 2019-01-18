package org.explang.truffle.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.array.ArrayArrayValue;
import org.explang.array.ArrayValue;
import org.explang.array.BooleanArrayValue;
import org.explang.array.DoubleArrayValue;
import org.explang.array.FunctionArrayValue;
import org.explang.array.LongArrayValue;
import org.explang.syntax.ArrayType;
import org.explang.syntax.FuncType;
import org.explang.truffle.ExplFunction;

public final class ArrayNodes {
  public static ExpressionNode booleans(boolean[] v) { return booleans(new BooleanArrayValue(v)); }
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
  public static ExpressionNode longs(long[] v) { return longs(new LongArrayValue(v)); }
  public static ExpressionNode longs(LongArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public LongArrayValue executeArray(VirtualFrame frame) { return v; }
      @Override
      public String toString() { return v.toString(); }
    };
  }
  public static ExpressionNode doubles(double[] v) { return doubles(new DoubleArrayValue(v)); }
  public static ExpressionNode doubles(DoubleArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public DoubleArrayValue executeArray(VirtualFrame frame) { return v;}
      @Override
      public String toString() { return v.toString(); }
    };
  }
  public static ExpressionNode arrays(ArrayType elementType, ArrayValue[] v) {
    return arrays(new ArrayArrayValue(elementType, v));
  }
  public static ExpressionNode arrays(ArrayArrayValue v) {
    return new BaseNode(v.getType()) {
      @Override
      public ArrayArrayValue executeArray(VirtualFrame frame) { return v;}
      @Override
      public String toString() { return v.toString(); }
    };
  }
  public static ExpressionNode functions(FuncType elementType, ExplFunction[] v) {
    return functions(new FunctionArrayValue(elementType, v));
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
