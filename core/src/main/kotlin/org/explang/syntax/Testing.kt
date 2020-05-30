package org.explang.syntax

import org.junit.Assert.fail

class TestParser(debug: Boolean) {
  private val parser = Parser(
      printTokens = debug,
      printParse = debug,
      printAst = debug,
      trace = debug
  )

  fun parse(expression: String): Parser.Result {
    val parse = parser.parse(expression)
    if (parse.failed()) {
      fail(parse.errorDetail())
    }
    return parse
  }
}
