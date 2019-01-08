package org.explang.truffle.compiler

import org.explang.syntax.ExBinaryOp
import org.explang.syntax.ExIf
import org.explang.syntax.ExLet
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree
import org.explang.syntax.ExUnaryOp
import org.explang.syntax.Parser
import org.explang.syntax.Type
import org.junit.Assert.assertEquals
import org.junit.Test

class TypeCheckerTest {
  @Test
  fun literals() {
    assertEquals(Type.BOOL, check("true").tree.tag.type)
    assertEquals(Type.DOUBLE, check("1").tree.tag.type)
    assertEquals(Type.DOUBLE, check("5.2").tree.tag.type)
  }

  @Test
  fun unaryOps() {
    check("-1").let {
      assertEquals(Type.DOUBLE, (it.tree as ExUnaryOp).operand.tag.type)
      assertEquals(Type.DOUBLE, it.tree.tag.type)
    }
  }

  @Test
  fun binOps() {
    assertEquals(Type.DOUBLE, check("1 + 2").tree.tag.type)
    assertEquals(Type.BOOL, check("1 > 2").tree.tag.type)
    assertEquals(Type.BOOL, check("1 == 2").tree.tag.type)
    assertEquals(Type.BOOL, check("true == false").tree.tag.type)
  }

  @Test
  fun iff() {
    assertEquals(Type.DOUBLE, check("if true then 1 else 2").tree.tag.type)
    assertEquals(Type.BOOL, check("if true then true else false").tree.tag.type)
  }

  @Test
  fun resolvedSymbol() {
    check("1 + a").let { (tree, resolver, bindings) ->
      val binOp = tree as ExBinaryOp
      assertEquals(Type.DOUBLE, binOp.tag.type)
      assertEquals(Type.DOUBLE, binOp.left.tag.type)
      assertEquals(Type.DOUBLE, binOp.right.tag.type) // Inferred
      assertEquals(Type.DOUBLE, bindings[resolver.resolve(binOp.right as ExSymbol<*>)])
    }
    check("a + 1").let { (tree, resolver, bindings) ->
      val binOp = tree as ExBinaryOp
      assertEquals(Type.DOUBLE, binOp.tag.type)
      assertEquals(Type.DOUBLE, binOp.left.tag.type) // Inferred
      assertEquals(Type.DOUBLE, binOp.right.tag.type)
      assertEquals(Type.DOUBLE, bindings[resolver.resolve(binOp.left as ExSymbol<*>)])
    }
    check("a + b").let { (tree, resolver, bindings) ->
      val binOp = tree as ExBinaryOp
      assertEquals(Type.NONE, binOp.tag.type)
      assertEquals(Type.NONE, binOp.left.tag.type)
      assertEquals(Type.NONE, binOp.right.tag.type)
      assertEquals(null, bindings[resolver.resolve(binOp.left as ExSymbol<*>)])
      assertEquals(null, bindings[resolver.resolve(binOp.right as ExSymbol<*>)])
    }
  }

  @Test
  fun iffSymbol() {
    check("if a then 1 else b").let { (tree, resolver, bindings) ->
      val iff = tree as ExIf
      assertEquals(Type.DOUBLE, iff.tag.type)
      assertEquals(Type.BOOL, iff.test.tag.type)
      assertEquals(Type.DOUBLE, iff.left.tag.type)
      assertEquals(Type.DOUBLE, iff.right.tag.type)
      assertEquals(Type.BOOL, bindings[resolver.resolve(iff.test as ExSymbol<*>)])
      assertEquals(Type.DOUBLE, bindings[resolver.resolve(iff.right as ExSymbol<*>)])
    }
  }

  @Test
  fun boundSymbol() {
    check("let a = 1 in a").let { (tree, resolver, bindings) ->
      val let = tree as ExLet
      val binding = let.bindings.first()

      assertEquals(Type.DOUBLE, let.tag.type)
      assertEquals(Type.NONE, binding.tag.type) // Remains untyped
      assertEquals(Type.NONE, binding.symbol.tag.type) // Remains untyped
      assertEquals(Type.DOUBLE, binding.value.tag.type)
      assertEquals(Type.DOUBLE, bindings[resolver.resolve(binding.symbol)])
    }
    check("let a = 1, b = a+a in a+b").let { (tree, resolver, bindings) ->
      val let = tree as ExLet

      assertEquals(Type.DOUBLE, let.tag.type)
      assertEquals(Type.DOUBLE, let.bindings[0].value.tag.type)
      assertEquals(Type.DOUBLE, let.bindings[1].value.tag.type)
      assertEquals(Type.DOUBLE, bindings[resolver.resolve(let.bindings[0].symbol)])
      assertEquals(Type.DOUBLE, bindings[resolver.resolve(let.bindings[1].symbol)])
    }
  }
}

private data class Result(
    val tree: ExTree<Analyzer.Tag>,
    val resolver: TypeChecker.SymbolResolver,
    val bindings: Map<Scope.Resolution, Type>
)

/** Resolves on the fly by name in a global binding scope. */
private class SimpleResolver(tree: ExTree<*>) :
    TypeChecker.SymbolResolver {
  private val rootScope = RootScope(tree)
  private val bindingScope = BindingScope(tree, rootScope)
  override fun resolve(symbol: ExSymbol<*>): Scope.Resolution {
    val maybeResolved = bindingScope.resolve(symbol)
    if (maybeResolved is Scope.Resolution.Unresolved) {
      bindingScope.define(symbol)
    }
    return bindingScope.resolve(symbol)
  }
}

private fun check(s: String): Result {
  val tree = parse(s)
  val resolver = SimpleResolver(tree)
  val types = TypeChecker.computeTypes(tree, resolver)
  return Result(tree, resolver, types.bindings)
}

private fun parse(s: String): ExTree<Analyzer.Tag> {
  return Parser().parse(s) { Analyzer.Tag() }.tree
}
