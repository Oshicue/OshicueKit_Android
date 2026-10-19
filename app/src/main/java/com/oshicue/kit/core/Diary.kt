package com.oshicue.kit.core

import java.time.Instant
import java.util.Locale

enum class DiaryStatus(val rawValue: String) {
    Draft("draft"),
    Ready("ready");

    companion object {
        fun from(value: String): DiaryStatus? = entries.firstOrNull { it.rawValue == value }
    }
}

/** Activity details are snapshots: ledger changes never rewrite a saved report. */
data class DiaryEventContext(
    val ledgerEventId: String? = null,
    val name: String = "",
    val date: String = LedgerDates.today(),
    val city: String = "",
    val venue: String = "",
    val address: String = ""
)

data class DiaryEntry(
    val id: String = newIdentifier(),
    val title: String = "",
    val body: String = "",
    val summary: String = "",
    val tags: List<String> = emptyList(),
    val status: DiaryStatus = DiaryStatus.Draft,
    val event: DiaryEventContext = DiaryEventContext(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val isMeaningfulContent: Boolean
        get() = (listOf(title, body, summary, event.name, event.city, event.venue, event.address) + tags)
            .any { it.isNotBlank() }

    /**
     * Backup timestamps have millisecond precision. Comparing that precision lets a backup
     * exported from an in-memory instant be reimported without a false conflict.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DiaryEntry) return false
        return id == other.id && title == other.title && body == other.body && summary == other.summary &&
            tags == other.tags && status == other.status && event == other.event &&
            DiaryImporter.roundedMilliseconds(createdAt) == DiaryImporter.roundedMilliseconds(other.createdAt) &&
            DiaryImporter.roundedMilliseconds(updatedAt) == DiaryImporter.roundedMilliseconds(other.updatedAt)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + summary.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + event.hashCode()
        result = 31 * result + DiaryImporter.roundedMilliseconds(createdAt).hashCode()
        result = 31 * result + DiaryImporter.roundedMilliseconds(updatedAt).hashCode()
        return result
    }
}

fun newIdentifier(): String = java.util.UUID.randomUUID().toString().lowercase(Locale.ROOT)

data class DiaryDocument(
    val schemaVersion: Int = 1,
    val entries: List<DiaryEntry> = emptyList()
) {
    /** A conflicting ID fails the entire import. No draft is silently overwritten. */
    fun merging(incoming: DiaryDocument): DiaryDocument {
        DiaryImporter.validate(this)
        DiaryImporter.validate(incoming)
        val existing = entries.associateBy { it.id }
        for (entry in incoming.entries) {
            val original = existing[entry.id]
            if (original != null && original != entry) {
                throw DiaryError(
                    "diary.error.entryConflict",
                    "Entry '${entry.id}' already exists with different contents. Edit the existing entry, or import it using a new ID.",
                    "$.entries[id=${entry.id}]"
                )
            }
        }
        val merged = DiaryDocument(entries = entries + incoming.entries.filter { it.id !in existing })
        DiaryImporter.validate(merged)
        return merged
    }
}

class DiaryError(val key: String, val detail: String, val path: String) : Exception("$path: $detail")

object DiaryCore {
    fun contextFrom(event: LedgerEvent, defaultDate: String = LedgerDates.today()): DiaryEventContext =
        DiaryEventContext(
            ledgerEventId = event.id,
            name = event.name,
            date = event.date?.takeIf { LedgerDates.parse(it) != null } ?: defaultDate,
            city = event.city ?: "",
            venue = event.venue ?: "",
            address = event.address ?: ""
        )

    fun normalizeTags(tags: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<String>()
        for (value in tags) {
            val tag = value.trim()
                .trim('#', '＃')
                .trim()
                .let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFC) }
            // Voiced kana and accents identify different tags; only case and canonical
            // equivalence fold together for deduplication.
            val identity = tag.lowercase(java.util.Locale.ROOT)
            if (tag.isEmpty() || !seen.add(identity)) continue
            result.add(tag)
        }
        return result
    }

    fun parseTags(text: String): List<String> =
        normalizeTags(text.split(',', '，', '、', ';', '；', '\n'))

    /** Every query word must occur somewhere in the report or its activity metadata. */
    fun search(entries: List<DiaryEntry>, query: String): List<DiaryEntry> {
        val words = query.split(Regex("[\\s\\p{Z}\\u0085]+")).filter { it.isNotEmpty() }
        if (words.isEmpty()) return entries
        return entries.filter { entry ->
            val searchable = (
                listOf(
                    entry.title, entry.body, entry.summary, entry.event.name, entry.event.date,
                    entry.event.city, entry.event.venue, entry.event.address
                ) + entry.tags
                ).joinToString("\n")
            words.all { searchContains(searchable, it) }
        }
    }

    /** Portable text includes context and summary as well as the unmodified report body. */
    fun plainText(entry: DiaryEntry): String {
        val sections = mutableListOf<String>()
        if (entry.title.isNotEmpty()) sections.add(entry.title)
        val metadata = listOf(entry.event.name, entry.event.date, entry.event.city, entry.event.venue, entry.event.address)
            .filter { it.isNotEmpty() }
        if (metadata.isNotEmpty()) sections.add(metadata.joinToString("\n"))
        if (entry.tags.isNotEmpty()) sections.add(entry.tags.joinToString(" ") { "#$it" })
        if (entry.summary.isNotEmpty()) sections.add(entry.summary)
        if (entry.body.isNotEmpty()) sections.add(entry.body)
        return sections.joinToString("\n\n") + "\n"
    }

    /** The body remains verbatim so existing Markdown and line breaks survive export. */
    fun markdown(entry: DiaryEntry): String {
        val sections = mutableListOf<String>()
        if (entry.title.isNotEmpty()) sections.add("# " + escapeInline(entry.title))
        val metadata = listOf(entry.event.name, entry.event.date, entry.event.city, entry.event.venue, entry.event.address)
            .filter { it.isNotEmpty() }
        if (metadata.isNotEmpty()) sections.add(metadata.joinToString("\n") { "- " + escapeInline(it) })
        if (entry.tags.isNotEmpty()) sections.add(entry.tags.joinToString(" ") { "#" + escapeInline(it) })
        if (entry.summary.isNotEmpty()) sections.add(entry.summary)
        if (entry.body.isNotEmpty()) sections.add(entry.body)
        return sections.joinToString("\n\n") + "\n"
    }

    private fun escapeInline(value: String): String {
        var escaped = value.replace("\\", "\\\\")
        for (character in listOf("`", "*", "_", "{", "}", "[", "]", "<", ">", "#")) {
            escaped = escaped.replace(character, "\\$character")
        }
        return escaped.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
    }
}

