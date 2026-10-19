package com.oshicue.kit.ui

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.KitDate
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerAnalytics
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.LedgerRankRow
import com.oshicue.kit.core.LedgerRecord
import com.oshicue.kit.core.LedgerStore
import com.oshicue.kit.core.LedgerSummary
import com.oshicue.kit.core.ScheduleStore
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun LedgerScreen(
    store: LedgerStore,
    scheduleStore: ScheduleStore,
    modifier: Modifier = Modifier
) {
    val kit = LocalKit.current
    var mode by remember { mutableStateOf("calendar") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var shownMonth by remember { mutableStateOf(YearMonth.now()) }
    var showAdd by remember { mutableStateOf(false) }
    var showManagement by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<LedgerRecord?>(null) }
    var deleteRecord by remember { mutableStateOf<LedgerRecord?>(null) }
    var pickerRow by remember { mutableStateOf<LedgerRankRow?>(null) }

    val day = KitDate.formatDay(selectedDate)
    val dailyRecords = store.document.records.filter { it.date == day }
    val dailyRows = remember(dailyRecords) {
        try {
            LedgerAnalytics.rankings(dailyRecords, com.oshicue.kit.core.LedgerGrouping.MemberByGroup, com.oshicue.kit.core.LedgerMetric.Quantity)
        } catch (_: Exception) {
            emptyList()
        }
    }
    val monthSummary: LedgerSummary = remember(store.document.records, shownMonth) {
        try {
            LedgerAnalytics.summarize(
                LedgerAnalytics.filtered(
                    store.document.records,
                    com.oshicue.kit.core.LedgerPeriod.Month,
                    KitDate.formatDay(shownMonth.atDay(1))
                )
            )
        } catch (_: Exception) {
            LedgerSummary()
        }
    }

    Column(modifier.fillMaxSize().background(kit.background)) {
        LedgerTopBar(
            onAdd = { showAdd = true },
            onManagement = { showManagement = true }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                L10n.text("ledger.title"),
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
                color = kit.ink
            )
        }
        Box(Modifier.padding(horizontal = 20.dp).padding(bottom = 14.dp)) {
            SegmentedControl(
                options = listOf(
                    "calendar" to L10n.text("ledger.calendar"),
                    "stats" to L10n.text("ledger.stats"),
                    "catalog" to L10n.text("ledger.catalog")
                ),
                selected = mode,
                onSelect = { mode = it }
            )
        }
        when (mode) {
            "calendar" -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
            ) {
                LedgerCalendarCard(
                    month = shownMonth,
                    selected = selectedDate,
                    records = store.document.records,
                    onMonthChange = {
                        shownMonth = it
                        selectedDate = it.atDay(1)
                    },
                    onSelect = {
                        selectedDate = it
                        if (YearMonth.from(it) != shownMonth) shownMonth = YearMonth.from(it)
                    }
                )
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryTile(
                        title = L10n.text("ledger.quantity"),
                        value = monthSummary.quantity.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryTile(
                        title = L10n.text("ledger.spending"),
                        value = if (monthSummary.amountsByCurrency.isEmpty()) "—" else moneyText(monthSummary.amountsByCurrency),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .width(4.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(kit.accent)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        KitDate.string(
                            selectedDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(),
                            "yyyy MMM d EEE",
                            ZoneId.systemDefault()
                        ),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = kit.ink
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        L10n.format("ledger.itemsCount", dailyRecords.sumOf { it.quantity }),
                        fontSize = 11.sp,
                        color = kit.muted
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (dailyRows.isEmpty()) {
                    SurfaceCard(corner = 18.dp) {
                        Column(
                            Modifier.fillMaxWidth().padding(25.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.CalendarMonth, null, tint = kit.accentText, modifier = Modifier.size(29.dp))
                            Spacer(Modifier.height(10.dp))
                            Text(L10n.text("ledger.noRecords"), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
                            Text(
                                L10n.text("ledger.noRecordsHint"),
                                fontSize = 11.5.sp,
                                color = kit.muted,
                                textAlign = TextAlign.Center
                            )
                            if (store.document.activeMemberships.isEmpty()) {
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    L10n.text("ledger.manage"),
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = kit.accentText,
                                    modifier = Modifier.clickable { mode = "catalog" }
                                )
                                Text(
                                    L10n.text("ledger.loadDemo"),
                                    fontSize = 11.5.sp,
                                    color = kit.accentText,
                                    modifier = Modifier.clickable { store.loadDemo() }
                                )
                            }
                        }
                    }
                } else {
                    dailyRows.forEach { row ->
                        DailyCard(row) {
                            val records = dailyRecords.filter {
                                it.memberId == row.memberId && it.groupId == row.groupId
                            }.sortedBy { it.id }
                            if (records.size == 1) {
                                editingRecord = records.first()
                            } else {
                                pickerRow = row
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(kit.surface)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            L10n.text("ledger.records") + " · " + dailyRecords.size,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = kit.ink
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    dailyRecords.sortedBy { it.id }.forEach { record ->
                        RecordRow(
                            record = record,
                            onClick = { editingRecord = record },
                            onDelete = { deleteRecord = record }
                        )
                    }
                }
                Spacer(Modifier.height(35.dp))
            }
            "stats" -> LedgerStatistics(
                store = store,
                referenceDate = selectedDate
            )
            else -> LedgerCatalogScreen(store)
        }
    }

    if (showAdd) {
        LedgerRecordEditor(
            store = store,
            scheduleStore = scheduleStore,
            date = day,
            record = null,
            onClose = { showAdd = false }
        )
    }
    editingRecord?.let { record ->
        LedgerRecordEditor(
            store = store,
            scheduleStore = scheduleStore,
            date = record.date,
            record = record,
            onClose = { editingRecord = null }
        )
    }
    pickerRow?.let { row ->
        val records = dailyRecords.filter { it.memberId == row.memberId && it.groupId == row.groupId }.sortedBy { it.id }
        LedgerDailyRecordPicker(
            store = store,
            scheduleStore = scheduleStore,
            row = row,
            records = records,
            onClose = { pickerRow = null }
        )
    }
    if (showManagement) {
        LedgerManagementSheet(store = store, onClose = { showManagement = false })
    }
    deleteRecord?.let { record ->
        ConfirmDialog(
            title = L10n.text("ledger.deleteTitle"),
            message = L10n.text("ledger.deleteMessage"),
            confirmLabel = L10n.text("ledger.delete"),
            onConfirm = { store.deleteRecord(record.id) },
            onDismiss = { deleteRecord = null }
        )
    }
}

@Composable
private fun LedgerCalendarCard(
    month: YearMonth,
    selected: LocalDate,
    records: List<LedgerRecord>,
    onMonthChange: (YearMonth) -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    val kit = LocalKit.current
    val firstOfMonth = month.atDay(1)
    // Sunday-first grid with only the weeks the month needs, like the iOS ledger calendar.
    val leading = firstOfMonth.dayOfWeek.value % 7
    val length = ((leading + month.lengthOfMonth() + 6) / 7) * 7
    val cells = (0 until length).map { offset -> firstOfMonth.plusDays((offset - leading).toLong()) }
    val countByDay = remember(records) { records.groupingBy { it.date }.fold(0) { acc, record -> acc + record.quantity } }
    val today = LocalDate.now()

    SurfaceCard(corner = 20.dp) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMonthChange(month.minusMonths(1)) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = L10n.text("ledger.previousMonth"), tint = kit.ink)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    KitDate.string(
                        firstOfMonth.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(),
                        "yyyy MMMM",
                        ZoneId.systemDefault()
                    ),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = kit.ink
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { onMonthChange(month.plusMonths(1)) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = L10n.text("ledger.nextMonth"), tint = kit.ink)
                }
            }
            Row(Modifier.fillMaxWidth()) {
                KitDate.veryShortWeekdays().forEach { title ->
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = kit.muted,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            cells.chunked(7).forEach { week ->
                Row(
                    Modifier.fillMaxWidth().height(47.dp).padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    week.forEach { date ->
                        val key = KitDate.formatDay(date)
                        val isSelected = date == selected
                        val inMonth = date.month == month.month
                        val count = countByDay[key] ?: 0
                        val shape = RoundedCornerShape(15.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(shape)
                                .background(if (isSelected) kit.accentFill else androidx.compose.ui.graphics.Color.Transparent)
                                .border(
                                    1.2.dp,
                                    if (date == today && !isSelected) kit.accent else androidx.compose.ui.graphics.Color.Transparent,
                                    shape
                                )
                                .clickable { onSelect(date) }
                                .semantics(mergeDescendants = true) {
                                    contentDescription = key + " · " + L10n.format("ledger.itemsCount", count)
                                    this.selected = isSelected
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    date.dayOfMonth.toString(),
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = when {
                                        isSelected -> kit.onAccent
                                        inMonth -> kit.ink
                                        else -> kit.muted
                                    }
                                )
                                Text(
                                    if (count == 0) " " else count.toString(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) kit.onAccent else kit.accentText
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
private fun SummaryTile(title: String, value: String, modifier: Modifier = Modifier) {
    val kit = LocalKit.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(kit.surface)
            .padding(14.dp)
    ) {
        Text(title, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = kit.muted)
        Spacer(Modifier.height(8.dp))
        Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = kit.ink, maxLines = 2)
    }
}

@Composable
private fun DailyCard(row: LedgerRankRow, onClick: () -> Unit) {
    val kit = LocalKit.current
    SurfaceCard {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            LedgerAvatar(
                member = null,
                color = row.color,
                size = 44.dp
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(row.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = kit.ink)
                Text(row.subtitle, fontSize = 10.sp, color = kit.muted)
                Spacer(Modifier.height(5.dp))
                FlowProductSummary(row.productRows)
                Spacer(Modifier.height(5.dp))
                Text(moneyText(row.amountsByCurrency), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    row.quantity.toString(),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = kit.readableDataColor(row.color)
                )
                Text("›", fontSize = 14.sp, color = kit.muted)
            }
        }
    }
}

@Composable
private fun RecordRow(record: LedgerRecord, onClick: () -> Unit, onDelete: () -> Unit) {
    val kit = LocalKit.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(kit.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(hexToColor(record.colorSnapshot)))
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(record.memberNameSnapshot, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
            Text(
                productDisplayName(record.productId, record.productNameSnapshot) + " × " + record.quantity,
                fontSize = 11.5.sp,
                color = kit.muted
            )
            record.eventNameSnapshot?.let { Text(it, fontSize = 10.5.sp, color = kit.muted) }
        }
        Text(
            LedgerCurrency.from(record.currency)?.format(
                record.unitPriceMinor * record.quantity,
                L10n.locale
            ) ?: "",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = kit.ink
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "×",
            fontSize = 15.sp,
            color = kit.muted,
            modifier = Modifier.clickable(onClick = onDelete).padding(horizontal = 4.dp)
        )
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun LedgerDailyRecordPicker(
    store: LedgerStore,
    scheduleStore: ScheduleStore,
    row: LedgerRankRow,
    records: List<LedgerRecord>,
    onClose: () -> Unit
) {
    val kit = LocalKit.current
    var editing by remember { mutableStateOf<LedgerRecord?>(null) }
    val current = editing
    if (current != null) {
        LedgerRecordEditor(
            store = store,
            scheduleStore = scheduleStore,
            date = current.date,
            record = current,
            onClose = { editing = null }
        )
    } else {
        ModalScreen(title = L10n.text("ledger.chooseRecord"), onClose = onClose) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LedgerAvatar(null, row.color, 45.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(row.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = kit.ink)
                        Text(row.subtitle, fontSize = 11.sp, color = kit.muted)
                        if (records.isNotEmpty()) {
                            Text(
                                KitDate.string(
                                    records[0].date.let { LedgerDates.parse(it) ?: LocalDate.now() }
                                        .atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(),
                                    "yyyy MMM d EEE",
                                    ZoneId.systemDefault()
                                ),
                                fontSize = 11.sp,
                                color = kit.muted
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(L10n.text("ledger.chooseRecordHint"), fontSize = 11.5.sp, color = kit.muted)
                Spacer(Modifier.height(10.dp))
                records.forEach { record ->
                    val currency = LedgerCurrency.from(record.currency)
                    val location = listOfNotNull(record.citySnapshot, record.venueSnapshot, record.addressSnapshot)
                        .filter { it.isNotEmpty() }.joinToString(" · ")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(kit.surface)
                            .clickable { editing = record }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                productDisplayName(record.productId, record.productNameSnapshot) + " × " + record.quantity,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = kit.ink
                            )
                            Text(
                                L10n.text("ledger.unitPrice") + " · " +
                                    (currency?.format(record.unitPriceMinor, L10n.locale) ?: ""),
                                fontSize = 11.sp,
                                color = kit.muted
                            )
                            record.eventNameSnapshot?.let { Text(it, fontSize = 11.sp, color = kit.muted) }
                            if (location.isNotEmpty()) Text(location, fontSize = 11.sp, color = kit.muted)
                            record.note?.let { Text(it, fontSize = 11.sp, color = kit.muted, maxLines = 2) }
                        }
                        Text(
                            currency?.format(record.unitPriceMinor * record.quantity, L10n.locale) ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = kit.ink
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("›", fontSize = 14.sp, color = kit.muted)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
