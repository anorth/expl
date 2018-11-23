package org.explang.truffle.compiler

import com.oracle.truffle.api.frame.FrameDescriptor
import com.oracle.truffle.api.frame.FrameSlot
import com.oracle.truffle.api.frame.FrameSlotKind
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.explang.truffle.Type
import java.util.Deque
import java.util.LinkedList

/**
 * Compile-time stack of symbol scopes, resolving to arguments and bindings.
 */
class Scope(ctx: ParseTree) {
  /** The result of resolving a name in some scope. */
  sealed class Resolution(val type: Type) {
    class Argument(type: Type, val index: Int) : Resolution(type)
    class Binding(type: Type, val slot: FrameSlot) : Resolution(type)
  }

  private class ScopeLevel(
      // The parse tree forming this scope.
      val ctx: ParseTree,
      // The frame descriptor for new function scopes.
      val frame: FrameDescriptor? = null,
      // A level will have only one of these non-empty, args for a function scope or
      // bindings for a let/where scope.
      val args: MutableMap<String, Resolution.Argument> = mutableMapOf(),
      val bindings: MutableMap<String, Resolution.Binding> = mutableMapOf()
  )

  private val stack: Deque<ScopeLevel> = LinkedList<ScopeLevel>() // Front is top of stack

  init {
    // The top level is an anonymous function scope.
    enterFunction(FrameDescriptor(), ctx)
  }

  /** Pushes a new function definition scope onto the stack. */
  fun enterFunction(descriptor: FrameDescriptor, ctx: ParseTree) {
    stack.push(ScopeLevel(ctx, descriptor))
  }

  /** Pushes a new binding scope onto the stack. */
  fun enterBinding(ctx: ParseTree) {
    stack.push(ScopeLevel(ctx))
  }

  /** Pops the top level off the stack. */
  fun exit() {
    stack.pop()
  }

  /**
   * Pops a level off the stack and asserts that it was the only level, returning that
   * level's frame descriptor.
   */
  fun popTopFrame(): FrameDescriptor {
    val level = stack.pop()
    require(stack.isEmpty()) { "Scope stack corrupted" }
    return level.frame!!
  }

  /**
   * Defines an argument name in the current level (which must be a function scope). Resolve
   * to indices in the order they are defined.
   */
  fun defineArgument(name: String, type: Type, ctx: ParserRuleContext): Resolution.Argument {
    assert(top.bindings.isEmpty()) { "Can't add argument to binding frame" }
    if (name in top.args) throw CompileError("Duplicate argument name $name", ctx)
    val arg = Resolution.Argument(type, top.args.size)
    top.args[name] = arg
    return arg
  }

  /**
   * Defines a binding name in the current level. Bindings resolve to frame slots in the
   * enclosing function frame.
   *
   * TODO: resolve colliding names for different bindings in the same function (shadowing).
   */
  fun defineBinding(name: String, type: Type, ctx: ParserRuleContext): Resolution.Binding {
    assert(top.args.isEmpty()) { "Can't add binding to argument frame" }
    if (name in top.bindings) throw CompileError("Duplicate binding for $name", ctx)
    val slot = topFrame.findOrAddFrameSlot(name, type, type.asSlotKind())
    val binding = Resolution.Binding(type, slot)
    top.bindings[name] = binding
    return binding
  }

  /** Resolves a name within the current function scope */
  fun resolve(name: String): Resolution? {
    val itr = stack.iterator()
    while (itr.hasNext()) {
      val scope = itr.next()
      val binding = scope.bindings[name]
      if (binding != null) return binding
      val arg = scope.args[name]
      if (arg != null) return arg
      if (scope.frame != null) break
    }
    return null
  }

  private val top get() = stack.first

  private val topFrame get() = stack.find { it.frame != null }!!.frame!!
}

fun Type.asSlotKind() = when (this) {
  Type.DOUBLE -> FrameSlotKind.Double
  else -> FrameSlotKind.Object
}
