package com.oshicue.kit.core

/**
 * A small, dependency-free JSON implementation.
 *
 * The iOS app relies on JSONEncoder with sorted keys, pretty printing and no slash escaping,
 * and on precise decoding failures that map to localization keys. Owning the parser lets the
 * Android port keep those behaviours and error paths identical without pulling in a serializer.
 */
sealed class JsonValue {
    abstract val typeLabel: String
}

class JsonObject(val fields: MutableMap<String, JsonValue> = LinkedHashMap()) : JsonValue() {
    override val typeLabel: String get() = "object"
    operator fun get(key: String): JsonValue? = fields[key]
    operator fun set(key: String, value: JsonValue) {
        fields[key] = value
    }

    fun put(key: String, value: String) = apply { fields[key] = JsonString(value) }
    fun put(key: String, value: Long) = apply { fields[key] = JsonNumber(value.toString()) }
    fun put(key: String, value: Int) = apply { fields[key] = JsonNumber(value.toString()) }
    fun put(key: String, value: Double) = apply { fields[key] = JsonNumber(formatDouble(value)) }
    fun put(key: String, value: Boolean) = apply { fields[key] = JsonBoolean(value) }
    fun put(key: String, value: JsonValue?) = apply { fields[key] = value ?: JsonNull }
    fun put(key: String, value: List<JsonValue>) = apply { fields[key] = JsonArray(value.toMutableList()) }
    fun optional(key: String, value: String?) = apply { if (value != null) fields[key] = JsonString(value) }
    fun optionalNumber(key: String, value: Double?) = apply { if (value != null) fields[key] = JsonNumber(formatDouble(value)) }
}

class JsonArray(val items: MutableList<JsonValue> = mutableListOf()) : JsonValue() {
    override val typeLabel: String get() = "array"
}

data class JsonString(val value: String) : JsonValue() {
    override val typeLabel: String get() = "string"
}

data class JsonNumber(val raw: String) : JsonValue() {
    override val typeLabel: String get() = "number"
    val double: Double get() = raw.toDouble()
    fun longValue(): Long? {
        if (raw.any { it == '.' || it == 'e' || it == 'E' }) {
            // A decimal or exponent form is only an integer when it is exactly integral.
            val decimal = raw.toBigDecimalOrNull() ?: return null
            return try { decimal.toBigIntegerExact().longValueExact() } catch (_: Exception) { null }
        }
        return raw.toLongOrNull()
    }
}

data class JsonBoolean(val value: Boolean) : JsonValue() {
    override val typeLabel: String get() = "boolean"
}

object JsonNull : JsonValue() {
    override val typeLabel: String get() = "null"
}

fun formatDouble(value: Double): String {
    if (!value.isFinite()) return "0"
    if (value == value.toLong().toDouble()) return value.toLong().toString()
    return value.toString()
}

class JsonParseException(message: String) : Exception(message)

object JsonParser {
    fun parse(text: String): JsonValue = Parser(text).parseDocument()

    private class Parser(private val text: String) {
        private var index = if (text.startsWith('\uFEFF')) 1 else 0
        private var depth = 0

        fun parseDocument(): JsonValue {
            skipWhitespace()
            val value = parseValue()
            skipWhitespace()
            if (index != text.length) fail("Unexpected character after the top-level value.")
            return value
        }

        private fun fail(message: String): Nothing {
            val upTo = text.substring(0, index.coerceAtMost(text.length))
            val line = upTo.count { it == '\n' } + 1
            val column = index - (upTo.lastIndexOf('\n') + 1) + 1
            throw JsonParseException("$message (line $line, column $column)")
        }

        private fun skipWhitespace() {
            while (index < text.length && text[index].let { it == ' ' || it == '\t' || it == '\n' || it == '\r' }) index++
        }

        private fun parseValue(): JsonValue {
            if (index >= text.length) fail("Unexpected end of input.")
            if (depth >= 200) fail("Nesting is too deep.")
            depth++
            try {
                return parseValueAt()
            } finally {
                depth--
            }
        }

        private fun parseValueAt(): JsonValue {
            return when (val c = text[index]) {
                '{' -> parseObject()
                '[' -> parseArray()
                '"' -> JsonString(parseString())
                't' -> { expectWord("true"); JsonBoolean(true) }
                'f' -> { expectWord("false"); JsonBoolean(false) }
                'n' -> { expectWord("null"); JsonNull }
                else -> if (c == '-' || c in '0'..'9') parseNumber() else fail("Unexpected character '$c'.")
            }
        }

        private fun expectWord(word: String) {
            if (!text.startsWith(word, index)) fail("Expected '$word'.")
            index += word.length
        }

