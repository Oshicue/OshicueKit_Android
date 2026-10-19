package com.oshicue.kit.ui

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.AgendaCore
import com.oshicue.kit.core.AgendaDocument
import com.oshicue.kit.core.AgendaEntry
import com.oshicue.kit.core.AgendaKind
import com.oshicue.kit.core.AgendaPresentation
import com.oshicue.kit.core.AgendaStore
import com.oshicue.kit.core.AgendaTransfer
import com.oshicue.kit.core.KitDate
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerDates
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun agendaKindName(kind: AgendaKind): String = L10n.text("agenda.kind." + kind.rawValue)

fun agendaKindIcon(kind: AgendaKind): ImageVector = when (kind) {
    AgendaKind.Live -> Icons.Filled.MusicNote
    AgendaKind.Flight -> Icons.Filled.Flight
    AgendaKind.Hotel -> Icons.Filled.Hotel
    AgendaKind.Restaurant -> Icons.Filled.Restaurant
    else -> Icons.Filled.Place
}

@Composable
fun AgendaScreen(
    store: AgendaStore,
    modifier: Modifier = Modifier
) {
    val kit = LocalKit.current
    val zone = remember { ZoneId.systemDefault() }
    var day by remember { mutableStateOf(LedgerDates.today()) }
    var showMonth by remember { mutableStateOf(YearMonth.now()) }
    var editorEntry by remember { mutableStateOf<AgendaEntry?>(null) }
    var editorIsNew by remember { mutableStateOf(true) }
    var detailEntryId by remember { mutableStateOf<String?>(null) }
    var showManagement by remember { mutableStateOf(false) }
    var storageMessage by remember { mutableStateOf<String?>(null) }

    val entries = AgendaCore.entriesOn(day, store.document)

    Column(modifier.fillMaxSize().background(kit.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandWordmark()
            Spacer(Modifier.weight(1f))
            CircleIconButton(Icons.Filled.MoreHoriz, L10n.text("agenda.management")) {
                showManagement = true
            }
            Spacer(Modifier.width(10.dp))
            AccentCircleButton(Icons.Filled.Add, L10n.text("agenda.add")) {
                editorEntry = AgendaCore.draft(day, zone)
                editorIsNew = true
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                L10n.text("agenda.title"),
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                color = kit.ink
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(kit.surface)
                    .clickable {
                        day = LedgerDates.today()
                        showMonth = YearMonth.now()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(L10n.text("agenda.today"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = kit.accentText)
            }
        }
        Spacer(Modifier.height(15.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AgendaCalendarCard(
                    month = showMonth,
                    selected = day,
                    document = store.document,
                    onMonthChange = { showMonth = it },
                    onSelect = { day = it }
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(day, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = kit.ink)
                    Spacer(Modifier.weight(1f))
                    Text(
                        L10n.format("agenda.count", entries.size),
                        fontSize = 13.sp,
                        color = kit.muted
                    )
                }
            }
            item {
                Text(L10n.text("agenda.calendar.hint"), fontSize = 11.5.sp, color = kit.muted)
            }
            if (entries.isEmpty()) {
                item {
                    SurfaceCard {
                        EmptyState(
                            title = L10n.text("agenda.empty"),
                            hint = L10n.text("agenda.empty.hint"),
                            symbol = Icons.Filled.CalendarMonth,
                            action = {
                                KitPrimaryButton(L10n.text("agenda.add")) {
                                    editorEntry = AgendaCore.draft(day, zone)
                                    editorIsNew = true
                                }
                            }
                        )
                    }
                }
            } else {
                items(entries, key = { it.id }) { entry ->
                    AgendaCard(entry) {
                        detailEntryId = entry.id
                    }
                }
            }
            val transfers = AgendaCore.transfersOn(day, store.document)
            if (transfers.isNotEmpty()) {
                item {
                    AgendaTransferList(transfers, store.document)
                }
            }
        }
    }

    val editing = editorEntry
    if (editing != null) {
        AgendaEditor(
            store = store,
            entry = editing,
            isNew = editorIsNew,
            onClose = { editorEntry = null },
            onSaved = { saved ->
                day = KitDate.day(saved.startsAt, saved.zone)
                showMonth = YearMonth.from(saved.startsAt.atZone(saved.zone).toLocalDate())
                editorEntry = null
            }
        )
    }
    val detailId = detailEntryId
    if (detailId != null) {
        val entry = store.document.entries.firstOrNull { it.id == detailId }
        if (entry != null) {
            AgendaDetailScreen(
                store = store,
                entry = entry,
                onClose = { detailEntryId = null },
                onEdit = {
                    editorEntry = entry
                    editorIsNew = false
                }
            )
        } else {
            detailEntryId = null
        }
    }
    if (showManagement) {
        AgendaManagementSheet(
            store = store,
            onClose = { showManagement = false },
            onApplied = { first ->
                if (first != null) {
                    day = KitDate.day(first.startsAt, first.zone)
                    showMonth = YearMonth.from(first.startsAt.atZone(first.zone).toLocalDate())
                }
                showManagement = false
            }
        )
    }
    val error = store.storageError
    if (error != null && storageMessage == null) {
        MessageDialog(L10n.text("settings.storageError"), error) {
            store.storageError = null
            storageMessage = null
        }
    }
}

@Composable
private fun AgendaCalendarCard(
    month: YearMonth,
    selected: String,
    document: AgendaDocument,
    onMonthChange: (YearMonth) -> Unit,
    onSelect: (String) -> Unit
) {
    val kit = LocalKit.current
    val firstDay = WeekFields.of(L10n.locale).firstDayOfWeek
    val today = LocalDate.now()
    val firstOfMonth = month.atDay(1)
    val leading = (firstOfMonth.dayOfWeek.value - firstDay.value + 7) % 7
    val length = ((leading + month.lengthOfMonth() + 6) / 7) * 7
    val cells = (0 until length).map { offset ->
        val day = offset - leading + 1
        if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
    }

    SurfaceCard(corner = 20.dp) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = L10n.text("ledger.previousMonth"), tint = kit.accentText)
                }
                Spacer(Modifier.weight(1f))
                Text(monthTitle(month), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = kit.ink)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = L10n.text("ledger.nextMonth"), tint = kit.accentText)
                }
            }
            Row(Modifier.fillMaxWidth()) {
                weekdaySymbols(firstDay).forEach { symbol ->
                    Text(
                        symbol,
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = kit.muted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.weight(1f))
                            return@forEach
                        }
                        val key = KitDate.formatDay(date)
                        val isSelected = key == selected
                        val isToday = date == today
                        val hasEntries = AgendaCore.entriesOn(key, document).isNotEmpty()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .padding(2.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) kit.accentFill else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { onSelect(key) }
                                .semantics(mergeDescendants = true) {
                                    contentDescription = key
                                    this.selected = isSelected
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        isSelected -> kit.onAccent
                                        isToday -> kit.accentText
                                        else -> kit.ink
                                    }
                                )
                                Box(
                                    Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                !hasEntries -> androidx.compose.ui.graphics.Color.Transparent
                                                isSelected -> kit.onAccent
                                                else -> kit.accentText
                                            }
                                        )
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
private fun AgendaCard(entry: AgendaEntry, onClick: () -> Unit) {
    val kit = LocalKit.current
    SurfaceCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .height(if (entry.flights.isEmpty()) 92.dp else 130.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(kit.accent)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = kit.ink)
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(agendaKindIcon(entry.kind), null, tint = kit.accentText, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(agendaKindName(entry.kind), fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = kit.accentText)
                }
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Schedule, null, tint = kit.accentText, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(AgendaPresentation.timeRange(entry), fontSize = 13.sp, color = kit.accentText)
                }
                entry.city?.let { city ->
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, null, tint = kit.muted, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(city.name, fontSize = 13.sp, color = kit.muted)
                    }
                }
                if (entry.kind == AgendaKind.Flight) {
                    val flight = entry.flights.firstOrNull()
                    val route = listOfNotNull(
                        flight?.departureLocation?.name ?: flight?.departureAirport,
                        flight?.arrivalLocation?.name ?: flight?.arrivalAirport
                    ).filter { it.isNotEmpty() }.joinToString(" → ")
                    if (route.isNotEmpty()) {
                        Spacer(Modifier.height(5.dp))
                        Text(route, fontSize = 13.sp, color = kit.ink)
                    }
                } else {
                    val place = entry.location
                    if (place != null) {
                        Spacer(Modifier.height(5.dp))
                        Text(place.name, fontSize = 13.sp, color = kit.ink)
                        if (place.address.isNotEmpty()) {
                            Text(place.address, fontSize = 11.sp, color = kit.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else if (entry.venue.isNotEmpty()) {
                        Spacer(Modifier.height(5.dp))
                        Text(entry.venue, fontSize = 11.sp, color = kit.muted)
                    }
                }
                if (entry.flights.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Text(entry.flights.joinToString(" · ") { it.number }, fontSize = 11.sp, color = kit.muted)
                }
                Spacer(Modifier.height(5.dp))
                Text(entry.timeZone, fontSize = 10.sp, color = kit.muted)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = kit.muted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AgendaDetailScreen(
    store: AgendaStore,
    entry: AgendaEntry,
    onClose: () -> Unit,
    onEdit: () -> Unit
) {
    val kit = LocalKit.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    ModalScreen(
        title = L10n.text("agenda.details"),
        onClose = onClose,
        leadingBack = true,
        actions = {
            KitTextAction(L10n.text("agenda.edit"), onClick = onEdit)
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(entry.title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = kit.ink)
            Spacer(Modifier.height(9.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(agendaKindIcon(entry.kind), null, tint = kit.accentText, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(agendaKindName(entry.kind), fontSize = 13.sp, color = kit.accentText, fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, null, tint = kit.accentText, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text(AgendaPresentation.timeRange(entry), fontSize = 14.sp, color = kit.ink)
            }
            if (entry.isAllDay) {
                Spacer(Modifier.height(5.dp))
                Text(
                    KitDate.string(entry.startsAt, "yyyy.MM.dd", entry.zone) + " – " +
                        KitDate.string(entry.endsAt.minusSeconds(1), "yyyy.MM.dd", entry.zone),
                    fontSize = 13.sp,
                    color = kit.muted
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(L10n.text("agenda.timeZone") + " · " + entry.timeZone, fontSize = 13.sp, color = kit.muted)

            if (entry.kind != AgendaKind.Flight) {
                entry.location?.let { place ->
                    Spacer(Modifier.height(18.dp))
                    SectionLabel(L10n.text("agenda.place"))
                    Spacer(Modifier.height(8.dp))
                    FormCard {
                        Text(place.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
                        if (place.address.isNotEmpty()) {
                            Text(place.address, fontSize = 12.5.sp, color = kit.muted)
                        }
                        Spacer(Modifier.height(8.dp))
                        KitSecondaryButton(L10n.text("agenda.city.map"), icon = Icons.Filled.Place) {
                            openMaps(context, place.latitude, place.longitude, place.name, place.address)
                        }
                    }
                }
            }
            entry.city?.let { city ->
                Spacer(Modifier.height(18.dp))
                SectionLabel(L10n.text("agenda.city"))
                Spacer(Modifier.height(8.dp))
                FormCard {
                    Text(
                        listOfNotNull(city.name, city.country).filter { it.isNotEmpty() }.joinToString(" · "),
                        fontSize = 14.sp,
                        color = kit.ink
                    )
                    if (entry.kind != AgendaKind.Flight && entry.venue.isNotEmpty()) {
                        Text(entry.venue, fontSize = 12.5.sp, color = kit.muted)
                    }
                    Spacer(Modifier.height(8.dp))
                    KitSecondaryButton(L10n.text("agenda.city.map"), icon = Icons.Filled.Place) {
                        val venue = if (entry.kind == AgendaKind.Flight) "" else entry.venue
                        openMaps(context, city.latitude, city.longitude, city.name, venue)
                    }
                }
            }
            if (entry.kind != AgendaKind.Flight && entry.city == null && entry.venue.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                SectionLabel(L10n.text("agenda.venue"))
                Spacer(Modifier.height(8.dp))
                FormCard { Text(entry.venue, fontSize = 14.sp, color = kit.ink) }
            }
            if (entry.flights.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                SectionLabel(L10n.text("agenda.flights"))
                Spacer(Modifier.height(8.dp))
                entry.flights.forEach { flight ->
                    FormCard {
                        flight.airline?.takeIf { it.isNotEmpty() }?.let {
                            Text(it, fontSize = 11.sp, color = kit.muted)
                        }
                        Text(flight.number, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = kit.ink)
                        val route = listOfNotNull(flight.departureAirport, flight.arrivalAirport)
                            .filter { it.isNotEmpty() }.joinToString(" → ")
                        if (route.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(route, fontSize = 13.sp, color = kit.ink)
                        }
                        flight.departureAt?.let { departure ->
                            FlightDetailRow(L10n.text("agenda.flight.departure"), departure, flight.departureTimeZone)
                        }
                        flight.departureLocation?.let { place ->
                            Text(L10n.text("agenda.flight.departurePlace"), fontSize = 11.sp, color = kit.muted)
                            Text(place.name, fontSize = 13.sp, color = kit.ink)
                            if (place.address.isNotEmpty()) Text(place.address, fontSize = 11.5.sp, color = kit.muted)
                        }
                        flight.arrivalAt?.let { arrival ->
                            FlightDetailRow(L10n.text("agenda.flight.arrival"), arrival, flight.arrivalTimeZone)
                        }
                        flight.arrivalLocation?.let { place ->
                            Text(L10n.text("agenda.flight.arrivalPlace"), fontSize = 11.sp, color = kit.muted)
                            Text(place.name, fontSize = 13.sp, color = kit.ink)
                            if (place.address.isNotEmpty()) Text(place.address, fontSize = 11.5.sp, color = kit.muted)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Text(L10n.text("agenda.flight.nativeHint"), fontSize = 11.sp, color = kit.muted)
            }
            if (entry.notes.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                SectionLabel(L10n.text("agenda.notes"))
                Spacer(Modifier.height(8.dp))
                FormCard { Text(entry.notes, fontSize = 13.5.sp, color = kit.ink) }
            }
            Spacer(Modifier.height(18.dp))
            KitSecondaryButton(L10n.text("agenda.calendar.add"), icon = Icons.Filled.Today) {
                insertCalendarEvent(context, entry)
            }
            Spacer(Modifier.height(6.dp))
            Text(L10n.text("agenda.calendar.copyHint"), fontSize = 11.sp, color = kit.muted)
            Spacer(Modifier.height(18.dp))
            KitSecondaryButton(L10n.text("agenda.delete"), icon = Icons.Filled.MoreHoriz) {
                confirmDelete = true
            }
            Spacer(Modifier.height(30.dp))
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = L10n.text("agenda.delete.confirm"),
            message = L10n.text("agenda.delete.hint"),
            confirmLabel = L10n.text("agenda.delete"),
            onConfirm = {
                if (store.delete(entry.id)) onClose()
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
private fun FlightDetailRow(label: String, date: Instant, zoneText: String?) {
    val kit = LocalKit.current
    val zone = zoneText?.let { KitDate.zone(it) } ?: KitDate.utc
    Spacer(Modifier.height(6.dp))
    Text(label + " · " + KitDate.string(date, "yyyy.MM.dd HH:mm", zone), fontSize = 13.sp, color = kit.ink)
    Text(zone.id, fontSize = 11.sp, color = kit.muted)
}

/** Opens installed map applications with a query, or a pin when coordinates exist. */
fun openMaps(context: android.content.Context, latitude: Double?, longitude: Double?, name: String, address: String) {
    val uri = if (latitude != null && longitude != null) {
        android.net.Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${android.net.Uri.encode(name)})")
    } else {
        val query = listOf(name, address).filter { it.isNotEmpty() }.joinToString(" ")
        android.net.Uri.parse("geo:0,0?q=${android.net.Uri.encode(query)}")
    }
    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // No map application installed; the coordinates remain visible in the detail screen.
    }
}

/** Android hands the event to the calendar app instead of presenting an in-process editor. */
fun insertCalendarEvent(context: android.content.Context, entry: AgendaEntry) {
    val precisePlace = if (entry.kind == AgendaKind.Flight) entry.flights.firstOrNull()?.departureLocation else entry.location
    val location = precisePlace?.let { listOf(it.name, it.address).filter { value -> value.isNotEmpty() }.joinToString(" · ") }
        ?: listOfNotNull(entry.city?.name ?: "", entry.venue).filter { it.isNotEmpty() }.joinToString(" · ")
    val notes = (listOf(entry.notes) + entry.flights.map { flight ->
        listOfNotNull(flight.airline, flight.number, flight.departureAirport, flight.arrivalAirport)
            .filter { it.isNotEmpty() }.joinToString(" · ")
    }).filter { it.isNotEmpty() }.joinToString("\n")
    val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
        setData(android.provider.CalendarContract.Events.CONTENT_URI)
        putExtra(android.provider.CalendarContract.EXTRA_EVENT_BEGIN_TIME, entry.startsAt.toEpochMilli())
        putExtra(android.provider.CalendarContract.EXTRA_EVENT_END_TIME, entry.endsAt.toEpochMilli())
        putExtra(android.provider.CalendarContract.Events.TITLE, entry.title)
        putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, location)
        putExtra(android.provider.CalendarContract.Events.DESCRIPTION, notes)
        putExtra(android.provider.CalendarContract.Events.ALL_DAY, entry.isAllDay)
        putExtra(android.provider.CalendarContract.Events.EVENT_TIMEZONE, entry.timeZone)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Some devices ship without a calendar provider; nothing else to do.
    }
}

@Composable
private fun AgendaTransferList(
    transfers: List<AgendaTransfer>,
    document: AgendaDocument
) {
    val kit = LocalKit.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var mode by remember { mutableStateOf("transit") }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L10n.text("agenda.travel.title"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = kit.ink)
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        SegmentedControl(
            options = listOf(
                "driving" to L10n.text("agenda.travel.driving"),
                "walking" to L10n.text("agenda.travel.walking"),
                "transit" to L10n.text("agenda.travel.transit")
            ),
            selected = mode,
            onSelect = { mode = it }
        )
        Spacer(Modifier.height(10.dp))
        transfers.forEach { transfer ->
            SurfaceCard(corner = 15.dp) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        transfer.from.title + " → " + transfer.to.title,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = kit.ink
                    )
                    val route = listOfNotNull(transfer.origin?.name, transfer.destination?.name).joinToString(" → ")
                    if (route.isNotEmpty()) {
                        Text(route, fontSize = 11.sp, color = kit.muted)
                    }
                    Spacer(Modifier.height(5.dp))
                    val sourceZone = if (transfer.from.kind == AgendaKind.Flight) {
                        transfer.from.flights.firstOrNull()?.arrivalTimeZone?.let { KitDate.zone(it) } ?: transfer.from.zone
                    } else transfer.from.zone
                    Text(
                        L10n.text("agenda.travel.departure") + " · " +
                            KitDate.string(transfer.departureAt, "MM.dd HH:mm", sourceZone),
                        fontSize = 11.5.sp,
                        color = kit.ink
                    )
                    Text(
                        L10n.text("agenda.travel.arrival") + " · " +
                            KitDate.string(transfer.arrivalDeadline, "MM.dd HH:mm", transfer.to.zone),
                        fontSize = 11.5.sp,
                        color = kit.ink
                    )
                    Spacer(Modifier.height(5.dp))
                    if (transfer.hasTimeConflict) {
                        Text(L10n.text("agenda.travel.overlap"), fontSize = 11.5.sp, color = kit.accentText, fontWeight = FontWeight.SemiBold)
                    } else {
                        Text(
                            L10n.format("agenda.travel.gapMinutes", (transfer.availableTime / 60_000).toInt()),
                            fontSize = 11.5.sp,
                            color = kit.muted
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(L10n.text("agenda.travel.unavailable"), fontSize = 11.sp, color = kit.muted)
                    if (transfer.origin != null && transfer.destination != null) {
                        Spacer(Modifier.height(8.dp))
                        KitSecondaryButton(L10n.text("agenda.travel.openMaps"), icon = Icons.Filled.Place) {
                            val origin = transfer.origin
                            val destination = transfer.destination
                            if (origin?.latitude != null && destination?.latitude != null) {
                                val uri = android.net.Uri.parse(
                                    "https://www.google.com/maps/dir/?api=1&origin=${origin.latitude},${origin.longitude}" +
                                        "&destination=${destination.latitude},${destination.longitude}&travelmode=$mode"
                                )
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                try { context.startActivity(intent) } catch (_: Exception) { }
                            } else {
                                val query = listOfNotNull(
                                    origin?.name ?: transfer.from.title,
                                    destination?.name ?: transfer.to.title
                                ).joinToString(" to ")
                                openMaps(context, null, null, query, "")
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Text(L10n.text("agenda.travel.hint"), fontSize = 11.sp, color = kit.muted)
        Spacer(Modifier.height(4.dp))
        Text(L10n.text("agenda.travel.allDayHint"), fontSize = 11.sp, color = kit.muted)
    }
}
