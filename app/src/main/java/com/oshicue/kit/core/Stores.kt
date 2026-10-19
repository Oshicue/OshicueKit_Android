package com.oshicue.kit.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * File-backed persistence shared by every module. Writes go to a temporary file first so a
 * failed save never publishes a partial edit, matching the atomic write on iOS. An unreadable
 * file is copied aside before a replacement document can overwrite it.
 */
class DocumentFile(private val directory: File, name: String) {
    val file = File(directory, name)
    private var unreadable = false

    fun exists(): Boolean = file.exists()

    fun size(): Long = if (file.exists()) file.length() else 0L

    fun readText(): String = file.readText(Charsets.UTF_8)

    fun readBytes(): ByteArray = file.readBytes()

    fun write(text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        directory.mkdirs()
        if (unreadable && file.exists()) {
            val recovery = File(directory, "${file.nameWithoutExtension}-recovery-${newIdentifier()}.json")
            file.copyTo(recovery, overwrite = true)
            unreadable = false
        }
        val temporary = File(directory, "${file.name}.tmp")
        temporary.writeBytes(bytes)
        try {
            java.nio.file.Files.move(
                temporary.toPath(),
                file.toPath(),
                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            )
        } catch (_: Exception) {
            // Some file systems cannot move atomically; fall back to a plain replacement.
            file.writeBytes(bytes)
            temporary.delete()
        }
    }

    fun markUnreadable() {
        unreadable = true
    }
}

private fun primaryErrorText(value: Throwable): String = when (value) {
    is ImportError -> L10n.text(value.key) + "\n" + value.path
    is LedgerError -> L10n.text(value.key) + "\n" + value.path
    is DiaryError -> L10n.text(value.key) + "\n" + value.path
    is AgendaError -> L10n.text(value.key) + "\n" + value.path
    else -> value.localizedMessage ?: ""
}

class ScheduleStore(directory: File) {
    var document by mutableStateOf(ScheduleDocument())
        private set
    var favorites by mutableStateOf<Set<String>>(emptySet())
        private set
    var isDemo by mutableStateOf(false)
        private set
    var storageError by mutableStateOf<String?>(null)

    private val storage = DocumentFile(directory, "schedule.json")

    init {
        if (storage.exists()) {
            try {
                val root = JsonParser.parse(storage.readText()).objectOrThrow("$")
                val state = ScheduleSavedState.fromJson(root)
                document = state.document
                favorites = state.favorites
                isDemo = state.isDemo
                if (state.needsFavoriteMigration) save()
            } catch (failure: Exception) {
                document = ScheduleDocument()
                favorites = emptySet()
                isDemo = false
                storage.markUnreadable()
                storageError = L10n.text("settings.readError") + "\n" + primaryErrorText(failure)
            }
        } else {
            val demo = ScheduleDemo.document()
            document = demo
            favorites = demo.favoritesForArtists(
                demo.events.firstOrNull()?.artists?.take(2)?.map { it.id }?.toSet() ?: emptySet()
            )
            isDemo = true
            save()
        }
    }

    fun isFavorite(sessionId: String): Boolean = sessionId in favorites

    fun toggleFavorite(sessionId: String) {
        if (sessionId !in document.sessionFavoriteIds) return
        favorites = if (sessionId in favorites) favorites - sessionId else favorites + sessionId
        save()
    }

    fun importDocument(incoming: ScheduleDocument) {
        document = if (isDemo) incoming else document.merging(incoming)
        favorites = if (isDemo) emptySet() else favorites.intersect(document.sessionFavoriteIds)
        isDemo = false
        save()
    }

    /** Commits the new file before updating the UI so a failed deletion remains retryable. */
    fun deleteEvent(id: String): Boolean {
        val result = document.deletingEvent(id, favorites) ?: return false
        val stillDemo = isDemo && result.document.events.isNotEmpty()
        return try {
            storage.write(ScheduleSavedState(result.document, result.favorites, stillDemo).toJson().serialize(pretty = true))
            document = result.document
            favorites = result.favorites
            isDemo = stillDemo
            storageError = null
            true
        } catch (failure: Exception) {
            storageError = L10n.text("settings.saveError") + "\n" + primaryErrorText(failure)
            false
        }
    }

