package com.ludicindex.app.data.local

import androidx.room.TypeConverter
import com.ludicindex.app.domain.model.EntryStatus

class Converters {
    @TypeConverter
    fun entryStatusToString(status: EntryStatus): String = status.name

    @TypeConverter
    fun stringToEntryStatus(value: String): EntryStatus =
        EntryStatus.entries.firstOrNull { it.name == value } ?: EntryStatus.WANT
}
