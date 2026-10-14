package com.oshicue.kit.core

import kotlinx.serialization.json.JsonElement
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.Instant

data class DiaryError(
    override val key: String,
    override val detail: String,
    override val path: String
) : Exception("$path: $detail"), KitIssue

object DiaryImporter {
    const val maximumDocumentBytes = 20 * 1024 * 1024

    /** Never repair malformed UTF-8 silently: doing so could rewrite a report's body. */
    fun parse(data: ByteArray): DiaryDocument {
        if (data.size > maximumDocumentBytes) throw error("documentSize", "The diary must not exceed 20 MB.", "$")
        val decoder = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
        val text = try {
            decoder.decode(ByteBuffer.wrap(data)).toString()
        } catch (_: Exception) {
            throw error("invalidJSON", "The diary must contain valid UTF-8 text.", "$")
        }
        return parse(text)
    }

    fun parse(text: String): DiaryDocument {
        if (text.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("documentSize", "The diary must not exceed 20 MB.", "$")
        }
        val root = KitJson.parse(text) ?: throw error("invalidJSON", "The text is not valid JSON.", "$")
        val document = decode(root)
        validate(document)
        return document
    }

    fun encode(document: DiaryDocument): String {
        validate(document)
        val text = JsonText.encode(encodeElement(document))
        if (text.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("documentSize", "The diary must not exceed 20 MB.", "$")
        }
        return text
    }

    fun validate(document: DiaryDocument) {
        if (document.schemaVersion != 1) throw error("schemaVersion", "Supported schemaVersion is 1.", "$.schemaVersion")
        val seen = mutableSetOf<String>()
        document.entries.forEachIndexed { index, entry ->
            val location = "$.entries[$index]"
            nonemptyID(entry.id, "$location.id")
            if (!seen.add(entry.id)) throw error("duplicateID", "Duplicate entry ID '${entry.id}'.", "$location.id")
            entry.event.ledgerEventID?.let { nonemptyID(it, "$location.event.ledgerEventID") }
            if (LedgerDates.parse(entry.event.date) == null) {
                throw error("date", "Use a valid Gregorian date in yyyy-MM-dd format.", "$location.event.date")
            }
            for ((field, date) in listOf("createdAt" to entry.createdAt, "updatedAt" to entry.updatedAt)) {
                if (ScheduleDates.parseTimestamp(ScheduleDates.timestamp(date)) == null) {
                    throw error("timestamp", "Use a valid timestamp between years 0001 and 9999.", "$location.$field")
                }
            }
            if (entry.updatedAt < entry.createdAt) {
                throw error("timestamp", "updatedAt must not precede createdAt.", "$location.updatedAt")
            }
        }
        // Also bounds manual edits, which do not pass through the JSON parser.
        val encoded = JsonText.encode(encodeElement(document))
        if (encoded.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("documentSize", "The diary must not exceed 20 MB.", "$")
        }
    }

    private fun error(category: String, detail: String, path: String) = DiaryError("diary.error.$category", detail, path)

    private fun nonemptyID(value: String, path: String) {
        if (value.trim().isEmpty()) throw error("emptyID", "The ID must not be empty.", path)
    }

    private fun rounded(date: Instant): Instant = Instant.ofEpochMilli(date.millisRounded())

    internal fun encodeElement(document: DiaryDocument): kotlinx.serialization.json.JsonObject {
        val root = JObj()
        root.putArray("entries", document.entries.map { entry ->
            JObj().apply {
                put("body", entry.body)
                put("createdAt", ScheduleDates.timestamp(rounded(entry.createdAt)))
                putElement("event", JObj().apply {
                    put("address", entry.event.address)
                    put("city", entry.event.city)
                    put("date", entry.event.date)
                    entry.event.ledgerEventID?.let { put("ledgerEventID", it) }
                    put("name", entry.event.name)
                    put("venue", entry.event.venue)
                }.build())
                put("id", entry.id)
                put("status", entry.status.raw)
                put("summary", entry.summary)
                putStringArray("tags", entry.tags)
                put("title", entry.title)
                put("updatedAt", ScheduleDates.timestamp(rounded(entry.updatedAt)))
            }.build()
        })
        root.put("schemaVersion", document.schemaVersion)
        return root.build()
    }

    private fun decode(root: JsonElement): DiaryDocument {
        val reader = JsonReader("diary.error") { key, detail, path -> DiaryError(key, detail, path) }
        val top = reader.obj(root, "$")
        val schemaVersion = reader.int(top, "schemaVersion", "$")
        val entries = reader.array(top, "entries", "$").mapIndexed { index, element ->
            val path = "$.entries[$index]"
            val obj = reader.obj(element, path)
            val eventPath = "$path.event"
            val event = reader.obj(obj["event"] ?: throw DiaryError("diary.error.missingField", "Required field 'event' is missing.", eventPath), eventPath)
            val statusText = reader.string(obj, "status", path)
            val status = DiaryStatus.fromRaw(statusText)
                ?: throw DiaryError("diary.error.invalidJSON", "Use 'draft' or 'ready' for status.", "$path.status")
            DiaryEntry(
                id = reader.string(obj, "id", path),
                title = reader.string(obj, "title", path),
                body = reader.string(obj, "body", path),
                summary = reader.string(obj, "summary", path),
                tags = reader.array(obj, "tags", path).mapIndexed { tagIndex, tag ->
                    (tag as? kotlinx.serialization.json.JsonPrimitive)?.takeIf { it.isString }?.content
                        ?: throw DiaryError("diary.error.fieldType", "Expected a string tag.", "$path.tags[$tagIndex]")
                },
                status = status,
                event = DiaryEventContext(
                    ledgerEventID = reader.optionalString(event, "ledgerEventID", eventPath),
                    name = reader.string(event, "name", eventPath),
                    date = reader.string(event, "date", eventPath),
                    city = reader.string(event, "city", eventPath),
                    venue = reader.string(event, "venue", eventPath),
                    address = reader.string(event, "address", eventPath)
                ),
                createdAt = reader.timestamp(obj, "createdAt", path),
                updatedAt = reader.timestamp(obj, "updatedAt", path)
            )
        }
        return DiaryDocument(schemaVersion = schemaVersion, entries = entries)
    }
}
