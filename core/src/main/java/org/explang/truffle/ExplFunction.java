package org.explang.truffle;

import java.util.Optional;
import javax.annotation.Nullable;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.MaterializedFrame;

/**
 * Type object for a function, which includes the call target root of the function
 * implementation's AST.
 *
 * Compilation generates a single {@link org.explang.truffle.nodes.FunctionDefinitionNode}
 * with a reference to this function object.
 * The function definition node may be executed multiple times, each time closing over different
 * values in the surrounding scope. These return a copy of this function object with the same
 * type and call target AST root, but a different closure frame.
 *
 * The closure frame is then passed to the inside of the function call by the
 * {@link org.explang.truffle.nodes.FunctionCallNode}.
 */
public class ExplFunction {
  private final Type type;
  private final RootCallTarget callTarget;
  private final @Nullable MaterializedFrame closure;

  /** Creates a new function object with no closure frame. */
  public static ExplFunction create(Type type, RootCallTarget callTarget) {
    return new ExplFunction(type, callTarget, null);
  }

  private ExplFunction(Type type, RootCallTarget callTarget, @Nullable MaterializedFrame closure) {
    assert type.isFunction() : "Expected a function type, got " + type;
    this.type = type;
    this.callTarget = callTarget;
    this.closure = closure;
  }

  public Type type() { return type; }
  public RootCallTarget callTarget() { return callTarget; }
  public Optional<MaterializedFrame> closure() { return Optional.ofNullable(closure); }

  public ExplFunction withClosure(MaterializedFrame closure) {
    return new ExplFunction(type, callTarget, closure);
  }

  @Override
  public String toString() {
    return type.toString();
  }
}