    /** Demo loading appends only demo IDs so an imported festival is never discarded. */
    fun loadDemo() {
        val demo = ScheduleDemo.document()
        document = document.merging(demo)
        favorites = favorites.intersect(document.sessionFavoriteIds)
        if (document.events.size == demo.events.size) isDemo = true
        save()
    }

    private fun save() {
        try {
            storage.write(ScheduleSavedState(document, favorites, isDemo).toJson().serialize(pretty = true))
            storageError = null
        } catch (failure: Exception) {
            storageError = L10n.text("settings.saveError") + "\n" + primaryErrorText(failure)
        }
    }
}

class LedgerStore(directory: File) {
    var document by mutableStateOf(LedgerDocument(products = LedgerDocument.defaultProducts))
        private set
    var isDemo by mutableStateOf(false)
        private set
    var lastAddedPrices by mutableStateOf<List<LedgerPrice>>(emptyList())
        private set
    var storageError by mutableStateOf<String?>(null)

    private var hasUserEdits = false
    private var unreadableFile = false
    private val storage = DocumentFile(directory, "ledger.json")

    val canReplaceDemo: Boolean get() = isDemo && !hasUserEdits

    init {
        if (storage.exists()) {
            try {
                val root = JsonParser.parse(storage.readText()).objectOrThrow("$")
                val loaded = LedgerCodec.fromJson(root.requireObject("document", "$"))
                LedgerImporter.validate(loaded)
                document = loaded
                isDemo = root.optionalBool("isDemo", "$") ?: false
                hasUserEdits = root.optionalBool("hasUserEdits", "$") ?: false
                lastAddedPrices = root.optionalArray("lastAddedPrices", "$")?.items?.map { priceFromJson(it) }
                    ?: LedgerPriceSuggestions.seededPrices(document.records)
            } catch (failure: Exception) {
                unreadableFile = true
                storage.markUnreadable()
                storageError = L10n.text("settings.readError") + "\n" + primaryErrorText(failure)
            }
        } else {
            document = LedgerDemo.document()
            isDemo = true
            lastAddedPrices = LedgerPriceSuggestions.seededPrices(document.records)
            try {
                persist(document, demo = true, userEdits = false, prices = lastAddedPrices)
            } catch (failure: Exception) {
                storageError = primaryErrorText(failure)
            }
        }
    }

    /** Validates the complete candidate and persists it before updating observers. */
    private fun transaction(update: (LedgerDocument) -> LedgerDocument) {
        transaction(update, remembering = lastAddedPrices)
    }

    private fun transaction(update: (LedgerDocument) -> LedgerDocument, remembering: List<LedgerPrice>) {
        val candidate = update(document)
        val userEdits = hasUserEdits || isDemo
        persist(candidate, demo = isDemo, userEdits = userEdits, prices = remembering)
        document = candidate
        hasUserEdits = userEdits
        lastAddedPrices = remembering
        storageError = null
    }

    fun commit(update: (LedgerDocument) -> LedgerDocument): Boolean = try {
        transaction(update)
        true
    } catch (failure: Exception) {
        storageError = primaryErrorText(failure)
        false
    }

    fun importDocument(incoming: LedgerDocument): Boolean = try {
        val replaceDemo = canReplaceDemo
        val candidate = if (replaceDemo) {
            LedgerImporter.validate(incoming)
            incoming
        } else {
            document.merging(incoming)
        }
        // Preserve edits to the initial examples when importing additional records.
        val includesDemo = isDemo && !replaceDemo
        val userEdits = includesDemo && hasUserEdits
        val prices = if (replaceDemo) LedgerPriceSuggestions.seededPrices(candidate.records) else lastAddedPrices
        persist(candidate, demo = includesDemo, userEdits = userEdits, prices = prices)
        document = candidate
        isDemo = includesDemo
        hasUserEdits = userEdits
        lastAddedPrices = prices
        storageError = null
        true
    } catch (failure: Exception) {
        storageError = primaryErrorText(failure)
        false
    }

