package org.explang.compiler

import org.explang.intermediate.ITree

class CompileError(msg: String, val tree: ITree) : Exception(msg)

inline fun check(tree: ITree, predicate: Boolean, msg: () -> String) {
  if (!predicate) {
    throw CompileError(msg(), tree)
  }
}
