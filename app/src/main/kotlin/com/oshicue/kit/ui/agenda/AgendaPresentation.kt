package com.oshicue.kit.ui.agenda

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.provider.CalendarContract
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import com.oshicue.kit.core.AgendaCity
import com.oshicue.kit.core.AgendaCore
import com.oshicue.kit.core.AgendaEntry
import com.oshicue.kit.core.AgendaKind
import com.oshicue.kit.core.AgendaPlace
import com.oshicue.kit.core.AgendaTransfer
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.theme.KitDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

enum class AgendaTravelMode(val raw: String, val travelMode: String) {
    driving("driving", "driving"),
    walking("walking", "walking"),
    transit("transit", "transit")
}

fun AgendaKind.displayName(): String = L10n.text("agenda.kind.$raw")

fun AgendaKind.symbol(): ImageVector = when (this) {
    AgendaKind.live -> Icons.Filled.GraphicEq
    AgendaKind.flight -> Icons.Filled.Flight
    AgendaKind.hotel -> Icons.Filled.Hotel
    AgendaKind.restaurant -> Icons.Filled.Restaurant
    AgendaKind.other -> Icons.Filled.Place
}

fun AgendaEntry.timeRangeText(): String {
    if (isAllDay) return L10n.text("agenda.allDay")
    if (kind == AgendaKind.flight) {
        val value = flights.firstOrNull()
        val departure = value?.departureAt
        val arrival = value?.arrivalAt
        val departureZone = AgendaCore.zoneOrNull(value?.departureTimeZone)
        val arrivalZone = AgendaCore.zoneOrNull(value?.arrivalTimeZone)
        if (departure != null && arrival != null && departureZone != null && arrivalZone != null) {
            return KitDate.string(departure, "MM.dd HH:mm", departureZone) + " · " + departureZone.id +
                " → " + KitDate.string(arrival, "MM.dd HH:mm", arrivalZone) + " · " + arrivalZone.id
        }
    }
    return KitDate.string(startsAt, "MM.dd HH:mm", timeZoneValue) + " – " +
        KitDate.string(endsAt, "MM.dd HH:mm", timeZoneValue)
}

fun AgendaEntry.dateRangeText(): String {
    val range = AgendaCore.dateRange(this)
        ?: return LedgerDates.string(startsAt, timeZoneValue)
    return if (range.first == range.second) range.first else range.first + " – " + range.second
}

fun AgendaEntry.startDay(): LocalDate = startsAt.atZone(timeZoneValue).toLocalDate()

fun AgendaEntry.endDisplayDay(): LocalDate =
    if (isAllDay) endsAt.minusNanos(1).atZone(timeZoneValue).toLocalDate()
    else endsAt.atZone(timeZoneValue).toLocalDate()

fun AgendaEntry.startText(): String =
    if (isAllDay) KitDate.string(startsAt, "yyyy.MM.dd", timeZoneValue)
    else KitDate.string(startsAt, "yyyy.MM.dd HH:mm", timeZoneValue)

fun AgendaEntry.endText(): String =
    if (isAllDay) KitDate.string(endDisplayDay(), "yyyy.MM.dd")
    else KitDate.string(endsAt, "yyyy.MM.dd HH:mm", timeZoneValue)

fun AgendaEntry.locationDescription(): String {
    val precise = if (kind == AgendaKind.flight) flights.firstOrNull()?.departureLocation else location
    if (precise != null) return listOf(precise.name, precise.address).filter { it.isNotEmpty() }.joinToString(" · ")
    return listOf(city?.name ?: "", venue).filter { it.isNotEmpty() }.joinToString(" · ")
}

fun AgendaEntry.flightDescription(): String = buildString {
    if (notes.isNotEmpty()) append(notes)
    for (flight in flights) {
        if (isNotEmpty()) append('\n')
        append(
            listOfNotNull(flight.airline, flight.number, flight.departureAirport, flight.arrivalAirport)
                .filter { it.isNotEmpty() }.joinToString(" · ")
        )
    }
}

fun openMap(context: Context, place: AgendaPlace) {
    val coordinates = if (place.latitude != null && place.longitude != null) {
        "${place.latitude},${place.longitude}"
    } else {
        null
    }
    val uri = if (coordinates != null) {
        Uri.parse("geo:$coordinates?q=$coordinates(${Uri.encode(place.name)})")
    } else {
        Uri.parse("geo:0,0?q=" + Uri.encode(listOf(place.name, place.address).filter { it.isNotEmpty() }.joinToString(" ")))
    }
    startMap(context, uri, listOf(place.name, place.address).filter { it.isNotEmpty() }.joinToString(" "))
}

fun openMap(context: Context, city: AgendaCity, venue: String) {
    val hasCoordinates = city.latitude != null && city.longitude != null
    val coordinates = if (hasCoordinates) "${city.latitude},${city.longitude}" else null
    val query = listOf(city.name, venue).filter { it.isNotEmpty() }.joinToString(" ")
    val uri = if (coordinates != null && venue.isEmpty()) {
        Uri.parse("geo:$coordinates?q=$coordinates(${Uri.encode(city.name)})")
    } else {
        Uri.parse("geo:0,0?q=" + Uri.encode(query))
    }
    startMap(context, uri, query)
}

