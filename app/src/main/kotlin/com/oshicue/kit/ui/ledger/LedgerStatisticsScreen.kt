package com.oshicue.kit.ui.ledger

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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.oshicue.kit.data.LedgerStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.KitDatePickerDialog
import com.oshicue.kit.ui.common.KitDropdownRow
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.kitPalette
import java.time.LocalDate
import java.util.Locale

@Composable
fun LedgerStatisticsScreen(store: LedgerStore, referenceDate: LocalDate) {
    val palette = kitPalette()
    var period by remember { mutableStateOf(LedgerPeriod.month) }
    var grouping by remember { mutableStateOf(LedgerGrouping.memberCombined) }
    var metric by remember { mutableStateOf(LedgerMetric.quantity) }
    var currency by remember { mutableStateOf("JPY") }
    var productID by remember { mutableStateOf("") }
    var groupID by remember { mutableStateOf("") }
    var anchor by remember { mutableStateOf(referenceDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(referenceDate) { anchor = referenceDate }

    val records = remember(store.document.records, period, anchor, metric, currency, productID, groupID) {
        LedgerAnalytics.filtered(
            store.document.records, period, LedgerPresentation.calendar.format(anchor),
            currency = if (metric == LedgerMetric.spending) currency else null
        ).filter { (productID.isEmpty() || it.productID == productID) && (groupID.isEmpty() || it.groupID == groupID) }
    }
    val summary = remember(records) { runCatching { LedgerAnalytics.summarize(records) }.getOrNull() }
    val rows = remember(records, grouping, metric, currency) {
        runCatching {
            LedgerAnalytics.rankings(records, grouping, metric, if (metric == LedgerMetric.spending) currency else null)
        }.getOrDefault(emptyList())
    }
    val periodTitle = when (period) {
        LedgerPeriod.day -> KitDate.string(anchor, "yyyy MMM d")
        LedgerPeriod.month -> KitDate.string(anchor, "yyyy MMMM")
        LedgerPeriod.year -> KitDate.string(anchor, "yyyy")
        LedgerPeriod.lifetime -> L10n.text("ledger.lifetime")
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 35.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SegmentedPicker(
            listOf(
                L10n.text("ledger.day") to (period == LedgerPeriod.day),
                L10n.text("ledger.month") to (period == LedgerPeriod.month),
                L10n.text("ledger.year") to (period == LedgerPeriod.year),
                L10n.text("ledger.lifetime") to (period == LedgerPeriod.lifetime)
            )
        ) { index ->
            period = listOf(LedgerPeriod.day, LedgerPeriod.month, LedgerPeriod.year, LedgerPeriod.lifetime)[index]
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.ChevronLeft,
                contentDescription = null,
                tint = if (period == LedgerPeriod.lifetime) palette.muted.copy(alpha = 0.4f) else palette.ink,
                modifier = Modifier.size(28.dp).clickable(enabled = period != LedgerPeriod.lifetime) {
                    anchor = when (period) {
                        LedgerPeriod.day -> anchor.minusDays(1)
                        LedgerPeriod.month -> anchor.minusMonths(1)
                        LedgerPeriod.year -> anchor.minusYears(1)
                        LedgerPeriod.lifetime -> anchor
                    }
                }
            )
            Spacer(modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = period != LedgerPeriod.lifetime) { showDatePicker = true }
            ) {
                Text(periodTitle, color = palette.ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (period != LedgerPeriod.lifetime) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = palette.muted, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = if (period == LedgerPeriod.lifetime) palette.muted.copy(alpha = 0.4f) else palette.ink,
                modifier = Modifier.size(28.dp).clickable(enabled = period != LedgerPeriod.lifetime) {
                    anchor = when (period) {
                        LedgerPeriod.day -> anchor.plusDays(1)
                        LedgerPeriod.month -> anchor.plusMonths(1)
                        LedgerPeriod.year -> anchor.plusYears(1)
                        LedgerPeriod.lifetime -> anchor
                    }
                }
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(16.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SegmentedPicker(
                listOf(
                    L10n.text("ledger.memberCombined") to (grouping == LedgerGrouping.memberCombined),
                    L10n.text("ledger.memberByGroup") to (grouping == LedgerGrouping.memberByGroup),
                    L10n.text("ledger.group") to (grouping == LedgerGrouping.group)
                )
            ) { index -> grouping = listOf(LedgerGrouping.memberCombined, LedgerGrouping.memberByGroup, LedgerGrouping.group)[index] }
            SegmentedPicker(
                listOf(
                    L10n.text("ledger.quantity") to (metric == LedgerMetric.quantity),
                    L10n.text("ledger.spending") to (metric == LedgerMetric.spending)
                )
            ) { index -> metric = if (index == 0) LedgerMetric.quantity else LedgerMetric.spending }
            Row(verticalAlignment = Alignment.CenterVertically) {
                KitDropdownRow(
                    label = L10n.text("ledger.type"),
                    value = productID,
                    options = listOf("" to L10n.text("ledger.allTypes")) + store.document.products.map {
                        it.id to LedgerPresentation.productDisplayName(it)
                    },
                    onSelect = { productID = it },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                KitDropdownRow(
                    label = L10n.text("ledger.group"),
                    value = groupID,
                    options = listOf("" to L10n.text("ledger.allGroups")) + store.document.groups.map { it.id to it.name },
                    onSelect = { groupID = it },
                    modifier = Modifier.weight(1f)
                )
            }
            if (metric == LedgerMetric.spending) {
                KitDropdownRow(
                    label = L10n.text("ledger.currency"),
                    value = currency,
                    options = LedgerCurrency.entries.map { it.code to it.code },
                    onSelect = { currency = it }
                )
            }
        }
        Text(
            L10n.text(if (grouping == LedgerGrouping.memberByGroup) "ledger.splitHint" else "ledger.combinedHint"),
            color = palette.muted,
            fontSize = 10.sp
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.BarChart, contentDescription = null, tint = palette.muted, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(L10n.text("ledger.total"), color = palette.muted, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                if (metric == LedgerMetric.quantity) {
                    L10n.format("ledger.itemsCount", summary?.quantity ?: 0)
                } else {
                    LedgerCurrency.fromCode(currency)?.format(summary?.amountsByCurrency?.get(currency) ?: 0L, L10n.currentLocale()) ?: ""
                },
                color = palette.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(L10n.format("ledger.membersCount", summary?.memberCount ?: 0), color = palette.muted, fontSize = 10.sp)
        }
        if (rows.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.BarChart, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(34.dp))
                Text(L10n.text("ledger.noRecords"), color = palette.ink, fontWeight = FontWeight.SemiBold)
                Text(L10n.text("ledger.noRecordsHint"), color = palette.muted, fontSize = 13.sp)
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(18.dp)).padding(15.dp)) {
                for (row in rows) {
                    RankRow(store, row, metric, currency)
                    if (row.id != rows.last().id) {
                        Spacer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 52.dp)
                                .height(1.dp)
                                .background(palette.line)
                        )
                    }
                }
            }
        }
        Text(L10n.text("ledger.currencyHint"), color = palette.muted, fontSize = 11.sp)
    }

    if (showDatePicker) {
        KitDatePickerDialog(anchor, { anchor = it }, { showDatePicker = false })
    }
}

