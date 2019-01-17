package org.explang.truffle.compiler

import org.explang.truffle.nodes.builtin.ArrayBuiltins
import org.explang.truffle.nodes.builtin.BuiltInNode
import org.explang.truffle.nodes.builtin.MathBuiltins

// All built-in functions by name.
val BUILT_INS = listOf(
    MathBuiltins.sqrt(),
    ArrayBuiltins.zeros()
).associateBy(BuiltInNode::name)
