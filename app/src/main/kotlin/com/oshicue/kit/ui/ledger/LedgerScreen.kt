package com.oshicue.kit.ui.ledger

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CurrencyYen
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.LedgerAnalytics
import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.LedgerGrouping
import com.oshicue.kit.core.LedgerMetric
import com.oshicue.kit.core.LedgerPeriod
import com.oshicue.kit.core.LedgerRankRow
import com.oshicue.kit.core.LedgerRecord
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.core.LedgerSummary
import com.oshicue.kit.data.LedgerStore
import com.oshicue.kit.data.ScheduleStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.BrandWordmark
import com.oshicue.kit.ui.common.CircleIconButton
import com.oshicue.kit.ui.common.KitAlert
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.kitPalette
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun LedgerScreen(store: LedgerStore, scheduleStore: ScheduleStore) {
    val palette = kitPalette()
    var mode by remember { mutableStateOf(0) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var shownMonth by remember { mutableStateOf(YearMonth.now()) }
    var showAdd by remember { mutableStateOf(false) }
    var showManagement by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var editRecord by remember { mutableStateOf<LedgerRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<LedgerRecord?>(null) }
    var dailySelection by remember { mutableStateOf<DailySelection?>(null) }

    val day = LedgerPresentation.calendar.format(selectedDate)
    val dailyRecords = store.document.records.filter { it.date == day }
    val dailyRows = remember(dailyRecords) {
        runCatching {
            LedgerAnalytics.rankings(dailyRecords, LedgerGrouping.memberByGroup, LedgerMetric.quantity)
        }.getOrDefault(emptyList())
    }
    val monthRecords = remember(store.document.records, shownMonth) {
        LedgerAnalytics.filtered(store.document.records, LedgerPeriod.month, shownMonth.atDay(1).toString())
    }
    val monthSummary: LedgerSummary? = remember(monthRecords) {
        runCatching { LedgerAnalytics.summarize(monthRecords) }.getOrNull()
    }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandWordmark()
            Spacer(modifier = Modifier.weight(1f))
            CircleIconButton(Icons.Filled.Tune, L10n.text("ledger.management")) { showManagement = true }
            Spacer(modifier = Modifier.width(10.dp))
            IconButton(
                onClick = { showAdd = true },
                modifier = Modifier.size(42.dp).background(palette.accentFill, CircleShape)
            ) {
                Icon(Icons.Filled.Add, contentDescription = L10n.text("ledger.add"), tint = palette.onAccent)
            }
        }
        Text(
            L10n.text("ledger.title"),
            color = palette.ink,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 17.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surfaceMuted)
                .padding(4.dp)
        ) {
            LedgerModeButton(L10n.text("ledger.calendar"), Icons.Filled.CalendarMonth, mode == 0, Modifier.weight(1f)) { mode = 0 }
            LedgerModeButton(L10n.text("ledger.stats"), Icons.Filled.CurrencyYen, mode == 1, Modifier.weight(1f)) { mode = 1 }
            LedgerModeButton(L10n.text("ledger.catalog"), Icons.Filled.Collections, mode == 2, Modifier.weight(1f)) { mode = 2 }
        }
        when (mode) {
            0 -> CalendarContent(
                store = store,
                selectedDate = selectedDate,
                shownMonth = shownMonth,
                monthSummary = monthSummary,
                dailyRecords = dailyRecords,
                dailyRows = dailyRows,
                onSelectDate = { selectedDate = it },
                onMoveMonth = { offset ->
                    shownMonth = shownMonth.plusMonths(offset.toLong())
                    selectedDate = shownMonth.atDay(1)
                },
                onShowDatePicker = { showDatePicker = true },
                onDailyRow = { row ->
                    val records = dailyRecords
                        .filter { it.memberID == row.memberID && it.groupID == row.groupID }
                        .sortedBy { it.id }
                    when {
                        records.size == 1 -> editRecord = records.first()
                        records.isNotEmpty() -> dailySelection = DailySelection(row, records)
                    }
                },
                onEditRecord = { editRecord = it },
                onDeleteRecord = { deleteRecord = it },
                onManage = { mode = 2 },
                onLoadDemo = { store.loadDemo() }
            )
            1 -> LedgerStatisticsScreen(store, selectedDate)
            else -> LedgerCatalogScreen(store)
        }
    }

    if (showAdd) {
        LedgerRecordEditor(store, scheduleStore, day, null) { showAdd = false }
    }
    editRecord?.let { record ->
        LedgerRecordEditor(store, scheduleStore, record.date, record) { editRecord = null }
    }
    dailySelection?.let { selection ->
        LedgerDailyRecordPicker(store, scheduleStore, selection) { dailySelection = null }
    }
    if (showManagement) {
        LedgerManagementSheet(store) { showManagement = false }
    }
    if (showDatePicker) {
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate()
                        selectedDate = picked
                        shownMonth = YearMonth.from(picked)
                    }
                    showDatePicker = false
                }) { Text(L10n.text("common.done"), color = palette.accentText) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text(L10n.text("common.cancel"), color = palette.muted) }
            }
        ) {
            DatePicker(state = state, showModeToggle = false)
        }
    }
    deleteRecord?.let { record ->
        KitAlert(
            title = L10n.text("ledger.deleteTitle"),
            message = L10n.text("ledger.deleteMessage"),
            confirmLabel = L10n.text("ledger.delete"),
            onConfirm = {
                store.deleteRecord(record.id)
                deleteRecord = null
            },
            onDismiss = { deleteRecord = null },
            destructive = true
        )
    }
}

