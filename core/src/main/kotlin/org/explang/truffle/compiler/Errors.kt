package org.explang.truffle.compiler

import org.explang.syntax.ExTree

class CompileError(msg: String, val tree: ExTree<Analyzer.Tag>) : Exception(msg)

inline fun check(tree: ExTree<Analyzer.Tag>, predicate: Boolean, msg: () -> String) {
  if (!predicate) {
    throw CompileError(msg(), tree)
  }
}
