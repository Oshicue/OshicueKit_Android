package com.oshicue.kit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.remember
import com.oshicue.kit.core.AppLanguage
import com.oshicue.kit.core.L10n
import com.oshicue.kit.ui.KitApp
import com.oshicue.kit.ui.KitAppState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        L10n.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            val state = remember {
                KitAppState(filesDir).also { L10n.setLanguage(it.settings.language) }
            }
            KitApp(state)
        }
    }

    override fun onResume() {
        super.onResume()
        // The system language may change while the app is in the background.
        if (L10n.selectedLanguage == AppLanguage.System) L10n.invalidate()
    }
}
