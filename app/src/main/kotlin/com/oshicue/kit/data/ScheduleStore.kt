package com.oshicue.kit.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.oshicue.kit.core.JObj
import com.oshicue.kit.core.JsonText
import com.oshicue.kit.core.KitJson
import com.oshicue.kit.core.ScheduleDemo
import com.oshicue.kit.core.ScheduleDocument
import com.oshicue.kit.core.ScheduleImporter
import com.oshicue.kit.core.deletingEvent
import com.oshicue.kit.i18n.L10n
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
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
                val root = KitJson.parse(file.readText())?.jsonObject
                    ?: throw IllegalStateException("Unreadable schedule state")
                val documentElement = root["document"]
                    ?: throw IllegalStateException("Missing schedule document")
                val parsed = ScheduleImporter.parse(JsonText.encode(documentElement))
                val favoritesValue = root["favorites"] as? JsonArray
                    ?: throw IllegalStateException("Missing favorites")
                val demoValue = (root["isDemo"] as? JsonPrimitive)?.content
                    ?: throw IllegalStateException("Missing demo marker")
                document = parsed
                favorites = favoritesValue.mapNotNull { (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.content }.toSet()
                isDemo = demoValue == "true"
            } catch (error: Exception) {
                document = ScheduleDocument(events = emptyList())
                favorites = emptySet()
                isDemo = false
                storageError = L10n.text("settings.readError") + "\n" + (error.message ?: error.toString())
            }
        } else {
            val demo = ScheduleDemo.document()
            document = demo
            favorites = demo.events.firstOrNull()?.artists?.take(2)?.map { it.id }?.toSet() ?: emptySet()
            isDemo = true
            save()
        }
    }

    fun isFavorite(artistID: String): Boolean = artistID in favorites

    fun toggleFavorite(artistID: String) {
        favorites = if (artistID in favorites) favorites - artistID else favorites + artistID
        save()
    }

    fun importDocument(incoming: ScheduleDocument) {
        document = if (isDemo) incoming else document.merging(incoming)
        if (isDemo) favorites = emptySet()
        isDemo = false
        save()
    }

    /** Commit the new file before updating the UI so a failed deletion remains retryable. */
    fun deleteEvent(id: String): Boolean {
        val result = document.deletingEvent(id, favorites) ?: return false
        val stillDemo = isDemo && result.document.events.isNotEmpty()
        return try {
            write(result.document, result.favorites, stillDemo)
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
        if (document.events.size == demo.events.size) isDemo = true
        save()
    }

    private fun save() {
        try {
            write(document, favorites, isDemo)
        } catch (error: Exception) {
            storageError = L10n.text("settings.saveError") + "\n" + (error.message ?: error.toString())
        }
    }

    private fun write(document: ScheduleDocument, favorites: Set<String>, isDemo: Boolean) {
        val documentElement = KitJson.parse(ScheduleImporter.encode(document)) ?: return
        val root = JObj()
        root.putElement("document", documentElement)
        root.putStringSet("favorites", favorites)
        root.put("isDemo", isDemo)
        KitFiles.writeAtomic(file, JsonText.encode(root.build()).toByteArray(Charsets.UTF_8))
    }
}
