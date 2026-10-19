package com.oshicue.kit.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.AgendaCity
import com.oshicue.kit.core.AgendaEntry
import com.oshicue.kit.core.AgendaFlight
import com.oshicue.kit.core.AgendaKind
import com.oshicue.kit.core.AgendaPlace
import com.oshicue.kit.core.AgendaStore
import com.oshicue.kit.core.KitDate
import com.oshicue.kit.core.L10n
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@Composable
fun AgendaEditor(
    store: AgendaStore,
    entry: AgendaEntry,
    isNew: Boolean,
    onClose: () -> Unit,
    onSaved: (AgendaEntry) -> Unit
) {
    val kit = LocalKit.current
    var draft by remember(entry.id) { mutableStateOf(entry) }
    var flight by remember(entry.id) {
        val existing = entry.flights.firstOrNull() ?: AgendaFlight()
        mutableStateOf(
            existing.copy(
                departureAt = existing.departureAt ?: entry.startsAt,
                arrivalAt = existing.arrivalAt ?: entry.endsAt,
                departureTimeZone = existing.departureTimeZone ?: entry.timeZone,
                arrivalTimeZone = existing.arrivalTimeZone ?: entry.timeZone
            )
        )
    }
    val legacyFlights = remember(entry.id) { if (entry.kind == AgendaKind.Flight) emptyList() else entry.flights }
    var error by remember { mutableStateOf<String?>(null) }
    var showKind by remember { mutableStateOf(false) }
    var showTimeZone by remember { mutableStateOf(false) }
    var showFlightDepartureZone by remember { mutableStateOf(false) }
    var showFlightArrivalZone by remember { mutableStateOf(false) }
    var showCitySearch by remember { mutableStateOf(false) }

    ModalScreen(
        title = L10n.text(if (isNew) "agenda.add" else "agenda.edit"),
        onClose = onClose,
        actions = {
            KitTextAction(L10n.text("agenda.save"), enabled = draft.title.isNotBlank()) {
                val saved = saveAgendaDraft(draft, flight, legacyFlights)
                if (store.upsert(saved)) {
                    onSaved(saved)
                } else {
                    error = store.storageError
                    store.storageError = null
                }
            }
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            FieldLabel(L10n.text("agenda.name"))
            KitTextField(draft.title, { draft = draft.copy(title = it) }, L10n.text("agenda.name"), textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp))
            Spacer(Modifier.height(14.dp))

            FieldLabel(L10n.text("agenda.kind"))
            SelectorRow(
                value = agendaKindName(draft.kind),
                leading = { Icon(agendaKindIcon(draft.kind), null, tint = kit.accentText, modifier = Modifier.size(17.dp)) },
                onClick = { showKind = true }
            )

            if (draft.kind != AgendaKind.Flight) {
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L10n.text("agenda.allDay"), fontSize = 14.sp, color = kit.ink)
                    Spacer(Modifier.weight(1f))
                    Switch(
                        checked = draft.isAllDay,
                        onCheckedChange = { draft = toggleAllDay(draft, it) }
                    )
                }
                Spacer(Modifier.height(4.dp))
                DateTimeField(
                    label = L10n.text("agenda.starts"),
                    value = draft.startsAt,
                    zoneId = draft.zone,
                    onPickDate = { draft = changeAgendaStart(draft, it, null) },
                    onPickTime = { draft = changeAgendaStart(draft, null, it) }
                )
                DateTimeField(
                    label = L10n.text("agenda.ends"),
                    value = draft.endsAt,
                    zoneId = draft.zone,
                    onPickDate = { draft = changeAgendaEnd(draft, it, null) },
                    onPickTime = { draft = changeAgendaEnd(draft, null, it) }
                )
                SelectorRow(
                    label = L10n.text("agenda.timeZone"),
                    value = draft.timeZone,
                    onClick = { showTimeZone = true }
                )
                Text(
                    L10n.text(if (draft.isAllDay) "agenda.allDay.hint" else "agenda.timeZone.hint"),
                    fontSize = 11.sp,
                    color = kit.muted
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionLabel(L10n.text("agenda.location"))
            Spacer(Modifier.height(8.dp))
            FieldLabel(L10n.text("agenda.city"))
            KitTextField(
                draft.city?.name ?: "",
                { value ->
                    draft = draft.copy(
                        city = if (value.trim().isEmpty()) null else AgendaCity(name = value, country = draft.city?.country,
                            latitude = draft.city?.latitude, longitude = draft.city?.longitude)
                    )
                },
                L10n.text("agenda.city")
            )
            Spacer(Modifier.height(8.dp))
            KitSecondaryButton(L10n.text("agenda.city.search"), icon = Icons.Filled.Search) {
                showCitySearch = true
            }
            draft.city?.country?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, fontSize = 11.5.sp, color = kit.muted)
            }
            Text(L10n.text("agenda.city.manualHint"), fontSize = 11.sp, color = kit.muted)
            if (draft.kind != AgendaKind.Flight) {
                Spacer(Modifier.height(10.dp))
                FieldLabel(L10n.text("agenda.venue"))
                KitTextField(draft.venue, { draft = draft.copy(venue = it) }, L10n.text("agenda.venue"))
                Spacer(Modifier.height(12.dp))
                AgendaPlaceFields(
                    title = L10n.text("agenda.place"),
                    place = draft.location,
                    onChange = { draft = draft.copy(location = it) }
                )
            }

            if (draft.kind == AgendaKind.Flight) {
                Spacer(Modifier.height(18.dp))
                AgendaFlightFields(
                    flight = flight,
                    defaultDate = draft.startsAt,
                    defaultTimeZone = draft.timeZone,
                    onChange = { flight = it },
                    onPickDepartureZone = { showFlightDepartureZone = true },
                    onPickArrivalZone = { showFlightArrivalZone = true }
                )
            }

            Spacer(Modifier.height(18.dp))
            FieldLabel(L10n.text("agenda.notes"))
            KitTextField(draft.notes, { draft = draft.copy(notes = it) }, L10n.text("agenda.notes"), singleLine = false)
            error?.let {
                Spacer(Modifier.height(12.dp))
                InlineError(it)
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showKind) {
        OptionSelector(
            title = L10n.text("agenda.kind"),
            options = AgendaKind.entries.map { it.rawValue to agendaKindName(it) },
            selected = draft.kind.rawValue,
            onDismiss = { showKind = false },
            onSelect = { value ->
                val kind = AgendaKind.from(value) ?: AgendaKind.Live
                draft = applyAgendaKind(draft, flight, kind)
            }
        )
    }
    if (showTimeZone) {
        TimeZoneSelector(draft.timeZone, { showTimeZone = false }) { identifier ->
            draft = changeAgendaTimeZone(draft, identifier)
        }
    }
    if (showFlightDepartureZone) {
        TimeZoneSelector(flight.departureTimeZone ?: draft.timeZone, { showFlightDepartureZone = false }) { identifier ->
            flight = changeFlightZone(flight, identifier, departure = true, defaultDate = draft.startsAt, defaultZone = draft.timeZone)
        }
    }
    if (showFlightArrivalZone) {
        TimeZoneSelector(flight.arrivalTimeZone ?: draft.timeZone, { showFlightArrivalZone = false }) { identifier ->
            flight = changeFlightZone(flight, identifier, departure = false, defaultDate = draft.endsAt, defaultZone = draft.timeZone)
        }
    }
    if (showCitySearch) {
        CitySearchDialog(
            onDismiss = { showCitySearch = false },
            onSelect = { city, identifier ->
                draft = draft.copy(city = city)
                if (identifier != null) draft = changeAgendaTimeZone(draft, identifier)
            }
        )
    }
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier.padding(bottom = 5.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = LocalKit.current.muted
    )
}

