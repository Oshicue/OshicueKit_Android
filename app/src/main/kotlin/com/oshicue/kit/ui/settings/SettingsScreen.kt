package com.oshicue.kit.ui.settings

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.MainActivity
import com.oshicue.kit.data.DiaryStore
import com.oshicue.kit.data.LedgerStore
import com.oshicue.kit.data.ScheduleStore
import com.oshicue.kit.i18n.AppLanguage
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.BrandWordmark
import com.oshicue.kit.ui.common.KitAlert
import com.oshicue.kit.ui.common.KitSheet
import com.oshicue.kit.ui.diary.DiaryManagementSheet
import com.oshicue.kit.ui.ledger.LedgerManagementSheet
import com.oshicue.kit.ui.schedule.ImportSheet
import com.oshicue.kit.ui.theme.kitPalette

@Composable
fun SettingsScreen(scheduleStore: ScheduleStore, ledgerStore: LedgerStore, diaryStore: DiaryStore) {
    val palette = kitPalette()
    val context = LocalContext.current
    var destination by remember { mutableStateOf<String?>(null) }
    var showLanguage by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val appVersion = "0.4.0 · 10"

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            L10n.text("settings.title"),
            color = palette.ink,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        SettingsSection(title = L10n.text("settings.language")) {
            SettingsRow(
                icon = Icons.Filled.Check,
                label = L10n.text("settings.language"),
                detail = L10n.resolvedLanguage(context).nativeName
            ) { showLanguage = true }
        }
        SettingsSection(title = L10n.text("settings.data"), footer = L10n.text("settings.localData")) {
            SettingsRow(Icons.Filled.CalendarMonth, L10n.text("settings.scheduleImport")) { destination = "schedule" }
            SettingsRow(Icons.AutoMirrored.Filled.MenuBook, L10n.text("settings.ledgerBackup")) { destination = "ledger" }
            SettingsRow(Icons.Filled.Inventory, L10n.text("settings.diaryBackup")) { destination = "diary" }
        }
        SettingsSection(title = L10n.text("settings.demoLabel"), footer = L10n.text("settings.demoHint")) {
            SettingsRow(Icons.Filled.SmartDisplay, L10n.text("settings.loadDemo")) { scheduleStore.loadDemo() }
        }
        SettingsSection(title = L10n.text("ledger.title"), footer = L10n.text("ledger.loadDemoHint")) {
            SettingsRow(Icons.Filled.Group, L10n.text("settings.ledgerDemo")) {
                if (!ledgerStore.loadDemo()) error = ledgerStore.storageError
            }
        }
        SettingsSection(title = L10n.text("settings.about")) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                BrandWordmark()
                Spacer(modifier = Modifier.height(10.dp))
                Row {
                    Text(L10n.text("settings.version"), color = palette.muted, fontSize = 14.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(appVersion, color = palette.ink, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Unbounded — SIL Open Font License 1.1", color = palette.muted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Text(L10n.text("settings.diaryAbout"), color = palette.muted, fontSize = 12.sp)
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showLanguage) {
        LanguageSheet(
            selected = L10n.selected(context),
            onSelect = { language ->
                L10n.setLanguage(context, language)
                showLanguage = false
                (context as? MainActivity)?.restartForLanguage()
            },
            onClose = { showLanguage = false }
        )
    }
    when (destination) {
        "schedule" -> ImportSheet(scheduleStore) { destination = null }
        "ledger" -> LedgerManagementSheet(ledgerStore) { destination = null }
        "diary" -> DiaryManagementSheet(diaryStore) { destination = null }
    }
    error?.let { message ->
        KitAlert(
            title = L10n.text("settings.storageError"),
            message = message,
            confirmLabel = L10n.text("common.close"),
            onConfirm = { error = null },
            onDismiss = { error = null },
            dismissLabel = L10n.text("common.close")
        )
    }
}

@Composable
private fun SettingsSection(title: String, footer: String? = null, content: @Composable () -> Unit) {
    val palette = kitPalette()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = palette.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Column(
            modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(16.dp)).padding(6.dp),
            content = { content() }
        )
        footer?.let { Text(it, color = palette.muted, fontSize = 11.sp) }
    }
}

@Composable
private fun SettingsRow(icon: ImageVector, label: String, detail: String? = null, onClick: () -> Unit) {
    val palette = kitPalette()
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, color = palette.ink, fontSize = 15.sp)
        Spacer(modifier = Modifier.weight(1f))
        detail?.let { Text(it, color = palette.muted, fontSize = 14.sp) }
    }
}

@Composable
private fun LanguageSheet(selected: AppLanguage, onSelect: (AppLanguage) -> Unit, onClose: () -> Unit) {
    val palette = kitPalette()
    KitSheet(L10n.text("settings.language"), onClose) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            for (language in AppLanguage.entries) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(language) }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(language.nativeName, color = palette.ink, fontSize = 15.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    if (language == selected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
