package com.oshicue.kit.ui.agenda

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.AgendaCore
import com.oshicue.kit.core.AgendaEntry
import com.oshicue.kit.core.AgendaFlight
import com.oshicue.kit.core.AgendaKind
import com.oshicue.kit.core.AgendaTransfer
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.data.AgendaStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.BrandWordmark
import com.oshicue.kit.ui.common.ChoiceChip
import com.oshicue.kit.ui.common.CircleIconButton
import com.oshicue.kit.ui.common.KitAlert
import com.oshicue.kit.ui.common.KitEmptyState
import com.oshicue.kit.ui.common.KitSheet
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.kitPalette
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.floor

@Composable
fun AgendaScreen(store: AgendaStore) {
    val palette = kitPalette()
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    var detailID by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<AgendaEntry?>(null) }
    var editingIsNew by remember { mutableStateOf(false) }
    var showManagement by remember { mutableStateOf(false) }

    val day = LedgerDates.string(selectedDay)
    val entries = remember(store.document, day) { AgendaCore.entries(day, store.document) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandWordmark()
            Spacer(modifier = Modifier.weight(1f))
            CircleIconButton(Icons.Filled.Tune, L10n.text("agenda.management")) { showManagement = true }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(palette.accentFill, CircleShape)
                    .clickable {
                        editing = AgendaCore.draft(day)
                        editingIsNew = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Add, contentDescription = L10n.text("agenda.add"), tint = palette.onAccent, modifier = Modifier.size(22.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 17.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(L10n.text("agenda.title"), color = palette.ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                L10n.text("agenda.today"),
                color = palette.accentText,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(palette.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, palette.line, RoundedCornerShape(12.dp))
                    .clickable {
                        selectedDay = LocalDate.now()
                        visibleMonth = YearMonth.now()
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            AgendaMonthCalendar(
                month = visibleMonth,
                selected = selectedDay,
                document = store.document,
                onMonthChange = { visibleMonth = it },
                onSelect = {
                    selectedDay = it
                    if (YearMonth.from(it) != visibleMonth) visibleMonth = YearMonth.from(it)
                }
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(KitDate.string(selectedDay, "yyyy MMM d EEE"), color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                Text(L10n.format("agenda.count", entries.size), color = palette.muted, fontSize = 13.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(L10n.text("agenda.calendar.hint"), color = palette.muted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(14.dp))
            if (entries.isEmpty()) {
                KitEmptyState(
                    symbol = Icons.Filled.CalendarMonth,
                    title = L10n.text("agenda.empty"),
                    hint = L10n.text("agenda.empty.hint")
                )
                Button(
                    onClick = {
                        editing = AgendaCore.draft(day)
                        editingIsNew = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L10n.text("agenda.add"))
                }
            } else {
                for (entry in entries) {
                    AgendaCard(entry) { detailID = entry.id }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
            AgendaTravelList(store.document, day)
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    editing?.let { entry ->
        AgendaEditor(
            store = store,
            initial = entry,
            isNew = editingIsNew,
            onSaved = { saved ->
                selectedDay = saved.startDay()
                visibleMonth = YearMonth.from(selectedDay)
            },
            onClose = { editing = null }
        )
    }
    detailID?.let { id ->
        val entry = store.document.entries.firstOrNull { it.id == id }
        if (entry == null) {
            LaunchedEffect(id) { detailID = null }
        } else {
            AgendaDetailSheet(
                store = store,
                entry = entry,
                onEdit = {
                    detailID = null
                    editing = entry
                    editingIsNew = false
                },
                onClose = { detailID = null }
            )
        }
    }
    if (showManagement) {
        AgendaManagementSheet(
            store = store,
            onClose = { showManagement = false },
            onApplied = { first ->
                if (first != null) {
                    selectedDay = first.startDay()
                    visibleMonth = YearMonth.from(selectedDay)
                }
                showManagement = false
            }
        )
    }
}

@Composable
private fun AgendaCard(entry: AgendaEntry, onClick: () -> Unit) {
    val palette = kitPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(palette.surface, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(palette.accent, RoundedCornerShape(3.dp))
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(entry.title, color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            AgendaKindLabel(entry.kind)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(entry.timeRangeText(), color = palette.accentText, fontSize = 13.sp)
            }
            entry.city?.let { city ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = palette.muted, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(city.name, color = palette.muted, fontSize = 13.sp)
                }
            }
            if (entry.kind == AgendaKind.flight) {
                val route = entry.flights.firstOrNull()?.let { flight ->
                    listOfNotNull(
                        flight.departureLocation?.name ?: flight.departureAirport,
                        flight.arrivalLocation?.name ?: flight.arrivalAirport
                    ).filter { it.isNotEmpty() }.joinToString(" → ")
                }
                if (!route.isNullOrEmpty()) Text(route, color = palette.ink, fontSize = 13.sp)
            } else {
                entry.location?.let { place ->
                    Text(place.name, color = palette.ink, fontSize = 13.sp)
                    if (place.address.isNotEmpty()) {
                        Text(place.address, color = palette.muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                } ?: entry.venue.takeIf { it.isNotEmpty() }?.let {
                    Text(it, color = palette.muted, fontSize = 12.sp)
                }
            }
            if (entry.flights.isNotEmpty()) {
                Text(entry.flights.map { it.number }.joinToString(" · "), color = palette.muted, fontSize = 12.sp)
            }
            Text(entry.timeZone, color = palette.muted, fontSize = 10.sp)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = palette.muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun AgendaKindLabel(kind: AgendaKind) {
    val palette = kitPalette()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(kind.symbol(), contentDescription = null, tint = palette.accentText, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(kind.displayName(), color = palette.accentText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AgendaMonthCalendar(
    month: YearMonth,
    selected: LocalDate,
    document: com.oshicue.kit.core.AgendaDocument,
    onMonthChange: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    val palette = kitPalette()
    val locale = L10n.currentLocale()
    val days = remember(month, locale) { monthDays(month, locale) }
    val marked = remember(document, month, locale) {
        days.filter { day -> AgendaCore.entries(LedgerDates.string(day), document).isNotEmpty() }.toSet()
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, RoundedCornerShape(20.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = null, tint = palette.muted)
            }
            Text(
                KitDate.string(month.atDay(1), "yyyy MMM"),
                color = palette.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.muted)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            for (label in weekdayLabels(locale)) {
                Text(
                    label,
                    color = palette.muted,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        for (week in days.chunked(7)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (date in week) {
                    val isSelected = date == selected
                    val isToday = date == LocalDate.now()
                    val inMonth = YearMonth.from(date) == month
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .padding(2.dp)
                            .background(
                                if (isSelected) palette.accentFill else Color.Transparent,
                                CircleShape
                            )
                            .clickable { onSelect(date) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                date.dayOfMonth.toString(),
                                color = when {
                                    isSelected -> palette.onAccent
                                    !inMonth -> palette.muted.copy(alpha = 0.45f)
                                    isToday -> palette.accentText
                                    else -> palette.ink
                                },
                                fontSize = 13.sp,
                                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                            )
                            if (date in marked) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .background(
                                            if (isSelected) palette.onAccent else palette.accentText,
                                            CircleShape
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
fun AgendaTravelList(document: com.oshicue.kit.core.AgendaDocument, day: String) {
    val palette = kitPalette()
    val context = LocalContext.current
    val transfers = remember(document, day) { com.oshicue.kit.core.agendaTransfers(day, document) }
    if (transfers.isEmpty()) return
    var mode by remember { mutableStateOf(AgendaTravelMode.transit) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Directions, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(L10n.text("agenda.travel.title"), color = palette.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (value in AgendaTravelMode.entries) {
                ChoiceChip(
                    label = L10n.text("agenda.travel." + value.raw),
                    selected = mode == value,
                    onClick = { mode = value }
                )
            }
        }
        for (transfer in transfers) {
            AgendaTransferCard(transfer, mode) {
                openRoute(context, transfer, mode)
            }
        }
        Text(L10n.text("agenda.travel.allDayHint"), color = palette.muted, fontSize = 11.sp)
    }
}

@Composable
private fun AgendaTransferCard(transfer: AgendaTransfer, mode: AgendaTravelMode, onOpen: () -> Unit) {
    val palette = kitPalette()
    val origin = transfer.origin
    val destination = transfer.destination
    val sourceZone = if (transfer.from.kind == AgendaKind.flight) {
        com.oshicue.kit.core.AgendaCore.zoneOrNull(transfer.from.flights.firstOrNull()?.arrivalTimeZone)
            ?: transfer.from.timeZoneValue
    } else {
        transfer.from.timeZoneValue
    }
    Column(
        modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(15.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(transfer.from.title + " → " + transfer.to.title, color = palette.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(
            listOfNotNull(origin?.name, destination?.name).joinToString(" → "),
            color = palette.muted,
            fontSize = 11.sp
        )
        Text(
            L10n.text("agenda.travel.departure") + " · " + instantText(transfer.departureAt, sourceZone),
            color = palette.muted,
            fontSize = 11.sp
        )
        Text(
            L10n.text("agenda.travel.arrival") + " · " + instantText(transfer.arrivalDeadline, transfer.to.timeZoneValue),
            color = palette.muted,
            fontSize = 11.sp
        )
        if (transfer.hasTimeConflict) {
            Text(L10n.text("agenda.travel.overlap"), color = palette.warningText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        } else {
            Text(
                L10n.format("agenda.travel.gapMinutes", floor(transfer.availableSeconds / 60).toInt()),
                color = palette.muted,
                fontSize = 11.sp
            )
        }
        if (origin == null || destination == null) {
            Text(L10n.text("agenda.travel.missingPlace"), color = palette.muted, fontSize = 11.sp)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Map, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(L10n.text("agenda.travel.openMaps"), color = palette.accentText, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun AgendaDetailSheet(store: AgendaStore, entry: AgendaEntry, onEdit: () -> Unit, onClose: () -> Unit) {
    val palette = kitPalette()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    KitSheet(L10n.text("agenda.details"), onClose) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(entry.title, color = palette.ink, fontSize = 23.sp, fontWeight = FontWeight.Bold)
            AgendaKindLabel(entry.kind)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Schedule, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(7.dp))
                Text(entry.timeRangeText(), color = palette.ink, fontSize = 14.sp)
            }
            if (entry.isAllDay) {
                Text(
                    KitDate.string(entry.startsAt, "yyyy.MM.dd", entry.timeZoneValue) + " – " +
                        KitDate.string(entry.endDisplayDay(), "yyyy.MM.dd", entry.timeZoneValue),
                    color = palette.muted,
                    fontSize = 13.sp
                )
            }
            Text(L10n.text("agenda.timeZone") + " · " + entry.timeZone, color = palette.muted, fontSize = 12.sp)
            entry.city?.let { city ->
                DetailSection(L10n.text("agenda.city")) {
                    Text(listOfNotNull(city.name, city.country).joinToString(" · "), color = palette.ink, fontSize = 14.sp)
                    if (entry.kind != AgendaKind.flight && entry.venue.isNotEmpty()) {
                        Text(entry.venue, color = palette.muted, fontSize = 12.sp)
                    }
                    TextLink(L10n.text("agenda.city.map"), Icons.Filled.Map) {
                        openMap(context, city, if (entry.kind == AgendaKind.flight) "" else entry.venue)
                    }
                }
            } ?: entry.venue.takeIf { it.isNotEmpty() }?.let { venue ->
                DetailSection(L10n.text("agenda.venue")) {
                    Text(venue, color = palette.ink, fontSize = 14.sp)
                }
            }
            if (entry.kind != AgendaKind.flight) {
                entry.location?.let { place ->
                    DetailSection(L10n.text("agenda.place")) {
                        Text(place.name, color = palette.ink, fontSize = 14.sp)
                        if (place.address.isNotEmpty()) Text(place.address, color = palette.muted, fontSize = 12.sp)
                        TextLink(L10n.text("agenda.city.map"), Icons.Filled.Map) { openMap(context, place) }
                    }
                }
            }
            if (entry.flights.isNotEmpty()) {
                DetailSection(L10n.text("agenda.flights")) {
                    for (flight in entry.flights) {
                        AgendaFlightDetail(flight)
                    }
                    Text(L10n.text("agenda.flight.nativeHint"), color = palette.muted, fontSize = 11.sp)
                }
            }
            if (entry.notes.isNotEmpty()) {
                DetailSection(L10n.text("agenda.notes")) {
                    Text(entry.notes, color = palette.ink, fontSize = 14.sp)
                }
            }
            error?.let { Text(it, color = palette.warningText, fontSize = 12.sp) }
            Button(
                onClick = {
                    if (!addToCalendar(context, entry)) {
                        Toast.makeText(context, L10n.text("settings.saveError"), Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = palette.surface, contentColor = palette.ink),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(L10n.text("agenda.calendar.add"))
            }
            Text(L10n.text("agenda.calendar.copyHint"), color = palette.muted, fontSize = 11.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceMuted, contentColor = palette.ink),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L10n.text("agenda.edit"))
                }
                Button(
                    onClick = { confirmDelete = true },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceMuted, contentColor = palette.warningText),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f).height(46.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L10n.text("agenda.delete"))
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
    if (confirmDelete) {
        KitAlert(
            title = L10n.text("agenda.delete.confirm"),
            message = L10n.text("agenda.delete.hint"),
            confirmLabel = L10n.text("agenda.delete"),
            onConfirm = {
                confirmDelete = false
                if (store.deleteEntry(entry.id)) {
                    onClose()
                } else {
                    error = store.storageError
                    store.storageError = null
                }
            },
            onDismiss = { confirmDelete = false },
            destructive = true
        )
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    val palette = kitPalette()
    Column(
        modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Text(title, color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun TextLink(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val palette = kitPalette()
    Row(
        modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = palette.accentText, fontSize = 12.sp)
    }
}

@Composable
private fun AgendaFlightDetail(flight: AgendaFlight) {
    val palette = kitPalette()
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        flight.airline?.takeIf { it.isNotEmpty() }?.let { Text(it, color = palette.muted, fontSize = 11.sp) }
        Row(
            modifier = Modifier.clickable { openFlight(context, flight.number) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Flight, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(flight.number, color = palette.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        val route = listOfNotNull(flight.departureAirport, flight.arrivalAirport)
            .filter { it.isNotEmpty() }.joinToString(" → ")
        if (route.isNotEmpty()) Text(route, color = palette.ink, fontSize = 13.sp)
        flight.departureAt?.let { date ->
            val zone = zoneIdOf(flight.departureTimeZone ?: "UTC")
            Text(L10n.text("agenda.flight.departure") + " · " + KitDate.string(date, "yyyy.MM.dd HH:mm", zone), color = palette.ink, fontSize = 13.sp)
            Text(zone.id, color = palette.muted, fontSize = 11.sp)
        }
        flight.departureLocation?.let { place ->
            Text(place.name, color = palette.ink, fontSize = 12.sp)
            if (place.address.isNotEmpty()) Text(place.address, color = palette.muted, fontSize = 11.sp)
            TextLink(L10n.text("agenda.city.map"), Icons.Filled.Map) { openMap(context, place) }
        }
        flight.arrivalAt?.let { date ->
            val zone = zoneIdOf(flight.arrivalTimeZone ?: "UTC")
            Text(L10n.text("agenda.flight.arrival") + " · " + KitDate.string(date, "yyyy.MM.dd HH:mm", zone), color = palette.ink, fontSize = 13.sp)
            Text(zone.id, color = palette.muted, fontSize = 11.sp)
        }
        flight.arrivalLocation?.let { place ->
            Text(place.name, color = palette.ink, fontSize = 12.sp)
            if (place.address.isNotEmpty()) Text(place.address, color = palette.muted, fontSize = 11.sp)
            TextLink(L10n.text("agenda.city.map"), Icons.Filled.Map) { openMap(context, place) }
        }
    }
}
