package com.oshicue.kit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.AgendaCore
import com.oshicue.kit.core.AgendaDocument
import com.oshicue.kit.core.AgendaEntry
import com.oshicue.kit.core.AgendaEntryDeletion
import com.oshicue.kit.core.AgendaEntryFilter
import com.oshicue.kit.core.AgendaImporter
import com.oshicue.kit.core.AgendaKind
import com.oshicue.kit.core.AgendaPresentation
import com.oshicue.kit.core.AgendaStore
import com.oshicue.kit.core.KitDate
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerDates
import java.time.LocalDate

@Composable
fun AgendaManagementSheet(
    store: AgendaStore,
    onClose: () -> Unit,
    onApplied: (AgendaEntry?) -> Unit
) {
    val kit = LocalKit.current
    var section by remember { mutableStateOf("records") }
    var search by remember { mutableStateOf("") }
    var useDateRange by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var kindText by remember { mutableStateOf("") }
    var cityText by remember { mutableStateOf("") }
    var allDayText by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeletion by remember { mutableStateOf<AgendaEntryDeletion?>(null) }
    var deletingAll by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showCitySelector by remember { mutableStateOf(false) }

    val filter = AgendaEntryFilter(
        startDate = if (useDateRange) KitDate.formatDay(startDate) else null,
        endDate = if (useDateRange) KitDate.formatDay(endDate) else null,
        kind = AgendaKind.from(kindText),
        city = cityText.ifEmpty { null },
        isAllDay = if (allDayText.isEmpty()) null else allDayText == "allDay",
        query = search
    )
    val entries = filter.matching(store.document.entries).sortedWith { left, right ->
        val leftStart = AgendaCore.dateRange(left)?.start ?: KitDate.day(left.startsAt, left.zone)
        val rightStart = AgendaCore.dateRange(right)?.start ?: KitDate.day(right.startsAt, right.zone)
        when {
            leftStart != rightStart -> rightStart.compareTo(leftStart)
            left.startsAt == right.startsAt -> left.id.compareTo(right.id)
            else -> right.startsAt.compareTo(left.startsAt)
        }
    }
    val visibleSelected = selected.intersect(entries.map { it.id }.toSet())

    ModalScreen(title = L10n.text("agenda.management"), onClose = onClose) {
        Column(Modifier.fillMaxSize().background(kit.background)) {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                SegmentedControl(
                    options = listOf(
                        "records" to L10n.text("management.records"),
                        "import" to L10n.text("agenda.import"),
                        "backup" to L10n.text("management.backup")
                    ),
                    selected = section,
                    onSelect = { section = it }
                )
            }
            when (section) {
                "records" -> Column(Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            KitTextField(search, { search = it; selected = emptySet() }, L10n.text("agenda.management.search"))
                        }
                        item { FilterDisclosure(
                            useDateRange = useDateRange,
                            onUseDateRange = { useDateRange = it },
                            startDate = startDate,
                            endDate = endDate,
                            onStartDate = { startDate = it },
                            onEndDate = { endDate = it },
                            validRange = !useDateRange || !startDate.isAfter(endDate),
                            options = buildList {
                                add(FilterOption("", L10n.text("management.all")))
                                AgendaKind.entries.forEach { add(FilterOption(it.rawValue, agendaKindName(it))) }
                            },
                            onSelect = { kindText = it },
                            selectedOption = kindText,
                            extra = {
                                val cityOptions = (store.document.entries.mapNotNull { it.city?.name } + cityText)
                                    .filter { it.isNotBlank() }.distinct().sorted()
                                Column {
                                    SelectorRow(
                                        label = L10n.text("agenda.city"),
                                        value = cityText.ifEmpty { L10n.text("management.all") },
                                        onClick = { showCitySelector = true }
                                    )
                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf("" to L10n.text("management.all")).forEach { (value, label) ->
                                            SelectableChip(label, cityText == value) { cityText = value; selected = emptySet() }
                                        }
                                        cityOptions.take(4).forEach { option ->
                                            SelectableChip(option, cityText == option) { cityText = option; selected = emptySet() }
                                        }
                                    }
                                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        SelectableChip(L10n.text("management.all"), allDayText.isEmpty()) { allDayText = ""; selected = emptySet() }
                                        SelectableChip(L10n.text("agenda.allDay"), allDayText == "allDay") { allDayText = "allDay"; selected = emptySet() }
                                        SelectableChip(L10n.text("agenda.management.timed"), allDayText == "timed") { allDayText = "timed"; selected = emptySet() }
                                    }
                                }
                            }
                        ) }
                        item {
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    L10n.text("management.selectFiltered"),
                                    fontSize = 12.sp,
                                    color = if (entries.isEmpty()) kit.muted else kit.accentText,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable(enabled = entries.isNotEmpty()) {
                                        selected = entries.map { it.id }.toSet()
                                    }
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    L10n.text("management.clearSelection"),
                                    fontSize = 12.sp,
                                    color = if (visibleSelected.isEmpty()) kit.muted else kit.accentText,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable(enabled = visibleSelected.isNotEmpty()) {
                                        selected = selected - visibleSelected
                                    }
                                )
                            }
                        }
                        item {
                            Text(
                                L10n.format("management.resultCount", entries.size),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = kit.muted
                            )
                        }
                        if (entries.isEmpty()) {
                            item { Text(L10n.text("management.empty"), fontSize = 13.sp, color = kit.muted, modifier = Modifier.padding(vertical = 14.dp)) }
                        }
                        items(entries, key = { it.id }) { entry ->
                            AgendaManagementRow(
                                entry = entry,
                                selected = entry.id in selected,
                                onClick = {
                                    selected = if (entry.id in selected) selected - entry.id else selected + entry.id
                                }
                            )
                        }
                        item {
                            Spacer(Modifier.height(12.dp))
                            KitSecondaryButton(L10n.text("agenda.management.clearAll"), icon = Icons.Filled.Delete,
                                enabled = store.document.entries.isNotEmpty()) {
                                if (store.document.entries.isNotEmpty()) {
                                    deletingAll = true
                                    pendingDeletion = AgendaEntryDeletion(store.document.entries, store.document.entries.map { it.id }.toSet())
                                }
                            }
                            Text(L10n.text("agenda.management.clearAllHint"), fontSize = 11.sp, color = kit.muted)
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(kit.surface)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            L10n.format("management.selectedCount", visibleSelected.size),
                            fontSize = 13.sp,
                            color = kit.ink
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            L10n.text("management.deleteSelected"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (visibleSelected.isEmpty()) kit.muted else kit.accentText,
                            modifier = Modifier.clickable(enabled = visibleSelected.isNotEmpty()) {
                                deletingAll = false
                                val deletion = AgendaEntryDeletion(store.document.entries, visibleSelected)
                                if (deletion.entryCount > 0) pendingDeletion = deletion
                            }
                        )
                    }
                }
                "import" -> AgendaImportPane(store) { first -> onApplied(first) }
                else -> AgendaBackupPane(store)
            }
        }
    }

    if (showCitySelector) {
        val cities = store.document.entries.mapNotNull { it.city?.name }.filter { it.isNotBlank() }.distinct().sorted()
        OptionSelector(
            title = L10n.text("agenda.city"),
            options = listOf("" to L10n.text("management.all")) + cities.map { it to it },
            selected = cityText,
            onDismiss = { showCitySelector = false },
            onSelect = { cityText = it; selected = emptySet() }
        )
    }
    pendingDeletion?.let { deletion ->
        ConfirmDialog(
            title = L10n.text(if (deletingAll) "agenda.management.clearTitle" else "agenda.management.deleteTitle"),
            message = L10n.format("agenda.management.deleteSummary", deletion.entryCount) + "\n\n" +
                L10n.text("agenda.management.deleteImpact"),
            confirmLabel = L10n.text(if (deletingAll) "agenda.management.clearAll" else "management.deleteSelected"),
            onConfirm = {
                if (store.deleteEntries(deletion.entryIds)) {
                    selected = selected - deletion.entryIds
                } else {
                    error = store.storageError
                    store.storageError = null
                }
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null }
        )
    }
    error?.let {
        MessageDialog(L10n.text("settings.saveError"), it) { error = null; store.storageError = null }
    }
}

