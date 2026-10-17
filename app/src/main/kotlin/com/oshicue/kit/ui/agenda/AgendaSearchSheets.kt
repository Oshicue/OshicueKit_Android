package com.oshicue.kit.ui.agenda

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.AgendaCity
import com.oshicue.kit.core.AgendaPlace
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.KitSheet
import com.oshicue.kit.ui.theme.kitPalette
import kotlinx.coroutines.delay

@Composable
private fun SearchField(value: String, placeholder: String, onValueChange: (String) -> Unit) {
    val palette = kitPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Search, contentDescription = null, tint = palette.muted, modifier = Modifier.size(16.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            placeholder = { Text(placeholder, color = palette.muted, fontSize = 13.sp) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = TextStyle(color = palette.ink, fontSize = 14.sp),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun AgendaCityPicker(onSelect: (AgendaCity) -> Unit, onClose: () -> Unit) {
    val palette = kitPalette()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<AgendaCity>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        results = emptyList()
        error = null
        if (query.trim().isEmpty()) return@LaunchedEffect
        delay(450)
        loading = true
        try {
            results = searchCities(context, query)
        } catch (value: Exception) {
            error = value.message ?: value.toString()
        } finally {
            loading = false
        }
    }

    KitSheet(L10n.text("agenda.city.search"), onClose) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            SearchField(query, L10n.text("agenda.city"), { query = it })
            if (query.trim().isEmpty()) {
                Spacer(modifier = Modifier.size(12.dp))
                Text(L10n.text("agenda.city.hint"), color = palette.muted, fontSize = 12.sp)
            }
            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator(modifier = Modifier.size(22.dp), color = palette.accentText) }
            }
            error?.let {
                Text(it, color = palette.warningText, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
            }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(results) { city ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(city)
                                onClose()
                            }
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(city.name, color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        val subtitle = listOfNotNull(city.country, city.latitude?.let { latitude ->
                            city.longitude?.let { longitude -> "%.5f, %.5f".format(latitude, longitude) }
                        }).joinToString(" · ")
                        if (subtitle.isNotEmpty()) {
                            Text(subtitle, color = palette.muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaPlacePicker(onSelect: (AgendaPlace) -> Unit, onClose: () -> Unit) {
    val palette = kitPalette()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<AgendaPlace>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        results = emptyList()
        error = null
        if (query.trim().isEmpty()) return@LaunchedEffect
        delay(450)
        loading = true
        try {
            results = searchPlaces(context, query)
        } catch (value: Exception) {
            error = value.message ?: value.toString()
        } finally {
            loading = false
        }
    }

    KitSheet(L10n.text("agenda.place.search"), onClose) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            SearchField(query, L10n.text("agenda.place.name"), { query = it })
            if (loading) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.Center
                ) { CircularProgressIndicator(modifier = Modifier.size(22.dp), color = palette.accentText) }
            }
            error?.let {
                Text(it, color = palette.warningText, fontSize = 12.sp, modifier = Modifier.padding(vertical = 10.dp))
            }
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(results) { place ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(place)
                                onClose()
                            }
                            .padding(vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(place.name, color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        if (place.address.isNotEmpty()) {
                            Text(place.address, color = palette.muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaTimeZonePicker(current: String, onSelect: (String) -> Unit, onClose: () -> Unit) {
    val palette = kitPalette()
    var query by remember { mutableStateOf("") }
    val zones = remember(query) {
        java.time.ZoneId.getAvailableZoneIds()
            .filter { query.isBlank() || it.replace('_', ' ').contains(query, ignoreCase = true) }
            .sorted()
    }

    KitSheet(L10n.text("agenda.timeZone"), onClose) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            SearchField(query, L10n.text("agenda.timeZone"), { query = it })
            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(zones) { zone ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(zone)
                                onClose()
                            }
                            .padding(vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(zoneText(zone), color = palette.ink, fontSize = 14.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        if (zone == current) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(17.dp))
                        }
                    }
                }
            }
        }
    }
}
