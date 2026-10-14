package com.oshicue.kit.core

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class DiaryStatus(val raw: String) {
    draft("draft"),
    ready("ready");

    companion object {
        fun fromRaw(value: String): DiaryStatus? = entries.firstOrNull { it.raw == value }
    }
}

/** Activity details are snapshots: ledger changes never rewrite a saved report. */
data class DiaryEventContext(
    val ledgerEventID: String? = null,
    val name: String = "",
    val date: String = LedgerDates.string(LocalDate.now()),
    val city: String = "",
    val venue: String = "",
    val address: String = ""
)

class DiaryEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val body: String = "",
    val summary: String = "",
    val tags: List<String> = emptyList(),
    val status: DiaryStatus = DiaryStatus.draft,
    val event: DiaryEventContext = DiaryEventContext(),
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    val isMeaningfulContent: Boolean
        get() = (listOf(title, body, summary, event.name, event.city, event.venue, event.address) + tags)
            .any { it.trim().isNotEmpty() }

    fun copy(
        id: String = this.id,
        title: String = this.title,
        body: String = this.body,
        summary: String = this.summary,
        tags: List<String> = this.tags,
        status: DiaryStatus = this.status,
        event: DiaryEventContext = this.event,
        createdAt: Instant = this.createdAt,
        updatedAt: Instant = this.updatedAt
    ) = DiaryEntry(id, title, body, summary, tags, status, event, createdAt, updatedAt)

    /** Backup timestamps have millisecond precision; comparing that precision avoids false conflicts. */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DiaryEntry) return false
        return id == other.id && title == other.title && body == other.body && summary == other.summary &&
            tags == other.tags && status == other.status && event == other.event &&
            createdAt.millisRounded() == other.createdAt.millisRounded() &&
            updatedAt.millisRounded() == other.updatedAt.millisRounded()
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + body.hashCode()
        result = 31 * result + summary.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + status.hashCode()
        result = 31 * result + event.hashCode()
        result = 31 * result + createdAt.millisRounded().hashCode()
        result = 31 * result + updatedAt.millisRounded().hashCode()
        return result
    }

    override fun toString(): String = "DiaryEntry(id=$id, title=$title, status=$status)"
}

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
        val merged = DiaryDocument(entries = entries + incoming.entries.filter { existing[it.id] == null })
        DiaryImporter.validate(merged)
        return merged
    }
}

object DiaryCore {
    fun context(from: LedgerEvent, defaultDate: String = LedgerDates.string(LocalDate.now())): DiaryEventContext {
        val eventDate = from.date?.takeIf { LedgerDates.parse(it) != null } ?: defaultDate
        return DiaryEventContext(
            ledgerEventID = from.id,
            name = from.name,
            date = eventDate,
            city = from.city ?: "",
            venue = from.venue ?: "",
            address = from.address ?: ""
        )
    }

    fun normalizeTags(tags: List<String>): List<String> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<String>()
        for (value in tags) {
            val tag = java.text.Normalizer.normalize(
                value.trim().trim('#', '＃').trim(),
                java.text.Normalizer.Form.NFC
            )
            // Voiced Japanese kana and accents identify different tags. Only case and
            // canonically equivalent Unicode encodings are equivalent for deduplication.
            val identity = tag.lowercase(java.util.Locale.ROOT)
            if (tag.isEmpty() || !seen.add(identity)) continue
            result.add(tag)
        }
        return result
    }

    fun parseTags(text: String): List<String> = normalizeTags(text.split(Regex("[,\\uFF0C\\u3001;\\uFF1B\\n]")))

    /** Every query word must occur somewhere in the report or its activity metadata. */
    fun search(entries: List<DiaryEntry>, query: String): List<DiaryEntry> {
        val words = KitText.words(query)
        if (words.isEmpty()) return entries
        return entries.filter { entry ->
            val searchable = (listOf(
                entry.title, entry.body, entry.summary, entry.event.name, entry.event.date,
                entry.event.city, entry.event.venue, entry.event.address
            ) + entry.tags).joinToString("\n")
            words.all { KitText.contains(searchable, it) }
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
            escaped = escaped.replace(character, "\\" + character)
        }
        return escaped.replace("\r\n", " ").replace("\n", " ").replace("\r", " ")
    }
}
