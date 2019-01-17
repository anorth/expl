package org.explang.truffle.nodes.builtin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Builtins {
  public static List<BuiltInNode> ALL = new ArrayList<>();
  public static Map<String, BuiltInNode> BY_NAME = new HashMap<>();

  static {
    ALL.add(MathBuiltins.sqrt());

    ALL.add(ArrayBuiltins.zeros());
    ALL.add(ArrayBuiltins.sum());

    for (BuiltInNode bi : ALL) {
      BY_NAME.put(bi.name, bi);
    }
  }
}