    /** Example purchases load only into an empty ledger, or into the existing example ledger. */
    fun loadDemo(): Boolean {
        val empty = document.groups.isEmpty() && document.members.isEmpty() &&
            document.memberships.isEmpty() && document.records.isEmpty() &&
            document.events.isEmpty() &&
            (document.products.isEmpty() || document.products == LedgerDocument.defaultProducts)
        if (!canReplaceDemo && !empty) {
            storageError = L10n.text("ledger.demoRequiresEmpty")
            return false
        }
        return try {
            val candidate = LedgerDemo.document()
            val prices = LedgerPriceSuggestions.seededPrices(candidate.records)
            persist(candidate, demo = true, userEdits = false, prices = prices)
            document = candidate
            isDemo = true
            hasUserEdits = false
            lastAddedPrices = prices
            storageError = null
            true
        } catch (failure: Exception) {
            storageError = primaryErrorText(failure)
            false
        }
    }

    fun upsertGroup(value: LedgerGroup): Boolean {
        if (!value.isActive) return unavailableCatalog()
        val existing = document.groups.firstOrNull { it.id == value.id }
        if (existing != null && !existing.isActive) return unavailableCatalog()
        return commit { it.copy(groups = upsert(it.groups, value) { item -> item.id }) }
    }

    fun upsertMember(value: LedgerMember): Boolean {
        if (!value.isActive) return unavailableCatalog()
        val existing = document.members.firstOrNull { it.id == value.id }
        if (existing != null && !existing.isActive) return unavailableCatalog()
        return commit { it.copy(members = upsert(it.members, value) { item -> item.id }) }
    }

    fun upsertMembership(value: LedgerMembership): Boolean {
        if (!value.isActive) return unavailableCatalog()
        val existing = document.memberships.firstOrNull { it.id == value.id }
        if (existing != null && !existing.isActive) return unavailableCatalog()
        if (document.activeMembers.none { it.id == value.memberId }) return unavailableCatalog()
        if (document.activeGroups.none { it.id == value.groupId }) return unavailableCatalog()
        return commit { it.copy(memberships = upsert(it.memberships, value) { item -> item.id }) }
    }

    fun upsertProduct(value: LedgerProduct): Boolean =
        commit { it.copy(products = upsert(it.products, value) { item -> item.id }) }

    fun upsertEvent(value: LedgerEvent): Boolean =
        commit { it.copy(events = upsert(it.events, value) { item -> item.id }) }

    fun saveMember(value: LedgerMember, membership: LedgerMembership): Boolean {
        if (!value.isActive || !membership.isActive || membership.memberId != value.id) return unavailableCatalog()
        val existingMember = document.members.firstOrNull { it.id == value.id }
        if (existingMember != null && !existingMember.isActive) return unavailableCatalog()
        val existingMembership = document.memberships.firstOrNull { it.id == membership.id }
        if (existingMembership != null && !existingMembership.isActive) return unavailableCatalog()
        if (document.activeGroups.none { it.id == membership.groupId }) return unavailableCatalog()
        return commit {
            it.copy(
                members = upsert(it.members, value) { item -> item.id },
                memberships = upsert(it.memberships, membership) { item -> item.id }
            )
        }
    }

    fun saveRecord(value: LedgerRecord): Boolean {
        // A historic purchase stays editable after catalog deletion. A new or reassigned
        // purchase must use an active affiliation, even if a stale editor was open.
        val original = document.records.firstOrNull { it.id == value.id }
        val keepsHistoricalAffiliation =
            original?.memberId == value.memberId && original?.groupId == value.groupId
        if (!keepsHistoricalAffiliation) {
            val available = document.activeMemberships.none {
                it.memberId == value.memberId && it.groupId == value.groupId
            }
            if (available) return unavailableCatalog()
        }
        val prices = if (original == null) {
            LedgerPriceSuggestions.remembering(
                LedgerPrice(value.productId, value.currency, value.unitPriceMinor),
                lastAddedPrices
            )
        } else {
            lastAddedPrices
        }
        return try {
            transaction({ it.copy(records = upsert(it.records, value) { item -> item.id }) }, remembering = prices)
            true
        } catch (failure: Exception) {
            storageError = primaryErrorText(failure)
            false
        }
    }

