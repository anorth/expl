package org.explang.truffle.compiler

import org.explang.truffle.nodes.builtin.BuiltInNode
import org.explang.truffle.nodes.builtin.SqrtBuiltIn

// All built-in functions by name.
val BUILT_INS = listOf(
    SqrtBuiltIn()
).associateBy(BuiltInNode::name)
