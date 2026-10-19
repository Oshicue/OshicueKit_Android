package com.oshicue.kit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.FestivalEvent
import com.oshicue.kit.core.FestivalSession
import com.oshicue.kit.core.KitDate
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.ScheduleFavorites
import com.oshicue.kit.core.ScheduleStore
import com.oshicue.kit.core.SessionKind
import com.oshicue.kit.core.TimelineItem
import com.oshicue.kit.core.TimelineLayout
import com.oshicue.kit.core.Venue
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class SessionEntry(val event: FestivalEvent, val session: FestivalSession) {
    val id: String get() = ScheduleFavorites.id(event.id, session.id)
    val artist: String get() = event.artist(session.artistId)?.name ?: session.artistId
    val venue: Venue get() = event.venue(session.venueId) ?: Venue(session.venueId, session.venueId, "#8F8F9B")
    val item: TimelineItem get() = TimelineItem(id, session.startsAt, session.endsAt)
}

@Composable
fun ScheduleScreen(store: ScheduleStore, modifier: Modifier = Modifier) {
    val kit = LocalKit.current
    var mine by remember { mutableStateOf(false) }
    var selectedEventIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    var kind by remember { mutableStateOf<SessionKind?>(null) }
    var showImport by remember { mutableStateOf(false) }
    var showEvents by remember { mutableStateOf(false) }
    var detail by remember { mutableStateOf<SessionEntry?>(null) }
    var zoom by remember { mutableStateOf(4.0) }
    var toast by remember { mutableStateOf<String?>(null) }
    var jumpNowToken by remember { mutableStateOf(0) }

    val document = store.document
    // Keep event selection in sync with imported or deleted timetables.
    LaunchedEffect(document.events.map { it.id }) {
        val ids = document.events.map { it.id }.toSet()
        if (selectedEventIds.isEmpty()) {
            selectedEventIds = ids
        } else {
            val newIds = ids - selectedEventIds
            val kept = selectedEventIds.intersect(ids)
            selectedEventIds = if (kept.isEmpty()) ids else kept + newIds
        }
    }

    val events = document.events.filter { it.id in selectedEventIds }
    val zone = events.firstOrNull()?.zone ?: document.events.firstOrNull()?.zone ?: ZoneId.systemDefault()
    val days = remember(events) {
        events.flatMap { it.days }.distinct().sorted()
    }
    val day = selectedDay?.takeIf { it in days } ?: days.firstOrNull()
        ?: LocalDate.now(zone)
    LaunchedEffect(days) {
        if (selectedDay == null || selectedDay !in days) {
            selectedDay = if (LocalDate.now(zone) in days) LocalDate.now(zone) else days.firstOrNull()
        }
    }
    val dayStart = day.atStartOfDay(zone).toInstant()
    val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant()

    val allEntries = remember(events, day) {
        events.flatMap { event ->
            event.sessions.filter { it.startsAt < dayEnd && it.endsAt > dayStart }
                .map { SessionEntry(event, it) }
        }.sortedBy { it.session.startsAt }
    }
    val visibleEntries = allEntries.filter { entry ->
        if (mine) store.isFavorite(entry.id) else kind == null || entry.session.kind == kind
    }
    val conflictCount = visibleEntries.indices.count { index ->
        (index + 1 until visibleEntries.size).any { other ->
            visibleEntries[index].session.startsAt < visibleEntries[other].session.endsAt &&
                visibleEntries[other].session.startsAt < visibleEntries[index].session.endsAt
        }
    }

    Column(modifier.fillMaxSize().background(kit.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandWordmark()
            Spacer(Modifier.weight(1f))
            CircleIconButton(Icons.Filled.FileDownload, L10n.text("import.title")) { showImport = true }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(5.dp).clip(CircleShape).background(kit.accent))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (events.size > 1) L10n.text("schedule.allEvents")
                        else events.firstOrNull()?.city ?: "OSHICUE KIT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                        color = kit.muted
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    L10n.text("schedule.title"),
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1).sp,
                    color = kit.ink
                )
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(kit.surface)
                    .border(1.dp, kit.line, RoundedCornerShape(12.dp))
                    .clickable { showEvents = true }
                    .padding(horizontal = 11.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (events.size == 1) events[0].name else L10n.text("schedule.events") + " · " + events.size,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = kit.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(5.dp))
                Text("▾", fontSize = 9.sp, color = kit.ink)
            }
        }
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(kit.surfaceMuted)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ModeButton(label = L10n.text("schedule.all"), selected = !mine, modifier = Modifier.weight(1f)) {
                mine = false
            }
            ModeButton(
                label = L10n.text("schedule.mine"),
                selected = mine,
                count = store.favorites.size,
                modifier = Modifier.weight(1f)
            ) { mine = true }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            days.forEach { value ->
                val selected = value == day
                val isToday = value == LocalDate.now(zone)
                Column(
                    modifier = Modifier
                        .width(68.dp)
                        .height(71.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (selected) kit.ink else kit.surface)
                        .clickable { selectedDay = value },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        KitDate.string(value.atTime(12, 0).atZone(zone).toInstant(), "EEE", zone),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (selected) kit.onInk else kit.ink
                    )
                    Text(
                        KitDate.string(value.atTime(12, 0).atZone(zone).toInstant(), "MM.dd", zone),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) kit.onInk else kit.ink
                    )
                    Box(
                        Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    !isToday -> Color.Transparent
                                    selected -> kit.onInk
                                    else -> kit.accent
                                }
                            )
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (mine) {
                Icon(Icons.Filled.Favorite, null, tint = kit.accentText, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text(L10n.text("schedule.allKinds"), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
                if (conflictCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        L10n.format("schedule.conflictsCount", conflictCount),
                        fontSize = 10.sp,
                        color = kit.warningText
                    )
                }
            } else {
                FilterChip(L10n.text("schedule.allKinds"), kind == null) { kind = null }
                Spacer(Modifier.width(4.dp))
                FilterChip(L10n.text("schedule.live"), kind == SessionKind.Live) { kind = SessionKind.Live }
                Spacer(Modifier.width(4.dp))
                FilterChip(L10n.text("schedule.goods"), kind == SessionKind.Goods) { kind = SessionKind.Goods }
            }
            Spacer(Modifier.weight(1f))
            ZoomMenu(zoom) { zoom = it }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                L10n.text("schedule.favoriteHint"),
                fontSize = 9.sp,
                color = kit.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(6.dp))
            Text(zone.id, fontSize = 9.sp, color = kit.muted, maxLines = 1)
        }
        if (visibleEntries.isEmpty()) {
            EmptyState(
                title = L10n.text(if (mine) "schedule.noFavorites" else "schedule.empty"),
                hint = L10n.text(if (mine) "schedule.noFavoritesHint" else "schedule.emptyHint"),
                symbol = if (mine) Icons.Filled.FavoriteBorder else Icons.Filled.LocationOn,
                action = if (!mine) {
                    { KitPrimaryButton(L10n.text("import.title")) { showImport = true } }
                } else null
            )
        } else {
            Box(Modifier.weight(1f)) {
                TimelineBoard(
                    entries = visibleEntries,
                    allEntries = allEntries,
                    mine = mine,
                    zone = zone,
                    day = day,
                    zoom = zoom,
                    favorites = store.favorites,
                    jumpToken = jumpNowToken,
                    onFavorite = { entry ->
                        store.toggleFavorite(entry.id)
                        val added = store.isFavorite(entry.id)
                        toast = L10n.text(if (added) "schedule.favoriteAdded" else "schedule.favoriteRemoved") +
                            " · " + entry.artist + " · " +
                            L10n.text(if (entry.session.kind == SessionKind.Live) "schedule.live" else "schedule.goods")
                    },
                    onDetail = { detail = it }
                )
                androidx.compose.material3.FloatingActionButton(
                    onClick = {
                        val today = LocalDate.now(zone)
                        if (today in days) selectedDay = today
                        jumpNowToken += 1
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                    containerColor = kit.ink,
                    contentColor = kit.onInk
                ) {
                    Row(Modifier.padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(L10n.text("schedule.now"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    val message = toast
    if (message != null) {
        LaunchedEffect(message) {
            delay(2000)
            toast = null
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Text(
                message,
                modifier = Modifier
                    .padding(bottom = 76.dp)
                    .clip(CircleShape)
                    .background(kit.ink)
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                color = kit.onInk,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }

    if (showImport) {
        ScheduleImportSheet(store = store, onClose = { showImport = false })
    }
    if (showEvents) {
        EventSelectionSheet(
            store = store,
            selection = selectedEventIds,
            onSelectionChange = { selectedEventIds = it },
            onClose = { showEvents = false }
        )
    }
    detail?.let { entry ->
        SessionDetailSheet(
            entry = entry,
            store = store,
            zone = zone,
            onClose = { detail = null }
        )
    }
}

@Composable
private fun ModeButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    count: Int? = null,
    onClick: () -> Unit
) {
    val kit = LocalKit.current
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) kit.surface else Color.Transparent)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (selected && label == L10n.text("schedule.mine")) {
            Icon(Icons.Filled.Favorite, null, tint = kit.accentText, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) kit.accentText else kit.muted,
            maxLines = 1
        )
        if (count != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (selected) kit.accent.copy(alpha = 0.1f) else kit.surface.copy(alpha = 0.5f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(count.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (selected) kit.accentText else kit.muted)
            }
        }
    }
}

@Composable
private fun ZoomMenu(zoom: Double, onSelect: (Double) -> Unit) {
    val kit = LocalKit.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⤢", fontSize = 14.sp, color = kit.muted)
            Spacer(Modifier.width(4.dp))
            Text(((zoom / 4.0) * 100).roundToInt().toString() + "%", fontSize = 10.sp, color = kit.muted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(2.0, 2.8, 4.0, 6.0).forEach { value ->
                DropdownMenuItem(
                    text = { Text(((value / 4.0) * 100).roundToInt().toString() + "%") },
                    leadingIcon = if (zoom == value) {
                        { Icon(Icons.Filled.Check, null, tint = kit.accentText) }
                    } else null,
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}

private data class VenueColumn(val event: FestivalEvent, val venue: Venue, val kind: SessionKind) {
    val id: String
        get() = "${event.id.toByteArray(Charsets.UTF_8).size}:${event.id}" +
            "${venue.id.toByteArray(Charsets.UTF_8).size}:${venue.id}${kind.rawValue}"
}

private data class VenueTrack(
    val column: VenueColumn,
    val entries: List<SessionEntry>,
    val layout: com.oshicue.kit.core.TimelineColumnLayout
) {
    val widthDp: Double get() = layout.width
}

@Composable
private fun TimelineBoard(
    entries: List<SessionEntry>,
    allEntries: List<SessionEntry>,
    mine: Boolean,
    zone: ZoneId,
    day: LocalDate,
    zoom: Double,
    favorites: Set<String>,
    jumpToken: Int,
    onFavorite: (SessionEntry) -> Unit,
    onDetail: (SessionEntry) -> Unit
) {
    val kit = LocalKit.current
    val density = LocalDensity.current
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    var viewportWidthPx by remember { mutableStateOf(0) }
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = Instant.now()
        }
    }

    val dayStart = day.atStartOfDay(zone).toInstant()
    val dayEnd = day.plusDays(1).atStartOfDay(zone).toInstant()
    val firstInstant = allEntries.minOfOrNull { it.session.startsAt } ?: dayStart
    val lastInstant = allEntries.maxOfOrNull { it.session.endsAt } ?: dayStart.plusSeconds(3600)
    val start = KitDate.startOfHour(maxOf(firstInstant, dayStart), zone)
    val clippedEnd = minOf(lastInstant, dayEnd)
    val endHour = KitDate.startOfHour(clippedEnd, zone)
    val roundedEnd = if (clippedEnd == endHour) clippedEnd else KitDate.startOfNextHour(clippedEnd, zone)
    val end = minOf(maxOf(roundedEnd, start.plusSeconds(3600)), dayEnd)

    val totalMinutes = (end.toEpochMilli() - start.toEpochMilli()) / 60_000.0
    val railWidth = 46.dp
    val headerHeight = 82.dp
    val minimumLaneWidth = 180.0

    fun yOffset(instant: Instant): Double = ((instant.toEpochMilli() - start.toEpochMilli()) / 60_000.0) * zoom

    fun blockHeight(entry: SessionEntry): Double {
        val from = maxOf(entry.session.startsAt, start)
        val to = minOf(entry.session.endsAt, end)
        return max(((to.toEpochMilli() - from.toEpochMilli()) / 60_000.0) * zoom - 3, 1.0)
    }

    fun readableLaneWidth(values: List<SessionEntry>): Double =
        values.fold(minimumLaneWidth) { width, entry ->
            max(width, laneWidthFor(entry.artist, blockHeight(entry), minimumLaneWidth, 11.0))
        }

    val tracks = remember(entries, zoom) {
        val result = mutableListOf<VenueTrack>()
        listOf(SessionKind.Live, SessionKind.Goods).forEach { sessionKind ->
            val uniqueEvents = allEntries.map { it.event }.distinctBy { it.id }
            for (event in uniqueEvents) {
                for (venue in event.venues) {
                    if (entries.none { it.event.id == event.id && it.session.venueId == venue.id && it.session.kind == sessionKind }) continue
                    val values = entries.filter {
                        it.event.id == event.id && it.session.venueId == venue.id && it.session.kind == sessionKind
                    }
                    val laneWidth = readableLaneWidth(values)
                    result.add(
                        VenueTrack(
                            VenueColumn(event, venue, sessionKind),
                            values,
                            TimelineLayout.column(values.map { it.item }, laneWidth, laneWidth)
                        )
                    )
                }
            }
        }
        result
    }

    val myLayout = remember(entries, zoom, viewportWidthPx) {
        val width = max(viewportWidthPx.toDouble() / density.density - railWidth.value, 1.0)
        TimelineLayout.column(entries.map { it.item }, width, readableLaneWidth(entries))
    }
    val contentWidth = if (mine) myLayout.width else tracks.sumOf { it.widthDp }
    val contentHeight = totalMinutes * zoom

    // A jump scrolls to the current time when it is inside the displayed range.
    LaunchedEffect(jumpToken, day) {
        val nowVisible = now >= start && now <= end
        val target = if (nowVisible) yOffset(now) else 0.0
        val targetPx = with(density) { target.dp.toPx() }.toInt()
        verticalScroll.animateScrollTo(max(0, targetPx - 220))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(kit.surface)
            .onSizeChanged { viewportWidthPx = it.width }
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(verticalScroll)) {
            Row(Modifier.fillMaxWidth()) {
                // Fixed-width time rail
                Column(Modifier.width(railWidth)) {
                    Box(
                        Modifier.fillMaxWidth().height(headerHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            KitDate.string(dayStart, "MMM", zone).uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = kit.muted
                        )
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(contentHeight.dp)
                            .background(kit.background)
                    ) {
                        var tick = start
                        while (tick <= end) {
                            Text(
                                KitDate.time(tick, zone),
                                modifier = Modifier
                                    .width(railWidth - 5.dp)
                                    .offset(y = (yOffset(tick) + 3).dp)
                                    .padding(end = 4.dp),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = kit.muted,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                            tick = tick.plusSeconds(1800)
                        }
                        if (now in start..end) {
                            Box(
                                Modifier
                                    .offset(y = (yOffset(now) - 4).dp)
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(kit.accent)
                            )
                        }
                    }
                }
                // Horizontally scrollable venue columns; shares the vertical scroll above.
                Box(
                    Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScroll)
                ) {
                    Column(Modifier.width(contentWidth.dp)) {
                        Row(Modifier.fillMaxWidth().height(headerHeight)) {
                            if (mine) {
                                Row(
                                    Modifier.fillMaxSize().background(kit.background).padding(horizontal = 15.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.Favorite, null, tint = kit.accentText, modifier = Modifier.size(13.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(L10n.text("schedule.mine"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
                                    Spacer(Modifier.weight(1f))
                                    Text(L10n.text("schedule.allKinds"), fontSize = 10.sp, color = kit.muted)
                                }
                            } else {
                                Row(Modifier.fillMaxSize()) {
                                    tracks.forEach { track ->
                                        Row(
                                            Modifier
                                                .width(track.widthDp.dp)
                                                .height(headerHeight)
                                        ) {
                                            repeat(track.layout.maximumLaneCount) {
                                                Column(
                                                    Modifier
                                                        .weight(1f)
                                                        .fillMaxSize()
                                                        .background(kit.surface)
                                                ) {
                                                    Row(
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .height(26.dp)
                                                            .background(
                                                                if (track.column.kind == SessionKind.Live) kit.surfaceSubtle
                                                                else kit.accent.copy(alpha = 0.07f)
                                                            )
                                                            .padding(horizontal = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            L10n.text(
                                                                if (track.column.kind == SessionKind.Live) "schedule.live" else "schedule.goods"
                                                            ),
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (track.column.kind == SessionKind.Live) kit.ink else kit.accentText
                                                        )
                                                    }
                                                    Row(
                                                        Modifier.fillMaxWidth().weight(1f).padding(horizontal = 9.dp, vertical = 5.dp)
                                                    ) {
                                                        Box(
                                                            Modifier
                                                                .width(4.dp)
                                                                .height(29.dp)
                                                                .clip(RoundedCornerShape(2.dp))
                                                                .background(hexToColor(track.column.venue.color))
                                                        )
                                                        Spacer(Modifier.width(6.dp))
                                                        Column {
                                                            Text(
                                                                track.column.venue.name,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = kit.ink,
                                                                maxLines = 2,
                                                                lineHeight = 12.sp
                                                            )
                                                            Text(track.column.event.city, fontSize = 8.sp, color = kit.muted, maxLines = 1)
                                                        }
                                                    }
                                                }
                                                Box(Modifier.width(1.dp).fillMaxSize().background(kit.line))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(contentHeight.dp)
                                .background(kit.surface)
                        ) {
                            // Hour grid
                            var tick = start
                            while (tick <= end) {
                                Box(
                                    Modifier
                                        .offset(y = yOffset(tick).dp)
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(kit.line)
                                )
                                val half = tick.plusSeconds(900)
                                if (half < end) {
                                    Box(
                                        Modifier
                                            .offset(y = yOffset(half).dp)
                                            .fillMaxWidth()
                                            .height(0.5.dp)
                                            .background(kit.line.copy(alpha = 0.4f))
                                    )
                                }
                                tick = tick.plusSeconds(1800)
                            }

                            if (mine) {
                                val placements = myLayout.placements.associateBy { it.id }
                                entries.forEach { entry ->
                                    val position = placements[entry.id] ?: return@forEach
                                    val laneWidth = contentWidth / position.laneCount
                                    SessionBlock(
                                        entry = entry,
                                        favorite = entry.id in favorites,
                                        merged = true,
                                        widthDp = max(laneWidth - 6, 1.0),
                                        heightDp = blockHeight(entry),
                                        zone = zone,
                                        xDp = position.lane * laneWidth + 3,
                                        yDp = yOffset(maxOf(entry.session.startsAt, start)) + 2,
                                        onFavorite = onFavorite,
                                        onDetail = onDetail
                                    )
                                }
                            } else {
                                var origin = 0.0
                                tracks.forEach { track ->
                                    val positions = track.layout.placements.associateBy { it.id }
                                    track.entries.forEach { entry ->
                                        val position = positions[entry.id] ?: return@forEach
                                        SessionBlock(
                                            entry = entry,
                                            favorite = entry.id in favorites,
                                            merged = false,
                                            widthDp = max(position.relativeWidth * track.widthDp - 6, 1.0),
                                            heightDp = blockHeight(entry),
                                            zone = zone,
                                            xDp = origin + position.relativeX * track.widthDp + 3,
                                            yDp = yOffset(maxOf(entry.session.startsAt, start)) + 2,
                                            onFavorite = onFavorite,
                                            onDetail = onDetail
                                        )
                                    }
                                    origin += track.widthDp
                                }
                            }

                            if (now in start..end) {
                                Box(
                                    Modifier
                                        .offset(y = yOffset(now).dp)
                                        .fillMaxWidth()
                                        .height(1.5.dp)
                                        .background(kit.accent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionBlock(
    entry: SessionEntry,
    favorite: Boolean,
    merged: Boolean,
    widthDp: Double,
    heightDp: Double,
    zone: ZoneId,
    xDp: Double,
    yDp: Double,
    onFavorite: (SessionEntry) -> Unit,
    onDetail: (SessionEntry) -> Unit
) {
    val kit = LocalKit.current
    val color = hexToColor(entry.venue.color)
    val inset = when {
        heightDp < 24 -> 1.0
        heightDp < 44 -> 2.0
        else -> 6.0
    }
    val availableHeight = max(heightDp - inset * 2, 1.0)
    val timeHeight = 12.0
    val titleSize = fitArtistSize(entry.artist, widthDp - 19 - if (favorite) 12 else 0, availableHeight, if (merged) 15.0 else 12.0)
    val titleHeight = max(textHeightFor(entry.artist, widthDp - 19, titleSize), titleSize * 1.25)
    val showTime = availableHeight - titleHeight >= timeHeight + 3
    val remaining = availableHeight - titleHeight - if (showTime) timeHeight + 3 else 0.0
    val showVenue = merged && remaining >= 27
    val showArea = remaining - (if (showVenue) 27.0 else 0.0) >= 19

    Box(
        Modifier
            .offset(x = xDp.dp, y = yDp.dp)
            .width(widthDp.dp)
            .height(heightDp.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(color.copy(alpha = 0.21f))
            .pointerInput(entry.id) {
                detectTapGestures(
                    onTap = { onDetail(entry) },
                    onDoubleTap = { onFavorite(entry) },
                    onLongPress = { onFavorite(entry) }
                )
            }
    ) {
        Row(Modifier.fillMaxSize()) {
            Box(Modifier.width(3.dp).fillMaxSize().background(color))
            Column(Modifier.weight(1f).padding(horizontal = 8.dp, vertical = inset.dp)) {
                if (showTime) {
                    Text(
                        KitDate.time(entry.session.startsAt, zone) + "–" + KitDate.time(entry.session.endsAt, zone),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = kit.ink,
                        maxLines = 1,
                        lineHeight = 10.sp
                    )
                }
                Text(
                    entry.artist,
                    fontSize = titleSize.sp,
                    fontWeight = FontWeight.Bold,
                    color = kit.ink,
                    lineHeight = (titleSize * 1.05).sp,
                    maxLines = max(1, (titleHeight / (titleSize * 1.15)).toInt()),
                    overflow = TextOverflow.Ellipsis
                )
                if (showVenue) {
                    Text(
                        L10n.text(if (entry.session.kind == SessionKind.Live) "schedule.live" else "schedule.goods") +
                            " · " + entry.venue.name,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = kit.ink,
                        maxLines = 2,
                        lineHeight = 11.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (showArea) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        entry.session.area ?: entry.event.name,
                        fontSize = 8.sp,
                        color = kit.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (favorite) {
            Icon(
                Icons.Filled.Favorite,
                null,
                tint = kit.accentText,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(10.dp)
            )
        }
    }
}

/** Approximate UIKit text measurement: CJK characters take a full em, Latin roughly half. */
private fun textWidthFactor(text: String): Double {
    var factor = 0.0
    for (character in text) {
        factor += if (character.code > 0x2E80) 1.0 else 0.55
    }
    return max(factor, 1.0)
}

private fun textHeightFor(artist: String, widthDp: Double, sizeDp: Double): Double {
    val perLine = max(widthDp / (sizeDp * 0.55), 1.0)
    val lines = ceil(textWidthFactor(artist) * sizeDp / max(widthDp - 6, 1.0)).coerceAtLeast(1.0)
    return lines * sizeDp * 1.15
}

private fun fitArtistSize(artist: String, widthDp: Double, heightDp: Double, preferred: Double): Double {
    var size = preferred
    val minimum = preferred * 11.0 / 12.0
    while (size > minimum && textHeightFor(artist, widthDp, size) > heightDp) {
        size = max(size - 0.5, minimum)
    }
    return size
}

private fun laneWidthFor(artist: String, heightDp: Double, minimumWidth: Double, minimumFont: Double): Double {
    val availableHeight = max(heightDp - (if (heightDp < 24) 2 else if (heightDp < 44) 4 else 12), 1.0)
    var width = minimumWidth
    val maximumWidth = minimumWidth * 320 / 180
    while (width < maximumWidth && textHeightFor(artist, width - 37, minimumFont) > availableHeight) {
        width = min(width + 20, maximumWidth)
    }
    return width
}
