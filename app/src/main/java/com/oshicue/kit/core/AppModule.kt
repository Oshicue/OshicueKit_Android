package com.oshicue.kit.core

/** The five top-level modules, with the persisted values used by earlier releases. */
enum class AppModule(val rawValue: Int) {
    Schedule(0),
    Ledger(1),
    Diary(2),
    Settings(3),
    Agenda(4);

    companion object {
        const val storageKey = "selectedModule"

        fun restored(rawValue: Int?): AppModule =
            entries.firstOrNull { it.rawValue == rawValue } ?: Agenda
    }
}

enum class ImportErrorKey(val key: String) {
    InvalidJson("import.error.invalidJSON"),
    Timestamp("import.error.timestamp"),
    MissingField("import.error.missingField"),
    FieldType("import.error.fieldType"),
    SchemaVersion("import.error.schemaVersion"),
    TimeZone("import.error.timeZone"),
    Color("import.error.color"),
    ArtistReference("import.error.artistReference"),
    VenueReference("import.error.venueReference"),
    Duration("import.error.duration"),
    EmptyField("import.error.emptyField"),
    DuplicateId("import.error.duplicateID")
}

/** Mirrors ImportError: a localization key plus the exact faulty input location. */
class ImportError(
    val key: String,
    val detail: String,
    val path: String
) : Exception("$path: $detail")

fun importIssue(category: ImportErrorKey, detail: String, path: String): Nothing =
    throw ImportError(category.key, detail, path)
