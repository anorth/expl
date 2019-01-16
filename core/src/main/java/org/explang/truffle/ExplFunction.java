package org.explang.truffle;

import java.util.Optional;
import javax.annotation.Nullable;

import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import org.explang.syntax.FuncType;

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
  private final FuncType type;
  private final RootCallTarget callTarget;
  private final Encloser encloser;
  private @Nullable MaterializedFrame closure = null;

  public ExplFunction(FuncType type, RootCallTarget callTarget, Encloser encloser) {
    this.type = type;
    this.callTarget = callTarget;
    this.encloser = encloser;
  }

  public FuncType type() { return type; }
  public RootCallTarget callTarget() { return callTarget; }
  public Optional<MaterializedFrame> closure() { return Optional.ofNullable(closure); }

  /** Returns a copy of this function, omitting the closure frame. */
  public ExplFunction copy() {
    return new ExplFunction(type, callTarget, encloser);
  }

  /** Computes the complete closure frame for this function, given a context frame. */
  public void capture(VirtualFrame frame) {
    this.closure = encloser.enclose(frame);
  }

  /** Captures a single value from a context frame into the existing closure frame. */
  public void capture(VirtualFrame frame, FrameSlot slot) {
    this.closure = encloser.enclose(frame, slot, this.closure);
  }

  @Override
  public String toString() {
    return type.toString();
  }
}
