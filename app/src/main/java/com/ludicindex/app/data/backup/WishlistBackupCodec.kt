package com.ludicindex.app.data.backup

import kotlinx.serialization.json.Json

/** Encodes/decodes the export file. Tolerant of unknown keys so newer exports still import. */
object WishlistBackupCodec {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(backup: WishlistBackup): String = json.encodeToString(WishlistBackup.serializer(), backup)

    /** @throws kotlinx.serialization.SerializationException when the text is not a valid backup. */
    fun decode(text: String): WishlistBackup = json.decodeFromString(WishlistBackup.serializer(), text)
}