@Composable
fun SelectorRow(
    value: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val kit = LocalKit.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(kit.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (label != null) {
            Text(label, fontSize = 13.5.sp, color = kit.ink)
            Spacer(Modifier.weight(1f))
        }
        if (leading != null) {
            leading()
            Spacer(Modifier.width(8.dp))
        }
        Text(value, fontSize = 13.5.sp, color = kit.accentText, fontWeight = FontWeight.Medium)
        if (label == null) Spacer(Modifier.weight(1f))
    }
}

fun toggleAllDay(entry: AgendaEntry, allDay: Boolean): AgendaEntry {
    if (!allDay) return entry.copy(isAllDay = false)
    val zone = entry.zone
    val start = entry.startsAt.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
    val midnight = entry.endsAt.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant()
    val end = if (midnight > start && midnight == entry.endsAt) midnight else midnight.plusSeconds(86_400)
    return entry.copy(isAllDay = true, startsAt = start, endsAt = end)
}

fun changeAgendaStart(entry: AgendaEntry, date: LocalDate?, time: LocalTime?): AgendaEntry {
    val zone = entry.zone
    val duration = entry.endsAt.toEpochMilli() - entry.startsAt.toEpochMilli()
    val newStart = when {
        date != null && entry.isAllDay -> date.atStartOfDay(zone).toInstant()
        date != null -> entry.startsAt.withDate(date, zone)
        time != null -> entry.startsAt.withTime(time, zone)
        else -> entry.startsAt
    }
    var newEnd = entry.endsAt
    if (!newEnd.isAfter(newStart)) {
        newEnd = if (entry.isAllDay) newStart.plusSeconds(86_400) else newStart.plusMillis(maxOf(60_000L, duration))
    }
    return entry.copy(startsAt = newStart, endsAt = newEnd)
}