@Composable
private fun LedgerModeButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val palette = kitPalette()
    Row(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) palette.surface else Color.Transparent)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) palette.accentText else palette.muted, modifier = Modifier.size(15.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, color = if (selected) palette.accentText else palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private data class DailySelection(val row: LedgerRankRow, val records: List<LedgerRecord>)

@Composable
private fun CalendarContent(
    store: LedgerStore,
    selectedDate: LocalDate,
    shownMonth: YearMonth,
    monthSummary: LedgerSummary?,
    dailyRecords: List<LedgerRecord>,
    dailyRows: List<LedgerRankRow>,
    onSelectDate: (LocalDate) -> Unit,
    onMoveMonth: (Int) -> Unit,
    onShowDatePicker: () -> Unit,
    onDailyRow: (LedgerRankRow) -> Unit,
    onEditRecord: (LedgerRecord) -> Unit,
    onDeleteRecord: (LedgerRecord) -> Unit,
    onManage: () -> Unit,
    onLoadDemo: () -> Unit
) {
    val palette = kitPalette()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 35.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        CalendarCard(store, selectedDate, shownMonth, onSelectDate, onMoveMonth, onShowDatePicker)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryTile(
                title = L10n.text("ledger.quantity"),
                value = (monthSummary?.quantity ?: 0).toString(),
                icon = Icons.Filled.Collections,
                modifier = Modifier.weight(1f)
            )
            SummaryTile(
                title = L10n.text("ledger.spending"),
                value = monthSummary?.let { if (it.amountsByCurrency.isEmpty()) "—" else LedgerPresentation.money(it.amountsByCurrency) } ?: "—",
                icon = Icons.Filled.CurrencyYen,
                modifier = Modifier.weight(1f)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(24.dp).background(palette.accent, RoundedCornerShape(3.dp)))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                KitDate.string(selectedDate, "yyyy MMM d EEE"),
                color = palette.ink,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                L10n.format("ledger.itemsCount", dailyRecords.sumOf { it.quantity }),
                color = palette.muted,
                fontSize = 11.sp
            )
        }
        if (dailyRows.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface, RoundedCornerShape(18.dp))
                    .padding(25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(28.dp))
                Text(L10n.text("ledger.noRecords"), color = palette.ink, fontWeight = FontWeight.SemiBold)
                Text(L10n.text("ledger.noRecordsHint"), color = palette.muted, fontSize = 12.sp)
                if (store.document.activeMemberships.isEmpty()) {
                    Text(
                        L10n.text("ledger.manage"),
                        color = palette.accentText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = onManage)
                    )
                    Text(
                        L10n.text("ledger.loadDemo"),
                        color = palette.accentText,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onLoadDemo() }
                    )
                }
            }
        } else {
            for (row in dailyRows) {
                DailyCard(store, row, onDailyRow)
            }
            RecordsDisclosure(dailyRecords, onEditRecord, onDeleteRecord)
        }
    }
}

