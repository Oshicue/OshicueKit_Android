package com.oshicue.kit.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.FestivalEvent
import com.oshicue.kit.core.SessionKind
import com.oshicue.kit.core.TimelineColumnLayout
import com.oshicue.kit.core.TimelineLayout
import com.oshicue.kit.core.Venue
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.hexColor
import com.oshicue.kit.ui.theme.kitPalette
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class VenueColumn(val event: FestivalEvent, val venue: Venue, val kind: SessionKind) {
    val id: String
        get() = "${event.id.toByteArray(Charsets.UTF_8).size}:${event.id}${venue.id.toByteArray(Charsets.UTF_8).size}:${venue.id}${kind.raw}"
}

private data class VenueTrack(
    val column: VenueColumn,
    val entries: List<SessionEntry>,
    val layout: TimelineColumnLayout
)

@Composable
fun TimelineBoard(
    entries: List<SessionEntry>,
    allEntries: List<SessionEntry>,
    mine: Boolean,
    zone: ZoneId,
    day: LocalDate,
    zoom: Float,
    favorites: Set<String>,
    jumpToken: Int,
    onFavorite: (SessionEntry) -> Unit,
    onDetail: (SessionEntry) -> Unit
) {
    val palette = kitPalette()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val railWidth = 46.dp
    val headerHeight = 82.dp
    val fontScale = density.fontScale
    val minimumLaneWidth = 180f * fontScale
    val artistSize = 12f
    val myArtistSize = 15f
    val textMeasurer = rememberTextMeasurer(cacheSize = 256)
    val typography = remember(textMeasurer, density.density, fontScale) {
        TimelineTypography(textMeasurer, density.density)
    }

    val dayStart = remember(day, zone) { day.atStartOfDay(zone).toInstant() }
    val dayEnd = remember(day, zone) { day.plusDays(1).atStartOfDay(zone).toInstant() }
    val start = remember(allEntries, dayStart, zone) {
        val first = allEntries.minOfOrNull { it.session.startsAt } ?: dayStart
        val clipped = if (first > dayStart) first else dayStart
        clipped.atZone(zone).truncatedTo(ChronoUnit.HOURS).toInstant()
    }
    val end = remember(allEntries, dayEnd, start, zone) {
        val last = allEntries.maxOfOrNull { it.session.endsAt } ?: start.plusSeconds(3600)
        val clipped = if (last < dayEnd) last else dayEnd
        val hour = clipped.atZone(zone).truncatedTo(ChronoUnit.HOURS).toInstant()
        val rounded = if (clipped == hour) clipped else hour.plusSeconds(3600)
        minOf(maxOf(rounded, start.plusSeconds(3600)), dayEnd)
    }
    val totalMinutes = Duration.between(start, end).toMinutes().coerceAtLeast(0)
    val heightDp = totalMinutes * zoom
    val ticks = remember(start, end) {
        val values = mutableListOf<Instant>()
        var tick = start
        while (tick <= end) {
            values.add(tick)
            tick = tick.plusSeconds(1800)
        }
        values
    }

    fun y(date: Instant): Float = (Duration.between(start, date).toMillis() / 60000.0).toFloat() * zoom
    fun isNowVisible(now: Instant): Boolean = now >= start && now <= end
    fun blockHeight(entry: SessionEntry): Float {
        val clippedStart = maxOf(entry.session.startsAt, start)
        val clippedEnd = minOf(entry.session.endsAt, end)
        return max(Duration.between(clippedStart, clippedEnd).toMillis() / 60000.0f * zoom - 3f, 1f)
    }

    val columns = remember(allEntries, entries) {
        val result = mutableListOf<VenueColumn>()
        for (kind in listOf(SessionKind.live, SessionKind.goods)) {
            val uniqueEvents = mutableListOf<FestivalEvent>()
            for (entry in allEntries) {
                if (uniqueEvents.none { it.id == entry.event.id }) uniqueEvents.add(entry.event)
            }
            for (event in uniqueEvents) {
                for (venue in event.venues) {
                    if (entries.any { it.event.id == event.id && it.session.venueID == venue.id && it.session.kind == kind }) {
                        result.add(VenueColumn(event, venue, kind))
                    }
                }
            }
        }
        result
    }

    fun readableLaneWidth(values: List<SessionEntry>): Float =
        values.fold(minimumLaneWidth) { width, entry ->
            max(
                width,
                typography.laneWidth(
                    artist = entry.artist,
                    height = blockHeight(entry),
                    minimumWidth = minimumLaneWidth,
                    minimumFontSize = artistSize * 11 / 12
                )
            )
        }

    val tracks = remember(entries, columns, zoom, typography) {
        columns.map { column ->
            val values = entries.filter {
                it.event.id == column.event.id && it.session.venueID == column.venue.id && it.session.kind == column.kind
            }
            val laneWidth = readableLaneWidth(values)
            VenueTrack(
                column = column,
                entries = values,
                layout = TimelineLayout.column(values.map { it.item }, laneWidth.toDouble(), laneWidth.toDouble())
            )
        }
    }

    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(15_000)
            now = Instant.now()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
            .padding(top = 1.dp)
    ) {
        val viewportWidth = maxWidth
        val viewportHeight = maxHeight
        val myLayout = TimelineLayout.column(
            entries.map { it.item },
            max((viewportWidth - railWidth).value.toDouble() * density.density, 1.0),
            readableLaneWidth(entries).toDouble()
        )
        val contentWidth = if (mine) {
            (myLayout.width / density.density).dp
        } else {
            tracks.fold(0.dp) { sum, track -> sum + track.layout.width.dp }
        }
        val vertical = rememberScrollState()
        val horizontal = rememberScrollState()

        fun scrollToNow(animate: Boolean) {
            val offset = ((y(now).toDouble() * density.density) - viewportHeight.value * density.density / 2).toInt()
            scope.launch {
                if (animate) vertical.animateScrollTo(offset.coerceAtLeast(0)) else vertical.scrollTo(offset.coerceAtLeast(0))
            }
        }

        LaunchedEffect(jumpToken) {
            if (isNowVisible(now)) scrollToNow(true) else scope.launch { vertical.animateScrollTo(0) }
        }
        LaunchedEffect(day) {
            if (isNowVisible(now)) scrollToNow(false) else scope.launch { vertical.scrollTo(0) }
        }

        CompositionLocalProvider(LocalTextStyle provides TextStyle.Default) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.verticalScroll(vertical).width(railWidth)) {
                Text(
                    KitDate.string(day, "MMM", zone),
                    color = palette.muted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(railWidth).height(headerHeight).padding(start = 8.dp, top = 12.dp)
                )
                Box(modifier = Modifier.width(railWidth).height(heightDp.dp).background(palette.background)) {
                    for (tick in ticks) {
                        Text(
                            KitDate.time(tick, zone),
                            color = palette.muted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .offset(x = 2.dp, y = (y(tick) + 3).dp)
                                .width(railWidth - 5.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                        )
                    }
                    if (isNowVisible(now)) {
                        Text(
                            KitDate.time(now, zone),
                            color = palette.onAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .offset(x = 1.dp, y = (y(now) - 8).dp)
                                .background(palette.accentFill, RoundedCornerShape(4.dp))
                                .padding(horizontal = 3.dp, vertical = 3.dp)
                        )
                        Box(
                            modifier = Modifier
                                .offset(x = railWidth - 5.dp, y = (y(now) - 3.5f).dp)
                                .size(7.dp)
                                .background(palette.accent, androidx.compose.foundation.shape.CircleShape)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .width((viewportWidth - railWidth).coerceAtLeast(1.dp))
                    .verticalScroll(vertical)
                    .horizontalScroll(horizontal)
            ) {
                BoardHeaders(mine, tracks, contentWidth, headerHeight)
                Box(modifier = Modifier.width(contentWidth).height(heightDp.dp)) {
                    for (tick in ticks) {
                        Box(
                            modifier = Modifier
                                .offset(y = y(tick).dp)
                                .width(contentWidth)
                                .height(1.dp)
                                .background(palette.line)
                        )
                    }
                    var quarter = start.plusSeconds(900)
                    while (quarter < end) {
                        Box(
                            modifier = Modifier
                                .offset(y = y(quarter).dp)
                                .width(contentWidth)
                                .height(0.5.dp)
                                .background(palette.line.copy(alpha = 0.4f))
                        )
                        quarter = quarter.plusSeconds(1800)
                    }
                    if (mine) {
                        val placements = myLayout.placements.associateBy { it.id }
                        for (entry in entries) {
                            val placement = placements[entry.id] ?: continue
                            val laneWidth = contentWidth / placement.laneCount
                            SessionBlock(
                                entry = entry,
                                favorite = favorites.contains(entry.id),
                                merged = true,
                                width = (laneWidth - 6.dp).coerceAtLeast(1.dp),
                                height = blockHeight(entry),
                                zone = zone,
                                preferredArtistSize = myArtistSize,
                                minimumArtistSize = artistSize * 11 / 12,
                                typography = typography,
                                onTap = { onDetail(entry) },
                                onDoubleTap = { onFavorite(entry) },
                                modifier = Modifier.offset(x = laneWidth * placement.lane + 3.dp, y = (y(entry.session.startsAt) + 2).dp)
                            )
                        }
                    } else {
                        var origin = 0.dp
                        for (track in tracks) {
                            val trackWidth = track.layout.width.dp
                            val placements = track.layout.placements.associateBy { it.id }
                            for (entry in track.entries) {
                                val placement = placements[entry.id] ?: continue
                                SessionBlock(
                                    entry = entry,
                                    favorite = favorites.contains(entry.id),
                                    merged = false,
                                    width = (placement.relativeWidth * trackWidth.value).dp - 6.dp,
                                    height = blockHeight(entry),
                                    zone = zone,
                                    preferredArtistSize = artistSize,
                                    minimumArtistSize = artistSize * 11 / 12,
                                    typography = typography,
                                    onTap = { onDetail(entry) },
                                    onDoubleTap = { onFavorite(entry) },
                                    modifier = Modifier.offset(
                                        x = origin + (placement.relativeX * trackWidth.value).dp + 3.dp,
                                        y = (y(entry.session.startsAt) + 2).dp
                                    )
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .offset(x = origin + trackWidth - 1.dp)
                                    .width(1.dp)
                                    .height(heightDp.dp)
                                    .background(palette.line.copy(alpha = 0.6f))
                            )
                            origin += trackWidth
                        }
                    }
                    if (isNowVisible(now)) {
                        Box(
                            modifier = Modifier
                                .offset(y = y(now).dp)
                                .width(contentWidth)
                                .height(1.5.dp)
                                .background(palette.accent)
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun BoardHeaders(mine: Boolean, tracks: List<VenueTrack>, contentWidth: Dp, headerHeight: Dp) {
    val palette = kitPalette()
    if (mine) {
        Row(
            modifier = Modifier.width(contentWidth).height(headerHeight).background(palette.background).padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Filled.Favorite, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(14.dp))
            Text(L10n.text("schedule.mine"), color = palette.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.weight(1f))
            Text(L10n.text("schedule.allKinds"), color = palette.muted, fontSize = 10.sp)
        }
    } else {
        Row(modifier = Modifier.height(headerHeight)) {
            for (track in tracks) {
                val column = track.column
                val trackWidth = track.layout.width.dp
                val lanes = track.layout.maximumLaneCount
                Row(modifier = Modifier.width(trackWidth)) {
                    repeat(lanes) {
                        Column(
                            modifier = Modifier
                                .width(trackWidth / lanes)
                                .background(palette.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .background(if (column.kind == SessionKind.live) palette.surfaceSubtle else palette.accent.copy(alpha = 0.07f))
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (column.kind == SessionKind.live) Icons.Filled.GraphicEq else Icons.Filled.ShoppingBag,
                                    contentDescription = null,
                                    tint = if (column.kind == SessionKind.live) palette.ink else palette.accentText,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    L10n.text(if (column.kind == SessionKind.live) "schedule.live" else "schedule.goods"),
                                    color = if (column.kind == SessionKind.live) palette.ink else palette.accentText,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .padding(horizontal = 9.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .width(4.dp)
                                        .height(29.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(hexColor(column.venue.color))
                                )
                                Column(modifier = Modifier.padding(top = 8.dp)) {
                                    Text(
                                        column.venue.name,
                                        color = palette.ink,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        column.event.city,
                                        color = palette.muted,
                                        fontSize = 8.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
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
    width: Dp,
    height: Float,
    zone: ZoneId,
    preferredArtistSize: Float,
    minimumArtistSize: Float,
    typography: TimelineTypography,
    onTap: () -> Unit,
    onDoubleTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = kitPalette()
    val inset = TimelineTypography.verticalInset(height)
    val textWidth = max(width.value - 19 - (if (favorite) 12 else 0), 1f)
    val availableHeight = max(height - inset * 2, 1f)
    val title = typography.fit(
        artist = entry.artist,
        width = textWidth,
        height = availableHeight,
        preferredSize = preferredArtistSize,
        minimumSize = minimumArtistSize
    )
    val timeHeight = typography.timeLineHeight()
    val showTime = availableHeight - title.height >= timeHeight + 3
    val remaining = availableHeight - title.height - (if (showTime) timeHeight + 3 else 0f)
    val venueText = L10n.text(if (entry.session.kind == SessionKind.live) "schedule.live" else "schedule.goods") +
        " · " + entry.venue.name
    // The venue row reserves its own measured two-line height; CJK fallback metrics can exceed the iOS 24pt budget.
    val venueWidth = max(width.value - 34, 1f)
    val venueHeight = if (merged) typography.wrappedHeight(venueText, venueWidth, 10f, FontWeight.Medium, maxLines = 2) else 0f
    val showVenue = merged && remaining >= venueHeight + 3
    val showArea = remaining - (if (showVenue) venueHeight + 3 else 0f) >= 19
    val venueColor = hexColor(entry.venue.color)

    Box(
        modifier = modifier
            .width(width)
            .height(height.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(venueColor.copy(alpha = 0.21f))
            .pointerInput(entry.id) {
                detectTapGestures(onTap = { onTap() }, onDoubleTap = { onDoubleTap() })
            }
    ) {
        Row(modifier = Modifier.fillMaxWidth().height(height.dp)) {
            Box(modifier = Modifier.width(3.dp).height(height.dp).background(venueColor))
            Column(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(vertical = inset.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                if (showTime) {
                    Text(
                        KitDate.time(entry.session.startsAt, zone) + "–" + KitDate.time(entry.session.endsAt, zone),
                        color = palette.ink,
                        style = typography.labelStyle(9f, FontWeight.Medium, monospace = true),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.height(timeHeight.dp)
                    )
                }
                Text(
                    entry.artist,
                    color = palette.ink,
                    style = typography.labelStyle(title.fontSize),
                    maxLines = if (title.lineCount == 0) Int.MAX_VALUE else title.lineCount,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = true,
                    modifier = Modifier.height(title.height.dp)
                )
                if (showVenue) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.height(venueHeight.dp)
                    ) {
                        Icon(
                            if (entry.session.kind == SessionKind.live) Icons.Filled.GraphicEq else Icons.Filled.ShoppingBag,
                            contentDescription = null,
                            tint = palette.ink,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            venueText,
                            color = palette.ink,
                            style = typography.labelStyle(10f, FontWeight.Medium),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (showArea) {
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        entry.session.area ?: entry.event.name,
                        color = palette.ink,
                        style = typography.labelStyle(8f, FontWeight.Normal),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        if (favorite) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                tint = palette.accentText,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(10.dp)
            )
        }
    }
}

/**
 * Measures with the same Compose text engine that renders the labels, so the fitted font size,
 * wrapped line count and reserved heights match the drawn text exactly.
 */
@Immutable
class TimelineTypography(
    private val measurer: TextMeasurer,
    private val density: Float
) {
    data class Fit(val fontSize: Float, val height: Float, val lineCount: Int)

    fun labelStyle(size: Float, weight: FontWeight = FontWeight.Bold, monospace: Boolean = false): TextStyle =
        TextStyle(
            fontSize = size.sp,
            fontWeight = weight,
            fontFamily = if (monospace) FontFamily.Monospace else null,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )

    /** Height of [text] wrapped into at most [maxLines] lines at [width], measured by the rendering engine. */
    fun wrappedHeight(text: String, width: Float, size: Float, weight: FontWeight = FontWeight.Bold, maxLines: Int = Int.MAX_VALUE): Float {
        val result = measurer.measure(
            text = AnnotatedString(text),
            style = labelStyle(size, weight),
            maxLines = maxLines,
            constraints = Constraints(maxWidth = max(1f, width * density).toInt())
        )
        return result.size.height / density
    }

    fun textHeight(artist: String, width: Float, size: Float, weight: FontWeight = FontWeight.Bold): Float {
        val result = measurer.measure(
            text = AnnotatedString(artist),
            style = labelStyle(size, weight),
            constraints = Constraints(maxWidth = max(1f, width * density).toInt())
        )
        return result.size.height / density
    }

    fun singleLineHeight(size: Float, weight: FontWeight = FontWeight.Bold): Float {
        val result = measurer.measure(text = AnnotatedString("Ag"), style = labelStyle(size, weight))
        return result.size.height / density
    }

    fun timeLineHeight(): Float {
        val result = measurer.measure(
            text = AnnotatedString("00:00"),
            style = labelStyle(9f, FontWeight.Medium, monospace = true)
        )
        return result.size.height / density
    }

    fun fit(artist: String, width: Float, height: Float, preferredSize: Float, minimumSize: Float): Fit {
        var size = preferredSize
        while (size > minimumSize && textHeight(artist, width, size) > height) {
            size = max(size - 0.5f, minimumSize)
        }
        val naturalHeight = textHeight(artist, width, size)
        val lineHeight = singleLineHeight(size)
        return Fit(
            fontSize = size,
            height = min(max(naturalHeight, lineHeight), height),
            lineCount = if (naturalHeight <= height) 0 else max(1, (height / lineHeight).toInt())
        )
    }

    fun laneWidth(artist: String, height: Float, minimumWidth: Float, minimumFontSize: Float): Float {
        val availableHeight = max(height - verticalInset(height) * 2, 1f)
        var width = minimumWidth
        val maximumWidth = minimumWidth * 320f / 180f
        while (width < maximumWidth && textHeight(artist, max(width - 37f, 1f), minimumFontSize) > availableHeight) {
            width = min(width + 20, maximumWidth)
        }
        return width
    }

    companion object {
        fun verticalInset(height: Float): Float = if (height < 24) 1f else if (height < 44) 2f else 6f
    }
}