private fun startMap(context: Context, uri: Uri, fallbackQuery: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: ActivityNotFoundException) {
        runCatching {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(fallbackQuery))
                )
            )
        }
    }
}

fun openRoute(context: Context, transfer: AgendaTransfer, mode: AgendaTravelMode) {
    val origin = transfer.origin
    val destination = transfer.destination
    if (origin == null || destination == null) return
    val originValue = if (origin.latitude != null && origin.longitude != null) {
        "${origin.latitude},${origin.longitude}"
    } else {
        listOf(origin.name, origin.address).filter { it.isNotEmpty() }.joinToString(" ")
    }
    val destinationValue = if (destination.latitude != null && destination.longitude != null) {
        "${destination.latitude},${destination.longitude}"
    } else {
        listOf(destination.name, destination.address).filter { it.isNotEmpty() }.joinToString(" ")
    }
    val url = "https://www.google.com/maps/dir/?api=1" +
        "&origin=" + Uri.encode(originValue) +
        "&destination=" + Uri.encode(destinationValue) +
        "&travelmode=" + mode.travelMode
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

fun openFlight(context: Context, number: String) {
    val value = number.trim()
    if (value.isEmpty()) return
    val url = "https://www.google.com/travel/flights?q=" + Uri.encode(value)
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

fun addToCalendar(context: Context, entry: AgendaEntry): Boolean {
    val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
    val zone = entry.timeZoneValue
    val begin = if (entry.isAllDay) {
        entry.startsAt.atZone(zone).toLocalDate().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    } else {
        entry.startsAt.toEpochMilli()
    }
    val end = if (entry.isAllDay) {
        entry.endsAt.atZone(zone).toLocalDate().atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    } else {
        entry.endsAt.toEpochMilli()
    }
    val location = entry.locationDescription()
    intent.putExtra(CalendarContract.Events.TITLE, entry.title)
    intent.putExtra(CalendarContract.Events.DESCRIPTION, entry.flightDescription())
    intent.putExtra(CalendarContract.Events.EVENT_LOCATION, location)
    intent.putExtra(CalendarContract.Events.ALL_DAY, entry.isAllDay)
    intent.putExtra(CalendarContract.Events.EVENT_TIMEZONE, entry.timeZone)
    intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
    intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}

fun zoneText(identifier: String): String = identifier.replace('_', ' ')

fun zoneIdOf(identifier: String): ZoneId = AgendaCore.zoneOrNull(identifier) ?: ZoneId.systemDefault()

fun instantText(value: Instant, zone: ZoneId, pattern: String = "MM.dd HH:mm"): String =
    KitDate.string(value, pattern, zone) + " · " + zone.id

fun agendaWeekStart(locale: Locale): DayOfWeek =
    WeekFields.of(locale).firstDayOfWeek

fun monthDays(month: YearMonth, locale: Locale): List<LocalDate> {
    val first = month.atDay(1)
    val start = first.with(java.time.temporal.TemporalAdjusters.previousOrSame(agendaWeekStart(locale)))
    return (0L..41L).map { start.plusDays(it) }
}

fun weekdayLabels(locale: Locale): List<String> {
    val start = agendaWeekStart(locale)
    return (0L..6L).map {
        start.plus(it).getDisplayName(java.time.format.TextStyle.SHORT, locale)
    }
}

@Suppress("DEPRECATION")
suspend fun searchCities(context: Context, query: String): List<AgendaCity> = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) throw IllegalStateException(L10n.text("agenda.error.city"))
    val results = Geocoder(context, L10n.currentLocale()).getFromLocationName(query, 8) ?: emptyList()
    results.mapNotNull { address ->
        val name = address.locality ?: address.subAdminArea ?: address.adminArea ?: address.featureName
        ?: address.getAddressLine(0)
        if (name.isNullOrBlank() || address.latitude == 0.0 && address.longitude == 0.0) return@mapNotNull null
        AgendaCity(
            name = name,
            country = address.countryName,
            latitude = address.latitude,
            longitude = address.longitude
        )
    }.distinctBy { it.name to Pair(it.latitude, it.longitude) }
}

@Suppress("DEPRECATION")
suspend fun searchPlaces(context: Context, query: String): List<AgendaPlace> = withContext(Dispatchers.IO) {
    if (!Geocoder.isPresent()) throw IllegalStateException(L10n.text("agenda.error.location"))
    val results = Geocoder(context, L10n.currentLocale()).getFromLocationName(query, 8) ?: emptyList()
    results.mapNotNull { address ->
        val name = address.featureName ?: address.thoroughfare ?: address.locality
        ?: address.getAddressLine(0)
        if (name.isNullOrBlank() || address.latitude == 0.0 && address.longitude == 0.0) return@mapNotNull null
        val lines = (0..address.maxAddressLineIndex).mapNotNull { address.getAddressLine(it) }
        AgendaPlace(
            name = name,
            address = lines.joinToString(", "),
            latitude = address.latitude,
            longitude = address.longitude
        )
    }.distinctBy { it.name to Pair(it.latitude, it.longitude) }
}
