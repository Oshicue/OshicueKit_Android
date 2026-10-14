package com.oshicue.kit.ui.ledger

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GroupRemove
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
import androidx.compose.runtime.Composable
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
import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.LedgerDocument
import com.oshicue.kit.core.LedgerImporter
import com.oshicue.kit.core.LedgerRecord
import com.oshicue.kit.core.LedgerRecordDeletion
import com.oshicue.kit.core.LedgerRecordFilter
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.activeGroups
import com.oshicue.kit.core.activeMembers
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.data.LedgerStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.KitAlert
import com.oshicue.kit.ui.common.KitDatePickerDialog
import com.oshicue.kit.ui.common.KitDropdownRow
import com.oshicue.kit.ui.common.KitSheet
import com.oshicue.kit.ui.common.kitStandardSort
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.kitPalette
import java.time.LocalDate

private enum class EventSelection { ALL, UNLINKED, EVENT }

@Composable
fun LedgerManagementSheet(store: LedgerStore, onClose: () -> Unit) {
    val palette = kitPalette()
    var section by remember { mutableStateOf(0) }
    KitSheet(L10n.text("ledger.management"), onClose) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp)
                    .background(palette.surfaceMuted, RoundedCornerShape(11.dp))
                    .padding(3.dp)
            ) {
                for ((index, label) in listOf(L10n.text("management.records"), L10n.text("management.backup")).withIndex()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(if (section == index) palette.surface else Color.Transparent, RoundedCornerShape(9.dp))
                            .clickable { section = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (section == index) palette.accentText else palette.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (section == 0) {
                LedgerRecordManagement(store)
            } else {
                LedgerBackupContent(store)
            }
        }
    }
}

