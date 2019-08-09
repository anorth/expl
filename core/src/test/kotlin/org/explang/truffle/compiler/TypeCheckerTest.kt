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
import org.explang.syntax.Type.Companion.BOOL
import org.explang.syntax.Type.Companion.DOUBLE
import org.explang.syntax.Type.Companion.LONG
import org.explang.syntax.Type.Companion.NONE
import org.explang.syntax.Type.Companion.function
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    assertEquals(LONG, check("1").tree.tag.type)
    assertEquals(DOUBLE, check("5.2").tree.tag.type)
  }

  @Test
  fun unaryOps() {
    check("-1").let {
      assertEquals(LONG, (it.tree as ExUnaryOp).operand.tag.type)
      assertEquals(LONG, it.tree.tag.type)
    }
  }

  @Test
  fun binOps() {
    assertEquals(LONG, check("1 + 2").tree.tag.type)
    assertEquals(BOOL, check("1 > 2").tree.tag.type)
    assertEquals(BOOL, check("1 == 2").tree.tag.type)
    assertEquals(BOOL, check("true == false").tree.tag.type)
  }

  @Test
  fun iff() {
    assertEquals(LONG, check("if true then 1 else 2").tree.tag.type)
    assertEquals(BOOL, check("if true then true else false").tree.tag.type)
  }

  @Test
  fun boundSymbol() {
    check("let a = 1 in a").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val binding = let.bindings.first()

      assertEquals(LONG, let.tag.type)
      assertEquals(NONE, binding.tag.type) // Remains untyped
      assertEquals(NONE, binding.symbol.tag.type) // Remains untyped
      assertEquals(LONG, binding.value.tag.type)
      assertEquals(LONG, symbols[resolver.resolve(binding.symbol)])
    }
    check("let a = 1 in 1 + a").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val binOp = let.bound as ExBinaryOp
      assertEquals(LONG, let.tag.type)
      assertEquals(LONG, binOp.tag.type)
      assertEquals(LONG, binOp.left.tag.type)
      assertEquals(LONG, binOp.right.tag.type) // Inferred
      assertEquals(LONG, symbols[resolver.resolve(binOp.right as ExSymbol<*>)])
    }
    check("let a = 1, b = a+a in a+b").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      assertEquals(LONG, let.tag.type)
      assertEquals(LONG, let.bindings[0].value.tag.type)
      assertEquals(LONG, let.bindings[1].value.tag.type)
      assertEquals(LONG, symbols[resolver.resolve(let.bindings[0].symbol)])
      assertEquals(LONG, symbols[resolver.resolve(let.bindings[1].symbol)])
    }
    check("let a = 1 in let b = a+a in a+b").let { (tree, resolver, symbols) ->
      val outer = tree as ExLet
      val inner = outer.bound as ExLet

      assertEquals(LONG, outer.tag.type)
      assertEquals(LONG, outer.bindings[0].value.tag.type)
      assertEquals(LONG, inner.bindings[0].value.tag.type)
      assertEquals(LONG, symbols[resolver.resolve(outer.bindings[0].symbol)])
      assertEquals(LONG, symbols[resolver.resolve(inner.bindings[0].symbol)])
    }
  }

  @Test
  fun nestedBinding() {
    check("let a = 1 in let b = a+a in a+b").let { (tree, resolver, symbols) ->
      val outer = tree as ExLet
      val inner = outer.bound as ExLet
      assertEquals(LONG, outer.tag.type)
      assertEquals(LONG, outer.bindings[0].value.tag.type)
      assertEquals(LONG, inner.bindings[0].value.tag.type)
      assertEquals(LONG, symbols[resolver.resolve(outer.bindings[0].symbol)])
      assertEquals(LONG, symbols[resolver.resolve(inner.bindings[0].symbol)])
    }
  }

  @Test
  fun iffSymbol() {
    check("let a = true in if a then 1 else 2").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val iff = let.bound as ExIf
      assertEquals(LONG, let.tag.type)
      assertEquals(LONG, iff.tag.type)
      assertEquals(BOOL, iff.test.tag.type)
      assertEquals(LONG, iff.left.tag.type)
      assertEquals(LONG, iff.right.tag.type)
      assertEquals(BOOL, symbols[resolver.resolve(iff.test as ExSymbol<*>)])
    }
    check("let a = 1, b = 2 in if false then a else b").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val iff = let.bound as ExIf
      assertEquals(LONG, let.tag.type)
      assertEquals(LONG, iff.tag.type)
      assertEquals(BOOL, iff.test.tag.type)
      assertEquals(LONG, iff.left.tag.type)
      assertEquals(LONG, iff.right.tag.type)
      assertEquals(LONG, symbols[resolver.resolve(iff.left as ExSymbol<*>)])
      assertEquals(LONG, symbols[resolver.resolve(iff.right as ExSymbol<*>)])
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
    check("((a: long, b: long) -> a > b)(2, 1)").let { (tree, _, _) ->
      val call = tree as ExCall
      assertEquals(BOOL, call.tag.type)
      assertEquals(function(BOOL, LONG, LONG), call.callee.tag.type)
      assertEquals(LONG, call.args[0].tag.type)
      assertEquals(LONG, call.args[1].tag.type)
    }
  }

  @Test
  fun closure() {
    check("let a = 1 in () -> a").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val fn = let.bound as ExLambda
      assertEquals(function(LONG), fn.tag.type)
      assertEquals(LONG, symbols[resolver.resolve((fn.body as ExSymbol<*>))])
    }
    check("(let a = 1 in () -> a)()").let { (tree, _, _) ->
      val call = tree as ExCall
      assertEquals(LONG, call.tag.type)
      assertEquals(function(LONG), call.callee.tag.type)
    }
    check("let a = 1 in (() -> a)()").let { (tree, _, _) ->
      val let = tree as ExLet
      val call = let.bound as ExCall
      assertEquals(LONG, let.tag.type)
      assertEquals(LONG, call.tag.type)
      assertEquals(function(LONG), call.callee.tag.type)
    }
  }

  @Test
  fun recursion() {
    check("let f = (x: long): long -> f(x+1) in f(1)").let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val outerCall = let.bound as ExCall
      val sym1 = let.bindings[0].symbol
      val fn = let.bindings[0].value as ExLambda
      val innerCall = fn.body as ExCall
      val sym2 = innerCall.callee as ExSymbol

      assertEquals(LONG, let.tag.type)
      assertEquals(LONG, outerCall.tag.type)
      assertEquals(LONG, innerCall.tag.type)
      assertEquals(function(LONG, LONG), sym2.tag.type)

      assertEquals(function(LONG, LONG), symbols[resolver.resolve((sym1))])
      assertEquals(function(LONG, LONG), symbols[resolver.resolve((sym2))])
    }
  }

  @Test
  fun higherOrderFunction() {
    check("""(inner: (->long)) -> inner""").let { (tree, _, _) ->
      val fn = tree as ExLambda
      assertEquals(NONE, fn.annotation)
      assertEquals(function(LONG), fn.parameters[0].tag.type)
      assertEquals(function(LONG), fn.body.tag.type)
      assertEquals(function(function(LONG), function(LONG)), fn.tag.type)
    }
    check("""let f = (inner: (->long)) -> inner in f""").let { (tree, _, _) ->
      val let = tree as ExLet
      assertEquals(function(function(LONG), function(LONG)), let.bound.tag.type)
      assertEquals(function(function(LONG), function(LONG)), let.tag.type)
    }
    check("""let
      |f = (inner: (->long)) -> inner,
      |g = () -> 1,
      |in f(g)""".trimMargin()).let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      assertEquals(function(LONG), let.bound.tag.type)
      assertEquals(function(LONG), let.tag.type)

      assertEquals(function(function(LONG), function(LONG)),
          symbols[resolver.resolve((let.bindings[0].symbol))])
      assertEquals(function(LONG),
          symbols[resolver.resolve((let.bindings[1].symbol))])
    }
    check("""let
      |apply = (f: (long->long), x: long) -> f(x),
      |inc = (x: long) -> x + 1,
      |in apply(inc, 1)""".trimMargin()).let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val call = let.bound as ExCall
      assertEquals(LONG, call.tag.type)

      assertEquals(function(LONG, function(LONG, LONG), LONG),
          symbols[resolver.resolve((let.bindings[0].symbol))])
      assertEquals(function(LONG, LONG),
          symbols[resolver.resolve((let.bindings[1].symbol))])
    }
    check("""let
      |adder = (x: long): (long->long) -> (y: long) -> x + y,
      |in adder(1)(5)""".trimMargin()).let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val call = let.bound as ExCall
      assertEquals(LONG, call.tag.type)

      assertEquals(function(function(LONG, LONG), LONG),
          symbols[resolver.resolve((let.bindings[0].symbol))])
    }
  }

  @Test
  fun builtins() {
    check("sqrt(2.0)", mapOf("sqrt" to function(DOUBLE, DOUBLE))).let { (tree, resolver, _) ->
      val call = tree as ExCall
      val builtin = call.callee as ExSymbol

      assertEquals(DOUBLE, call.tag.type)
      assertEquals(function(DOUBLE, DOUBLE), builtin.tag.type)

      assertTrue(resolver.resolve(builtin) is Scope.Resolution.Environment)
    }

    check("let x = 2.0 in sqrt(x)",
        mapOf("sqrt" to function(DOUBLE, DOUBLE))).let { (tree, resolver, _) ->
      val let = tree as ExLet
      val call = let.bound as ExCall
      val builtin = call.callee as ExSymbol

      assertEquals(DOUBLE, let.tag.type)
      assertEquals(DOUBLE, call.tag.type)
      assertEquals(function(DOUBLE, DOUBLE), builtin.tag.type)

      assertTrue(resolver.resolve(builtin) is Scope.Resolution.Environment)
    }

    check("let s(x: double) = sqrt(x) in s(2.0)",
        mapOf("sqrt" to function(DOUBLE, DOUBLE))).let { (tree, resolver, symbols) ->
      val let = tree as ExLet
      val binding = let.bindings[0]
      val lambda = binding.value as ExLambda
      val call = lambda.body as ExCall
      val builtin = call.callee as ExSymbol

      assertEquals(DOUBLE, let.tag.type)
      assertEquals(function(DOUBLE, DOUBLE), lambda.tag.type)
      assertEquals(DOUBLE, call.tag.type)
      assertEquals(function(DOUBLE, DOUBLE), builtin.tag.type)

      assertEquals(function(DOUBLE, DOUBLE),
          symbols[resolver.resolve((binding.symbol))])

      // Resolve directly to environment, not closure.
      assertTrue(resolver.resolve(builtin) is Scope.Resolution.Environment)
    }
  }

  private fun check(s: String, builtins: Map<String, Type> = mapOf()): Result {
    val tree = parser.parse(s).syntax!!
    val resolver = Scoper.buildResolver(tree, builtins.keys)
    val types = TypeChecker.computeTypes(tree, resolver, builtins)
    return Result(tree, resolver, types.resolutions)
  }
}
