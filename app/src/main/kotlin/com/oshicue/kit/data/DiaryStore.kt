package com.oshicue.kit.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.oshicue.kit.core.DiaryDocument
import com.oshicue.kit.core.DiaryEntry
import com.oshicue.kit.core.DiaryError
import com.oshicue.kit.core.DiaryImporter
import com.oshicue.kit.core.KitIssue
import com.oshicue.kit.core.deletingEntries
import com.oshicue.kit.i18n.L10n
import java.io.File

/** Diary storage is independent of both purchase records and festival schedules. */
class DiaryStore(private val context: Context) {
    var document: DiaryDocument by mutableStateOf(DiaryDocument())
        private set
    var storageError: String? by mutableStateOf(null)

    private val file = File(KitFiles.directory(context), "diary.json")
    private var unreadableFile = false

    init {
        if (file.exists()) {
            try {
                if (file.length() > DiaryImporter.maximumDocumentBytes) {
                    throw DiaryError("diary.error.documentSize", "The diary must not exceed 20 MB.", "$")
                }
                document = DiaryImporter.parse(file.readBytes())
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

    /** A failed write leaves observers and the persisted document unchanged. */
    fun upsertEntry(entry: DiaryEntry): Boolean {
        var candidate = document
        val index = candidate.entries.indexOfFirst { it.id == entry.id }
        candidate = if (index >= 0) {
            candidate.copy(entries = candidate.entries.toMutableList().also { it[index] = entry })
        } else {
            candidate.copy(entries = candidate.entries + entry)
        }
        return commit(candidate)
    }

    fun deleteEntry(id: String): Boolean = commit(document.copy(entries = document.entries.filter { it.id != id }))

    fun deleteEntries(ids: Set<String>): Boolean {
        if (ids.isEmpty()) return true
        return commit(document.deletingEntries(ids))
    }

    fun importDocument(incoming: DiaryDocument): Boolean = try {
        commit(document.merging(incoming))
    } catch (error: Exception) {
        storageError = message(error)
        false
    }

    fun exportJSON(): String = DiaryImporter.encode(document)

    private fun commit(candidate: DiaryDocument): Boolean = try {
        persist(candidate)
        document = candidate
        storageError = null
        true
    } catch (error: Exception) {
        storageError = message(error)
        false
    }

    private fun persist(candidate: DiaryDocument) {
        val bytes = DiaryImporter.encode(candidate).toByteArray(Charsets.UTF_8)
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
