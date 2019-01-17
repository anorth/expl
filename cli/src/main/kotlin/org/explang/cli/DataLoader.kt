package org.explang.cli

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.explang.array.AbstractArray
import org.explang.array.ArrayOfDouble
import org.explang.array.EmptyArray
import org.explang.array.Shape
import org.explang.syntax.Type
import java.io.File

class DataLoader {
  private val data = mutableMapOf<String, Any>()
  private val mapper = ObjectMapper()

  fun forEach(handler: (String, Type, Any) -> Unit) {
    data.forEach { key, value ->
      handler(key, typeForValue(value), value)
    }
  }

// At present, data values are bound directly into the code rather than looked up in a frame.
//  fun frameDescriptor(): FrameDescriptor {
//    val d = FrameDescriptor()
//    data.forEach { key, value ->
//      d.addSlot(key, typeForValue(value))
//    }
//    return d
//  }


  fun loadJsonToEnv(path: String) {
    println(File(path).absolutePath)
    val json = mapper.readTree(File(path))
    when (json) {
      is ArrayNode -> data["data"] = loadArray(json)
      is ObjectNode -> {
        for (elm in json.fields()) {
          data[elm.key] = loadValue(elm.value)
        }
      }
    }
  }

  private fun loadValue(node: JsonNode): Any {
    return when (node) {
      is ArrayNode -> loadArray(node)
      is TextNode -> node.toString()
      is DoubleNode, is IntNode -> node.doubleValue()
      else -> {
        throw RuntimeException("Can't load json node $node")
      }
    }
  }

  private fun loadArray(array: ArrayNode): Any {
    return if (array.size() == 0) {
      return EmptyArray(Shape(0))
    } else {
      val first = array.get(0)
      if (first.isNumber) {
        loadDoubleArray(array)
      } else {
        // TODO: handle nested arrays
        throw RuntimeException("Can't handle json value $first")
      }
    }
  }

  private fun loadDoubleArray(array: ArrayNode): ArrayOfDouble {
    val values = DoubleArray(array.size())
    for (i in 0..values.lastIndex) {
      values[i] = array[i].doubleValue()
    }
    return ArrayOfDouble.of(values)
  }
}

private fun typeForValue(value: Any): Type {
  return when (value) {
    is Boolean -> Type.BOOL
    is Double -> Type.DOUBLE
    is AbstractArray -> Type.array(value.element, value.shape.dimensions)
    else -> Type.NONE
  }
}
