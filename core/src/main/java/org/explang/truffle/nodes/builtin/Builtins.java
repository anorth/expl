package org.explang.truffle.nodes.builtin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

public final class Builtins {
  public static final List<BuiltInNode> ALL = new ArrayList<>();

  static {
    addBuiltinsFromClass(MathBuiltins.class);
    addBuiltinsFromClass(RangeBuiltins.class);
    addBuiltinsFromClass(SliceBuiltins.class);
  }

  private static void addBuiltinsFromClass(Class<?> clazz) {
    for (Method method : clazz.getDeclaredMethods()) {
      int mods = method.getModifiers();
      if (isStatic(mods) && isPublic(mods) && method.getReturnType().equals(BuiltInNode.class)) {
        try {
          Object node = method.invoke(null);
          ALL.add((BuiltInNode) node);
        } catch (IllegalAccessException | InvocationTargetException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }
}