data class FilterOption(val value: String, val label: String)

@Composable
fun FilterDisclosure(
    useDateRange: Boolean,
    onUseDateRange: (Boolean) -> Unit,
    startDate: LocalDate,
    endDate: LocalDate,
    onStartDate: (LocalDate) -> Unit,
    onEndDate: (LocalDate) -> Unit,
    validRange: Boolean,
    options: List<FilterOption>,
    selectedOption: String,
    onSelect: (String) -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    val kit = LocalKit.current
    var expanded by remember { mutableStateOf(false) }
    var showStart by remember { mutableStateOf(false) }
    var showEnd by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(kit.surface)
            .padding(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(L10n.text("management.filters"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
            Spacer(Modifier.weight(1f))
            Text(if (expanded) "▾" else "▸", color = kit.muted)
        }
        if (expanded) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L10n.text("management.dateRange"), fontSize = 13.sp, color = kit.ink)
                Spacer(Modifier.weight(1f))
                androidx.compose.material3.Switch(checked = useDateRange, onCheckedChange = onUseDateRange)
            }
            if (useDateRange) {
                SelectorRow(label = L10n.text("management.startDate"), value = KitDate.formatDay(startDate), onClick = { showStart = true })
                SelectorRow(label = L10n.text("management.endDate"), value = KitDate.formatDay(endDate), onClick = { showEnd = true })
                if (!validRange) {
                    Text(L10n.text("management.invalidDateRange"), fontSize = 11.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { option ->
                    SelectableChip(option.label, selectedOption == option.value) { onSelect(option.value) }
                }
            }
            extra?.invoke()
        }
    }
    if (showStart) {
        DatePickerSheet(startDate, { showStart = false }) { onStartDate(it); showStart = false }
    }
    if (showEnd) {
        DatePickerSheet(endDate, { showEnd = false }) { onEndDate(it); showEnd = false }
    }
}

