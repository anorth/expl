package org.explang.truffle.compiler

import com.oracle.truffle.api.frame.FrameDescriptor
import org.explang.truffle.Type

fun FrameDescriptor.addSlot(identifier: String, type: Type) =
    this.addFrameSlot(identifier, type, type.asSlotKind())!!

fun FrameDescriptor.findOrAddSlot(identifier: String, type: Type) =
    this.findOrAddFrameSlot(identifier, type, type.asSlotKind())!!
