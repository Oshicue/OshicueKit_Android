package com.oshicue.kit.ui.agenda

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.AgendaCore
import com.oshicue.kit.core.AgendaDocument
import com.oshicue.kit.core.AgendaEntry
import com.oshicue.kit.core.AgendaEntryDeletion
import com.oshicue.kit.core.AgendaEntryFilter
import com.oshicue.kit.core.AgendaImporter
import com.oshicue.kit.core.AgendaKind
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.data.AgendaStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.KitAlert
import com.oshicue.kit.ui.common.KitDatePickerDialog
import com.oshicue.kit.ui.common.KitDropdownRow
import com.oshicue.kit.ui.common.KitSheet
import com.oshicue.kit.ui.common.kitStandardSort
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.kitPalette
import java.time.LocalDate

@Composable
fun AgendaManagementSheet(
    store: AgendaStore,
    onApplied: ((AgendaEntry?) -> Unit)? = null,
    onClose: () -> Unit
) {
    val palette = kitPalette()
    var section by remember { mutableStateOf(0) }
    KitSheet(L10n.text("agenda.management"), onClose) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp)
                    .background(palette.surfaceMuted, RoundedCornerShape(11.dp))
                    .padding(3.dp)
            ) {
                for ((index, label) in listOf(
                    L10n.text("management.records"),
                    L10n.text("agenda.import"),
                    L10n.text("management.backup")
                ).withIndex()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(if (section == index) palette.surface else Color.Transparent, RoundedCornerShape(9.dp))
                            .clickable { section = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (section == index) palette.accentText else palette.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
            }
            when (section) {
                0 -> AgendaRecordManagement(store)
                1 -> AgendaImportContent(store, onApplied ?: { onClose() })
                else -> AgendaBackupContent(store)
            }
        }
    }
}

