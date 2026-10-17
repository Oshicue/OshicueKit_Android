package com.oshicue.kit.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.StickyNote2
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.oshicue.kit.data.AgendaStore
import com.oshicue.kit.data.DiaryStore
import com.oshicue.kit.data.LedgerStore
import com.oshicue.kit.data.ScheduleStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.agenda.AgendaScreen
import com.oshicue.kit.ui.common.KitMessageAlert
import com.oshicue.kit.ui.diary.DiaryScreen
import com.oshicue.kit.ui.ledger.LedgerScreen
import com.oshicue.kit.ui.schedule.ScheduleScreen
import com.oshicue.kit.ui.settings.SettingsScreen
import com.oshicue.kit.ui.theme.kitPalette

private data class RootTab(val module: Int, val labelKey: String, val icon: ImageVector)

@Composable
fun RootScreen(
    scheduleStore: ScheduleStore,
    ledgerStore: LedgerStore,
    diaryStore: DiaryStore,
    agendaStore: AgendaStore
) {
    val context = LocalContext.current
    val palette = kitPalette()
    val preferences = remember(context) { context.getSharedPreferences("oshicue-kit", android.content.Context.MODE_PRIVATE) }
    var tab by remember {
        val saved = preferences.getInt("selectedModule", AgendaModule)
        val restored = if (saved in 0..4) saved else AgendaModule
        if (restored != saved) preferences.edit().putInt("selectedModule", restored).apply()
        mutableIntStateOf(restored)
    }
    val tabs = listOf(
        RootTab(AgendaModule, "agenda.title", Icons.Filled.CalendarMonth),
        RootTab(0, "tab.schedule", Icons.Filled.Event),
        RootTab(1, "ledger.title", Icons.AutoMirrored.Filled.MenuBook),
        RootTab(2, "diary.title", Icons.AutoMirrored.Filled.StickyNote2),
        RootTab(3, "settings.title", Icons.Filled.Settings)
    )

    Scaffold(
        containerColor = palette.background,
        bottomBar = {
            NavigationBar(containerColor = palette.surface) {
                for (item in tabs) {
                    NavigationBarItem(
                        selected = tab == item.module,
                        onClick = {
                            tab = item.module
                            preferences.edit().putInt("selectedModule", item.module).apply()
                        },
                        icon = { Icon(item.icon, contentDescription = L10n.text(item.labelKey)) },
                        label = { Text(L10n.text(item.labelKey), fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = palette.accentText,
                            selectedTextColor = palette.accentText,
                            unselectedIconColor = palette.muted,
                            unselectedTextColor = palette.muted,
                            indicatorColor = palette.surfaceMuted
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                AgendaModule -> AgendaScreen(agendaStore)
                0 -> ScheduleScreen(scheduleStore)
                1 -> LedgerScreen(ledgerStore, scheduleStore)
                2 -> DiaryScreen(diaryStore, ledgerStore)
                else -> SettingsScreen(scheduleStore, ledgerStore, diaryStore, agendaStore)
            }
        }
    }

    scheduleStore.storageError?.let { error ->
        KitMessageAlert(L10n.text("settings.storageError"), error) { scheduleStore.storageError = null }
    }
    agendaStore.storageError?.let { error ->
        KitMessageAlert(L10n.text("settings.storageError"), error) { agendaStore.storageError = null }
    }
}

private const val AgendaModule = 4
