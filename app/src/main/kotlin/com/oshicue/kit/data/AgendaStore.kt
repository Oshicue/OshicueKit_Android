package com.oshicue.kit.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.oshicue.kit.core.AgendaDocument
import com.oshicue.kit.core.AgendaEntry
import com.oshicue.kit.core.AgendaError
import com.oshicue.kit.core.AgendaImporter
import com.oshicue.kit.core.KitIssue
import com.oshicue.kit.core.deletingEntries
import com.oshicue.kit.i18n.L10n
import java.io.File

/** Plans are independent of the timetable, its favorites, the ledger and the diary. */
class AgendaStore(private val context: Context) {
    var document: AgendaDocument by mutableStateOf(AgendaDocument())
        private set
    var storageError: String? by mutableStateOf(null)

    private val file = File(KitFiles.directory(context), "agenda.json")
    private var unreadableFile = false

    init {
        if (file.exists()) {
            try {
                if (file.length() > AgendaImporter.maximumDocumentBytes) {
                    throw AgendaError("agenda.error.size", "Plans must not exceed 10 MB.", "$")
                }
                document = AgendaImporter.parse(file.readBytes())
            } catch (error: Exception) {
                unreadableFile = true
                storageError = L10n.text("settings.readError") + "\n" + message(error)
            }
        } else {
            try {
                persist(document)
            } catch (error: Exception) {
                storageError = message(error)
            }
        }
    }

    fun upsert(entry: AgendaEntry): Boolean {
        val entries = document.entries.toMutableList()
        val index = entries.indexOfFirst { it.id == entry.id }
        if (index >= 0) entries[index] = entry else entries.add(entry)
        return commit(document.copy(entries = entries))
    }

    fun deleteEntry(id: String): Boolean = deleteEntries(setOf(id))

    fun deleteEntries(ids: Set<String>): Boolean {
        if (ids.isEmpty()) return true
        return commit(document.deletingEntries(ids))
    }

    fun importDocument(incoming: AgendaDocument): Boolean = try {
        commit(document.merging(incoming))
    } catch (error: Exception) {
        storageError = message(error)
        false
    }

    fun exportJSON(): String = AgendaImporter.encode(document)

    private fun commit(candidate: AgendaDocument): Boolean = try {
        persist(candidate)
        document = candidate
        storageError = null
        true
    } catch (error: Exception) {
        storageError = message(error)
        false
    }

    private fun persist(candidate: AgendaDocument) {
        val bytes = AgendaImporter.encode(candidate).toByteArray(Charsets.UTF_8)
        if (unreadableFile && file.exists()) {
            KitFiles.backUpUnreadable(file)
            unreadableFile = false
        }
        KitFiles.writeAtomic(file, bytes)
    }

    private fun message(error: Exception): String {
        if (error is KitIssue) return L10n.text(error.key) + "\n" + error.path
        return error.message ?: error.toString()
    }
}
