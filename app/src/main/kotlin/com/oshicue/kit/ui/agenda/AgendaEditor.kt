package com.oshicue.kit.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.AgendaCity
import com.oshicue.kit.core.AgendaCore
import com.oshicue.kit.core.AgendaEntry
import com.oshicue.kit.core.AgendaFlight
import com.oshicue.kit.core.AgendaKind
import com.oshicue.kit.core.AgendaPlace
import com.oshicue.kit.data.AgendaStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.KitDatePickerDialog
import com.oshicue.kit.ui.common.KitDropdownRow
import com.oshicue.kit.ui.common.KitField
import com.oshicue.kit.ui.common.KitSheet
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.kitPalette
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

private const val StartTarget = "start"
private const val EndTarget = "end"
private const val FlightDepartureTarget = "flightDeparture"
private const val FlightArrivalTarget = "flightArrival"

private const val EntryZoneTarget = "entry"
private const val DepartureZoneTarget = "departure"
private const val ArrivalZoneTarget = "arrival"

@Composable
fun AgendaEditor(
    store: AgendaStore,
    initial: AgendaEntry,
    isNew: Boolean,
    onSaved: (AgendaEntry) -> Unit,
    onClose: () -> Unit
) {
    val palette = kitPalette()
    val context = LocalContext.current
    var entry by remember { mutableStateOf(initial) }
    val previousFlights = remember { if (initial.kind == AgendaKind.flight) emptyList() else initial.flights }
    var flight by remember {
        val original = initial.flights.firstOrNull() ?: AgendaFlight()
        mutableStateOf(
            original.copy(
                departureAt = original.departureAt ?: initial.startsAt,
                arrivalAt = original.arrivalAt ?: initial.endsAt,
                departureTimeZone = original.departureTimeZone ?: initial.timeZone,
                arrivalTimeZone = original.arrivalTimeZone ?: initial.timeZone
            )
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    var dateTarget by remember { mutableStateOf<String?>(null) }
    var timeTarget by remember { mutableStateOf<String?>(null) }
    var pendingDate by remember { mutableStateOf<LocalDate?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCityPicker by remember { mutableStateOf(false) }
    var showTimeZonePicker by remember { mutableStateOf(false) }
    var timeZoneTarget by remember { mutableStateOf(EntryZoneTarget) }

    fun startOfDay(date: LocalDate, zone: ZoneId): java.time.Instant =
        date.atStartOfDay(zone).toInstant()

    fun applyDate(target: String, date: LocalDate) {
        when (target) {
            StartTarget -> {
                val zone = entry.timeZoneValue
                if (entry.isAllDay) {
                    val start = startOfDay(date, zone)
                    val end = if (entry.endsAt <= start) startOfDay(date.plusDays(1), zone) else entry.endsAt
                    entry = entry.copy(startsAt = start, endsAt = end)
                } else {
                    pendingDate = date
                    timeTarget = StartTarget
                    showTimePicker = true
                }
            }
            EndTarget -> {
                val zone = entry.timeZoneValue
                if (entry.isAllDay) {
                    var end = startOfDay(date.plusDays(1), zone)
                    if (end <= entry.startsAt) end = startOfDay(entry.startsAt.atZone(zone).toLocalDate().plusDays(1), zone)
                    entry = entry.copy(endsAt = end)
                } else {
                    pendingDate = date
                    timeTarget = EndTarget
                    showTimePicker = true
                }
            }
            FlightDepartureTarget, FlightArrivalTarget -> {
                pendingDate = date
                timeTarget = target
                showTimePicker = true
            }
        }
    }

    fun applyTime(time: LocalTime) {
        val date = pendingDate ?: return
        when (timeTarget) {
            StartTarget -> {
                val zone = entry.timeZoneValue
                val duration = Duration.between(entry.startsAt, entry.endsAt)
                val start = date.atTime(time).atZone(zone).toInstant()
                var end = entry.endsAt
                if (end <= start) end = start.plusSeconds(maxOf(60, duration.seconds))
                entry = entry.copy(startsAt = start, endsAt = end)
            }
            EndTarget -> {
                val zone = entry.timeZoneValue
                var end = date.atTime(time).atZone(zone).toInstant()
                if (end <= entry.startsAt) end = entry.startsAt.plusSeconds(3600)
                entry = entry.copy(endsAt = end)
            }
            FlightDepartureTarget -> {
                val zone = zoneIdOf(flight.departureTimeZone ?: entry.timeZone)
                flight = flight.copy(departureAt = date.atTime(time).atZone(zone).toInstant())
            }
            FlightArrivalTarget -> {
                val zone = zoneIdOf(flight.arrivalTimeZone ?: entry.timeZone)
                flight = flight.copy(arrivalAt = date.atTime(time).atZone(zone).toInstant())
            }
        }
    }

    fun changeEntryZone(identifier: String) {
        if (identifier == entry.timeZone) return
        val oldZone = entry.timeZoneValue
        val newZone = zoneIdOf(identifier)
        val start = entry.startsAt.atZone(oldZone).toLocalDateTime().atZone(newZone).toInstant()
        val end = entry.endsAt.atZone(oldZone).toLocalDateTime().atZone(newZone).toInstant()
        entry = entry.copy(timeZone = identifier, startsAt = start, endsAt = end)
    }

    fun changeFlightZone(isDeparture: Boolean, identifier: String) {
        if (isDeparture) {
            if (identifier == (flight.departureTimeZone ?: entry.timeZone)) return
            val oldZone = zoneIdOf(flight.departureTimeZone ?: entry.timeZone)
            val newZone = zoneIdOf(identifier)
            val date = flight.departureAt ?: entry.startsAt
            val moved = date.atZone(oldZone).toLocalDateTime().atZone(newZone).toInstant()
            flight = flight.copy(departureTimeZone = identifier, departureAt = moved)
        } else {
            if (identifier == (flight.arrivalTimeZone ?: entry.timeZone)) return
            val oldZone = zoneIdOf(flight.arrivalTimeZone ?: entry.timeZone)
            val newZone = zoneIdOf(identifier)
            val date = flight.arrivalAt ?: entry.endsAt
            val moved = date.atZone(oldZone).toLocalDateTime().atZone(newZone).toInstant()
            flight = flight.copy(arrivalTimeZone = identifier, arrivalAt = moved)
        }
    }

    fun save() {
        val title = entry.title.trim()
        if (title.isEmpty()) {
            error = L10n.text("agenda.error.title")
            return
        }
        var candidate = entry.copy(title = title)
        if (candidate.kind == AgendaKind.flight) {
            val departureAt = flight.departureAt ?: candidate.startsAt
            val arrivalAt = flight.arrivalAt ?: candidate.endsAt
            val departureZone = flight.departureTimeZone ?: candidate.timeZone
            val arrivalZone = flight.arrivalTimeZone ?: candidate.timeZone
            val normalized = flight.copy(
                number = flight.number.trim().uppercase(),
                departureAt = departureAt,
                arrivalAt = arrivalAt,
                departureTimeZone = departureZone,
                arrivalTimeZone = arrivalZone
            )
            candidate = candidate.copy(
                isAllDay = false,
                startsAt = departureAt,
                endsAt = arrivalAt,
                timeZone = departureZone,
                flights = listOf(normalized)
            )
        } else {
            candidate = candidate.copy(flights = previousFlights)
        }
        if (store.upsert(candidate)) {
            onSaved(candidate)
            onClose()
        } else {
            error = store.storageError
            store.storageError = null
        }
    }

    KitSheet(L10n.text(if (isNew) "agenda.add" else "agenda.edit"), onClose) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    KitField(L10n.text("agenda.name"), entry.title) {
                        entry = entry.copy(title = it)
                    }
                    KitDropdownRow(
                        label = L10n.text("agenda.kind"),
                        value = entry.kind.displayName(),
                        options = AgendaKind.entries.map { it.raw to it.displayName() },
                        onSelect = { value ->
                            val kind = AgendaKind.fromRaw(value) ?: return@KitDropdownRow
                            entry = entry.copy(kind = kind)
                            if (kind == AgendaKind.flight) {
                                entry = entry.copy(isAllDay = false)
                                if (flight.number.isEmpty()) {
                                    flight = flight.copy(
                                        departureAt = entry.startsAt,
                                        arrivalAt = entry.endsAt,
                                        departureTimeZone = entry.timeZone,
                                        arrivalTimeZone = entry.timeZone
                                    )
                                }
                            }
                        }
                    )
                    if (entry.kind != AgendaKind.flight) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(L10n.text("agenda.allDay"), color = palette.ink, fontSize = 15.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = entry.isAllDay,
                                onCheckedChange = { allDay ->
                                    if (allDay) {
                                        val zone = entry.timeZoneValue
                                        val start = AgendaCore.startOfDay(entry.startsAt, zone)
                                        val endDate = entry.endsAt.atZone(zone).toLocalDate()
                                        val startDate = start.atZone(zone).toLocalDate()
                                        val end = if (endDate > startDate) {
                                            endDate.atStartOfDay(zone).toInstant()
                                        } else {
                                            startDate.plusDays(1).atStartOfDay(zone).toInstant()
                                        }
                                        entry = entry.copy(isAllDay = true, startsAt = start, endsAt = end)
                                    } else {
                                        entry = entry.copy(isAllDay = false)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = palette.accentFill)
                            )
                        }
                        AgendaDateTimeRow(L10n.text("agenda.starts"), entry.startText()) {
                            dateTarget = StartTarget
                        }
                        AgendaDateTimeRow(L10n.text("agenda.ends"), entry.endText()) {
                            dateTarget = EndTarget
                        }
                        AgendaNavigationRow(L10n.text("agenda.timeZone"), zoneText(entry.timeZone)) {
                            timeZoneTarget = EntryZoneTarget
                            showTimeZonePicker = true
                        }
                        Text(
                            L10n.text(if (entry.isAllDay) "agenda.allDay.hint" else "agenda.timeZone.hint"),
                            color = palette.muted,
                            fontSize = 11.sp
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(L10n.text("agenda.location"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    KitField(L10n.text("agenda.city"), entry.city?.name ?: "") { value ->
                        val name = value.trim()
                        entry = entry.copy(city = if (name.isEmpty()) null else AgendaCity(name))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showCityPicker = true }.padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.text("agenda.city.search"), color = palette.accentText, fontSize = 14.sp)
                    }
                    entry.city?.country?.let { Text(it, color = palette.muted, fontSize = 12.sp) }
                    if (entry.kind != AgendaKind.flight) {
                        KitField(L10n.text("agenda.venue"), entry.venue) { entry = entry.copy(venue = it) }
                        AgendaPlaceFields(L10n.text("agenda.place"), entry.location) {
                            entry = entry.copy(location = it)
                        }
                    }
                    Text(L10n.text("agenda.city.manualHint"), color = palette.muted, fontSize = 11.sp)
                }

                if (entry.kind == AgendaKind.flight) {
                    Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                        Text(L10n.text("agenda.kind.flight"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        KitField(L10n.text("agenda.flight.number"), flight.number) {
                            flight = flight.copy(number = it)
                        }
                        KitField(L10n.text("agenda.flight.airline"), flight.airline ?: "") {
                            flight = flight.copy(airline = it.ifEmpty { null })
                        }
                        KitField(L10n.text("agenda.flight.from"), flight.departureAirport ?: "") {
                            flight = flight.copy(departureAirport = it.ifEmpty { null })
                        }
                        KitField(L10n.text("agenda.flight.to"), flight.arrivalAirport ?: "") {
                            flight = flight.copy(arrivalAirport = it.ifEmpty { null })
                        }
                        if (flight.number.trim().isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { openFlight(context, flight.number) }.padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Flight, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(L10n.text("agenda.flight.query"), color = palette.accentText, fontSize = 14.sp)
                            }
                        }
                        val departureZone = zoneIdOf(flight.departureTimeZone ?: entry.timeZone)
                        val arrivalZone = zoneIdOf(flight.arrivalTimeZone ?: entry.timeZone)
                        AgendaDateTimeRow(
                            L10n.text("agenda.flight.departure"),
                            flight.departureAt?.let { KitDate.string(it, "yyyy.MM.dd HH:mm", departureZone) } ?: ""
                        ) { dateTarget = FlightDepartureTarget }
                        AgendaNavigationRow(
                            L10n.text("agenda.flight.departureZone"),
                            zoneText(flight.departureTimeZone ?: entry.timeZone)
                        ) {
                            timeZoneTarget = DepartureZoneTarget
                            showTimeZonePicker = true
                        }
                        AgendaPlaceFields(L10n.text("agenda.flight.departurePlace"), flight.departureLocation) {
                            flight = flight.copy(departureLocation = it)
                        }
                        AgendaDateTimeRow(
                            L10n.text("agenda.flight.arrival"),
                            flight.arrivalAt?.let { KitDate.string(it, "yyyy.MM.dd HH:mm", arrivalZone) } ?: ""
                        ) { dateTarget = FlightArrivalTarget }
                        AgendaNavigationRow(
                            L10n.text("agenda.flight.arrivalZone"),
                            zoneText(flight.arrivalTimeZone ?: entry.timeZone)
                        ) {
                            timeZoneTarget = ArrivalZoneTarget
                            showTimeZonePicker = true
                        }
                        AgendaPlaceFields(L10n.text("agenda.flight.arrivalPlace"), flight.arrivalLocation) {
                            flight = flight.copy(arrivalLocation = it)
                        }
                        Text(L10n.text("agenda.flight.recordedHint"), color = palette.muted, fontSize = 11.sp)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    KitField(L10n.text("agenda.notes"), entry.notes) {
                        entry = entry.copy(notes = it)
                    }
                }

                error?.let { Text(it, color = palette.warningText, fontSize = 12.sp) }

                Button(
                    onClick = { save() },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text(L10n.text("agenda.save"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    dateTarget?.let { target ->
        val current = when (target) {
            StartTarget -> entry.startsAt.atZone(entry.timeZoneValue).toLocalDate()
            EndTarget -> if (entry.isAllDay) entry.endDisplayDay()
            else entry.endsAt.atZone(entry.timeZoneValue).toLocalDate()
            FlightDepartureTarget -> (flight.departureAt ?: entry.startsAt)
                .atZone(zoneIdOf(flight.departureTimeZone ?: entry.timeZone)).toLocalDate()
            else -> (flight.arrivalAt ?: entry.endsAt)
                .atZone(zoneIdOf(flight.arrivalTimeZone ?: entry.timeZone)).toLocalDate()
        }
        KitDatePickerDialog(
            initial = current,
            onPick = { applyDate(target, it) },
            onDismiss = { dateTarget = null }
        )
    }

    if (showTimePicker) {
        val initialTime = when (timeTarget) {
            StartTarget -> entry.startsAt.atZone(entry.timeZoneValue).toLocalTime()
            EndTarget -> entry.endsAt.atZone(entry.timeZoneValue).toLocalTime()
            FlightDepartureTarget -> (flight.departureAt ?: entry.startsAt)
                .atZone(zoneIdOf(flight.departureTimeZone ?: entry.timeZone)).toLocalTime()
            else -> (flight.arrivalAt ?: entry.endsAt)
                .atZone(zoneIdOf(flight.arrivalTimeZone ?: entry.timeZone)).toLocalTime()
        }
        val label = when (timeTarget) {
            EndTarget -> L10n.text("agenda.ends")
            FlightDepartureTarget -> L10n.text("agenda.flight.departure")
            FlightArrivalTarget -> L10n.text("agenda.flight.arrival")
            else -> L10n.text("agenda.starts")
        }
        KitTimePickerDialog(initialTime, label, { applyTime(it); showTimePicker = false; dateTarget = null }) {
            showTimePicker = false
            dateTarget = null
        }
    }

    if (showCityPicker) {
        AgendaCityPicker({ entry = entry.copy(city = it) }, { showCityPicker = false })
    }
    if (showTimeZonePicker) {
        val current = when (timeZoneTarget) {
            DepartureZoneTarget -> flight.departureTimeZone ?: entry.timeZone
            ArrivalZoneTarget -> flight.arrivalTimeZone ?: entry.timeZone
            else -> entry.timeZone
        }
        AgendaTimeZonePicker(current, { identifier ->
            when (timeZoneTarget) {
                DepartureZoneTarget -> changeFlightZone(true, identifier)
                ArrivalZoneTarget -> changeFlightZone(false, identifier)
                else -> changeEntryZone(identifier)
            }
        }, { showTimeZonePicker = false })
    }
}

@Composable
private fun AgendaDateTimeRow(label: String, value: String, onClick: () -> Unit) {
    val palette = kitPalette()
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = palette.ink, fontSize = 15.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = palette.accentText, fontSize = 14.sp)
    }
}

@Composable
private fun AgendaNavigationRow(label: String, value: String, onClick: () -> Unit) {
    val palette = kitPalette()
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = palette.ink, fontSize = 15.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = palette.accentText, fontSize = 14.sp)
    }
}

@Composable
private fun AgendaPlaceFields(title: String, place: AgendaPlace?, onChange: (AgendaPlace?) -> Unit) {
    val palette = kitPalette()
    var showPicker by remember { mutableStateOf(false) }
    Text(title, color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    KitField(L10n.text("agenda.place.name"), place?.name ?: "") { value ->
        val candidate = (place ?: AgendaPlace(name = "")).copy(
            name = value,
            latitude = null,
            longitude = null
        )
        onChange(if (candidate.name.isBlank() && candidate.address.isBlank()) null else candidate)
    }
    KitField(L10n.text("agenda.place.address"), place?.address ?: "") { value ->
        val candidate = (place ?: AgendaPlace(name = "")).copy(
            address = value,
            latitude = null,
            longitude = null
        )
        onChange(if (candidate.name.isBlank() && candidate.address.isBlank()) null else candidate)
    }
    Row(
        modifier = Modifier.fillMaxWidth().clickable { showPicker = true }.padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Map, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(L10n.text("agenda.place.search"), color = palette.accentText, fontSize = 14.sp)
    }
    if (place?.latitude != null && place.longitude != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = palette.muted, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(L10n.text("agenda.place.selected"), color = palette.muted, fontSize = 12.sp)
        }
    }
    if (place != null) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onChange(null) }.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, tint = palette.warningText, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(L10n.text("agenda.place.clear"), color = palette.warningText, fontSize = 13.sp)
        }
    }
    if (showPicker) {
        AgendaPlacePicker({ onChange(it); showPicker = false }, { showPicker = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KitTimePickerDialog(
    initial: LocalTime,
    label: String,
    onPick: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = kitPalette()
    val context = LocalContext.current
    val state = rememberTimePickerState(
        initialHour = initial.hour,
        initialMinute = initial.minute,
        is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(label, color = palette.ink) },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)) }) {
                Text(L10n.text("common.done"), color = palette.accentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(L10n.text("common.cancel"), color = palette.muted) }
        },
        containerColor = palette.surface
    )
}
