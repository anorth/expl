package org.explang.truffle.compiler


import org.explang.syntax.ExBinaryOp
import org.explang.syntax.ExCall
import org.explang.syntax.ExIf
import org.explang.syntax.ExLambda
import org.explang.syntax.ExLet
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree
import org.explang.syntax.ExUnaryOp
import org.explang.syntax.Parser
import org.explang.syntax.Type
import org.explang.syntax.Type.BOOL
import org.explang.syntax.Type.DOUBLE
import org.explang.syntax.Type.NONE
import org.explang.syntax.Type.function
import org.junit.Assert.assertEquals
import org.junit.Test

class TypeCheckerTest {
  @Test
  fun literals() {
    assertEquals(BOOL, check("true").tree.tag.type)
    assertEquals(DOUBLE, check("1").tree.tag.type)
    assertEquals(DOUBLE, check("5.2").tree.tag.type)
  }

  @Test
  fun unaryOps() {
    check("-1").let {
      assertEquals(DOUBLE, (it.tree as ExUnaryOp).operand.tag.type)
      assertEquals(DOUBLE, it.tree.tag.type)
    }
  }

  @Test
  fun binOps() {
    assertEquals(DOUBLE, check("1 + 2").tree.tag.type)
    assertEquals(BOOL, check("1 > 2").tree.tag.type)
    assertEquals(BOOL, check("1 == 2").tree.tag.type)
    assertEquals(BOOL, check("true == false").tree.tag.type)
  }

  @Test
  fun iff() {
    assertEquals(DOUBLE, check("if true then 1 else 2").tree.tag.type)
    assertEquals(BOOL, check("if true then true else false").tree.tag.type)
  }

  @Test
  fun resolvedSymbol() {
    check("1 + a").let { (tree, resolver, bindings) ->
      val binOp = tree as ExBinaryOp
      assertEquals(DOUBLE, binOp.tag.type)
      assertEquals(DOUBLE, binOp.left.tag.type)
      assertEquals(DOUBLE, binOp.right.tag.type) // Inferred
      assertEquals(DOUBLE, bindings[resolver.resolve(binOp.right as ExSymbol<*>)])
    }
    check("a + 1").let { (tree, resolver, bindings) ->
      val binOp = tree as ExBinaryOp
      assertEquals(DOUBLE, binOp.tag.type)
      assertEquals(DOUBLE, binOp.left.tag.type) // Inferred
      assertEquals(DOUBLE, binOp.right.tag.type)
      assertEquals(DOUBLE, bindings[resolver.resolve(binOp.left as ExSymbol<*>)])
    }
    check("a + b").let { (tree, resolver, bindings) ->
      val binOp = tree as ExBinaryOp
      assertEquals(NONE, binOp.tag.type)
      assertEquals(NONE, binOp.left.tag.type)
      assertEquals(NONE, binOp.right.tag.type)
      assertEquals(null, bindings[resolver.resolve(binOp.left as ExSymbol<*>)])
      assertEquals(null, bindings[resolver.resolve(binOp.right as ExSymbol<*>)])
    }
  }

  @Test
  fun iffSymbol() {
    check("if a then 1 else b").let { (tree, resolver, bindings) ->
      val iff = tree as ExIf
      assertEquals(DOUBLE, iff.tag.type)
      assertEquals(BOOL, iff.test.tag.type)
      assertEquals(DOUBLE, iff.left.tag.type)
      assertEquals(DOUBLE, iff.right.tag.type)
      assertEquals(BOOL, bindings[resolver.resolve(iff.test as ExSymbol<*>)])
      assertEquals(DOUBLE, bindings[resolver.resolve(iff.right as ExSymbol<*>)])
    }
  }

  @Test
  fun boundSymbol() {
    check("let a = 1 in a").let { (tree, resolver, bindings) ->
      val let = tree as ExLet
      val binding = let.bindings.first()

      assertEquals(DOUBLE, let.tag.type)
      assertEquals(NONE, binding.tag.type) // Remains untyped
      assertEquals(NONE, binding.symbol.tag.type) // Remains untyped
      assertEquals(DOUBLE, binding.value.tag.type)
      assertEquals(DOUBLE, bindings[resolver.resolve(binding.symbol)])
    }
    check("let a = 1, b = a+a in a+b").let { (tree, resolver, bindings) ->
      val let = tree as ExLet

      assertEquals(DOUBLE, let.tag.type)
      assertEquals(DOUBLE, let.bindings[0].value.tag.type)
      assertEquals(DOUBLE, let.bindings[1].value.tag.type)
      assertEquals(DOUBLE, bindings[resolver.resolve(let.bindings[0].symbol)])
      assertEquals(DOUBLE, bindings[resolver.resolve(let.bindings[1].symbol)])
    }
  }

  @Test
  fun simpleLambda() {
    check("() -> true").let { (tree, _, _) ->
      val fn = tree as ExLambda
      assertEquals(function(BOOL), fn.tag.type)
    }
    check("x: bool -> x").let { (tree, _, _) ->
      val fn = tree as ExLambda
      assertEquals(function(BOOL, BOOL), fn.tag.type)
    }
    check("(x: bool) -> x").let { (tree, _, _) ->
      val fn = tree as ExLambda
      assertEquals(function(BOOL, BOOL), fn.tag.type)
    }
    check("(a: double, b: double) -> a > b").let { (tree, _, _) ->
      val fn = tree as ExLambda
      assertEquals(function(BOOL, DOUBLE, DOUBLE), fn.tag.type)
    }
  }

  @Test
  fun simpleCall() {
    check("(() -> true)()").let { (tree, _, _) ->
      val call = tree as ExCall
      assertEquals(BOOL, call.tag.type)
    }
    check("(x: bool -> x)(false)").let { (tree, _, _) ->
      val call = tree as ExCall
      assertEquals(BOOL, call.tag.type)
      assertEquals(function(BOOL, BOOL), call.callee.tag.type)
      assertEquals(BOOL, call.args[0].tag.type)
    }
    check("((a: double, b: double) -> a > b)(2, 1)").let { (tree, _, _) ->
      val call = tree as ExCall
      assertEquals(BOOL, call.tag.type)
      assertEquals(function(BOOL, DOUBLE, DOUBLE), call.callee.tag.type)
      assertEquals(DOUBLE, call.args[0].tag.type)
      assertEquals(DOUBLE, call.args[1].tag.type)
    }
  }

  @Test
  fun boundInLambda() {
    check("let a = 1 in () -> a").let { (tree, resolver, bindings) ->
      val let = tree as ExLet
      val fn = let.bound as ExLambda
      assertEquals(function(DOUBLE), fn.tag.type)
      assertEquals(DOUBLE, bindings[resolver.resolve((fn.body as ExSymbol<*>))])
    }
    check("(let a = 1 in () -> a)()").let { (tree, _, _) ->
      val call = tree as ExCall
      assertEquals(DOUBLE, call.tag.type)
      assertEquals(function(DOUBLE), call.callee.tag.type)
    }
    check("let a = 1 in (() -> a)()").let { (tree, _, _) ->
      val let = tree as ExLet
      val call = let.bound as ExCall
      assertEquals(DOUBLE, let.tag.type)
      assertEquals(DOUBLE, call.tag.type)
      assertEquals(function(DOUBLE), call.callee.tag.type)
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