/** Filters apply to the activity's civil date, matching the diary's ordinary grouping. */
data class DiaryEntryFilter(
    val startDate: String? = null,
    val endDate: String? = null,
    val status: DiaryStatus? = null,
    val query: String = ""
) {
    val hasValidDateRange: Boolean
        get() {
            if (startDate != null && LedgerDates.parse(startDate) == null) return false
            if (endDate != null && LedgerDates.parse(endDate) == null) return false
            if (startDate != null && endDate != null && startDate > endDate) return false
            return true
        }

    fun matching(entries: List<DiaryEntry>): List<DiaryEntry> {
        if (!hasValidDateRange) return emptyList()
        return DiaryCore.search(entries, query).filter { entry ->
            (startDate == null || entry.event.date >= startDate) &&
                (endDate == null || entry.event.date <= endDate) &&
                (status == null || entry.status == status)
        }
    }
}

fun DiaryDocument.deletingEntries(ids: Set<String>): DiaryDocument =
    copy(entries = entries.filter { it.id !in ids })

object DiaryImporter {
    const val maximumDocumentBytes = 20 * 1024 * 1024

    /** Never repairs malformed UTF-8 silently: doing so could rewrite a report's body. */
    fun parse(data: ByteArray): DiaryDocument {
        if (data.size > maximumDocumentBytes) {
            throw error("documentSize", "The diary must not exceed 20 MB.", "$")
        }
        val text = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                .decode(java.nio.ByteBuffer.wrap(data))
                .toString()
        } catch (_: java.nio.charset.CharacterCodingException) {
            throw error("invalidJSON", "The diary must contain valid UTF-8 text.", "$")
        }
        return parse(text)
    }

    fun parse(text: String): DiaryDocument {
        if (text.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("documentSize", "The diary must not exceed 20 MB.", "$")
        }
        val root = try {
            JsonParser.parse(text).objectOrThrow("$")
        } catch (failure: JsonParseException) {
            throw error("invalidJSON", failure.message ?: "The diary could not be decoded.", "$")
        } catch (failure: JsonIssue) {
            throw errorForIssue(failure)
        }
        val document = try {
            DiaryCodec.fromJson(root)
        } catch (failure: JsonIssue) {
            throw errorForIssue(failure)
        } catch (failure: DiaryError) {
            throw failure
        }
        validate(document)
        return document
    }

    fun encode(document: DiaryDocument): String {
        validate(document)
        val text = DiaryCodec.toJson(document).serialize(pretty = true)
        if (text.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("documentSize", "The diary must not exceed 20 MB.", "$")
        }
        return text
    }

    private fun errorForIssue(issue: JsonIssue): DiaryError = when (issue.category) {
        "missingField" -> error("missingField", issue.detail, issue.path)
        "fieldType", "valueNotFound" -> error("fieldType", issue.detail, issue.path)
        else -> error("invalidJSON", issue.detail, issue.path)
    }

    fun validate(document: DiaryDocument) {
        if (document.schemaVersion != 1) throw error("schemaVersion", "Supported schemaVersion is 1.", "$.schemaVersion")
        val seen = mutableSetOf<String>()
        document.entries.forEachIndexed { index, entry ->
            val location = "$.entries[$index]"
            nonemptyId(entry.id, "$location.id")
            if (!seen.add(entry.id)) {
                throw error("duplicateID", "Duplicate entry ID '${entry.id}'.", "$location.id")
            }
            entry.event.ledgerEventId?.let { nonemptyId(it, "$location.event.ledgerEventID") }
            if (LedgerDates.parse(entry.event.date) == null) {
                throw error("date", "Use a valid Gregorian date in yyyy-MM-dd format.", "$location.event.date")
            }
            for ((field, date) in listOf("createdAt" to entry.createdAt, "updatedAt" to entry.updatedAt)) {
                val representable = try {
                    KitDate.parseTimestamp(KitDate.timestamp(date)) != null
                } catch (_: ArithmeticException) {
                    false
                }
                if (!representable) {
                    throw error("timestamp", "Use a valid timestamp between years 0001 and 9999.", "$location.$field")
                }
            }
            if (entry.updatedAt.isBefore(entry.createdAt)) {
                throw error("timestamp", "updatedAt must not precede createdAt.", "$location.updatedAt")
            }
        }
        val encoded = DiaryCodec.toJson(document).serialize(pretty = true)
        if (encoded.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("documentSize", "The diary must not exceed 20 MB.", "$")
        }
    }

    /** Rounds to milliseconds instead of truncating, matching entry comparison. */
    fun encodeTimestamp(instant: Instant): String =
        KitDate.timestamp(Instant.ofEpochMilli(roundedMilliseconds(instant)))

    fun roundedMilliseconds(instant: Instant): Long =
        instant.toEpochMilli() + if (instant.nano % 1_000_000 >= 500_000) 1 else 0

    private fun error(category: String, detail: String, path: String): DiaryError =
        DiaryError("diary.error.$category", detail, path)

    private fun nonemptyId(value: String, path: String) {
        if (value.isBlank()) throw error("emptyID", "The ID must not be empty.", path)
    }
}