@Composable
private fun SegmentedPicker(options: List<Pair<String, Boolean>>, onSelect: (Int) -> Unit) {
    val palette = kitPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(palette.surfaceMuted)
            .padding(3.dp)
    ) {
        options.forEachIndexed { index, (label, selected) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) palette.surface else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) palette.accentText else palette.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RankRow(store: LedgerStore, row: LedgerRankRow, metric: LedgerMetric, currency: String) {
    val palette = kitPalette()
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(11.dp)) {
        Text(
            row.rank.toString(),
            color = if (row.rank <= 3) palette.accentText else palette.muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(16.dp).padding(top = 13.dp)
        )
        LedgerAvatar(store.document.members.firstOrNull { it.id == row.memberID }, row.color, 36.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.name, color = palette.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    if (metric == LedgerMetric.quantity) row.quantity.toString()
                    else LedgerCurrency.fromCode(currency)?.format(row.amountsByCurrency[currency] ?: 0L, L10n.currentLocale()) ?: "",
                    color = palette.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Row {
                Text(row.subtitle, color = palette.muted, fontSize = 10.sp, maxLines = 2, modifier = Modifier.weight(1f))
                Text(
                    String.format(Locale.US, "%.1f%%", row.share * 100),
                    color = palette.muted,
                    fontSize = 9.sp
                )
            }
            FlowProductSummary(row)
            Box(modifier = Modifier.fillMaxWidth().height(3.dp).padding(top = 5.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(com.oshicue.kit.ui.theme.hexColor(row.color).copy(alpha = 0.17f), RoundedCornerShape(50))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(row.share.toFloat().coerceIn(0.02f, 1f))
                        .height(3.dp)
                        .background(palette.readableDataColor(row.color), RoundedCornerShape(50))
                )
            }
        }
    }
}
