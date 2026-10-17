package com.oshicue.kit.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import com.oshicue.kit.data.ScheduleStore
import com.oshicue.kit.core.FestivalEvent
import com.oshicue.kit.core.FestivalSession
import com.oshicue.kit.core.SessionKind
import com.oshicue.kit.core.TimelineLayout
import com.oshicue.kit.core.Venue
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.BrandWordmark
import com.oshicue.kit.ui.common.CircleIconButton
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.hexColor
import com.oshicue.kit.ui.theme.kitPalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

data class SessionEntry(val event: FestivalEvent, val session: FestivalSession) {
    val id: String get() = com.oshicue.kit.core.ScheduleFavorites.id(event.id, session.id)
    val artist: String get() = event.artist(session.artistID)?.name ?: session.artistID
    val venue: Venue get() = event.venue(session.venueID)!!
    val item: com.oshicue.kit.core.TimelineItem get() = com.oshicue.kit.core.TimelineItem(id, session.startsAt, session.endsAt)
}

@Composable
fun ScheduleScreen(store: ScheduleStore) {
    val palette = kitPalette()
    val view = LocalView.current
    var mine by remember { mutableStateOf(false) }
    var selectedEventIDs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var kind by remember { mutableStateOf<SessionKind?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var showEvents by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<SessionEntry?>(null) }
    var jumpToken by remember { mutableIntStateOf(0) }
    var zoom by remember { mutableFloatStateOf(4f) }
    var toast by remember { mutableStateOf<String?>(null) }
    var toastRequest by remember { mutableIntStateOf(0) }
    var previousEventIDs by remember { mutableStateOf<Set<String>?>(null) }

    val events = store.document.events.filter { it.id in selectedEventIDs }
    val zone: ZoneId = events.firstOrNull()?.timeZoneValue
        ?: store.document.events.firstOrNull()?.timeZoneValue
        ?: ZoneId.systemDefault()

    val days = remember(selectedEventIDs, store.document, zone) {
        val values = sortedSetOf<LocalDate>()
        for (event in store.document.events.filter { it.id in selectedEventIDs }) {
            for (session in event.sessions) {
                var date = session.startsAt.atZone(zone).toLocalDate()
                while (date.atStartOfDay(zone).toInstant() < session.endsAt) {
                    values.add(date)
                    date = date.plusDays(1)
                }
            }
        }
        values.toList()
    }
    val today = LocalDate.now(zone)
    val day = selectedDay?.takeIf { it in days } ?: days.firstOrNull() ?: today

    LaunchedEffect(store.document.events.map { it.id }) {
        val ids = store.document.events.map { it.id }.toSet()
        val previous = previousEventIDs
        val next = when {
            previous == null -> ids
            previous.intersect(ids).isEmpty() -> ids
            else -> selectedEventIDs.intersect(ids) + (ids - previous)
        }
        previousEventIDs = ids
        if (next != selectedEventIDs) selectedEventIDs = next
    }
    LaunchedEffect(selectedEventIDs, days) {
        if (selectedDay == null || selectedDay !in days) {
            selectedDay = if (today in days) today else days.firstOrNull()
        }
    }

    val allEntries = remember(events, day, zone) {
        val start = day.atStartOfDay(zone).toInstant()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant()
        events.flatMap { event ->
            event.sessions.filter { it.startsAt < end && it.endsAt > start }
                .map { SessionEntry(event, it) }
        }.sortedBy { it.session.startsAt }
    }
    val visibleEntries = allEntries.filter { entry ->
        if (mine) store.isFavorite(entry.id) else kind == null || entry.session.kind == kind
    }
    val conflictCount = remember(visibleEntries) {
        var count = 0
        for (i in visibleEntries.indices) {
            for (j in i + 1 until visibleEntries.size) {
                if (visibleEntries[i].session.startsAt < visibleEntries[j].session.endsAt &&
                    visibleEntries[j].session.startsAt < visibleEntries[i].session.endsAt
                ) {
                    count += 1
                    break
                }
            }
        }
        count
    }

    LaunchedEffect(toastRequest) {
        if (toast != null) {
            kotlinx.coroutines.delay(2000)
            toast = null
        }
    }

    fun toggleFavorite(entry: SessionEntry) {
        store.toggleFavorite(entry.id)
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        toast = L10n.text(if (store.isFavorite(entry.id)) "schedule.favoriteAdded" else "schedule.favoriteRemoved") +
            " · " + entry.artist + " · " + L10n.text(if (entry.session.kind == SessionKind.live) "schedule.live" else "schedule.goods")
        toastRequest += 1
    }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandWordmark()
            Spacer(modifier = Modifier.weight(1f))
            CircleIconButton(Icons.Filled.Download, L10n.text("import.title")) { showImport = true }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 18.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(5.dp).background(palette.accent, CircleShape))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (events.size > 1) L10n.text("schedule.allEvents") else events.firstOrNull()?.city ?: "OSHICUE KIT",
                        color = palette.muted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp
                    )
                }
                Spacer(modifier = Modifier.height(7.dp))
                Text(L10n.text("schedule.title"), color = palette.ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .padding(top = 19.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface)
                    .clickable { showEvents = true }
                    .padding(11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Filled.Layers, contentDescription = null, tint = palette.ink, modifier = Modifier.size(13.dp))
                Text(
                    text = if (events.size == 1) events[0].name else L10n.text("schedule.events") + " · " + events.size,
                    color = palette.ink,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 110.dp)
                )
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = palette.ink, modifier = Modifier.size(11.dp))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surfaceMuted)
                .padding(4.dp)
        ) {
            ModeButton(
                label = L10n.text("schedule.all"),
                icon = Icons.Filled.Splitscreen,
                selected = !mine,
                badge = null,
                modifier = Modifier.weight(1f)
            ) { mine = false }
            ModeButton(
                label = L10n.text("schedule.mine"),
                icon = Icons.Filled.Favorite,
                selected = mine,
                badge = store.favorites.size,
                modifier = Modifier.weight(1f)
            ) { mine = true }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (value in days) {
                val selected = value == day
                Column(
                    modifier = Modifier
                        .width(68.dp)
                        .height(71.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) palette.ink else palette.surface)
                        .clickable { selectedDay = value },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        KitDate.string(value, "EEE", zone),
                        color = if (selected) palette.onInk else palette.ink,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        KitDate.string(value, "MM.dd", zone),
                        color = if (selected) palette.onInk else palette.ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (value == today) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(if (selected) palette.onInk else palette.accent, CircleShape)
                        )
                    } else {
                        Spacer(modifier = Modifier.size(4.dp))
                    }
                }
            }
        }
        FilterBar(
            mine = mine,
            kind = kind,
            conflictCount = conflictCount,
            zoom = zoom,
            zoneID = zone.id,
            onKind = { kind = it },
            onZoom = { zoom = it }
        )
        if (visibleEntries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    if (mine) Icons.Filled.Favorite else Icons.Filled.Splitscreen,
                    contentDescription = null,
                    tint = palette.accentText,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    L10n.text(if (mine) "schedule.noFavorites" else "schedule.empty"),
                    color = palette.ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    L10n.text(if (mine) "schedule.noFavoritesHint" else "schedule.emptyHint"),
                    color = palette.muted,
                    fontSize = 13.sp
                )
                if (!mine) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showImport = true },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent)
                    ) { Text(L10n.text("import.title")) }
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                TimelineBoard(
                    entries = visibleEntries,
                    allEntries = allEntries,
                    mine = mine,
                    zone = zone,
                    day = day,
                    zoom = zoom,
                    favorites = store.favorites,
                    jumpToken = jumpToken,
                    onFavorite = ::toggleFavorite,
                    onDetail = { detail = it }
                )
                Button(
                    onClick = {
                        if (today in days) selectedDay = today
                        jumpToken += 1
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.ink, contentColor = palette.onInk),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = L10n.text("schedule.jumpNow"), modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(L10n.text("schedule.now"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    toast?.let { message ->
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Text(
                message,
                color = palette.onInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(bottom = 14.dp)
                    .background(palette.ink, RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            )
        }
    }

    if (showImport) {
        ImportSheet(store) { showImport = false }
    }
    if (showEvents) {
        EventSelectionSheet(
            store = store,
            selection = selectedEventIDs,
            onSelection = { selectedEventIDs = it },
            onClose = { showEvents = false }
        )
    }
    detail?.let { entry ->
        SessionDetailSheet(entry, store, zone) { detail = null }
    }
}

@Composable
private fun ModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badge: Int?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = kitPalette()
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) palette.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) palette.accentText else palette.muted, modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            label,
            color = if (selected) palette.accentText else palette.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        if (badge != null) {
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                badge.toString(),
                color = if (selected) palette.accentText else palette.muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(if (selected) palette.accent.copy(alpha = 0.1f) else palette.surface.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun FilterBar(
    mine: Boolean,
    kind: SessionKind?,
    conflictCount: Int,
    zoom: Float,
    zoneID: String,
    onKind: (SessionKind?) -> Unit,
    onZoom: (Float) -> Unit
) {
    val palette = kitPalette()
    var zoomMenu by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (mine) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(13.dp))
                Text(L10n.text("schedule.allKinds"), color = palette.ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                if (conflictCount > 0) {
                    Text(
                        L10n.format("schedule.conflictsCount", conflictCount),
                        color = palette.warningText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                KindChip(L10n.text("schedule.allKinds"), kind == null) { onKind(null) }
                KindChip(L10n.text("schedule.live"), kind == SessionKind.live) { onKind(SessionKind.live) }
                KindChip(L10n.text("schedule.goods"), kind == SessionKind.goods) { onKind(SessionKind.goods) }
            }
            Spacer(modifier = Modifier.weight(1f))
            Box {
                Icon(
                    Icons.Filled.ZoomIn,
                    contentDescription = L10n.text("schedule.zoom"),
                    tint = palette.muted,
                    modifier = Modifier.size(20.dp).clickable { zoomMenu = true }
                )
                DropdownMenu(expanded = zoomMenu, onDismissRequest = { zoomMenu = false }) {
                    for (value in listOf(2f, 2.8f, 4f, 6f)) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "${((value / 4f * 100).roundToInt())}%",
                                    color = if (zoom == value) palette.accentText else palette.ink
                                )
                            },
                            onClick = { onZoom(value); zoomMenu = false }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row {
            Text(L10n.text("schedule.favoriteHint"), color = palette.muted, fontSize = 9.sp)
            Spacer(modifier = Modifier.weight(1f))
            Text(zoneID, color = palette.muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun KindChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = kitPalette()
    Text(
        label,
        color = if (selected) palette.ink else palette.muted,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) palette.surface else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp)
    )
}
