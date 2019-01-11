package org.explang.truffle.compiler


import org.explang.syntax.ExBinaryOp
import org.explang.syntax.ExCall
import org.explang.syntax.ExIf
import org.explang.syntax.ExLambda
import org.explang.syntax.ExLet
import org.explang.syntax.ExSymbol
import org.explang.syntax.ExTree
import org.explang.syntax.ExUnaryOp
import org.explang.syntax.TestParser
import org.explang.syntax.Type
import org.explang.syntax.Type.BOOL
import org.explang.syntax.Type.DOUBLE
import org.explang.syntax.Type.NONE
import org.explang.syntax.Type.function
import org.junit.Assert.assertEquals
import org.junit.Test

class TypeCheckerTest {
  private data class Result(
      val tree: ExTree<Analyzer.Tag>,
      val resolver: Resolver,
      val symbols: Map<Scope.Resolution, Type>
  )

  private val parser = TestParser(debug = false)

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
  fun boundSymbol() {
    check("let a = 1 in a").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val binding = let.bindings.first()

      assertEquals(DOUBLE, let.tag.type)
      assertEquals(NONE, binding.tag.type) // Remains untyped
      assertEquals(NONE, binding.symbol.tag.type) // Remains untyped
      assertEquals(DOUBLE, binding.value.tag.type)
      assertEquals(DOUBLE, symbols[resolver.resolve(binding.symbol)])
    }
    check("let a = 1 in 1 + a").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val binOp = let.bound as ExBinaryOp
      assertEquals(DOUBLE, let.tag.type)
      assertEquals(DOUBLE, binOp.tag.type)
      assertEquals(DOUBLE, binOp.left.tag.type)
      assertEquals(DOUBLE, binOp.right.tag.type) // Inferred
      assertEquals(DOUBLE, symbols[resolver.resolve(binOp.right as ExSymbol<*>)])
    }
    check("let a = 1, b = a+a in a+b").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      assertEquals(DOUBLE, let.tag.type)
      assertEquals(DOUBLE, let.bindings[0].value.tag.type)
      assertEquals(DOUBLE, let.bindings[1].value.tag.type)
      assertEquals(DOUBLE, symbols[resolver.resolve(let.bindings[0].symbol)])
      assertEquals(DOUBLE, symbols[resolver.resolve(let.bindings[1].symbol)])
    }
    check("let a = 1 in let b = a+a in a+b").let { (tree, resolver, symbols) ->
      val outer = tree as ExLet
      val inner = outer.bound as ExLet

      assertEquals(DOUBLE, outer.tag.type)
      assertEquals(DOUBLE, outer.bindings[0].value.tag.type)
      assertEquals(DOUBLE, inner.bindings[0].value.tag.type)
      assertEquals(DOUBLE, symbols[resolver.resolve(outer.bindings[0].symbol)])
      assertEquals(DOUBLE, symbols[resolver.resolve(inner.bindings[0].symbol)])
    }
  }

  @Test
  fun nestedBinding() {
    check("let a = 1 in let b = a+a in a+b").let { (tree, resolver, symbols) ->
      val outer = tree as ExLet
      val inner = outer.bound as ExLet
      assertEquals(DOUBLE, outer.tag.type)
      assertEquals(DOUBLE, outer.bindings[0].value.tag.type)
      assertEquals(DOUBLE, inner.bindings[0].value.tag.type)
      assertEquals(DOUBLE, symbols[resolver.resolve(outer.bindings[0].symbol)])
      assertEquals(DOUBLE, symbols[resolver.resolve(inner.bindings[0].symbol)])
    }
  }

  @Test
  fun iffSymbol() {
    check("let a = true in if a then 1 else 2").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val iff = let.bound as ExIf
      assertEquals(DOUBLE, let.tag.type)
      assertEquals(DOUBLE, iff.tag.type)
      assertEquals(BOOL, iff.test.tag.type)
      assertEquals(DOUBLE, iff.left.tag.type)
      assertEquals(DOUBLE, iff.right.tag.type)
      assertEquals(BOOL, symbols[resolver.resolve(iff.test as ExSymbol<*>)])
    }
    check("let a = 1, b = 2 in if false then a else b").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val iff = let.bound as ExIf
      assertEquals(DOUBLE, let.tag.type)
      assertEquals(DOUBLE, iff.tag.type)
      assertEquals(BOOL, iff.test.tag.type)
      assertEquals(DOUBLE, iff.left.tag.type)
      assertEquals(DOUBLE, iff.right.tag.type)
      assertEquals(DOUBLE, symbols[resolver.resolve(iff.left as ExSymbol<*>)])
      assertEquals(DOUBLE, symbols[resolver.resolve(iff.right as ExSymbol<*>)])
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
  fun closure() {
    check("let a = 1 in () -> a").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val fn = let.bound as ExLambda
      assertEquals(function(DOUBLE), fn.tag.type)
      assertEquals(DOUBLE, symbols[resolver.resolve((fn.body as ExSymbol<*>))])
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

  @Test
  fun recursion() {
    check("let f = (x: double): double -> f(x+1) in f(1)").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val outerCall = let.bound as ExCall
      val sym1 = let.bindings[0].symbol
      val fn = let.bindings[0].value as ExLambda
      val innerCall = fn.body as ExCall
      val sym2 = innerCall.callee as ExSymbol

      assertEquals(DOUBLE, let.tag.type)
      assertEquals(DOUBLE, outerCall.tag.type)
      assertEquals(DOUBLE, innerCall.tag.type)
      assertEquals(function(DOUBLE, DOUBLE), sym2.tag.type)

      assertEquals(function(DOUBLE, DOUBLE), symbols[resolver.resolve((sym1))])
      assertEquals(function(DOUBLE, DOUBLE), symbols[resolver.resolve((sym2))])
    }
  }

  @Test
  fun higherOrderFunction() {
    check("""(inner: (->double)) -> inner""").let { (tree, _, _) ->
      val fn = tree as ExLambda
      assertEquals(NONE, fn.annotation)
      assertEquals(function(DOUBLE), fn.parameters[0].tag.type)
      assertEquals(function(DOUBLE), fn.body.tag.type)
      assertEquals(function(function(DOUBLE), function(DOUBLE)), fn.tag.type)
    }
    check("""let f = (inner: (->double)) -> inner in f""").let { (tree, _, _) ->
      val let = tree as ExLet
      assertEquals(function(function(DOUBLE), function(DOUBLE)), let.bound.tag.type)
      assertEquals(function(function(DOUBLE), function(DOUBLE)), let.tag.type)
    }
    check("""let
      |f = (inner: (->double)) -> inner,
      |g = () -> 1,
      |in f(g)""".trimMargin()).let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      assertEquals(function(DOUBLE), let.bound.tag.type)
      assertEquals(function(DOUBLE), let.tag.type)

      assertEquals(function(function(DOUBLE), function(DOUBLE)),
          symbols[resolver.resolve((let.bindings[0].symbol))])
      assertEquals(function(DOUBLE),
          symbols[resolver.resolve((let.bindings[1].symbol))])
    }
    check("""let
      |apply = (f: (double->double), x: double) -> f(x),
      |inc = (x: double) -> x + 1,
      |in apply(inc, 1)""".trimMargin()).let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val call = let.bound as ExCall
      assertEquals(DOUBLE, call.tag.type)

      assertEquals(function(DOUBLE, function(DOUBLE, DOUBLE), DOUBLE),
          symbols[resolver.resolve((let.bindings[0].symbol))])
      assertEquals(function(DOUBLE, DOUBLE),
          symbols[resolver.resolve((let.bindings[1].symbol))])
    }
    check("""let
      |adder = (x: double): (double->double) -> (y: double) -> x + y,
      |in adder(1)(5)""".trimMargin()).let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val call = let.bound as ExCall
      assertEquals(DOUBLE, call.tag.type)

      assertEquals(function(function(DOUBLE, DOUBLE), DOUBLE),
          symbols[resolver.resolve((let.bindings[0].symbol))])
    }
  }

  private fun check(s: String): Result {
    val tree = parser.parse(s).syntax!!
    val resolver = Scoper.buildResolver(tree)
    val types = TypeChecker.computeTypes(tree, resolver)
    return Result(tree, resolver, types.resolutions)
  }
}