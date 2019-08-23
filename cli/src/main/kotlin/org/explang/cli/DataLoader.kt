package org.explang.cli

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.DoubleNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.explang.array.BooleanSliceValue
import org.explang.array.DoubleSliceValue
import org.explang.syntax.Type
import java.io.File

class DataLoader {
  private data class Datum(val type: Type, val obj: Any)

  private val data = mutableMapOf<String, Datum>()
  private val mapper = ObjectMapper()

  fun forEach(handler: (String, Type, Any) -> Unit) {
    data.forEach { (key, value) ->
      handler(key, value.type, value.obj)
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

  private fun loadValue(node: JsonNode): Datum {
    return when (node) {
      is ArrayNode -> loadArray(node)
      is TextNode -> Datum(Type.NONE, node.toString())
      is DoubleNode -> Datum(Type.DOUBLE, node.doubleValue())
      is IntNode -> Datum(Type.LONG, node.longValue())
      else -> {
        throw RuntimeException("Can't load json node $node")
      }
    }
  }

  private fun loadArray(array: ArrayNode): Datum {
    return if (array.size() == 0) {
      return Datum(Type.slice(Type.BOOL), BooleanSliceValue.of(BooleanArray(0)))
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

  private fun loadDoubleArray(array: ArrayNode): Datum {
    val values = DoubleArray(array.size())
    for (i in 0..values.lastIndex) {
      values[i] = array[i].doubleValue()
    }
    return Datum(Type.slice(Type.DOUBLE), DoubleSliceValue.of(values))
  }
}
