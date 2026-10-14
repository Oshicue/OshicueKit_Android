package com.oshicue.kit.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.oshicue.kit.core.JObj
import com.oshicue.kit.core.JsonText
import com.oshicue.kit.core.KitIssue
import com.oshicue.kit.core.KitJson
import com.oshicue.kit.core.LedgerDemo
import com.oshicue.kit.core.LedgerDocument
import com.oshicue.kit.core.LedgerError
import com.oshicue.kit.core.LedgerEvent
import com.oshicue.kit.core.LedgerGroup
import com.oshicue.kit.core.LedgerImporter
import com.oshicue.kit.core.LedgerMember
import com.oshicue.kit.core.LedgerMembership
import com.oshicue.kit.core.LedgerProduct
import com.oshicue.kit.core.LedgerRecord
import com.oshicue.kit.core.activeGroups
import com.oshicue.kit.core.activeMembers
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.core.deletingGroups
import com.oshicue.kit.core.deletingMember
import com.oshicue.kit.core.deletingMembers
import com.oshicue.kit.core.deletingGroup
import com.oshicue.kit.core.deletingRecords
import com.oshicue.kit.i18n.L10n
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.File

/** The ledger is independent of festival schedules. A failed save never publishes a partial edit. */
class LedgerStore(private val context: Context) {
    var document: LedgerDocument by mutableStateOf(LedgerDocument(products = LedgerDocument.defaultProducts))
        private set
    var isDemo: Boolean by mutableStateOf(false)
        private set
    var storageError: String? by mutableStateOf(null)
    private var hasUserEdits = false
    val canReplaceDemo: Boolean get() = isDemo && !hasUserEdits

    private val file = File(KitFiles.directory(context), "ledger.json")
    private var unreadableFile = false

    init {
        if (file.exists()) {
            try {
                val root = KitJson.parse(file.readText())?.jsonObject
                    ?: throw IllegalStateException("Unreadable ledger state")
                val documentElement = root["document"] ?: throw IllegalStateException("Missing ledger document")
                val parsed = LedgerImporter.parse(JsonText.encode(documentElement))
                val demoValue = (root["isDemo"] as? JsonPrimitive)?.content ?: throw IllegalStateException("Missing demo marker")
                document = parsed
                isDemo = demoValue == "true"
                hasUserEdits = ((root["hasUserEdits"] as? JsonPrimitive)?.content ?: "false") == "true"
            } catch (error: Exception) {
                unreadableFile = true
                storageError = L10n.text("settings.readError") + "\n" + message(error)
            }
        } else {
            document = LedgerDemo.document()
            isDemo = true
            try {
                persist(document, demo = true, userEdits = false)
            } catch (error: Exception) {
                storageError = message(error)
            }
        }
    }

    /** Validates the complete candidate and atomically persists it before updating observers. */
    fun transaction(update: (LedgerDocument) -> LedgerDocument) {
        val candidate = update(document)
        val userEdits = hasUserEdits || isDemo
        persist(candidate, demo = isDemo, userEdits = userEdits)
        document = candidate
        hasUserEdits = userEdits
        storageError = null
    }

    fun commit(update: (LedgerDocument) -> LedgerDocument): Boolean = try {
        transaction(update)
        true
    } catch (error: Exception) {
        storageError = message(error)
        false
    }

    fun importDocument(incoming: LedgerDocument): Boolean {
        return try {
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
            persist(candidate, demo = includesDemo, userEdits = userEdits)
            document = candidate
            isDemo = includesDemo
            hasUserEdits = userEdits
            storageError = null
            true
        } catch (error: Exception) {
            storageError = message(error)
            false
        }
    }