fun changeAgendaEnd(entry: AgendaEntry, date: LocalDate?, time: LocalTime?): AgendaEntry {
    val zone = entry.zone
    val newEnd = when {
        date != null && entry.isAllDay -> date.plusDays(1).atStartOfDay(zone).toInstant()
        date != null -> entry.endsAt.withDate(date, zone)
        time != null -> entry.endsAt.withTime(time, zone)
        else -> entry.endsAt
    }
    return entry.copy(endsAt = newEnd)
}

/** Selecting a destination keeps the intended wall-clock times in that city. */
fun changeAgendaTimeZone(entry: AgendaEntry, identifier: String): AgendaEntry {
    val target = KitDate.zone(identifier) ?: return entry
    if (identifier == entry.timeZone) return entry
    val oldZone = entry.zone
    val oldStart = LocalDateTime.ofInstant(entry.startsAt, oldZone)
    val oldEnd = LocalDateTime.ofInstant(entry.endsAt, oldZone)
    return entry.copy(
        timeZone = identifier,
        startsAt = oldStart.atZone(target).toInstant(),
        endsAt = oldEnd.atZone(target).toInstant()
    )
}

fun applyAgendaKind(entry: AgendaEntry, flight: AgendaFlight, kind: AgendaKind): AgendaEntry {
    if (kind != AgendaKind.Flight) return entry.copy(kind = kind)
    val updatedFlight = if (entry.flights.isEmpty() && flight.number.isEmpty()) {
        flight.copy(
            departureAt = entry.startsAt,
            arrivalAt = entry.endsAt,
            departureTimeZone = entry.timeZone,
            arrivalTimeZone = entry.timeZone
        )
    } else {
        flight
    }
    return entry.copy(kind = kind, isAllDay = false, flights = if (entry.flights.isEmpty()) listOf(updatedFlight) else entry.flights)
}

fun changeFlightZone(
    flight: AgendaFlight,
    identifier: String,
    departure: Boolean,
    defaultDate: Instant,
    defaultZone: String
): AgendaFlight {
    val target = KitDate.zone(identifier) ?: return flight
    val oldIdentifier = if (departure) flight.departureTimeZone ?: defaultZone else flight.arrivalTimeZone ?: defaultZone
    val oldZone = KitDate.zone(oldIdentifier) ?: ZoneId.systemDefault()
    val date = if (departure) flight.departureAt ?: defaultDate else flight.arrivalAt ?: defaultDate
    val local = LocalDateTime.ofInstant(date, oldZone)
    val moved = local.atZone(target).toInstant()
    return if (departure) {
        flight.copy(departureTimeZone = identifier, departureAt = moved)
    } else {
        flight.copy(arrivalTimeZone = identifier, arrivalAt = moved)
    }
}

/** Trims and validates the draft, and for flights derives the activity times from the flight. */
fun saveAgendaDraft(entry: AgendaEntry, flight: AgendaFlight, legacyFlights: List<AgendaFlight>): AgendaEntry {
    var result = entry.copy(title = entry.title.trim())
    if (result.kind == AgendaKind.Flight) {
        val number = flight.number.trim().uppercase()
        val departureAt = flight.departureAt ?: result.startsAt
        val arrivalAt = flight.arrivalAt ?: result.endsAt
        val departureZone = flight.departureTimeZone ?: result.timeZone
        val arrivalZone = flight.arrivalTimeZone ?: result.timeZone
        result = result.copy(
            isAllDay = false,
            startsAt = departureAt,
            endsAt = arrivalAt,
            timeZone = departureZone,
            flights = listOf(flight.copy(number = number, departureAt = departureAt, arrivalAt = arrivalAt,
                departureTimeZone = departureZone, arrivalTimeZone = arrivalZone))
        )
    } else {
        result = result.copy(flights = legacyFlights)
    }
    return result
}