object DiaryCodec {
    fun toJson(document: DiaryDocument): JsonObject = JsonObject().apply {
        put("schemaVersion", document.schemaVersion)
        put("entries", JsonArray(document.entries.map { entry ->
            JsonObject().apply {
                put("id", entry.id)
                put("title", entry.title)
                put("body", entry.body)
                put("summary", entry.summary)
                put("tags", JsonArray(entry.tags.map { JsonString(it) as JsonValue }.toMutableList()))
                put("status", entry.status.rawValue)
                put("event", JsonObject().apply {
                    entry.event.ledgerEventId?.let { put("ledgerEventID", it) }
                    put("name", entry.event.name)
                    put("date", entry.event.date)
                    put("city", entry.event.city)
                    put("venue", entry.event.venue)
                    put("address", entry.event.address)
                })
                put("createdAt", DiaryImporter.encodeTimestamp(entry.createdAt))
                put("updatedAt", DiaryImporter.encodeTimestamp(entry.updatedAt))
            }
        }.toMutableList()))
    }

    fun fromJson(root: JsonObject): DiaryDocument = DiaryDocument(
        schemaVersion = root.requireInt("schemaVersion", "$"),
        entries = root.requireArray("entries", "$").items.mapIndexed { index, value ->
            val json = value.objectOrThrow("$.entries[$index]")
            val path = "$.entries[$index]"
            val statusText = json.requireString("status", path)
            val status = DiaryStatus.from(statusText)
                ?: throw DiaryError("diary.error.invalidJSON", "Unknown diary status.", "$path.status")
            val eventJson = json.requireObject("event", path)
            val eventPath = "$path.event"
            DiaryEntry(
                id = json.requireString("id", path),
                title = json.requireString("title", path),
                body = json.requireString("body", path),
                summary = json.requireString("summary", path),
                tags = json.requireArray("tags", path).items.map { it.stringOrThrow("$path.tags") },
                status = status,
                event = DiaryEventContext(
                    ledgerEventId = eventJson.optionalString("ledgerEventID", eventPath),
                    name = eventJson.requireString("name", eventPath),
                    date = eventJson.requireString("date", eventPath),
                    city = eventJson.requireString("city", eventPath),
                    venue = eventJson.requireString("venue", eventPath),
                    address = eventJson.requireString("address", eventPath)
                ),
                createdAt = timestampOrThrow(json, "createdAt", path),
                updatedAt = timestampOrThrow(json, "updatedAt", path)
            )
        }
    )

    private fun timestampOrThrow(json: JsonObject, key: String, path: String): Instant {
        val value = json.requireString(key, path)
        return KitDate.parseTimestamp(value)
            ?: throw DiaryError(
                "diary.error.timestamp",
                "Use a valid ISO 8601 timestamp with Z or an explicit UTC offset.",
                "$path.$key"
            )
    }
}
