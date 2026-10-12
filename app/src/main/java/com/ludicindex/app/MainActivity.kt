package com.ludicindex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ludicindex.app.data.prefs.ThemeMode
import com.ludicindex.app.ui.LudicApp
import com.ludicindex.app.ui.theme.LudicIndexTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as LudicIndexApplication).container

        setContent {
            val themeMode by container.settingsStore.themeMode
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val dynamicColor by container.settingsStore.dynamicColor
                .collectAsStateWithLifecycle(initialValue = true)

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            LudicIndexTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
            ) {
                LudicApp(container)
            }
        }
    }
}
