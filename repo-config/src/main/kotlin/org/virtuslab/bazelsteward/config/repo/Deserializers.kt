package org.virtuslab.bazelsteward.config.repo

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.TextNode
import org.virtuslab.bazelsteward.core.PathPattern
import org.virtuslab.bazelsteward.core.common.PinningStrategy
import org.virtuslab.bazelsteward.core.library.BumpingStrategy
import org.virtuslab.bazelsteward.core.library.VersioningSchema

class ListOrItemDeserializer : StdDeserializer<List<*>>(List::class.java), ContextualDeserializer {
  private lateinit var type: JavaType

  override fun createContextual(ctxt: DeserializationContext?, property: BeanProperty): JsonDeserializer<*> {
    type = property.type.containedType(0)
    return this
  }

  override fun deserialize(jp: JsonParser?, ctxt: DeserializationContext?): List<*> {
    return if (jp?.currentToken == JsonToken.START_ARRAY) {
      val listType = ctxt!!.typeFactory.constructCollectionType(List::class.java, type)
      val deserializer = ctxt.findRootValueDeserializer(listType)
      deserializer.deserialize(jp, ctxt) as List<*>
    } else {
      listOf<Any>(ctxt!!.readValue(jp, type))
    }
  }
}

class VersioningSchemaDeserializer : StdDeserializer<VersioningSchema?>(VersioningSchema::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): VersioningSchema? {
    val versioningFieldValue = (jp.codec.readTree<JsonNode>(jp) as? TextNode)?.asText().toString()
    if (versioningFieldValue.startsWith("regex:")) {
      return VersioningSchema.Regex(versioningFieldValue.removePrefix("regex:").toRegex())
    }
    return VersioningSchema::class.sealedSubclasses.firstOrNull { it.simpleName?.lowercase() == versioningFieldValue }?.objectInstance
  }
}

class PinningStrategyDeserializer : StdDeserializer<PinningStrategy?>(PinningStrategy::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): PinningStrategy {
    val pinFieldValue = (jp.codec.readTree<JsonNode>(jp) as? TextNode)?.asText().toString()
    return PinningStrategy.parse(pinFieldValue)
  }
}

class BumpingStrategyDeserializer : StdDeserializer<BumpingStrategy?>(BumpingStrategy::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): BumpingStrategy? {
    return (jp.codec.readTree<JsonNode>(jp) as? TextNode)?.asText()?.toString()?.let { fieldValue ->
      val str = fieldValue.lowercase()
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
      BumpingStrategy.valueOf(str)
    }
  }
}

class DependencyNameFilterDeserializer : StdDeserializer<DependencyNameFilter?>(DependencyNameFilter::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): DependencyNameFilter? {
    return (jp.codec.readTree<JsonNode>(jp) as? TextNode)?.asText()?.toString()?.let { fieldValue ->
      DependencyNameFilter.parse(fieldValue)
    }
  }
}

class PathPatternDeserializer : StdDeserializer<PathPattern?>(PathPattern::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): PathPattern {
    return (jp.codec.readTree<JsonNode>(jp) as? TextNode)?.asText().toString().let { PathPattern.parse(it) }
  }
}
