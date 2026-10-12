package com.ludicindex.app.data.prefs

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromKey(key: String?): ThemeMode =
            entries.firstOrNull { it.name == key } ?: SYSTEM
    }
}
