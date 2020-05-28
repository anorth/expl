package org.explang.syntax

import org.explang.analysis.Analyzer
import org.junit.Assert.fail

class TestParser(debug: Boolean) {
  private val parser = Parser(
      printTokens = debug,
      printParse = debug,
      printAst = debug,
      trace = debug
  )

  fun parse(expression: String): Parser.Result<Analyzer.Tag> {
    val parse = parser.parse(expression) { Analyzer.Tag() }
    if (parse.failed()) {
      fail(parse.errorDetail())
    }
    return parse
  }
}