@Composable
fun AgendaPlaceFields(
    title: String,
    place: AgendaPlace?,
    onChange: (AgendaPlace?) -> Unit
) {
    val kit = LocalKit.current
    var showPicker by remember { mutableStateOf(false) }
    Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
    Spacer(Modifier.height(6.dp))
    FieldLabel(L10n.text("agenda.place.name"))
    KitTextField(
        place?.name ?: "",
        { value ->
            val candidate = (place ?: AgendaPlace(name = "")).copy(
                name = value, latitude = null, longitude = null
            )
            onChange(if (candidate.name.isEmpty() && candidate.address.isEmpty()) null else candidate)
        },
        L10n.text("agenda.place.name")
    )
    Spacer(Modifier.height(6.dp))
    FieldLabel(L10n.text("agenda.place.address"))
    KitTextField(
        place?.address ?: "",
        { value ->
            val candidate = (place ?: AgendaPlace(name = "")).copy(
                address = value, latitude = null, longitude = null
            )
            onChange(if (candidate.name.isEmpty() && candidate.address.isEmpty()) null else candidate)
        },
        L10n.text("agenda.place.address")
    )
    Spacer(Modifier.height(8.dp))
    KitSecondaryButton(L10n.text("agenda.place.search"), icon = Icons.Filled.LocationOn) {
        showPicker = true
    }
    if (place?.latitude != null && place.longitude != null) {
        Text(L10n.text("agenda.place.selected"), fontSize = 11.sp, color = kit.muted)
    }
    if (place != null) {
        Spacer(Modifier.height(6.dp))
        KitSecondaryButton(L10n.text("agenda.place.clear"), icon = Icons.Filled.Close) { onChange(null) }
    }
    if (showPicker) {
        PlaceSearchDialog(
            onDismiss = { showPicker = false },
            onSelect = { selected -> onChange(selected) }
        )
    }
}

@Composable
private fun AgendaFlightFields(
    flight: AgendaFlight,
    defaultDate: Instant,
    defaultTimeZone: String,
    onChange: (AgendaFlight) -> Unit,
    onPickDepartureZone: () -> Unit,
    onPickArrivalZone: () -> Unit
) {
    val kit = LocalKit.current
    SectionLabel(L10n.text("agenda.kind.flight"))
    Spacer(Modifier.height(8.dp))
    FieldLabel(L10n.text("agenda.flight.number"))
    KitTextField(
        flight.number,
        { onChange(flight.copy(number = it.uppercase())) },
        L10n.text("agenda.flight.number"),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    )
    Spacer(Modifier.height(8.dp))
    FieldLabel(L10n.text("agenda.flight.airline"))
    KitTextField(flight.airline ?: "", { onChange(flight.copy(airline = it.ifEmpty { null })) }, L10n.text("agenda.flight.airline"))
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Column(Modifier.weight(1f)) {
            FieldLabel(L10n.text("agenda.flight.from"))
            KitTextField(flight.departureAirport ?: "", { onChange(flight.copy(departureAirport = it.ifEmpty { null })) }, L10n.text("agenda.flight.from"))
        }
        Column(Modifier.weight(1f)) {
            FieldLabel(L10n.text("agenda.flight.to"))
            KitTextField(flight.arrivalAirport ?: "", { onChange(flight.copy(arrivalAirport = it.ifEmpty { null })) }, L10n.text("agenda.flight.to"))
        }
    }
    Spacer(Modifier.height(10.dp))
    val departureZone = KitDate.zoneOrUtc(flight.departureTimeZone ?: defaultTimeZone)
    val arrivalZone = KitDate.zoneOrUtc(flight.arrivalTimeZone ?: defaultTimeZone)
    DateTimeField(
        label = L10n.text("agenda.flight.departure"),
        value = flight.departureAt ?: defaultDate,
        zoneId = departureZone,
        onPickDate = { onChange(flight.copy(departureAt = (flight.departureAt ?: defaultDate).withDate(it, departureZone))) },
        onPickTime = { onChange(flight.copy(departureAt = (flight.departureAt ?: defaultDate).withTime(it, departureZone))) }
    )
    SelectorRow(
        label = L10n.text("agenda.flight.departureZone"),
        value = flight.departureTimeZone ?: defaultTimeZone,
        onClick = onPickDepartureZone
    )
    Spacer(Modifier.height(8.dp))
    DateTimeField(
        label = L10n.text("agenda.flight.arrival"),
        value = flight.arrivalAt ?: defaultDate,
        zoneId = arrivalZone,
        onPickDate = { onChange(flight.copy(arrivalAt = (flight.arrivalAt ?: defaultDate).withDate(it, arrivalZone))) },
        onPickTime = { onChange(flight.copy(arrivalAt = (flight.arrivalAt ?: defaultDate).withTime(it, arrivalZone))) }
    )
    SelectorRow(
        label = L10n.text("agenda.flight.arrivalZone"),
        value = flight.arrivalTimeZone ?: defaultTimeZone,
        onClick = onPickArrivalZone
    )
    Spacer(Modifier.height(10.dp))
    AgendaPlaceFields(L10n.text("agenda.flight.departurePlace"), flight.departureLocation) {
        onChange(flight.copy(departureLocation = it))
    }
    Spacer(Modifier.height(10.dp))
    AgendaPlaceFields(L10n.text("agenda.flight.arrivalPlace"), flight.arrivalLocation) {
        onChange(flight.copy(arrivalLocation = it))
    }
    Spacer(Modifier.height(6.dp))
    Text(L10n.text("agenda.flight.recordedHint"), fontSize = 11.sp, color = kit.muted)
}

