package org.explang.truffle.compiler

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameDescriptor
import org.explang.syntax.TestParser

class TestCompiler(private val debug: Boolean) {
  private val parser = TestParser(debug)
  private val compiler = Compiler(debug)

  fun eval(expression: String): Any {
    val parse = parser.parse(expression)
    try {
      val truffleEntry = compiler.compile(parse.syntax!!)
      val topFrame = Truffle.getRuntime().createVirtualFrame(arrayOfNulls(0), FrameDescriptor())
      val result = truffleEntry.executeDeclaredType(topFrame)
      if (debug) {
        println("*Result*")
        println(result)
      }
      return result
    } catch (e: CompileError) {
      println("*Compile failed*")
      println(expression)
      println(" ".repeat(parse.tokens[e.tree.tokenRange.start].startIndex) + "^")
      println(e.message)
      throw e
    }
  }
}
