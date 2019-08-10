package org.explang.truffle.compiler

import com.oracle.truffle.api.frame.FrameDescriptor
import com.oracle.truffle.api.frame.FrameSlot
import com.oracle.truffle.api.frame.FrameSlotKind
import org.explang.syntax.NoneType
import org.explang.syntax.PrimType
import org.explang.syntax.Type

fun FrameDescriptor.addSlot(identifier: String, type: Type) =
    this.addFrameSlot(identifier, type, type.asSlotKind())!!

fun FrameDescriptor.findSlot(identifier: String): FrameSlot =
    this.findFrameSlot(identifier) ?: throw RuntimeException("No slot for $identifier")

fun FrameDescriptor.findOrAddSlot(identifier: String, type: Type) =
    this.findOrAddFrameSlot(identifier, type, type.asSlotKind())!!

fun Type.asSlotKind() = when {
  this is PrimType -> when (this) {
    PrimType.BOOL -> FrameSlotKind.Boolean
    PrimType.LONG -> FrameSlotKind.Long
    PrimType.DOUBLE -> FrameSlotKind.Double
  }
  this == NoneType -> FrameSlotKind.Illegal
  else -> FrameSlotKind.Object
}