@Composable
private fun LedgerRecordManagement(store: LedgerStore) {
    val palette = kitPalette()
    var search by remember { mutableStateOf("") }
    var showFilters by remember { mutableStateOf(false) }
    var useDateRange by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var memberID by remember { mutableStateOf("") }
    var groupID by remember { mutableStateOf("") }
    var productID by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("") }
    var eventSelection by remember { mutableStateOf(EventSelection.ALL) }
    var eventID by remember { mutableStateOf("") }
    var selectedIDs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeletion by remember { mutableStateOf<LedgerRecordDeletion?>(null) }
    var deletingAll by remember { mutableStateOf(false) }
    var showCatalogManagement by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }
    var deletingCatalogMode by remember { mutableStateOf(false) }

    val filter = LedgerRecordFilter(
        startDate = if (useDateRange) LedgerPresentation.calendar.format(startDate) else null,
        endDate = if (useDateRange) LedgerPresentation.calendar.format(endDate) else null,
        memberID = memberID.ifEmpty { null },
        groupID = groupID.ifEmpty { null },
        productID = productID.ifEmpty { null },
        currency = currency.ifEmpty { null },
        eventID = if (eventSelection == EventSelection.EVENT) eventID else null,
        requiresNoEvent = eventSelection == EventSelection.UNLINKED,
        query = search
    )
    val records = remember(filter, store.document.records) {
        filter.matching(store.document.records)
            .sortedWith(compareByDescending<LedgerRecord> { it.date }.thenBy { it.id })
    }

    fun options(values: List<Pair<String, String>>): List<Pair<String, String>> {
        val seen = mutableSetOf<String>()
        return values.filter { seen.add(it.first) }.sortedWith { left, right ->
            if (left.second == right.second) left.first.compareTo(right.first) else kitStandardSort(left.second, right.second)
        }
    }

    val memberOptions = options(
        store.document.records.map { it.memberID to it.memberNameSnapshot } + store.document.members.map { it.id to it.name }
    )
    val groupOptions = options(
        store.document.records.map { it.groupID to it.groupNameSnapshot } + store.document.groups.map { it.id to it.name }
    )
    val productOptions = options(
        store.document.records.map { it.productID to LedgerPresentation.productName(it.productID, it.productNameSnapshot) } +
            store.document.products.map { it.id to LedgerPresentation.productName(it.id, it.name) }
    )
    val eventOptions = options(
        store.document.records.mapNotNull { record -> record.eventID?.let { it to (record.eventNameSnapshot ?: it) } } +
            store.document.events.map { it.id to it.name }
    )

    fun prepareDeletion(all: Boolean) {
        val ids = if (all) store.document.records.map { it.id }.toSet() else selectedIDs
        val deletion = LedgerRecordDeletion(store.document.records, ids)
        if (deletion.recordCount == 0) return
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
                androidx.compose.material3.TextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    placeholder = { Text(L10n.text("management.search"), color = palette.muted, fontSize = 13.sp) },
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = TextStyle(color = palette.ink, fontSize = 14.sp)
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
                        label = L10n.text("ledger.member"),
                        value = memberID,
                        options = listOf("" to L10n.text("management.all")) + memberOptions,
                        onSelect = { memberID = it }
                    )
                    KitDropdownRow(
                        label = L10n.text("ledger.group"),
                        value = groupID,
                        options = listOf("" to L10n.text("management.all")) + groupOptions,
                        onSelect = { groupID = it }
                    )
                    KitDropdownRow(
                        label = L10n.text("ledger.type"),
                        value = productID,
                        options = listOf("" to L10n.text("management.all")) + productOptions,
                        onSelect = { productID = it }
                    )
                    KitDropdownRow(
                        label = L10n.text("ledger.currency"),
                        value = currency,
                        options = listOf("" to L10n.text("management.all")) + LedgerCurrency.entries.map { it.code to it.code },
                        onSelect = { currency = it }
                    )
                    KitDropdownRow(
                        label = L10n.text("ledger.event"),
                        value = when (eventSelection) {
                            EventSelection.UNLINKED -> L10n.text("ledger.noEvent")
                            EventSelection.EVENT -> eventOptions.firstOrNull { it.first == eventID }?.second ?: ""
                            EventSelection.ALL -> ""
                        },
                        options = listOf(
                            "" to L10n.text("management.all"),
                            "@unlinked" to L10n.text("ledger.noEvent")
                        ) + eventOptions.map { it.first to it.second },
                        onSelect = { id ->
                            when (id) {
                                "" -> eventSelection = EventSelection.ALL
                                "@unlinked" -> eventSelection = EventSelection.UNLINKED
                                else -> {
                                    eventSelection = EventSelection.EVENT
                                    eventID = id
                                }
                            }
                        }
                    )
                    Text(L10n.text("management.selectionHint"), color = palette.muted, fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    L10n.text("management.selectFiltered"),
                    color = if (records.isEmpty()) palette.muted.copy(alpha = 0.5f) else palette.accentText,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(enabled = records.isNotEmpty()) { selectedIDs = records.map { it.id }.toSet() }
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    L10n.text("management.clearSelection"),
                    color = if (selectedIDs.isEmpty()) palette.muted.copy(alpha = 0.5f) else palette.accentText,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(enabled = selectedIDs.isNotEmpty()) { selectedIDs = emptySet() }
                )
            }
            Text(L10n.format("management.resultCount", records.size), color = palette.muted, fontSize = 13.sp)
            if (records.isEmpty()) {
                Text(L10n.text("management.empty"), color = palette.muted, fontSize = 14.sp)
            }
            for (record in records) {
                val selected = record.id in selectedIDs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedIDs = if (selected) selectedIDs - record.id else selectedIDs + record.id
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
                        Text(record.memberNameSnapshot + " · " + record.groupNameSnapshot, color = palette.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            record.date + " · " + LedgerPresentation.productName(record.productID, record.productNameSnapshot) + " × " + record.quantity,
                            color = palette.muted,
                            fontSize = 12.sp
                        )
                        record.eventNameSnapshot?.let { Text(it, color = palette.muted, fontSize = 12.sp) }
                        record.note?.takeIf { it.isNotEmpty() }?.let {
                            Text(it, color = palette.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    Text(
                        LedgerPresentation.money(mapOf(record.currency to (record.totalMinor ?: 0L))),
                        color = palette.ink,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showCatalogManagement = true }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.GroupRemove, contentDescription = null, tint = palette.ink, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(L10n.text("management.catalog"), color = palette.ink, fontSize = 15.sp)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = palette.muted, modifier = Modifier.size(16.dp))
            }
            Text(
                L10n.text("management.clearAll"),
                color = if (store.document.records.isEmpty()) palette.muted.copy(alpha = 0.5f) else palette.warningText,
                fontSize = 15.sp,
                modifier = Modifier
                    .clickable(enabled = store.document.records.isNotEmpty()) { prepareDeletion(all = true) }
                    .padding(vertical = 8.dp)
            )
            Text(L10n.text("management.clearAllHint"), color = palette.muted, fontSize = 11.sp)
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
    if (showCatalogManagement) {
        LedgerCatalogManagementSheet(store) { showCatalogManagement = false }
    }
    pendingDeletion?.let { deletion ->
        KitAlert(
            title = L10n.text(if (deletingAll) "ledger.management.clearTitle" else "ledger.management.deleteTitle"),
            message = L10n.format("ledger.management.deleteSummary", deletion.recordCount, deletion.quantity.toString()) +
                "\n" + LedgerPresentation.money(deletion.amountsByCurrency) +
                "\n\n" + L10n.text("ledger.management.deleteImpact"),
            confirmLabel = L10n.text(if (deletingAll) "management.clearAll" else "management.deleteSelected"),
            onConfirm = {
                if (store.deleteRecords(deletion.recordIDs)) selectedIDs = selectedIDs - deletion.recordIDs
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null },
            destructive = true
        )
    }
}