@Composable
private fun AgendaRecordManagement(store: AgendaStore) {
    val palette = kitPalette()
    var search by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var useDateRange by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var kind by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var timeZone by remember { mutableStateOf("") }
    var allDay by remember { mutableStateOf("") }
    var selectedIDs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeletion by remember { mutableStateOf<AgendaEntryDeletion?>(null) }
    var deletingAll by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val filter = AgendaEntryFilter(
        startDate = if (useDateRange) LedgerDates.string(startDate) else null,
        endDate = if (useDateRange) LedgerDates.string(endDate) else null,
        kind = AgendaKind.fromRaw(kind),
        city = city.ifEmpty { null },
        place = place.ifEmpty { null },
        timeZone = timeZone.ifEmpty { null },
        isAllDay = if (allDay.isEmpty()) null else allDay == "allDay",
        query = search
    )
    val entries = remember(filter, store.document.entries) {
        filter.matching(store.document.entries).sortedWith(
            compareByDescending<AgendaEntry> {
                AgendaCore.dateRange(it)?.first ?: LedgerDates.string(it.startsAt, it.timeZoneValue)
            }.thenByDescending { it.startsAt }.thenBy { it.id }
        )
    }
    LaunchedEffect(filter) { selectedIDs = emptySet() }
    LaunchedEffect(entries.map { it.id }) {
        selectedIDs = selectedIDs.intersect(entries.map { it.id }.toSet())
    }

    fun optionValues(values: List<String>): List<Pair<String, String>> {
        val unique = values.filter { it.isNotBlank() }.toSet()
        return unique.sortedWith { left, right -> kitStandardSort(left, right) }.map { it to it }
    }

    val cityOptions = optionValues(
        store.document.entries.mapNotNull { it.city?.name } + city
    )
    val placeOptions = optionValues(
        store.document.entries.flatMap { entry ->
            listOfNotNull(entry.location?.name, entry.venue.ifEmpty { null }) + entry.flights.flatMap {
                listOfNotNull(it.departureLocation?.name, it.arrivalLocation?.name, it.departureAirport, it.arrivalAirport)
            }
        } + place
    )
    val timeZoneOptions = optionValues(
        store.document.entries.flatMap { entry ->
            listOf(entry.timeZone) + entry.flights.flatMap {
                listOfNotNull(it.departureTimeZone, it.arrivalTimeZone)
            }
        } + timeZone
    )

    fun prepareDeletion(all: Boolean) {
        val ids = if (all) store.document.entries.map { it.id }.toSet() else selectedIDs
        val deletion = AgendaEntryDeletion(store.document.entries, ids)
        if (deletion.entryCount == 0) return
        deletingAll = all
        pendingDeletion = deletion
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = palette.muted, modifier = Modifier.size(16.dp))
                TextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    placeholder = { Text(L10n.text("agenda.management.search"), color = palette.muted, fontSize = 13.sp) },
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
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showFilters = !showFilters },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(L10n.text("management.filters"), color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = palette.muted, modifier = Modifier.size(18.dp))
            }
            if (showFilters) {
                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(L10n.text("management.dateRange"), color = palette.ink, fontSize = 14.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = useDateRange,
                            onCheckedChange = { useDateRange = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = palette.accentFill)
                        )
                    }
                    if (useDateRange) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(L10n.text("management.startDate"), color = palette.ink, fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(KitDate.string(startDate, "yyyy MMM d"), color = palette.accentText, fontSize = 14.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(L10n.text("management.endDate"), color = palette.ink, fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(KitDate.string(endDate, "yyyy MMM d"), color = palette.accentText, fontSize = 14.sp)
                        }
                        if (!filter.hasValidDateRange) {
                            Text(L10n.text("management.invalidDateRange"), color = palette.warningText, fontSize = 12.sp)
                        }
                    }
                    KitDropdownRow(
                        label = L10n.text("agenda.kind"),
                        value = AgendaKind.fromRaw(kind)?.displayName() ?: "",
                        options = listOf("" to L10n.text("management.all")) +
                            AgendaKind.entries.map { it.raw to it.displayName() },
                        onSelect = { kind = it }
                    )
                    KitDropdownRow(
                        label = L10n.text("agenda.city"),
                        value = city,
                        options = listOf("" to L10n.text("management.all")) + cityOptions,
                        onSelect = { city = it }
                    )
                    KitDropdownRow(
                        label = L10n.text("agenda.place"),
                        value = place,
                        options = listOf("" to L10n.text("management.all")) + placeOptions,
                        onSelect = { place = it }
                    )
                    KitDropdownRow(
                        label = L10n.text("agenda.timeZone"),
                        value = timeZone,
                        options = listOf("" to L10n.text("management.all")) + timeZoneOptions,
                        onSelect = { timeZone = it }
                    )
                    KitDropdownRow(
                        label = L10n.text("agenda.allDay"),
                        value = when (allDay) {
                            "allDay" -> L10n.text("agenda.allDay")
                            "timed" -> L10n.text("agenda.management.timed")
                            else -> ""
                        },
                        options = listOf(
                            "" to L10n.text("management.all"),
                            "allDay" to L10n.text("agenda.allDay"),
                            "timed" to L10n.text("agenda.management.timed")
                        ),
                        onSelect = { allDay = it }
                    )
                    Text(L10n.text("management.selectionHint"), color = palette.muted, fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    L10n.text("management.selectFiltered"),
                    color = if (entries.isEmpty()) palette.muted.copy(alpha = 0.5f) else palette.accentText,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(enabled = entries.isNotEmpty()) {
                        selectedIDs = entries.map { it.id }.toSet()
                    }
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    L10n.text("management.clearSelection"),
                    color = if (selectedIDs.isEmpty()) palette.muted.copy(alpha = 0.5f) else palette.accentText,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(enabled = selectedIDs.isNotEmpty()) { selectedIDs = emptySet() }
                )
            }
            Text(L10n.format("management.resultCount", entries.size), color = palette.muted, fontSize = 13.sp)
            if (entries.isEmpty()) {
                Text(L10n.text("management.empty"), color = palette.muted, fontSize = 14.sp)
            }
            for (entry in entries) {
                val selected = entry.id in selectedIDs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedIDs = if (selected) selectedIDs - entry.id else selectedIDs + entry.id
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) palette.accentText else palette.muted,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(entry.title, color = palette.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            entry.dateRangeText() + " · " + entry.kind.displayName(),
                            color = palette.muted,
                            fontSize = 12.sp
                        )
                        Text(entry.timeRangeText(), color = palette.muted, fontSize = 12.sp)
                        val places = listOfNotNull(entry.city?.name, entry.location?.name)
                        if (places.isNotEmpty()) Text(places.joinToString(" · "), color = palette.muted, fontSize = 12.sp)
                        if (entry.flights.isNotEmpty()) {
                            Text(entry.flights.map { it.number }.joinToString(" · "), color = palette.muted, fontSize = 12.sp)
                        }
                        Text(entry.timeZone, color = palette.muted, fontSize = 10.sp)
                        if (entry.notes.isNotEmpty()) {
                            Text(entry.notes, color = palette.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                L10n.text("agenda.management.clearAll"),
                color = if (store.document.entries.isEmpty()) palette.muted.copy(alpha = 0.5f) else palette.warningText,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable(enabled = store.document.entries.isNotEmpty()) { prepareDeletion(all = true) }
                    .padding(vertical = 8.dp)
            )
            Text(L10n.text("agenda.management.clearAllHint"), color = palette.muted, fontSize = 11.sp)
            error?.let { Text(it, color = palette.warningText, fontSize = 12.sp) }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(palette.surface).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(L10n.format("management.selectedCount", selectedIDs.size), color = palette.ink, fontSize = 14.sp)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { prepareDeletion(all = false) },
                enabled = selectedIDs.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceMuted, contentColor = palette.warningText),
                shape = RoundedCornerShape(12.dp)
            ) { Text(L10n.text("management.deleteSelected"), fontSize = 13.sp) }
        }
    }

    if (showStartPicker) KitDatePickerDialog(startDate, { startDate = it }, { showStartPicker = false })
    if (showEndPicker) KitDatePickerDialog(endDate, { endDate = it }, { showEndPicker = false })
    pendingDeletion?.let { deletion ->
        KitAlert(
            title = L10n.text(if (deletingAll) "agenda.management.clearTitle" else "agenda.management.deleteTitle"),
            message = L10n.format("agenda.management.deleteSummary", deletion.entryCount) +
                "\n\n" + L10n.text("agenda.management.deleteImpact"),
            confirmLabel = L10n.text(if (deletingAll) "agenda.management.clearAll" else "management.deleteSelected"),
            onConfirm = {
                if (store.deleteEntries(deletion.entryIDs)) {
                    selectedIDs = selectedIDs - deletion.entryIDs
                } else {
                    error = store.storageError
                    store.storageError = null
                }
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null },
            destructive = true
        )
    }
}