@Composable
private fun CalendarCard(
    store: LedgerStore,
    selectedDate: LocalDate,
    shownMonth: YearMonth,
    onSelectDate: (LocalDate) -> Unit,
    onMoveMonth: (Int) -> Unit,
    onShowDatePicker: () -> Unit
) {
    val palette = kitPalette()
    val locale = L10n.currentLocale()
    val firstOfMonth = shownMonth.atDay(1)
    val leading = firstOfMonth.dayOfWeek.value % 7
    val length = ((leading + shownMonth.lengthOfMonth() + 6) / 7) * 7
    val today = LocalDate.now()

    Column(
        modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(20.dp)).padding(17.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onMoveMonth(-1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = L10n.text("ledger.previousMonth"), tint = palette.ink)
            }
            Spacer(modifier = Modifier.weight(1f))
            Row(
                modifier = Modifier.clickable(onClick = onShowDatePicker),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(KitDate.string(firstOfMonth, "yyyy MMMM"), color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(7.dp))
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = palette.ink, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = { onMoveMonth(1) }) {
                Icon(Icons.Filled.ChevronRight, contentDescription = L10n.text("ledger.nextMonth"), tint = palette.ink)
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            val symbols = listOf(
                java.time.DayOfWeek.SUNDAY, java.time.DayOfWeek.MONDAY, java.time.DayOfWeek.TUESDAY,
                java.time.DayOfWeek.WEDNESDAY, java.time.DayOfWeek.THURSDAY, java.time.DayOfWeek.FRIDAY, java.time.DayOfWeek.SATURDAY
            )
            for (symbol in symbols) {
                Text(
                    symbol.getDisplayName(TextStyle.NARROW, locale),
                    color = palette.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        for (rowIndex in 0 until length / 7) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (columnIndex in 0 until 7) {
                    val offset = rowIndex * 7 + columnIndex - leading
                    val date = firstOfMonth.plusDays(offset.toLong())
                    val inMonth = YearMonth.from(date) == shownMonth
                    val key = LedgerPresentation.calendar.format(date)
                    val count = store.document.records.filter { it.date == key }.sumOf { it.quantity }
                    val selected = date == selectedDate
                    val isToday = date == today
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(47.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(if (selected) palette.accentFill else Color.Transparent)
                            .clickable { onSelectDate(date) }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            date.dayOfMonth.toString(),
                            color = when {
                                selected -> palette.onAccent
                                inMonth -> palette.ink
                                else -> palette.muted
                            },
                            fontSize = 15.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                        Text(
                            if (count == 0) " " else count.toString(),
                            color = if (selected) palette.onAccent else palette.accentText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SummaryTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    val palette = kitPalette()
    Column(
        modifier = modifier
            .height(96.dp)
            .background(palette.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = palette.muted, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(title, color = palette.muted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        Text(value, color = palette.ink, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DailyCard(store: LedgerStore, row: LedgerRankRow, onClick: (LedgerRankRow) -> Unit) {
    val palette = kitPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, RoundedCornerShape(17.dp))
            .clickable { onClick(row) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LedgerAvatar(store.document.members.firstOrNull { it.id == row.memberID }, row.color, 44.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(row.name, color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(row.subtitle, color = palette.muted, fontSize = 10.sp)
            FlowProductSummary(row)
            Text(LedgerPresentation.money(row.amountsByCurrency), color = palette.ink, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                row.quantity.toString(),
                color = palette.readableDataColor(row.color),
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.muted, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun FlowProductSummary(row: LedgerRankRow) {
    val palette = kitPalette()
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (product in row.productRows) {
            Text(
                LedgerPresentation.productName(product.id, product.name) + " × " + product.quantity,
                color = palette.ink,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(palette.background, RoundedCornerShape(50))
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun RecordsDisclosure(
    dailyRecords: List<LedgerRecord>,
    onEditRecord: (LedgerRecord) -> Unit,
    onDeleteRecord: (LedgerRecord) -> Unit
) {
    val palette = kitPalette()
    var expanded by remember { mutableStateOf(false) }
    var menuRecord by remember { mutableStateOf<LedgerRecord?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                L10n.text("ledger.records") + " · " + dailyRecords.size,
                color = palette.ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = palette.muted, modifier = Modifier.size(16.dp))
        }
        if (expanded) {
            Spacer(modifier = Modifier.height(10.dp))
            for (record in dailyRecords.sortedBy { it.id }) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = { onEditRecord(record) },
                                onLongClick = { menuRecord = record }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(modifier = Modifier.size(7.dp).background(com.oshicue.kit.ui.theme.hexColor(record.colorSnapshot), CircleShape))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(record.memberNameSnapshot, color = palette.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                LedgerPresentation.productName(record.productID, record.productNameSnapshot) + " × " + record.quantity,
                                color = palette.muted,
                                fontSize = 12.sp
                            )
                            record.eventNameSnapshot?.let { event ->
                                Text(event, color = palette.muted, fontSize = 11.sp)
                            }
                        }
                        Text(
                            LedgerCurrency.fromCode(record.currency)?.format(record.unitPriceMinor * record.quantity, L10n.currentLocale()) ?: "",
                            color = palette.ink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.muted, modifier = Modifier.size(13.dp))
                    }
                    DropdownMenu(expanded = menuRecord?.id == record.id, onDismissRequest = { menuRecord = null }) {
                        DropdownMenuItem(
                            text = { Text(L10n.text("ledger.edit"), color = palette.ink) },
                            onClick = {
                                menuRecord = null
                                onEditRecord(record)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(L10n.text("ledger.delete"), color = palette.warningText) },
                            onClick = {
                                menuRecord = null
                                onDeleteRecord(record)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerDailyRecordPicker(
    store: LedgerStore,
    scheduleStore: ScheduleStore,
    selection: DailySelection,
    onClose: () -> Unit
) {
    val palette = kitPalette()
    var editRecord by remember { mutableStateOf<LedgerRecord?>(null) }
    val editing = editRecord
    if (editing != null) {
        LedgerRecordEditor(store, scheduleStore, editing.date, editing) { editRecord = null }
        return
    }
    com.oshicue.kit.ui.common.KitSheet(L10n.text("ledger.chooseRecord"), onClose) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                LedgerAvatar(store.document.members.firstOrNull { it.id == selection.row.memberID }, selection.row.color, 45.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(selection.row.name, color = palette.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(selection.row.subtitle, color = palette.muted, fontSize = 12.sp)
                    Text(
                        KitDate.string(LedgerPresentation.date(selection.records[0].date), "yyyy MMM d EEE"),
                        color = palette.muted,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(L10n.text("ledger.chooseRecordHint"), color = palette.muted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (record in selection.records) {
                    val currency = LedgerCurrency.fromCode(record.currency)
                    val location = listOfNotNull(record.citySnapshot, record.venueSnapshot, record.addressSnapshot)
                        .filter { it.isNotEmpty() }.joinToString(" · ")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(palette.surface, RoundedCornerShape(14.dp))
                            .clickable { editRecord = record }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(
                                LedgerPresentation.productName(record.productID, record.productNameSnapshot) + " × " + record.quantity,
                                color = palette.ink,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                L10n.text("ledger.unitPrice") + " · " + (currency?.format(record.unitPriceMinor, L10n.currentLocale()) ?: ""),
                                color = palette.muted,
                                fontSize = 12.sp
                            )
                            record.eventNameSnapshot?.let { Text(it, color = palette.muted, fontSize = 12.sp) }
                            if (location.isNotEmpty()) Text(location, color = palette.muted, fontSize = 12.sp)
                            record.note?.let { Text(it, color = palette.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                        }
                        Text(
                            currency?.format(record.unitPriceMinor * record.quantity, L10n.currentLocale()) ?: "",
                            color = palette.ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.muted, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}
