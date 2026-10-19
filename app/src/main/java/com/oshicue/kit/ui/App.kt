package com.oshicue.kit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NoteAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.AgendaStore
import com.oshicue.kit.core.AppLanguage
import com.oshicue.kit.core.AppModule
import com.oshicue.kit.core.DiaryStore
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerStore
import com.oshicue.kit.core.ScheduleStore
import com.oshicue.kit.core.SettingsStore
import java.io.File

/** Holds the long-lived stores so tab switches keep their state. */
class KitAppState(baseDirectory: File) {
    val directory: File = File(baseDirectory, "OshicueKit").apply { mkdirs() }
    val settings = SettingsStore(directory)
    val schedule = ScheduleStore(directory)
    val ledger = LedgerStore(directory)
    val diary = DiaryStore(directory)
    val agenda = AgendaStore(directory)
}

@Composable
fun KitApp(state: KitAppState) {
    var languageVersion by remember { mutableStateOf(0) }
    var module by remember { mutableStateOf(state.settings.selectedModule) }

    /** Switches language without recreating the stores; L10n re-reads every string. */
    fun changeLanguage(language: AppLanguage) {
        state.settings.updateLanguage(language)
        L10n.setLanguage(language)
        languageVersion += 1
    }

    key(languageVersion) {
        LocalizedConfiguration {
            KitTheme {
                KitScaffold(
                    state = state,
                    module = module,
                    onModuleChange = {
                        module = it
                        state.settings.updateSelectedModule(it)
                    },
                    onLanguageChange = ::changeLanguage
                )
            }
        }
    }
}

/**
 * Gives Material date/time pickers and other locale readers the app language, not only the device one.
 * Dialog windows re-provide the device configuration, so wrap dialog content in this as well.
 */
@Composable
internal fun LocalizedConfiguration(content: @Composable () -> Unit) {
    val base = androidx.compose.ui.platform.LocalConfiguration.current
    val locale = L10n.locale
    val configuration = remember(base, locale) {
        android.content.res.Configuration(base).apply { setLocale(locale) }
    }
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.ui.platform.LocalConfiguration provides configuration,
        content = content
    )
}

@Composable
private fun KitScaffold(
    state: KitAppState,
    module: AppModule,
    onModuleChange: (AppModule) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit
) {
    val kit = LocalKit.current
    val modules: List<Triple<AppModule, String, ImageVector>> = listOf(
        Triple(AppModule.Agenda, L10n.text("agenda.title"), Icons.Filled.CalendarViewDay),
        Triple(AppModule.Schedule, L10n.text("tab.schedule"), Icons.Filled.CalendarMonth),
        Triple(AppModule.Ledger, L10n.text("ledger.title"), Icons.Filled.MenuBook),
        Triple(AppModule.Diary, L10n.text("diary.title"), Icons.Filled.NoteAlt),
        Triple(AppModule.Settings, L10n.text("settings.title"), Icons.Filled.Settings)
    )

    Scaffold(
        containerColor = kit.background,
        bottomBar = {
            NavigationBar(containerColor = kit.surface, tonalElevation = 0.dp) {
                modules.forEach { (value, label, icon) ->
                    NavigationBarItem(
                        selected = module == value,
                        onClick = { onModuleChange(value) },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(21.dp)) },
                        label = {
                            Text(
                                label,
                                fontSize = 10.sp,
                                fontWeight = if (module == value) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = kit.accentText,
                            selectedTextColor = kit.accentText,
                            unselectedIconColor = kit.muted,
                            unselectedTextColor = kit.muted,
                            indicatorColor = kit.accent.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Top))
        ) {
            when (module) {
                AppModule.Agenda -> AgendaScreen(state.agenda)
                AppModule.Schedule -> ScheduleScreen(state.schedule)
                AppModule.Ledger -> LedgerScreen(state.ledger, state.schedule)
                AppModule.Diary -> DiaryScreen(state.diary, state.ledger)
                AppModule.Settings -> SettingsScreen(
                    settings = state.settings,
                    scheduleStore = state.schedule,
                    ledgerStore = state.ledger,
                    diaryStore = state.diary,
                    agendaStore = state.agenda,
                    onLanguageChange = onLanguageChange
                )
            }
        }
    }
}
