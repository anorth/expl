package org.explang.truffle.compiler

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameDescriptor
import org.explang.syntax.TestParser
import org.explang.truffle.nodes.ExpressionNode

class TestCompiler(private val debug: Boolean) {
  private val parser = TestParser(debug)
  private val compiler = Compiler(debug)

  fun compile(expression: String): ExpressionNode {
    val parse = parser.parse(expression)
    try {
      return compiler.compile(parse.syntax!!)
    } catch (e: CompileError) {
      println("*Compile failed*")
      println(expression)
      println(" ".repeat(parse.tokens[e.tree.tokenRange.start].startIndex) + "^")
      println(e.message)
      throw e
    }
  }

  fun eval(expression: String): Any {
    return eval(compile(expression))
  }

  fun eval(node: ExpressionNode): Any {
    val topFrame = Truffle.getRuntime().createVirtualFrame(arrayOfNulls(0), FrameDescriptor())
    val result = node.executeDeclaredType(topFrame)
    if (debug) {
      println("*Result*")
      println(result)
    }
    return result
  }
}