    fun deleteRecord(id: String): Boolean = commit { it.copy(records = it.records.filter { record -> record.id != id }) }

    fun deleteRecords(ids: Set<String>): Boolean {
        if (ids.isEmpty()) return true
        return commit { it.deletingRecords(ids) }
    }

    fun deleteMembers(ids: Set<String>): Boolean {
        if (ids.isEmpty()) return true
        return commit { it.deletingMembers(ids) }
    }

    fun deleteGroups(ids: Set<String>): Boolean {
        if (ids.isEmpty()) return true
        return commit { it.deletingGroups(ids) }
    }

    fun deleteMember(id: String): Boolean = commit { it.deletingMember(id) }

    fun deleteGroup(id: String): Boolean = commit { it.deletingGroup(id) }

    private fun unavailableCatalog(): Boolean {
        storageError = L10n.text("ledger.catalogUnavailable")
        return false
    }

    private fun <T> upsert(items: List<T>, value: T, idOf: (T) -> String): List<T> {
        val index = items.indexOfFirst { idOf(it) == idOf(value) }
        return if (index >= 0) items.toMutableList().apply { this[index] = value } else items + value
    }

    private fun persist(candidate: LedgerDocument, demo: Boolean, userEdits: Boolean, prices: List<LedgerPrice>) {
        LedgerImporter.validate(candidate)
        val root = JsonObject().apply {
            put("document", LedgerCodec.toJson(candidate))
            put("isDemo", demo)
            put("hasUserEdits", userEdits)
            put("lastAddedPrices", JsonArray(prices.map {
                JsonObject().apply {
                    put("productID", it.productId)
                    put("currency", it.currency)
                    put("unitPriceMinor", it.unitPriceMinor)
                }
            }.toMutableList()))
        }
        storage.write(root.serialize(pretty = true))
    }

    private fun priceFromJson(value: JsonValue): LedgerPrice {
        val json = value.objectOrThrow("$.lastAddedPrices[]")
        return LedgerPrice(
            json.requireString("productID", "$.lastAddedPrices[]"),
            json.requireString("currency", "$.lastAddedPrices[]"),
            json.requireLong("unitPriceMinor", "$.lastAddedPrices[]")
        )
    }

    fun exportJson(): String = LedgerImporter.encode(document)
}

class DiaryStore(directory: File) {
    var document by mutableStateOf(DiaryDocument())
        private set
    var storageError by mutableStateOf<String?>(null)

    private val storage = DocumentFile(directory, "diary.json")

    init {
        if (storage.exists()) {
            try {
                if (storage.size() > DiaryImporter.maximumDocumentBytes) {
                    throw DiaryError("diary.error.documentSize", "The diary must not exceed 20 MB.", "$")
                }
                document = DiaryImporter.parse(storage.readBytes())
            } catch (failure: Exception) {
                storage.markUnreadable()
                storageError = L10n.text("settings.readError") + "\n" + primaryErrorText(failure)
            }
        } else {
            try {
                persist(document)
            } catch (failure: Exception) {
                storageError = primaryErrorText(failure)
            }
        }
    }

    /** A failed write leaves observers and the persisted document unchanged. */
    fun upsertEntry(entry: DiaryEntry): Boolean {
        val index = document.entries.indexOfFirst { it.id == entry.id }
        val candidate = if (index >= 0) {
            document.copy(entries = document.entries.toMutableList().apply { this[index] = entry })
        } else {
            document.copy(entries = document.entries + entry)
        }
        return commit(candidate)
    }

    fun deleteEntry(id: String): Boolean =
        commit(document.copy(entries = document.entries.filter { it.id != id }))

    fun deleteEntries(ids: Set<String>): Boolean {
        if (ids.isEmpty()) return true
        return commit(document.deletingEntries(ids))
    }

