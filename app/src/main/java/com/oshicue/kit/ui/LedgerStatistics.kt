package com.oshicue.kit.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.KitDate
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerAnalytics
import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.LedgerGrouping
import com.oshicue.kit.core.LedgerMetric
import com.oshicue.kit.core.LedgerPeriod
import com.oshicue.kit.core.LedgerRankRow
import com.oshicue.kit.core.LedgerStore
import com.oshicue.kit.core.LedgerSummary
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun LedgerStatistics(store: LedgerStore, referenceDate: LocalDate) {
    val kit = LocalKit.current
    var period by remember { mutableStateOf(LedgerPeriod.Month) }
    var grouping by remember { mutableStateOf(LedgerGrouping.MemberCombined) }
    var metric by remember { mutableStateOf(LedgerMetric.Quantity) }
    var currency by remember { mutableStateOf("JPY") }
    var productId by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf("") }
    var anchor by remember { mutableStateOf(referenceDate) }
    var showDate by remember { mutableStateOf(false) }

    val records = remember(store.document.records, period, anchor, metric, currency, productId, groupId) {
        LedgerAnalytics.filtered(
            store.document.records,
            period,
            KitDate.formatDay(anchor),
            if (metric == LedgerMetric.Spending) currency else null
        ).filter {
            (productId.isEmpty() || it.productId == productId) && (groupId.isEmpty() || it.groupId == groupId)
        }
    }
    val summary: LedgerSummary = remember(records) {
        try {
            LedgerAnalytics.summarize(records)
        } catch (_: Exception) {
            LedgerSummary()
        }
    }
    val rows: List<LedgerRankRow> = remember(records, grouping, metric, currency) {
        try {
            LedgerAnalytics.rankings(records, grouping, metric, if (metric == LedgerMetric.Spending) currency else null)
        } catch (_: Exception) {
            emptyList()
        }
    }
    val periodTitle = when (period) {
        LedgerPeriod.Day -> KitDate.string(
            anchor.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(), "yyyy MMM d", ZoneId.systemDefault()
        )
        LedgerPeriod.Month -> KitDate.string(
            anchor.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(), "yyyy MMMM", ZoneId.systemDefault()
        )
        LedgerPeriod.Year -> KitDate.string(
            anchor.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(), "yyyy", ZoneId.systemDefault()
        )
        LedgerPeriod.Lifetime -> L10n.text("ledger.lifetime")
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        SegmentedControl(
            options = listOf(
                "day" to L10n.text("ledger.day"),
                "month" to L10n.text("ledger.month"),
                "year" to L10n.text("ledger.year"),
                "lifetime" to L10n.text("ledger.lifetime")
            ),
            selected = period.name.lowercase(Locale.ROOT),
            onSelect = { value ->
                period = when (value) {
                    "day" -> LedgerPeriod.Day
                    "year" -> LedgerPeriod.Year
                    "lifetime" -> LedgerPeriod.Lifetime
                    else -> LedgerPeriod.Month
                }
            }
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(kit.surface)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { anchor = movePeriod(anchor, period, -1) }, enabled = period != LedgerPeriod.Lifetime) {
                Icon(Icons.Filled.ChevronLeft, null, tint = kit.muted)
            }
            Text(
                periodTitle,
                modifier = Modifier.weight(1f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = kit.ink,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(onClick = { anchor = movePeriod(anchor, period, 1) }, enabled = period != LedgerPeriod.Lifetime) {
                Icon(Icons.Filled.ChevronRight, null, tint = kit.muted)
            }
        }
        if (period != LedgerPeriod.Lifetime) {
            Spacer(Modifier.height(6.dp))
            KitSecondaryButton(L10n.text("ledger.date")) { showDate = true }
        }
        Spacer(Modifier.height(12.dp))
        FormCard {
            SegmentedControl(
                options = listOf(
                    "combined" to L10n.text("ledger.memberCombined"),
                    "byGroup" to L10n.text("ledger.memberByGroup"),
                    "group" to L10n.text("ledger.group")
                ),
                selected = when (grouping) {
                    LedgerGrouping.MemberCombined -> "combined"
                    LedgerGrouping.MemberByGroup -> "byGroup"
                    else -> "group"
                },
                onSelect = {
                    grouping = when (it) {
                        "byGroup" -> LedgerGrouping.MemberByGroup
                        "group" -> LedgerGrouping.Group
                        else -> LedgerGrouping.MemberCombined
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            SegmentedControl(
                options = listOf(
                    "quantity" to L10n.text("ledger.quantity"),
                    "spending" to L10n.text("ledger.spending")
                ),
                selected = if (metric == LedgerMetric.Quantity) "quantity" else "spending",
                onSelect = {
                    metric = if (it == "spending") LedgerMetric.Spending else LedgerMetric.Quantity
                }
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(kit.surfaceSubtle)
                        .padding(10.dp)
                ) {
                    Column {
                        Text(L10n.text("ledger.type"), fontSize = 10.sp, color = kit.muted)
                        Text(
                            store.document.products.firstOrNull { it.id == productId }?.let {
                                productDisplayName(it.id, it.name)
                            } ?: L10n.text("ledger.allTypes"),
                            fontSize = 12.sp,
                            color = kit.ink,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            SelectableChip(L10n.text("ledger.allTypes"), productId.isEmpty()) { productId = "" }
                            store.document.products.forEach { item ->
                                SelectableChip(productDisplayName(item.id, item.name), productId == item.id) {
                                    productId = item.id
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                SelectableChip(L10n.text("ledger.allGroups"), groupId.isEmpty()) { groupId = "" }
                store.document.groups.forEach { group ->
                    SelectableChip(group.name, groupId == group.id) { groupId = group.id }
                }
            }
            if (metric == LedgerMetric.Spending) {
                Spacer(Modifier.height(8.dp))
                Text(L10n.text("ledger.currency"), fontSize = 11.sp, color = kit.muted)
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    LedgerCurrency.entries.forEach { value ->
                        SelectableChip(value.code, currency == value.code) { currency = value.code }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            L10n.text(if (grouping == LedgerGrouping.MemberByGroup) "ledger.splitHint" else "ledger.combinedHint"),
            fontSize = 10.sp,
            color = kit.muted
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(L10n.text("ledger.total"), fontSize = 11.sp, color = kit.muted)
            Spacer(Modifier.weight(1f))
            Text(
                if (metric == LedgerMetric.Quantity) L10n.format("ledger.itemsCount", summary.quantity)
                else LedgerCurrency.from(currency)?.format(summary.amountsByCurrency[currency] ?: 0L, L10n.locale) ?: "",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = kit.ink
            )
            Spacer(Modifier.width(6.dp))
            Text(L10n.format("ledger.membersCount", summary.memberCount), fontSize = 10.sp, color = kit.muted)
        }
        Spacer(Modifier.height(10.dp))
        if (rows.isEmpty()) {
            EmptyState(
                title = L10n.text("ledger.noRecords"),
                hint = L10n.text("ledger.noRecordsHint")
            )
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(kit.surface)
                    .padding(15.dp)
            ) {
                rows.forEachIndexed { index, row ->
                    RankRow(row, metric, currency, store)
                    if (index < rows.lastIndex) DividerLine(Modifier.padding(start = 52.dp, top = 4.dp, bottom = 4.dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(L10n.text("ledger.currencyHint"), fontSize = 10.sp, color = kit.muted)
        Spacer(Modifier.height(35.dp))
    }

    if (showDate) {
        DatePickerSheet(anchor, { showDate = false }) {
            anchor = it
            showDate = false
        }
    }
}

private fun movePeriod(anchor: LocalDate, period: LedgerPeriod, amount: Long): LocalDate = when (period) {
    LedgerPeriod.Day -> anchor.plusDays(amount)
    LedgerPeriod.Month -> anchor.plusMonths(amount)
    LedgerPeriod.Year -> anchor.plusYears(amount)
    LedgerPeriod.Lifetime -> anchor
}

@Composable
private fun RankRow(row: LedgerRankRow, metric: LedgerMetric, currency: String, store: LedgerStore) {
    val kit = LocalKit.current
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.Top) {
        Text(
            row.rank.toString(),
            modifier = Modifier.width(16.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (row.rank <= 3) kit.accentText else kit.muted
        )
        Spacer(Modifier.width(8.dp))
        LedgerAvatar(
            member = store.document.members.firstOrNull { it.id == row.memberId },
            color = row.color,
            size = 36.dp
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
                Spacer(Modifier.weight(1f))
                Text(
                    if (metric == LedgerMetric.Quantity) row.quantity.toString()
                    else LedgerCurrency.from(currency)?.format(row.amountsByCurrency[currency] ?: 0L, L10n.locale) ?: "",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = kit.ink
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.subtitle, fontSize = 10.sp, color = kit.muted, maxLines = 2)
                Spacer(Modifier.weight(1f))
                Text(
                    percentText(row.share),
                    fontSize = 9.sp,
                    color = kit.muted
                )
            }
            Spacer(Modifier.height(4.dp))
            FlowProductSummary(row.productRows)
            Spacer(Modifier.height(5.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(hexToColor(row.color).copy(alpha = 0.17f))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(row.share.toFloat().coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(kit.readableDataColor(row.color))
                )
            }
        }
    }
}

private fun percentText(share: Double): String {
    val formatter = java.text.NumberFormat.getPercentInstance(L10n.locale)
    formatter.minimumFractionDigits = 1
    formatter.maximumFractionDigits = 1
    return formatter.format(share)
}
