package com.ludicindex.app.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.ludicindex.app.di.AppContainer

val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer was not provided. Wrap the UI in LudicApp(container = ...).")
}