/**
 * Android has no MKLocalSearch equivalent without an API key, so the picker uses the
 * platform Geocoder. Results carry no time zone; the caller keeps the current zone.
 */
@Composable
fun CitySearchDialog(
    onDismiss: () -> Unit,
    onSelect: (AgendaCity, String?) -> Unit
) {
    var query by remember { mutableStateOf("") }
    PlaceSearchDialog(
        title = L10n.text("agenda.city.search"),
        hint = L10n.text("agenda.city.hint"),
        onDismiss = onDismiss,
        onQueryChange = { query = it },
        onSelect = { place ->
            onSelect(AgendaCity(place.name, null, place.latitude, place.longitude), null)
        }
    )
}

@Composable
fun PlaceSearchDialog(
    title: String = L10n.text("agenda.place.search"),
    hint: String = L10n.text("agenda.place.name"),
    onDismiss: () -> Unit,
    onQueryChange: (String) -> Unit = {},
    onSelect: (AgendaPlace) -> Unit
) {
    val kit = LocalKit.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<PlaceResult>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold, color = kit.ink) },
        text = {
            Column {
                KitTextField(query, { value ->
                    query = value
                    onQueryChange(value)
                    loading = value.isNotBlank()
                    if (value.isBlank()) {
                        results = emptyList()
                        error = null
                        loading = false
                    } else {
                        searchPlaces(context, value) { found, failure ->
                            results = found
                            error = failure
                            loading = false
                        }
                    }
                }, hint)
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                if (query.isBlank()) {
                    Text(hint, fontSize = 11.5.sp, color = kit.muted)
                }
                error?.let { Text(it, fontSize = 11.5.sp, color = kit.accentText) }
                androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(results) { result ->
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(
                                        AgendaPlace(
                                            name = result.name,
                                            address = result.address,
                                            latitude = result.latitude,
                                            longitude = result.longitude
                                        )
                                    )
                                    onDismiss()
                                }
                                .padding(vertical = 9.dp)
                        ) {
                            Text(result.name, fontSize = 13.5.sp, color = kit.ink, fontWeight = FontWeight.Medium)
                            if (result.address.isNotEmpty()) {
                                Text(result.address, fontSize = 11.sp, color = kit.muted, maxLines = 2)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(L10n.text("common.cancel"), color = kit.muted)
            }
        }
    )
}

data class PlaceResult(val name: String, val address: String, val latitude: Double?, val longitude: Double?)

/**
 * Android Geocoder results have no time zone. The caller keeps its current zone; a manual
 * time-zone selection remains available in the editor.
 */
fun searchPlaces(
    context: android.content.Context,
    query: String,
    callback: (List<PlaceResult>, String?) -> Unit
) {
    // Place results follow the device language, as MKLocalSearch does on iOS.
    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
    if (!android.location.Geocoder.isPresent()) {
        callback(emptyList(), L10n.text("agenda.city.hint"))
        return
    }
    try {
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocationName(query, 8)
        val results = addresses.orEmpty().map { address ->
            val name = address.locality ?: address.subAdminArea ?: address.adminArea ?: address.featureName ?: query
            val line = listOfNotNull(
                address.thoroughfare, address.subThoroughfare, address.locality, address.adminArea, address.countryName
            ).distinct().joinToString(", ")
            PlaceResult(name, line, address.latitude, address.longitude)
        }
        callback(results, null)
    } catch (failure: Exception) {
        callback(emptyList(), failure.localizedMessage)
    }
}