@Composable
private fun AgendaImportContent(store: AgendaStore, onApplied: (AgendaEntry?) -> Unit) {
    val palette = kitPalette()
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<AgendaDocument?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        try {
            val documents = mutableListOf<AgendaDocument>()
            for (uri in uris) {
                val input = context.contentResolver.openInputStream(uri)?.readBytes()
                    ?: throw IllegalStateException("Unreadable file")
                documents.add(AgendaImporter.parse(input))
            }
            val combined = AgendaImporter.combining(documents)
            checkPreview(store, combined)
            text = AgendaImporter.encode(combined)
            preview = combined
            error = null
        } catch (value: Exception) {
            preview = null
            error = agendaError(value)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(palette.surface, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Download, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(L10n.text("agenda.import"), color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("JSON · UTF-8", color = palette.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
        Text(L10n.text("agenda.backup.importHint"), color = palette.muted, fontSize = 12.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier.clickable {
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = palette.ink, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(L10n.text("ledger.file"), color = palette.ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.clickable {
                    val template = runCatching {
                        context.assets.open("agenda-template.json").bufferedReader().use { it.readText() }
                    }.getOrNull() ?: ""
                    text = template
                    preview = null
                    error = null
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = palette.ink, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(L10n.text("agenda.backup.template"), color = palette.ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
        TextField(
            value = text,
            onValueChange = {
                text = it
                preview = null
                error = null
            },
            placeholder = { Text("JSON · UTF-8", color = palette.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = palette.ink),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = palette.surface,
                unfocusedContainerColor = palette.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )
        Button(
            onClick = {
                try {
                    val incoming = AgendaImporter.parse(text)
                    checkPreview(store, incoming)
                    preview = incoming
                    error = null
                } catch (value: Exception) {
                    preview = null
                    error = agendaError(value)
                }
            },
            enabled = text.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text(L10n.text("import.validate"), fontWeight = FontWeight.SemiBold) }
        error?.let { message ->
            Text(
                message,
                color = palette.warningText,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.warningText.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            )
        }
        preview?.let { document ->
            Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF2E9E5B), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L10n.text("agenda.backup.preview"), color = palette.ink, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                val existing = store.document.entries.map { it.id }.toSet()
                val updates = document.entries.count { it.id in existing }
                Text(
                    L10n.format("agenda.backup.summary", document.entries.size - updates, updates),
                    color = palette.ink,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                for (entry in document.entries.take(8)) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Text(entry.title, color = palette.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            LedgerDates.string(entry.startsAt, entry.timeZoneValue) + " · " +
                                (entry.city?.name ?: entry.timeZone),
                            color = palette.muted,
                            fontSize = 11.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (store.importDocument(document)) {
                            val first = document.entries.minByOrNull { it.startsAt }
                            onApplied(first)
                        } else {
                            error = store.storageError
                            store.storageError = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) { Text(L10n.text("agenda.backup.apply"), fontWeight = FontWeight.SemiBold) }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun AgendaBackupContent(store: AgendaStore) {
    val palette = kitPalette()
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = pendingExport
        if (uri != null && content != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            } catch (value: Exception) {
                error = value.message
            }
        }
        pendingExport = null
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(palette.surface, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Inventory, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(L10n.text("agenda.backup"), color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(L10n.text("agenda.backupHint"), color = palette.muted, fontSize = 12.sp)
            }
        }
        Button(
            onClick = {
                try {
                    pendingExport = store.exportJSON()
                    exportLauncher.launch("OshicueKit-agenda-" + LedgerDates.string(LocalDate.now()))
                } catch (value: Exception) {
                    error = agendaError(value)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = palette.surface, contentColor = palette.ink),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(L10n.text("agenda.backup.export"))
        }
        error?.let { message ->
            Text(
                message,
                color = palette.warningText,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.warningText.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            )
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

private fun checkPreview(store: AgendaStore, incoming: AgendaDocument) {
    if (incoming.entries.isEmpty()) {
        throw com.oshicue.kit.core.AgendaError("agenda.error.empty", "There are no plans to import.", "$.entries")
    }
    store.document.merging(incoming)
}

private fun agendaError(error: Exception): String {
    if (error is com.oshicue.kit.core.AgendaError) {
        return L10n.text(error.key) + "\n" + error.path
    }
    return error.message ?: error.toString()
}
