package com.oshicue.kit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MenuBook
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.AgendaStore
import com.oshicue.kit.core.AppLanguage
import com.oshicue.kit.core.DiaryStore
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerStore
import com.oshicue.kit.core.ScheduleStore
import com.oshicue.kit.core.SettingsStore

@Composable
fun SettingsScreen(
    settings: SettingsStore,
    scheduleStore: ScheduleStore,
    ledgerStore: LedgerStore,
    diaryStore: DiaryStore,
    agendaStore: AgendaStore,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val kit = LocalKit.current
    var showLanguage by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(kit.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            L10n.text("settings.title"),
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            color = kit.ink
        )
        Spacer(Modifier.height(16.dp))

        SectionLabel(L10n.text("settings.language"))
        Spacer(Modifier.height(6.dp))
        SettingRow(
            title = L10n.text("settings.language"),
            value = settings.language.nativeName,
            symbol = Icons.Filled.Check
        ) { showLanguage = true }

        Spacer(Modifier.height(18.dp))
        SectionLabel(L10n.text("settings.data"))
        Spacer(Modifier.height(6.dp))
        SettingRow(L10n.text("agenda.management"), symbol = Icons.Filled.CalendarViewDay) { destination = "agenda" }
        SettingRow(L10n.text("settings.scheduleImport"), symbol = Icons.Filled.CalendarMonth) { destination = "schedule" }
        SettingRow(L10n.text("settings.ledgerBackup"), symbol = Icons.Filled.MenuBook) { destination = "ledger" }
        SettingRow(L10n.text("settings.diaryBackup"), symbol = Icons.Filled.Archive) { destination = "diary" }
        Spacer(Modifier.height(6.dp))
        Text(L10n.text("settings.localData"), fontSize = 11.sp, color = kit.muted)

        Spacer(Modifier.height(20.dp))
        SectionLabel(L10n.text("settings.about"))
        Spacer(Modifier.height(8.dp))
        FormCard {
            BrandWordmark()
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(L10n.text("settings.version"), fontSize = 12.5.sp, color = kit.muted)
                Spacer(Modifier.weight(1f))
                Text("0.5.3 · 16", fontSize = 12.5.sp, color = kit.ink)
            }
            Spacer(Modifier.height(6.dp))
            Text("Unbounded — SIL Open Font License 1.1", fontSize = 11.sp, color = kit.muted)
            Spacer(Modifier.height(4.dp))
            Text(L10n.text("settings.diaryAbout"), fontSize = 11.sp, color = kit.muted)
        }
        Spacer(Modifier.height(40.dp))
    }

    if (showLanguage) {
        val options = AppLanguage.entries.map { it.id to it.nativeName }
        OptionSelector(
            title = L10n.text("settings.language"),
            options = options,
            selected = settings.language.id,
            onDismiss = { showLanguage = false },
            onSelect = { id ->
                val language = AppLanguage.fromId(id)
                settings.updateLanguage(language)
                L10n.invalidate()
                onLanguageChange(language)
            }
        )
    }

    when (destination) {
        "agenda" -> AgendaManagementSheet(
            store = agendaStore,
            onClose = { destination = null },
            onApplied = { destination = null }
        )
        "schedule" -> ScheduleImportSheet(store = scheduleStore, onClose = { destination = null })
        "ledger" -> LedgerManagementSheet(store = ledgerStore, onClose = { destination = null })
        "diary" -> DiaryManagementSheet(store = diaryStore, onClose = { destination = null })
        else -> Unit
    }
}

@Composable
private fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    symbol: ImageVector? = null,
    onClick: () -> Unit
) {
    val kit = LocalKit.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(kit.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (symbol != null) {
            Icon(symbol, null, tint = kit.accentText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(title, fontSize = 13.5.sp, color = kit.ink)
        Spacer(Modifier.weight(1f))
        if (value != null) {
            Text(value, fontSize = 12.5.sp, color = kit.muted, maxLines = 1)
            Spacer(Modifier.width(6.dp))
        }
        Text("›", fontSize = 15.sp, color = kit.muted)
    }
}