@Composable
private fun LedgerCatalogManagementSheet(store: LedgerStore, onClose: () -> Unit) {
    val palette = kitPalette()
    var mode by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var selectedIDs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingIDs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showConfirmation by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun matches(value: String): Boolean = com.oshicue.kit.ui.common.kitStandardContains(value, search.trim())

    val visibleMembers = store.document.activeMembers.filter { member ->
        matches(member.name) || store.document.activeMemberships.any { affiliation ->
            affiliation.memberID == member.id && store.document.activeGroups.any { it.id == affiliation.groupID && matches(it.name) }
        }
    }.sortedWith { left, right -> if (left.name == right.name) left.id.compareTo(right.id) else kitStandardSort(left.name, right.name) }
    val visibleGroups = store.document.activeGroups.filter { matches(it.name) }
        .sortedWith { left, right -> if (left.name == right.name) left.id.compareTo(right.id) else kitStandardSort(left.name, right.name) }
    val visibleIDs = if (mode == 0) visibleMembers.map { it.id }.toSet() else visibleGroups.map { it.id }.toSet()

    KitSheet(L10n.text("management.catalog"), onClose) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surfaceMuted, RoundedCornerShape(11.dp))
                        .padding(3.dp)
                ) {
                    for ((index, label) in listOf(L10n.text("ledger.catalog.members"), L10n.text("ledger.group")).withIndex()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp)
                                .background(if (mode == index) palette.surface else Color.Transparent, RoundedCornerShape(9.dp))
                                .clickable {
                                    mode = index
                                    selectedIDs = emptySet()
                                    pendingIDs = emptySet()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (mode == index) palette.accentText else palette.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                androidx.compose.material3.TextField(
                    value = search,
                    onValueChange = { search = it; selectedIDs = emptySet() },
                    singleLine = true,
                    placeholder = { Text(L10n.text("management.catalog.search"), color = palette.muted, fontSize = 13.sp) },
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = palette.surface,
                        unfocusedContainerColor = palette.surface,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = TextStyle(color = palette.ink, fontSize = 14.sp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(L10n.text("management.catalog.hint"), color = palette.muted, fontSize = 11.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        L10n.text("management.selectFiltered"),
                        color = if (visibleIDs.isEmpty()) palette.muted.copy(alpha = 0.5f) else palette.accentText,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(enabled = visibleIDs.isNotEmpty()) { selectedIDs = visibleIDs }
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        L10n.text("management.clearSelection"),
                        color = if (selectedIDs.isEmpty()) palette.muted.copy(alpha = 0.5f) else palette.accentText,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable(enabled = selectedIDs.isNotEmpty()) { selectedIDs = emptySet() }
                    )
                }
                Text(L10n.format("management.selectedCount", selectedIDs.size), color = palette.muted, fontSize = 12.sp)
                if (visibleIDs.isEmpty()) {
                    Text(L10n.text("management.catalog.empty"), color = palette.muted, fontSize = 14.sp)
                } else if (mode == 0) {
                    for (member in visibleMembers) {
                        SelectionRow(member.id, member.name, member.id in selectedIDs) {
                            selectedIDs = if (member.id in selectedIDs) selectedIDs - member.id else selectedIDs + member.id
                        }
                    }
                } else {
                    for (group in visibleGroups) {
                        SelectionRow(group.id, group.name, group.id in selectedIDs) {
                            selectedIDs = if (group.id in selectedIDs) selectedIDs - group.id else selectedIDs + group.id
                        }
                    }
                }
                Text(
                    L10n.text("management.deleteSelected"),
                    color = if (selectedIDs.isEmpty()) palette.muted.copy(alpha = 0.5f) else palette.warningText,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable(enabled = selectedIDs.isNotEmpty()) {
                            pendingIDs = selectedIDs.intersect(visibleIDs)
                            showConfirmation = pendingIDs.isNotEmpty()
                        }
                        .padding(vertical = 10.dp)
                )
                error?.let { Text(it, color = palette.warningText, fontSize = 12.sp) }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showConfirmation) {
        KitAlert(
            title = L10n.text("management.catalog.deleteTitle"),
            message = L10n.format(
                if (mode == 0) "management.catalog.memberImpact" else "management.catalog.groupImpact",
                pendingIDs.size
            ),
            confirmLabel = L10n.text("management.deleteSelected"),
            onConfirm = {
                val ids = pendingIDs
                val success = if (mode == 0) store.deleteMembers(ids) else store.deleteGroups(ids)
                if (success) {
                    selectedIDs = selectedIDs - ids
                } else {
                    error = store.storageError
                    store.storageError = null
                }
                pendingIDs = emptySet()
                showConfirmation = false
            },
            onDismiss = { pendingIDs = emptySet(); showConfirmation = false },
            destructive = true
        )
    }
}

@Composable
private fun SelectionRow(id: String, title: String, selected: Boolean, onToggle: () -> Unit) {
    val palette = kitPalette()
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) palette.accentText else palette.muted,
            modifier = Modifier.size(20.dp)
        )
        Text(title, color = palette.ink, fontSize = 15.sp)
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun LedgerBackupContent(store: LedgerStore) {
    val palette = kitPalette()
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<LedgerDocument?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

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

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        try {
            var combined = LedgerDocument()
            for (uri in uris) {
                val input = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Unreadable file")
                combined = combined.merging(LedgerImporter.parse(input))
            }
            checkPreview(store, combined)
            text = LedgerImporter.encode(combined)
            preview = combined
            error = null
        } catch (value: Exception) {
            preview = null
            error = LedgerPresentation.error(value)
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
                Icon(Icons.Filled.Inventory, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(L10n.text("ledger.backup"), color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(L10n.text("ledger.backupHint"), color = palette.muted, fontSize = 12.sp)
            }
        }
        Button(
            onClick = {
                try {
                    pendingExport = LedgerImporter.encode(store.document)
                    exportLauncher.launch("OshicueKit-ledger-" + LedgerPresentation.calendar.format(LocalDate.now()))
                } catch (value: Exception) {
                    error = LedgerPresentation.error(value)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = palette.surface, contentColor = palette.ink),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(L10n.text("ledger.export"))
        }
        Spacer(modifier = Modifier.height(2.dp).fillMaxWidth().background(palette.line))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L10n.text("ledger.import"), color = palette.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.clickable {
                    val template = runCatching {
                        context.assets.open("ledger-template.json").bufferedReader().use { it.readText() }
                    }.getOrNull() ?: ""
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("ledger-template.json", template))
                    copied = true
                },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (copied) Icons.Filled.Verified else Icons.Filled.ContentCopy,
                    contentDescription = null,
                    tint = palette.ink,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(L10n.text(if (copied) "import.copied" else "ledger.copyTemplate"), color = palette.ink, fontSize = 11.sp)
            }
        }
        Text(L10n.text("ledger.importHint"), color = palette.muted, fontSize = 12.sp)
        Row(
            modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = palette.ink, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(L10n.text("ledger.file"), color = palette.ink, fontSize = 14.sp)
        }
        androidx.compose.material3.TextField(
            value = text,
            onValueChange = {
                text = it
                preview = null
                error = null
            },
            placeholder = { Text("JSON · UTF-8", color = palette.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = palette.ink),
            colors = androidx.compose.material3.TextFieldDefaults.colors(
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
                    val input = LedgerImporter.parse(text)
                    checkPreview(store, input)
                    preview = input
                    error = null
                } catch (value: Exception) {
                    preview = null
                    error = LedgerPresentation.error(value)
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
                    Text(L10n.text("ledger.preview"), color = palette.ink, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(L10n.format("ledger.summary", document.members.size, document.records.size), color = palette.ink, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    L10n.text(if (store.canReplaceDemo) "ledger.replacesDemo" else "ledger.restoreHint"),
                    color = palette.muted,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (store.importDocument(document)) {
                            preview = null
                            text = ""
                        } else {
                            error = store.storageError
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) { Text(L10n.text("ledger.apply"), fontWeight = FontWeight.SemiBold) }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

private fun checkPreview(store: LedgerStore, input: LedgerDocument) {
    if (input.members.isEmpty() && input.groups.isEmpty() && input.products.isEmpty() &&
        input.events.isEmpty() && input.records.isEmpty()
    ) {
        throw com.oshicue.kit.core.LedgerError("ledger.error.emptyImport", "There is no catalog or record to import.", "$")
    }
    // Preview validates against saved data too, including conflicting historical IDs.
    if (!store.canReplaceDemo) store.document.merging(input)
}
