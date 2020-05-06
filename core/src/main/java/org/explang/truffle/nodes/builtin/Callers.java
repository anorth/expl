package org.explang.truffle.nodes.builtin;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.nodes.DirectCallNode;
import org.explang.truffle.ExplFunction;

interface Callers {
  /** Wraps a call to a unary function */
  class Call1 {
    private final DirectCallNode node;
    private final Object[] args = new Object[2];

    Call1(ExplFunction fn) {
      this.node = Truffle.getRuntime().createDirectCallNode(fn.callTarget());
      args[0] = fn.closure().orElse(null);
    }

    boolean callBoolean(Object arg) {
      args[1] = arg;
      return (boolean) node.call(args);
    }
    long callLong(Object arg) {
      args[1] = arg;
      return (long) node.call(args);
    }
    double callDouble(Object arg) {
      args[1] = arg;
      return (double) node.call(args);
    }
  }

  class Call2 {
    private final DirectCallNode node;
    private final Object[] args = new Object[3];

    Call2(ExplFunction fn) {
      this.node = Truffle.getRuntime().createDirectCallNode(fn.callTarget());
      args[0] = fn.closure().orElse(null);
    }

    boolean callBoolean(Object a1, Object a2) {
      args[1] = a1;
      args[2] = a2;
      return (boolean) node.call(args);
    }
    long callLong(Object a1, Object a2) {
      args[1] = a1;
      args[2] = a2;
      return (long) node.call(args);
    }
    double callDouble(Object a1, Object a2) {
      args[1] = a1;
      args[2] = a2;
      return (double) node.call(args);
    }
  }
}
