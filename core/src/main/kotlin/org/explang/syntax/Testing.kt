package org.explang.syntax

import org.explang.truffle.compiler.Analyzer
import org.junit.Assert.fail

class TestParser(debug: Boolean) {
  private val parser = Parser(
      printTokens = debug,
      printParse = debug,
      printAst = debug,
      trace = debug
  )

  fun parse(expression: String): ExTree<Analyzer.Tag> {
    val parse = parser.parse(expression) { Analyzer.Tag() }
    parse.error?.let { error ->
      fail("Parse failed ${error.line}:${error.charPositionInLine} ${error.msg}")
    }
    return parse.syntax!!
  }
}
