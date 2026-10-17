package com.oshicue.kit.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.oshicue.kit.core.JsonText
import com.oshicue.kit.core.KitJson
import com.oshicue.kit.core.ScheduleDemo
import com.oshicue.kit.core.ScheduleDocument
import com.oshicue.kit.core.ScheduleSavedState
import com.oshicue.kit.core.deletingEvent
import com.oshicue.kit.core.favoritesForArtistIDs
import com.oshicue.kit.core.sessionFavoriteIDs
import com.oshicue.kit.i18n.L10n
import java.io.File

class ScheduleStore(private val context: Context) {
    var document: ScheduleDocument by mutableStateOf(ScheduleDocument(events = emptyList()))
        private set
    var favorites: Set<String> by mutableStateOf(emptySet())
        private set
    var isDemo: Boolean by mutableStateOf(false)
        private set
    var storageError: String? by mutableStateOf(null)

    private val file: File = File(KitFiles.directory(context), "schedule.json")

    init {
        if (file.exists()) {
            try {
                val element = KitJson.parse(file.readText())
                    ?: throw IllegalStateException("Unreadable schedule state")
                val state = ScheduleSavedState.decode(element)
                document = state.document
                favorites = state.favorites
                isDemo = state.isDemo
                if (state.needsFavoriteMigration) save()
            } catch (error: Exception) {
                document = ScheduleDocument(events = emptyList())
                favorites = emptySet()
                isDemo = false
                storageError = L10n.text("settings.readError") + "\n" + (error.message ?: error.toString())
            }
        } else {
            val demo = ScheduleDemo.document()
            document = demo
            favorites = demo.favoritesForArtistIDs(demo.events.firstOrNull()?.artists?.take(2)?.map { it.id }?.toSet() ?: emptySet())
            isDemo = true
            save()
        }
    }

    fun isFavorite(sessionID: String): Boolean = sessionID in favorites

    fun toggleFavorite(sessionID: String) {
        if (sessionID !in document.sessionFavoriteIDs) return
        favorites = if (sessionID in favorites) favorites - sessionID else favorites + sessionID
        save()
    }

    fun importDocument(incoming: ScheduleDocument) {
        document = if (isDemo) incoming else document.merging(incoming)
        favorites = if (isDemo) emptySet() else favorites.intersect(document.sessionFavoriteIDs)
        isDemo = false
        save()
    }

    /** Commit the new file before updating the UI so a failed deletion remains retryable. */
    fun deleteEvent(id: String): Boolean {
        val result = document.deletingEvent(id, favorites) ?: return false
        val stillDemo = isDemo && result.document.events.isNotEmpty()
        return try {
            write(ScheduleSavedState(result.document, result.favorites, stillDemo))
            document = result.document
            favorites = result.favorites
            isDemo = stillDemo
            storageError = null
            true
        } catch (error: Exception) {
            storageError = L10n.text("settings.saveError") + "\n" + (error.message ?: error.toString())
            false
        }
    }

    /** Demo loading appends only demo IDs so an imported festival is never discarded. */
    fun loadDemo() {
        val demo = ScheduleDemo.document()
        document = document.merging(demo)
        favorites = favorites.intersect(document.sessionFavoriteIDs)
        if (document.events.size == demo.events.size) isDemo = true
        save()
    }

    private fun save() {
        try {
            write(ScheduleSavedState(document, favorites, isDemo))
        } catch (error: Exception) {
            storageError = L10n.text("settings.saveError") + "\n" + (error.message ?: error.toString())
        }
    }

    private fun write(state: ScheduleSavedState) {
        KitFiles.writeAtomic(file, JsonText.encode(state.encode()).toByteArray(Charsets.UTF_8))
    }
}
