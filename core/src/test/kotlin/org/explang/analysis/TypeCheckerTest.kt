package org.explang.analysis

import org.explang.interpreter.Environment
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
      val tree: ITree,
      val resolver: Resolver,
      val symbols: Map<Scope.Resolution, Type>
  )

  private val parser = TestParser(debug = false)
  private val compiler = IntermediateCompiler()
  private val intrinsics = Environment.withOperators().types()

  @Test
  fun literals() {
    assertEquals(BOOL, check("true").tree.type)
    assertEquals(LONG, check("1").tree.type)
    assertEquals(DOUBLE, check("5.2").tree.type)
  }

  @Test
  fun unaryOps() {
    check("-1").let {
      assertEquals(LONG, (it.tree as ICall).args[0].type)
      assertEquals(LONG, it.tree.type)
    }
  }

  @Test
  fun binOps() {
    assertEquals(LONG, check("1 + 2").tree.type)
    assertEquals(BOOL, check("1 > 2").tree.type)
    assertEquals(BOOL, check("1 == 2").tree.type)
    assertEquals(BOOL, check("true == false").tree.type)
  }

  @Test
  fun iff() {
    assertEquals(LONG, check("if true then 1 else 2").tree.type)
    assertEquals(BOOL, check("if true then true else false").tree.type)
  }

  @Test
  fun boundSymbol() {
    check("let a = 1 in a").let { (tree, resolver, symbols) ->
      val let = tree as ILet
      val binding = let.bindings.first()

      assertEquals(LONG, let.type)
      assertEquals(NONE, binding.type) // Remains untyped
      assertEquals(NONE, binding.symbol.type) // Remains untyped
      assertEquals(LONG, binding.value.type)
      assertEquals(LONG, symbols[resolver.resolve(binding.symbol)])
    }
    check("let a = 1 in 1 + a").let { (tree, resolver, symbols) ->
      val let = tree as ILet
      val binOp = let.bound as ICall
      assertEquals(LONG, let.type)
      assertEquals(LONG, binOp.type)
      assertEquals(LONG, binOp.args[0].type)
      assertEquals(LONG, binOp.args[1].type) // Inferred
      assertEquals(LONG, symbols[resolver.resolve(binOp.args[1] as ISymbol)])
    }
    check("let a = 1, b = a+a in a+b").let { (tree, resolver, symbols) ->
      val let = tree as ILet
      assertEquals(LONG, let.type)
      assertEquals(LONG, let.bindings[0].value.type)
      assertEquals(LONG, let.bindings[1].value.type)
      assertEquals(LONG, symbols[resolver.resolve(let.bindings[0].symbol)])
      assertEquals(LONG, symbols[resolver.resolve(let.bindings[1].symbol)])
    }
    check("let a = 1 in let b = a+a in a+b").let { (tree, resolver, symbols) ->
      val outer = tree as ILet
      val inner = outer.bound as ILet

      assertEquals(LONG, outer.type)
      assertEquals(LONG, outer.bindings[0].value.type)
      assertEquals(LONG, inner.bindings[0].value.type)
      assertEquals(LONG, symbols[resolver.resolve(outer.bindings[0].symbol)])
      assertEquals(LONG, symbols[resolver.resolve(inner.bindings[0].symbol)])
    }
  }

  @Test
  fun nestedBinding() {
    check("let a = 1 in let b = a+a in a+b").let { (tree, resolver, symbols) ->
      val outer = tree as ILet
      val inner = outer.bound as ILet
      assertEquals(LONG, outer.type)
      assertEquals(LONG, outer.bindings[0].value.type)
      assertEquals(LONG, inner.bindings[0].value.type)
      assertEquals(LONG, symbols[resolver.resolve(outer.bindings[0].symbol)])
      assertEquals(LONG, symbols[resolver.resolve(inner.bindings[0].symbol)])
    }
  }

  @Test
  fun iffSymbol() {
    check("let a = true in if a then 1 else 2").let { (tree, resolver, symbols) ->
      val let = tree as ILet
      val iff = let.bound as IIf
      assertEquals(LONG, let.type)
      assertEquals(LONG, iff.type)
      assertEquals(BOOL, iff.test.type)
      assertEquals(LONG, iff.left.type)
      assertEquals(LONG, iff.right.type)
      assertEquals(BOOL, symbols[resolver.resolve(iff.test as ISymbol)])
    }
    check("let a = 1, b = 2 in if false then a else b").let { (tree, resolver, symbols) ->
      val let = tree as ILet
      val iff = let.bound as IIf
      assertEquals(LONG, let.type)
      assertEquals(LONG, iff.type)
      assertEquals(BOOL, iff.test.type)
      assertEquals(LONG, iff.left.type)
      assertEquals(LONG, iff.right.type)
      assertEquals(LONG, symbols[resolver.resolve(iff.left as ISymbol)])
      assertEquals(LONG, symbols[resolver.resolve(iff.right as ISymbol)])
    }
  }

  @Test
  fun simpleLambda() {
    check("() -> true").let { (tree, _, _) ->
      val fn = tree as ILambda
      assertEquals(function(BOOL), fn.type)
    }
    check("x: bool -> x").let { (tree, _, _) ->
      val fn = tree as ILambda
      assertEquals(function(BOOL, BOOL), fn.type)
    }
    check("(x: bool) -> x").let { (tree, _, _) ->
      val fn = tree as ILambda
      assertEquals(function(BOOL, BOOL), fn.type)
    }
    check("(a: double, b: double) -> a > b").let { (tree, _, _) ->
      val fn = tree as ILambda
      assertEquals(function(BOOL, DOUBLE, DOUBLE), fn.type)
    }
  }

  @Test
  fun simpleCall() {
    check("(() -> true)()").let { (tree, _, _) ->
      val call = tree as ICall
      assertEquals(BOOL, call.type)
    }
    check("(x: bool -> x)(false)").let { (tree, _, _) ->
      val call = tree as ICall
      assertEquals(BOOL, call.type)
      assertEquals(function(BOOL, BOOL), call.callee.type)
      assertEquals(BOOL, call.args[0].type)
    }
    check("((a: long, b: long) -> a > b)(2, 1)").let { (tree, _, _) ->
      val call = tree as ICall
      assertEquals(BOOL, call.type)
      assertEquals(function(BOOL, LONG, LONG), call.callee.type)
      assertEquals(LONG, call.args[0].type)
      assertEquals(LONG, call.args[1].type)
    }
  }

  @Test
  fun closure() {
    check("let a = 1 in () -> a").let { (tree, resolver, symbols) ->
      val let = tree as ILet
      val fn = let.bound as ILambda
      assertEquals(function(LONG), fn.type)
      assertEquals(LONG, symbols[resolver.resolve((fn.body as ISymbol))])
    }
    check("(let a = 1 in () -> a)()").let { (tree, _, _) ->
      val call = tree as ICall
      assertEquals(LONG, call.type)
      assertEquals(function(LONG), call.callee.type)
    }
    check("let a = 1 in (() -> a)()").let { (tree, _, _) ->
      val let = tree as ILet
      val call = let.bound as ICall
      assertEquals(LONG, let.type)
      assertEquals(LONG, call.type)
      assertEquals(function(LONG), call.callee.type)
    }
  }

  @Test
  fun recursion() {
    check("let f = (x: long): long -> f(x+1) in f(1)").let { (tree, resolver, symbols) ->
      val let = tree as ILet
      val outerCall = let.bound as ICall
      val sym1 = let.bindings[0].symbol
      val fn = let.bindings[0].value as ILambda
      val innerCall = fn.body as ICall
      val sym2 = innerCall.callee as ISymbol

      assertEquals(LONG, let.type)
      assertEquals(LONG, outerCall.type)
      assertEquals(LONG, innerCall.type)
      assertEquals(function(LONG, LONG), sym2.type)

      assertEquals(function(LONG, LONG), symbols[resolver.resolve((sym1))])
      assertEquals(function(LONG, LONG), symbols[resolver.resolve((sym2))])
    }
  }

  @Test
  fun higherOrderFunction() {
    check("""(inner: (->long)) -> inner""").let { (tree, _, _) ->
      val fn = tree as ILambda
      assertEquals(NONE, fn.annotation)
      assertEquals(function(LONG), fn.parameters[0].type)
      assertEquals(function(LONG), fn.body.type)
      assertEquals(function(function(LONG), function(LONG)), fn.type)
    }
    check("""let f = (inner: (->long)) -> inner in f""").let { (tree, _, _) ->
      val let = tree as ILet
      assertEquals(function(function(LONG), function(LONG)), let.bound.type)
      assertEquals(function(function(LONG), function(LONG)), let.type)
    }
    check("""let
      |f = (inner: (->long)) -> inner,
      |g = () -> 1,
      |in f(g)""".trimMargin()).let { (tree, resolver, symbols) ->
      val let = tree as ILet
      assertEquals(function(LONG), let.bound.type)
      assertEquals(function(LONG), let.type)

      assertEquals(function(function(LONG), function(LONG)),
          symbols[resolver.resolve((let.bindings[0].symbol))])
      assertEquals(function(LONG),
          symbols[resolver.resolve((let.bindings[1].symbol))])
    }
    check("""let
      |apply = (f: (long->long), x: long) -> f(x),
      |inc = (x: long) -> x + 1,
      |in apply(inc, 1)""".trimMargin()).let { (tree, resolver, symbols) ->
      val let = tree as ILet
      val call = let.bound as ICall
      assertEquals(LONG, call.type)

      assertEquals(function(LONG, function(LONG, LONG), LONG),
          symbols[resolver.resolve((let.bindings[0].symbol))])
      assertEquals(function(LONG, LONG),
          symbols[resolver.resolve((let.bindings[1].symbol))])
    }
    check("""let
      |adder = (x: long): (long->long) -> (y: long) -> x + y,
      |in adder(1)(5)""".trimMargin()).let { (tree, resolver, symbols) ->
      val let = tree as ILet
      val call = let.bound as ICall
      assertEquals(LONG, call.type)

      assertEquals(function(function(LONG, LONG), LONG),
          symbols[resolver.resolve((let.bindings[0].symbol))])
    }
  }

  @Test
  fun builtins() {
    val builtins = mapOf("sqrt" to listOf(function(DOUBLE, DOUBLE)))
    check("sqrt(2.0)", builtins).let { (tree, resolver, _) ->
      val call = tree as ICall
      val builtin = call.callee as ISymbol

      assertEquals(DOUBLE, call.type)
      assertEquals(function(DOUBLE, DOUBLE), builtin.type)

      assertTrue(resolver.resolve(builtin) is Scope.Resolution.Environment)
    }

    check("let x = 2.0 in sqrt(x)", builtins).let { (tree, resolver, _) ->
      val let = tree as ILet
      val call = let.bound as ICall
      val builtin = call.callee as ISymbol

      assertEquals(DOUBLE, let.type)
      assertEquals(DOUBLE, call.type)
      assertEquals(function(DOUBLE, DOUBLE), builtin.type)

      assertTrue(resolver.resolve(builtin) is Scope.Resolution.Environment)
    }

    check("let s(x: double) = sqrt(x) in s(2.0)", builtins).let { (tree, resolver, symbols) ->
      val let = tree as ILet
      val binding = let.bindings[0]
      val lambda = binding.value as ILambda
      val call = lambda.body as ICall
      val builtin = call.callee as ISymbol

      assertEquals(DOUBLE, let.type)
      assertEquals(function(DOUBLE, DOUBLE), lambda.type)
      assertEquals(DOUBLE, call.type)
      assertEquals(function(DOUBLE, DOUBLE), builtin.type)

      assertEquals(function(DOUBLE, DOUBLE),
          symbols[resolver.resolve((binding.symbol))])

      // Resolve directly to environment, not closure.
      assertTrue(resolver.resolve(builtin) is Scope.Resolution.Environment)
    }
  }

  private fun check(s: String, builtins: Map<String, List<Type>> = mapOf()): Result {
    val env = intrinsics + builtins
    val syntax = parser.parse(s).syntax!!
    val tree = compiler.transform(syntax)
    val resolver = Scoper.buildResolver(tree, env.keys)
    val types = TypeChecker.computeTypes(tree, resolver, env)
    return Result(tree, resolver, types.resolutions)
  }
}
