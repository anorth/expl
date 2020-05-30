package org.explang.syntax

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ParserTest {
  @Test
  fun simpleExpressions() {
    val p = parse("1")
    assertNull(p.error)
  }

  @Test
  fun extraneousTokens() {
    val p = parse("1 + 2 a")
    assertNotNull(p.error)
    assertTrue("trailing" in p.error!!.reason)
  }

  @Test
  fun testSyntaxErrorTruncatedExpression() {
    val p = parse("let a = 1")
    assertNotNull(p.error)
    assertTrue("expecting" in p.error!!.reason)
  }

  @Test
  fun testSyntaxErrorMissingAnnotation() {
    val p = parse("let f = (x) -> 2*x")
    assertNotNull(p.error)
    // This is not the best error message
    assertTrue("expecting" in p.error!!.reason)
  }
}

private fun parse(expression: String): Parser.Result {
  val parser = Parser()
  return parser.parse(expression)
}