    /** Example purchases are loaded only into an empty ledger, or into the existing example ledger. */
    fun loadDemo(): Boolean {
        val empty = document.groups.isEmpty() && document.members.isEmpty() && document.memberships.isEmpty() &&
            document.records.isEmpty() && document.events.isEmpty() &&
            (document.products.isEmpty() || document.products == LedgerDocument.defaultProducts)
        if (!(canReplaceDemo || empty)) {
            storageError = L10n.text("ledger.demoRequiresEmpty")
            return false
        }
        return try {
            val candidate = LedgerDemo.document()
            persist(candidate, demo = true, userEdits = false)
            document = candidate
            isDemo = true
            hasUserEdits = false
            storageError = null
            true
        } catch (error: Exception) {
            storageError = message(error)
            false
        }
    }

    fun upsertGroup(value: LedgerGroup): Boolean {
        if (!value.isActive || document.groups.firstOrNull { it.id == value.id }?.isActive == false) return unavailableCatalog()
        return commit { it.copy(groups = upsertBy(value, it.groups) { item -> item.id }) }
    }

    fun upsertMember(value: LedgerMember): Boolean {
        if (!value.isActive || document.members.firstOrNull { it.id == value.id }?.isActive == false) return unavailableCatalog()
        return commit { it.copy(members = upsertBy(value, it.members) { item -> item.id }) }
    }

    fun upsertMembership(value: LedgerMembership): Boolean {
        if (!value.isActive ||
            document.memberships.firstOrNull { it.id == value.id }?.isActive == false ||
            document.activeMembers.none { it.id == value.memberID } ||
            document.activeGroups.none { it.id == value.groupID }
        ) return unavailableCatalog()
        return commit { it.copy(memberships = upsertBy(value, it.memberships) { item -> item.id }) }
    }

    fun upsertProduct(value: LedgerProduct): Boolean =
        commit { it.copy(products = upsertBy(value, it.products) { item -> item.id }) }

    fun upsertEvent(value: LedgerEvent): Boolean =
        commit { it.copy(events = upsertBy(value, it.events) { item -> item.id }) }

    fun saveMember(value: LedgerMember, membership: LedgerMembership): Boolean {
        if (!value.isActive || !membership.isActive || membership.memberID != value.id ||
            document.members.firstOrNull { it.id == value.id }?.isActive == false ||
            document.memberships.firstOrNull { it.id == membership.id }?.isActive == false ||
            document.activeGroups.none { it.id == membership.groupID }
        ) return unavailableCatalog()
        return commit {
            it.copy(
                members = upsertBy(value, it.members) { item -> item.id },
                memberships = upsertBy(membership, it.memberships) { item -> item.id }
            )
        }
    }

    fun saveRecord(value: LedgerRecord): Boolean {
        // A historic purchase stays editable after catalog deletion. A new/reassigned purchase
        // must use an active affiliation, including when a stale editor was open during deletion.
        val original = document.records.firstOrNull { it.id == value.id }
        val keepsHistoricalAffiliation = original?.memberID == value.memberID && original?.groupID == value.groupID
        if (!keepsHistoricalAffiliation &&
            document.activeMemberships.none { it.memberID == value.memberID && it.groupID == value.groupID }
        ) return unavailableCatalog()
        return commit { it.copy(records = upsertBy(value, it.records) { item -> item.id }) }
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

    private fun persist(candidate: LedgerDocument, demo: Boolean, userEdits: Boolean) {
        LedgerImporter.validate(candidate)
        if (unreadableFile && file.exists()) {
            KitFiles.backUpUnreadable(file)
            unreadableFile = false
        }
        val root = JObj()
        root.putElement("document", LedgerImporter.encodeElement(candidate))
        root.put("hasUserEdits", userEdits)
        root.put("isDemo", demo)
        KitFiles.writeAtomic(file, JsonText.encode(root.build()).toByteArray(Charsets.UTF_8))
    }

    private fun message(error: Exception): String {
        if (error is KitIssue) return L10n.text(error.key) + "\n" + error.path
        return error.message ?: error.toString()
    }
}

private fun <T> upsertBy(value: T, array: List<T>, idOf: (T) -> String): List<T> {
    val index = array.indexOfFirst { idOf(it) == idOf(value) }
    return if (index >= 0) array.toMutableList().also { it[index] = value } else array + value
}
