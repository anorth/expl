package org.explang.cli

import com.oracle.truffle.api.Truffle
import com.oracle.truffle.api.frame.FrameDescriptor
import com.xenomachina.argparser.ArgParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.CommonTokenStream
import org.explang.parser.ExplLexer
import org.explang.parser.ExplParser
import org.explang.truffle.compiler.ExplCompiler
import org.explang.truffle.nodes.ExpressionNode
import org.explang.truffle.nodes.ExpressionRootNode

// See https://github.com/xenomachina/kotlin-argparser
class Args(parser: ArgParser) {
  val trace by parser.flagging("--trace", help = "Print parser trace")
  val showTokens by parser.flagging("--show-tokens", help = "Print tokens to standard output")
  val showTree by parser.flagging("--show-tree", help = "Print parse tree to standard output")
  val expression by parser.positional("EXPRESSION", help = "Expression to evaluate")
}

class Cli {
  fun run(args: Args) {
    val lexer = ExplLexer(CharStreams.fromString(args.expression))
    val tokens = CommonTokenStream(lexer)
    val parser = ExplParser(tokens)
    parser.buildParseTree = args.showTree
    parser.isTrace = args.trace

    // Show tokens
    if (args.showTokens) {
      println("*Tokens*")
      tokens.fill()
      for (tok in tokens.tokens) {
        if (tok is CommonToken) {
          println(tok.toString(lexer))
        } else {
          println(tok)
        }
      }
    }

    val parse = parser.expression()
    if (args.showTree) {
      println("*Tree*")
      println(parse.toStringTree(parser))
    }

    val compiler = ExplCompiler()
    val ast = compiler.compile(parse)
    val result = evaluate(ast)
    println("*Result*")
    println(result)
  }

  private fun evaluate(expr: ExpressionNode): Any {
    val baseFrame = Truffle.getRuntime().createVirtualFrame(arrayOf(), FrameDescriptor())
    val mf = baseFrame.materialize()

    // Evaluate the expression
    val rootNode = ExpressionRootNode(expr, mf.frameDescriptor)
    val target = Truffle.getRuntime().createCallTarget(rootNode)
    return target.call(mf)
  }
}

fun main(args: Array<String>) {
  val pargs = ArgParser(args).parseInto(::Args)
  Cli().run(pargs)
}
