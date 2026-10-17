package com.oshicue.kit.core

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

object ScheduleFavorites {
    /**
     * Session IDs are unique within one event, so both IDs identify a selection.
     * The event's byte length keeps arbitrary IDs from producing ambiguous keys.
     */
    fun id(eventID: String, sessionID: String): String =
        eventID.toByteArray(Charsets.UTF_8).size.toString() + ":" + eventID + sessionID
}

val ScheduleDocument.sessionFavoriteIDs: Set<String>
    get() = events.flatMap { event ->
        event.sessions.map { ScheduleFavorites.id(event.id, it.id) }
    }.toSet()

/** Expands legacy group selections to the sessions they previously displayed. */
fun ScheduleDocument.favoritesForArtistIDs(artistIDs: Set<String>): Set<String> =
    events.flatMap { event ->
        event.sessions.filter { it.artistID in artistIDs }
            .map { ScheduleFavorites.id(event.id, it.id) }
    }.toSet()

/** Local persistence has its own version; the imported timetable schema stays unchanged. */
class ScheduleSavedState(
    val document: ScheduleDocument,
    favorites: Set<String>,
    val isDemo: Boolean,
    val needsFavoriteMigration: Boolean = false
) {
    val favorites: Set<String> = favorites.intersect(document.sessionFavoriteIDs)

    fun encode(): JsonObject {
        val documentElement = KitJson.parse(ScheduleImporter.encode(document))
            ?: throw IllegalStateException("Unreadable schedule document")
        val root = JObj()
        root.putElement("document", documentElement)
        root.putStringSet("favorites", favorites)
        root.put("isDemo", isDemo)
        root.put("favoritesVersion", currentVersion)
        return root.build()
    }

    companion object {
        const val currentVersion = 2

        fun decode(element: JsonElement): ScheduleSavedState {
            val root = element as? JsonObject ?: throw IllegalStateException("Unreadable schedule state")
            val documentElement = root["document"] ?: throw IllegalStateException("Missing schedule document")
            val document = ScheduleImporter.parse(JsonText.encode(documentElement))
            val version = favoritesVersion(root["favoritesVersion"])
            val favoritesValue = root["favorites"] as? JsonArray ?: throw IllegalStateException("Missing favorites")
            val savedFavorites = favoritesValue.mapNotNull { (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.content }.toSet()
            val demoValue = (root["isDemo"] as? JsonPrimitive)?.content ?: throw IllegalStateException("Missing demo marker")
            val isDemo = demoValue == "true"
            return when (version) {
                1 -> ScheduleSavedState(document, document.favoritesForArtistIDs(savedFavorites), isDemo, needsFavoriteMigration = true)
                2 -> {
                    val favorites = savedFavorites.intersect(document.sessionFavoriteIDs)
                    ScheduleSavedState(document, favorites, isDemo, needsFavoriteMigration = favorites != savedFavorites)
                }
                else -> throw IllegalStateException("Unsupported schedule favorites version $version.")
            }
        }

        private fun favoritesVersion(value: JsonElement?): Int {
            if (value == null) return 1
            val primitive = value as? JsonPrimitive
            if (primitive == null || primitive.isString) {
                throw IllegalStateException("Unsupported schedule favorites version.")
            }
            return primitive.content.toIntOrNull()
                ?: throw IllegalStateException("Unsupported schedule favorites version.")
        }
    }
}