    fun importDocument(incoming: DiaryDocument): Boolean = try {
        commit(document.merging(incoming))
    } catch (failure: Exception) {
        storageError = primaryErrorText(failure)
        false
    }

    fun exportJson(): String = DiaryImporter.encode(document)

    private fun commit(candidate: DiaryDocument): Boolean = try {
        persist(candidate)
        document = candidate
        storageError = null
        true
    } catch (failure: Exception) {
        storageError = primaryErrorText(failure)
        false
    }

    private fun persist(candidate: DiaryDocument) {
        val text = DiaryImporter.encode(candidate)
        if (text.toByteArray(Charsets.UTF_8).size > DiaryImporter.maximumDocumentBytes) {
            throw DiaryError("diary.error.documentSize", "The diary must not exceed 20 MB.", "$")
        }
        storage.write(text)
    }
}

class AgendaStore(directory: File) {
    var document by mutableStateOf(AgendaDocument())
        private set
    var storageError by mutableStateOf<String?>(null)

    private val storage = DocumentFile(directory, "agenda.json")

    init {
        if (storage.exists()) {
            try {
                if (storage.size() > AgendaImporter.maximumDocumentBytes) {
                    throw AgendaError("agenda.error.size", "$")
                }
                document = AgendaImporter.parse(storage.readText())
            } catch (failure: Exception) {
                storage.markUnreadable()
                storageError = primaryErrorText(failure)
            }
        } else {
            try {
                persist(document)
            } catch (failure: Exception) {
                storageError = primaryErrorText(failure)
            }
        }
    }

    fun upsert(entry: AgendaEntry): Boolean {
        val index = document.entries.indexOfFirst { it.id == entry.id }
        val candidate = if (index >= 0) {
            document.copy(entries = document.entries.toMutableList().apply { this[index] = entry })
        } else {
            document.copy(entries = document.entries + entry)
        }
        return commit(candidate)
    }

    fun delete(id: String): Boolean = deleteEntries(setOf(id))

    fun deleteEntries(ids: Set<String>): Boolean {
        if (ids.isEmpty()) return true
        return commit(document.deletingEntries(ids))
    }

    fun importDocument(incoming: AgendaDocument): Boolean = try {
        commit(document.merging(incoming))
    } catch (failure: Exception) {
        storageError = primaryErrorText(failure)
        false
    }

    fun exportJson(): String = AgendaImporter.encode(document)

    private fun commit(candidate: AgendaDocument): Boolean = try {
        persist(candidate)
        document = candidate
        storageError = null
        true
    } catch (failure: Exception) {
        storageError = primaryErrorText(failure)
        false
    }

    private fun persist(candidate: AgendaDocument) {
        storage.write(AgendaImporter.encode(candidate))
    }
}

/** User preferences stored alongside the documents. */
class SettingsStore(directory: File) {
    private val storage = DocumentFile(directory, "settings.json")
    var language by mutableStateOf(AppLanguage.System)
        private set
    var selectedModule by mutableStateOf(AppModule.Agenda)
        private set

    init {
        if (storage.exists()) {
            try {
                val root = JsonParser.parse(storage.readText()).objectOrThrow("$")
                language = AppLanguage.fromId(root.optionalString("appLanguage", "$"))
                val storedModule = root.optionalLong("selectedModule", "$")
                selectedModule = if (storedModule != null && storedModule in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                    AppModule.restored(storedModule.toInt())
                } else {
                    AppModule.Agenda
                }
            } catch (_: Exception) {
                // A damaged preference file falls back to defaults; documents stay untouched.
            }
        }
    }

    fun updateLanguage(value: AppLanguage) {
        language = value
        save()
    }

    fun updateSelectedModule(value: AppModule) {
        selectedModule = value
        save()
    }

    private fun save() {
        val root = JsonObject().apply {
            put("appLanguage", language.id)
            put("selectedModule", selectedModule.rawValue)
        }
        try {
            storage.write(root.serialize(pretty = true))
        } catch (_: Exception) {
            // Preferences are best-effort; the documents remain the source of truth.
        }
    }
}
