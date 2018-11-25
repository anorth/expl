package org.explang.truffle.compiler

import com.oracle.truffle.api.frame.FrameDescriptor
import com.oracle.truffle.api.frame.FrameSlot
import com.oracle.truffle.api.frame.FrameSlotKind
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.explang.parser.ExplParser
import org.explang.truffle.Type
import java.util.Deque
import java.util.LinkedList

/**
 * Compile-time stack of symbol scopes, resolving to arguments and bindings.
 */
class Scope(ctx: ParseTree) {
  /** The result of resolving a name in some scope. */
  sealed class Resolution {
    abstract val ctx: ParserRuleContext
    abstract val type: Type
    abstract val name: String // For inspection

    data class Argument(
        override val ctx: ParserRuleContext,
        override val type: Type,
        override val name: String,
        val index: Int) :
        Resolution()

    data class Local(
        override val ctx: ParserRuleContext,
        override val type: Type,
        override val name: String,
        val slot: FrameSlot) :
        Resolution()
  }

  private class ScopeLevel(
      // The parse tree forming this scope.
      val ctx: ParseTree,
      // Frame descriptor for new function scopes.
      val descriptor: FrameDescriptor? = null,
      // Variables from higher scopes closed over by a function scope.
      val closedOver: MutableSet<Resolution> = mutableSetOf(),

      // A level will have only one of these non-empty, args for a function scope or
      // bindings for a let/where scope.
      val args: MutableMap<String, Resolution.Argument> = mutableMapOf(),
      val bindings: MutableMap<String, Resolution.Local> = mutableMapOf()
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

  /**
   * Pops the top level off the stack.
   *
   * @return resolutions made within the scope that resolved outside it
   */
  fun exit(): Set<Resolution> {
    return stack.pop().closedOver
  }

  /**
   * Pops a level off the stack and asserts that it was the only level, returning that
   * level's frame descriptor.
   */
  fun popTopFrame(): FrameDescriptor {
    val level = stack.pop()
    require(stack.isEmpty()) { "Scope stack corrupted" }
    return level.descriptor!!
  }

  /**
   * Defines an argument name in the current level (which must be a function scope). Resolve
   * to indices in the order they are defined.
   */
  fun defineArgument(type: Type, ctx: ExplParser.SymbolContext): Resolution.Argument {
    val name = ctx.text
    assert(top.bindings.isEmpty()) { "Can't add argument to binding scope" }
    if (name in top.args) throw CompileError("Duplicate argument name $name", ctx)
    val arg = Resolution.Argument(ctx, type, name, top.args.size)
    top.args[name] = arg
    return arg
  }

  /**
   * Defines a binding name in the current level. Bindings resolve to descriptor slots in the
   * enclosing function descriptor.
   *
   * TODO: resolve colliding names for different bindings in the same function (shadowing).
   */
  fun defineBinding(type: Type, ctx: ExplParser.BindingContext): Resolution.Local {
    val name = ctx.symbol().text
    assert(top.args.isEmpty()) { "Can't add binding to function scope" }
    if (name in top.bindings) throw CompileError("Duplicate binding for $name", ctx)
    val slot = topDescriptor.findOrAddFrameSlot(name, type, type.asSlotKind())
    val binding = Resolution.Local(ctx, type, name, slot)
    top.bindings[name] = binding
    return binding
  }

  /** Resolves a name in an enclosing function or local scope. */
  fun resolve(ctx: ExplParser.SymbolContext): Resolution? {
    val name = ctx.text
    val itr = stack.iterator()
    // First, resolve only up to the directly enclosing function scope.
    var level: ScopeLevel
    do {
      level = itr.next()
      val r = level.bindings[name] ?: level.args[name]
      if (r != null) return r
    } while (level.descriptor == null && itr.hasNext())

    val closure = level
    val descriptor = level.descriptor!!

    // Now resolve in enclosing scopes.
    while (itr.hasNext()) {
      val scope = itr.next()
      val r = scope.bindings[name] ?: scope.args[name]
      if (r != null) {
        // Record that this symbol was closed over.
        closure.closedOver.add(r)
        // Add a slot in the immediately enclosing function frame where the name will be resolved
        // inside the function call. A call preamble will copy values here.
        val slot = descriptor.findOrAddFrameSlot(name, r.type, r.type.asSlotKind())
        return Resolution.Local(r.ctx, r.type, r.name, slot)
      }
    }
    return null
  }

  private val top get() = stack.first

  private val topDescriptor get() = stack.find { it.descriptor != null }!!.descriptor!!
}

fun Type.asSlotKind() = when (this) {
  Type.DOUBLE -> FrameSlotKind.Double
  else -> FrameSlotKind.Object
}