@Composable
private fun AgendaManagementRow(entry: AgendaEntry, selected: Boolean, onClick: () -> Unit) {
    val kit = LocalKit.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(kit.surface)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Checkmark(selected)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(entry.title, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
            val range = AgendaCore.dateRange(entry)
            val rangeText = if (range == null) KitDate.day(entry.startsAt, entry.zone)
            else if (range.start == range.end) range.start else range.start + " – " + range.end
            Text(rangeText + " · " + agendaKindName(entry.kind), fontSize = 11.sp, color = kit.muted)
            Text(AgendaPresentation.timeRange(entry), fontSize = 11.sp, color = kit.muted)
            val locations = listOfNotNull(entry.city?.name, entry.location?.name ?: entry.venue.ifEmpty { null })
            if (locations.isNotEmpty()) {
                Text(locations.joinToString(" · "), fontSize = 11.sp, color = kit.muted)
            }
            if (entry.flights.isNotEmpty()) {
                Text(entry.flights.joinToString(" · ") { it.number }, fontSize = 11.sp, color = kit.muted)
            }
            Text(entry.timeZone, fontSize = 10.sp, color = kit.muted)
            if (entry.notes.isNotEmpty()) {
                Text(entry.notes, fontSize = 11.sp, color = kit.muted, maxLines = 2)
            }
        }
    }
}

@Composable
private fun AgendaImportPane(store: AgendaStore, onApplied: (AgendaEntry?) -> Unit) {
    val kit = LocalKit.current
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<AgendaDocument?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<AgendaDocument?>(null) }
    val pickFiles = rememberFilePicker(multiple = true) { contents ->
        try {
            val documents = contents.map { AgendaImporter.parse(it) }
            val combined = AgendaImporter.combining(documents)
            val encoded = AgendaImporter.encode(combined)
            text = encoded
            preview = combined
            error = null
        } catch (failure: Exception) {
            preview = null
            error = AgendaPresentation.error(failure)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionLabel(L10n.text("agenda.import"))
        Spacer(Modifier.height(6.dp))
        Text(L10n.text("agenda.backup.importHint"), fontSize = 12.5.sp, color = kit.muted)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KitSecondaryButton(L10n.text("ledger.file"), icon = Icons.Filled.FileOpen, modifier = Modifier.weight(1f)) {
                pickFiles()
            }
            KitSecondaryButton(L10n.text("agenda.backup.template"), icon = Icons.Filled.Add, modifier = Modifier.weight(1f)) {
                text = loadTemplateText(context, "agenda-template.json")
                preview = null
                error = null
            }
        }
        Spacer(Modifier.height(10.dp))
        JsonEditorField(text = text, onValueChange = {
            text = it
            preview = pending
            pending = null
            error = null
        }, placeholder = "JSON · UTF-8", minHeight = 230.dp)
        Spacer(Modifier.height(10.dp))
        KitPrimaryButton(L10n.text("import.validate"), enabled = text.isNotBlank()) {
            try {
                val incoming = AgendaImporter.parse(text)
                if (incoming.entries.isEmpty()) throw com.oshicue.kit.core.AgendaError("agenda.error.empty", "$.entries")
                store.document.merging(incoming)
                preview = incoming
                error = null
            } catch (failure: Exception) {
                preview = null
                error = AgendaPresentation.error(failure)
            }
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            InlineError(it)
        }
        preview?.let { document ->
            Spacer(Modifier.height(14.dp))
            FormCard {
                Text(L10n.text("agenda.backup.preview"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = kit.ink)
                val existing = store.document.entries.map { it.id }.toSet()
                val updates = document.entries.count { it.id in existing }
                Text(
                    L10n.format("agenda.backup.summary", document.entries.size - updates, updates),
                    fontSize = 12.5.sp,
                    color = kit.ink
                )
                document.entries.take(8).forEach { entry ->
                    Text(entry.title, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
                    Text(
                        KitDate.day(entry.startsAt, entry.zone) + " · " + (entry.city?.name ?: entry.timeZone),
                        fontSize = 11.sp,
                        color = kit.muted
                    )
                }
                Spacer(Modifier.height(10.dp))
                KitPrimaryButton(L10n.text("agenda.backup.apply")) {
                    if (store.importDocument(document)) {
                        onApplied(document.entries.minByOrNull { it.startsAt })
                    } else {
                        error = store.storageError
                        store.storageError = null
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun AgendaBackupPane(store: AgendaStore) {
    val kit = LocalKit.current
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }
    val exporter = rememberTextExporter(onError = { error = it })
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionLabel(L10n.text("agenda.backup"))
        Spacer(Modifier.height(6.dp))
        Text(L10n.text("agenda.backupHint"), fontSize = 12.5.sp, color = kit.muted)
        Spacer(Modifier.height(12.dp))
        KitSecondaryButton(L10n.text("agenda.backup.export"), icon = Icons.Filled.Share) {
            try {
                exporter("OshicueKit-agenda-" + LedgerDates.today() + ".json", store.exportJson())
            } catch (failure: Exception) {
                error = AgendaPresentation.error(failure)
            }
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            InlineError(it)
        }
    }
}
