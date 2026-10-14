package com.oshicue.kit.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.Locale

/** Common shape of the structured import/validation errors surfaced with a localized key and JSON path. */
interface KitIssue {
    val key: String
    val detail: String
    val path: String
}

typealias IssueFactory = (key: String, detail: String, path: String) -> Exception

object KitJson {
    private val parser = Json { isLenient = false }

    fun parse(text: String): JsonElement? = try {
        parser.parseToJsonElement(text)
    } catch (_: Exception) {
        null
    }
}

/** Builds JSON objects whose keys are emitted in sorted order, matching the iOS encoder's output. */
class JObj {
    private val map = LinkedHashMap<String, JsonElement>()

    fun put(key: String, value: String) { map[key] = JsonPrimitive(value) }
    fun put(key: String, value: Long) { map[key] = JsonPrimitive(value) }
    fun put(key: String, value: Int) { map[key] = JsonPrimitive(value) }
    fun put(key: String, value: Boolean) { map[key] = JsonPrimitive(value) }
    fun putString(key: String, value: String?) { if (value != null) put(key, value) }
    fun putElement(key: String, value: JsonElement) { map[key] = value }
    fun putArray(key: String, values: List<JsonElement>) { map[key] = JsonArray(values) }
    fun putStringArray(key: String, values: List<String>) { map[key] = JsonArray(values.map { JsonPrimitive(it) }) }
    fun putStringSet(key: String, values: Collection<String>) { map[key] = JsonArray(values.sorted().map { JsonPrimitive(it) }) }

    fun build(): JsonObject {
        val sorted = java.util.TreeMap<String, JsonElement>()
        sorted.putAll(map)
        return JsonObject(sorted)
    }
}

object JsonText {
    /** Pretty prints with sorted keys, two-space indentation and an unescaped solidus, like the app's exports. */
    fun encode(element: JsonElement): String {
        val builder = StringBuilder()
        write(builder, element, 0)
        return builder.toString()
    }

    private fun write(builder: StringBuilder, element: JsonElement, depth: Int) {
        when (element) {
            is JsonNull -> builder.append("null")
            is JsonPrimitive -> {
                if (element.isString) writeString(builder, element.content) else builder.append(element.content)
            }
            is JsonArray -> {
                if (element.isEmpty()) { builder.append("[]"); return }
                builder.append("[\n")
                element.forEachIndexed { index, item ->
                    indent(builder, depth + 1)
                    write(builder, item, depth + 1)
                    if (index != element.size - 1) builder.append(",")
                    builder.append("\n")
                }
                indent(builder, depth)
                builder.append("]")
            }
            is JsonObject -> {
                if (element.isEmpty()) { builder.append("{}"); return }
                builder.append("{\n")
                val entries = element.entries.toList()
                entries.forEachIndexed { index, (key, value) ->
                    indent(builder, depth + 1)
                    writeString(builder, key)
                    builder.append(" : ")
                    write(builder, value, depth + 1)
                    if (index != entries.size - 1) builder.append(",")
                    builder.append("\n")
                }
                indent(builder, depth)
                builder.append("}")
            }
        }
    }

    private fun indent(builder: StringBuilder, depth: Int) {
        repeat(depth) { builder.append("  ") }
    }

    private fun writeString(builder: StringBuilder, value: String) {
        builder.append('"')
        for (character in value) {
            when (character) {
                '"' -> builder.append("\\\"")
                '\\' -> builder.append("\\\\")
                '\n' -> builder.append("\\n")
                '\r' -> builder.append("\\r")
                '\t' -> builder.append("\\t")
                '\b' -> builder.append("\\b")
                '\u000C' -> builder.append("\\f")
                else -> if (character < ' ') builder.append("\\u%04x".format(character.code)) else builder.append(character)
            }
        }
        builder.append('"')
    }
}

object KitText {
    /** Case- and diacritic-insensitive word search shared by record and entry filters. */
    fun fallsBack(value: String): String {
        val decomposed = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
        val builder = StringBuilder()
        for (character in decomposed) {
            val type = Character.getType(character)
            if (type == Character.NON_SPACING_MARK.toInt() || type == Character.COMBINING_SPACING_MARK.toInt()) continue
            builder.append(character)
        }
        return builder.toString().lowercase(Locale.ROOT)
    }

    fun contains(haystack: String, needle: String): Boolean = fallsBack(haystack).contains(fallsBack(needle))

    fun standardCompare(left: String, right: String): Int {
        val collator = java.text.Collator.getInstance(Locale.ROOT)
        collator.strength = java.text.Collator.SECONDARY
        val result = collator.compare(left, right)
        return if (result == 0) left.compareTo(right) else result
    }

    fun standardContains(value: String, search: String): Boolean =
        search.isEmpty() || fallsBack(value).contains(fallsBack(search))

    fun words(query: String): List<String> = query.split(Regex("\\s+")).filter { it.isNotEmpty() }
}

fun kotlinx.serialization.json.JsonObject.stringValue(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

internal fun typeName(element: JsonElement?): String = when (element) {
    null, is JsonNull -> "null"
    is JsonPrimitive -> when {
        element.isString -> "a string"
        element.content == "true" || element.content == "false" -> "a boolean"
        else -> "a number"
    }
    is JsonObject -> "a dictionary"
    is JsonArray -> "an array"
}