        private fun parseObject(): JsonObject {
            index++ // {
            val result = JsonObject()
            skipWhitespace()
            if (index < text.length && text[index] == '}') { index++; return result }
            while (true) {
                skipWhitespace()
                if (index >= text.length || text[index] != '"') fail("Expected a quoted object key.")
                val key = parseString()
                skipWhitespace()
                if (index >= text.length || text[index] != ':') fail("Expected ':' after the object key.")
                index++
                skipWhitespace()
                result.fields[key] = parseValue()
                skipWhitespace()
                if (index >= text.length) fail("Unterminated object.")
                when (text[index]) {
                    ',' -> index++
                    '}' -> { index++; return result }
                    else -> fail("Expected ',' or '}' in the object.")
                }
            }
        }

        private fun parseArray(): JsonArray {
            index++ // [
            val result = JsonArray()
            skipWhitespace()
            if (index < text.length && text[index] == ']') { index++; return result }
            while (true) {
                skipWhitespace()
                result.items.add(parseValue())
                skipWhitespace()
                if (index >= text.length) fail("Unterminated array.")
                when (text[index]) {
                    ',' -> index++
                    ']' -> { index++; return result }
                    else -> fail("Expected ',' or ']' in the array.")
                }
            }
        }

        private fun parseString(): String {
            index++ // opening quote
            val builder = StringBuilder()
            while (true) {
                if (index >= text.length) fail("Unterminated string.")
                val c = text[index++]
                when (c) {
                    '"' -> return builder.toString()
                    '\\' -> {
                        if (index >= text.length) fail("Unterminated escape sequence.")
                        when (val escape = text[index++]) {
                            '"' -> builder.append('"')
                            '\\' -> builder.append('\\')
                            '/' -> builder.append('/')
                            'b' -> builder.append('\b')
                            'f' -> builder.append('\u000C')
                            'n' -> builder.append('\n')
                            'r' -> builder.append('\r')
                            't' -> builder.append('\t')
                            'u' -> {
                                if (index + 4 > text.length) fail("Incomplete unicode escape.")
                                val hex = text.substring(index, index + 4)
                                if (!hex.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                                    fail("Invalid unicode escape.")
                                }
                                val code = hex.toInt(16)
                                index += 4
                                if (code in 0xD800..0xDBFF) {
                                    // A high surrogate must be followed by a low surrogate.
                                    if (index + 6 > text.length || text[index] != '\\' || text[index + 1] != 'u') {
                                        fail("Unpaired surrogate in unicode escape.")
                                    }
                                    val lowHex = text.substring(index + 2, index + 6)
                                    if (!lowHex.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }) {
                                        fail("Invalid unicode escape.")
                                    }
                                    val low = lowHex.toInt(16)
                                    if (low !in 0xDC00..0xDFFF) fail("Unpaired surrogate in unicode escape.")
                                    builder.append(code.toChar()).append(low.toChar())
                                    index += 6
                                } else if (code in 0xDC00..0xDFFF) {
                                    fail("Unpaired surrogate in unicode escape.")
                                } else {
                                    builder.append(code.toChar())
                                }
                            }
                            else -> fail("Invalid escape sequence '\\$escape'.")
                        }
                    }
                    else -> {
                        if (c < ' ') fail("Control character in string.")
                        builder.append(c)
                    }
                }
            }
        }

        private fun parseNumber(): JsonNumber {
            val start = index
            if (text[index] == '-') index++
            if (index >= text.length) fail("Incomplete number.")
            if (text[index] == '0') index++
            else if (text[index] in '1'..'9') { while (index < text.length && text[index].isDigit()) index++ }
            else fail("Invalid number.")
            if (index < text.length && text[index] == '.') {
                index++
                if (index >= text.length || !text[index].isDigit()) fail("Invalid fraction in number.")
                while (index < text.length && text[index].isDigit()) index++
            }
            if (index < text.length && (text[index] == 'e' || text[index] == 'E')) {
                index++
                if (index < text.length && (text[index] == '+' || text[index] == '-')) index++
                if (index >= text.length || !text[index].isDigit()) fail("Invalid exponent in number.")
                while (index < text.length && text[index].isDigit()) index++
            }
            return JsonNumber(text.substring(start, index))
        }
    }
}

/** Serializes with UTF-16-ordered keys, matching JSONEncoder's sortedKeys option. */
fun JsonValue.serialize(pretty: Boolean = false): String {
    val builder = StringBuilder()
    writeJson(builder, this, pretty, 0)
    return builder.toString()
}

private fun writeJson(builder: StringBuilder, value: JsonValue, pretty: Boolean, depth: Int) {
    when (value) {
        is JsonObject -> {
            val keys = value.fields.keys.sorted()
            if (keys.isEmpty()) { builder.append("{}"); return }
            builder.append('{')
            keys.forEachIndexed { position, key ->
                if (position > 0) builder.append(',')
                if (pretty) { builder.append('\n'); indent(builder, depth + 1) }
                appendJsonString(builder, key)
                builder.append(if (pretty) " : " else ":")
                writeJson(builder, value.fields.getValue(key), pretty, depth + 1)
            }
            if (pretty) { builder.append('\n'); indent(builder, depth) }
            builder.append('}')
        }
        is JsonArray -> {
            if (value.items.isEmpty()) { builder.append("[]"); return }
            builder.append('[')
            value.items.forEachIndexed { position, item ->
                if (position > 0) builder.append(',')
                if (pretty) { builder.append('\n'); indent(builder, depth + 1) }
                writeJson(builder, item, pretty, depth + 1)
            }
            if (pretty) { builder.append('\n'); indent(builder, depth) }
            builder.append(']')
        }
        is JsonString -> appendJsonString(builder, value.value)
        is JsonNumber -> builder.append(value.raw)
        is JsonBoolean -> builder.append(if (value.value) "true" else "false")
        JsonNull -> builder.append("null")
    }
}

