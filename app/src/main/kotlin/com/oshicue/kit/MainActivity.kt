package com.oshicue.kit

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.oshicue.kit.i18n.AppLanguage
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.RootScreen
import com.oshicue.kit.ui.theme.OshicueKitTheme
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val preferences = newBase.getSharedPreferences(L10n.preferencesName, Context.MODE_PRIVATE)
        val language = AppLanguage.fromRaw(preferences.getString(L10n.preferenceKey, AppLanguage.system.rawValue))
        if (language == AppLanguage.system) {
            super.attachBaseContext(newBase)
            return
        }
        val locale = Locale.forLanguageTag(language.localeID)
        val configuration = Configuration(newBase.resources.configuration)
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        L10n.attach(this)
        val stores = KitApplication.stores(this)
        setContent {
            OshicueKitTheme {
                RootScreen(stores.scheduleStore, stores.ledgerStore, stores.diaryStore, stores.agendaStore)
            }
        }
    }

    fun restartForLanguage() {
        recreate()
    }
}