private fun indent(builder: StringBuilder, depth: Int) {
    repeat(depth) { builder.append("  ") }
}

private fun appendJsonString(builder: StringBuilder, value: String) {
    builder.append('"')
    for (c in value) {
        when (c) {
            '"' -> builder.append("\\\"")
            '\\' -> builder.append("\\\\")
            '\n' -> builder.append("\\n")
            '\r' -> builder.append("\\r")
            '\t' -> builder.append("\\t")
            '\b' -> builder.append("\\b")
            '\u000C' -> builder.append("\\f")
            else -> if (c < ' ') builder.append("\\u").append(String.format(java.util.Locale.ROOT, "%04x", c.code)) else builder.append(c)
        }
    }
    builder.append('"')
}

/** Decoding failures use the same categories as the iOS JSONDecoder mapping. */
class JsonIssue(val category: String, val detail: String, val path: String) : Exception("$path: $detail")

private fun typeIssue(expected: String, found: JsonValue?, path: String): Nothing {
    if (found == null || found is JsonNull) {
        throw JsonIssue("valueNotFound", "Expected $expected but found null instead.", path)
    }
    throw JsonIssue("fieldType", "Expected to decode $expected but found ${found.typeLabel} instead.", path)
}

fun JsonValue?.objectOrThrow(path: String): JsonObject =
    this as? JsonObject ?: typeIssue("a dictionary", this, path)

fun JsonValue?.arrayOrThrow(path: String): JsonArray =
    this as? JsonArray ?: typeIssue("an array", this, path)

fun JsonValue?.stringOrThrow(path: String): String =
    (this as? JsonString)?.value ?: typeIssue("a string", this, path)

fun JsonValue?.boolOrThrow(path: String): Boolean =
    (this as? JsonBoolean)?.value ?: typeIssue("a boolean", this, path)

fun JsonValue?.numberOrThrow(path: String): Double =
    (this as? JsonNumber)?.double ?: typeIssue("a number", this, path)

fun JsonValue?.longOrThrow(path: String): Long {
    val number = this as? JsonNumber ?: typeIssue("an integer", this, path)
    return number.longValue() ?: typeIssue("an integer", this, path)
}

fun JsonObject.fieldOrNull(key: String): JsonValue? = fields[key]

fun JsonObject.requireField(key: String, path: String): JsonValue {
    val value = fields[key]
    if (value == null) {
        throw JsonIssue("missingField", "Required field '$key' is missing.", "$path.$key")
    }
    return value
}

fun JsonObject.requireString(key: String, path: String): String = requireField(key, path).stringOrThrow("$path.$key")

fun JsonObject.optionalString(key: String, path: String): String? {
    val value = fields[key] ?: return null
    if (value is JsonNull) return null
    return value.stringOrThrow("$path.$key")
}

fun JsonObject.optionalBool(key: String, path: String): Boolean? {
    val value = fields[key] ?: return null
    if (value is JsonNull) return null
    return value.boolOrThrow("$path.$key")
}

fun JsonObject.requireBool(key: String, path: String): Boolean = requireField(key, path).boolOrThrow("$path.$key")

fun JsonObject.requireLong(key: String, path: String): Long = requireField(key, path).longOrThrow("$path.$key")

fun JsonObject.optionalLong(key: String, path: String): Long? {
    val value = fields[key] ?: return null
    if (value is JsonNull) return null
    return value.longOrThrow("$path.$key")
}

fun JsonObject.requireInt(key: String, path: String): Int {
    val value = requireLong(key, path)
    if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) {
        throw JsonIssue("fieldType", "The integer is out of range.", "$path.$key")
    }
    return value.toInt()
}

fun JsonObject.optionalDouble(key: String, path: String): Double? {
    val value = fields[key] ?: return null
    if (value is JsonNull) return null
    return value.numberOrThrow("$path.$key")
}

fun JsonObject.requireObject(key: String, path: String): JsonObject = requireField(key, path).objectOrThrow("$path.$key")

fun JsonObject.optionalObject(key: String, path: String): JsonObject? {
    val value = fields[key] ?: return null
    if (value is JsonNull) return null
    return value.objectOrThrow("$path.$key")
}

fun JsonObject.requireArray(key: String, path: String): JsonArray = requireField(key, path).arrayOrThrow("$path.$key")

fun JsonObject.optionalArray(key: String, path: String): JsonArray? {
    val value = fields[key] ?: return null
    if (value is JsonNull) return null
    return value.arrayOrThrow("$path.$key")
}
