package com.ludicindex.app.data.backup

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WishlistBackupCodecTest {

    @Test
    fun roundTripsEveryField() {
        val original = WishlistBackup(
            exportedAt = 1_760_000_000,
            items = listOf(
                BackupItem(
                    igdbId = 1942,
                    name = "The Witcher 3: Wild Hunt",
                    coverImageId = "coaarl",
                    releaseDate = 1_431_993_600,
                    rating = 92.75,
                    summary = "An open-world action RPG.",
                    platform = "Steam",
                    note = "wait for the next-gen patch",
                    addedAt = 1_759_000_000,
                    tags = listOf("Backlog", "PC"),
                ),
            ),
        )

        val decoded = WishlistBackupCodec.decode(WishlistBackupCodec.encode(original))

        assertEquals(original, decoded)
        assertEquals(WishlistBackup.SCHEMA_VERSION, decoded.schemaVersion)
        assertEquals(WishlistBackup.APP_ID, decoded.app)
    }

    @Test
    fun encodedJsonUsesSnakeCaseKeys() {
        val json = WishlistBackupCodec.encode(
            WishlistBackup(exportedAt = 1, items = emptyList()),
        )

        assertTrue(json.contains("\"schema_version\""))
        assertTrue(json.contains("\"exported_at\""))
        assertTrue(json.contains("\"items\""))
    }

    @Test
    fun fillsDefaultsForMissingOptionalFields() {
        val json = """
            {
              "exported_at": 42,
              "items": [
                { "igdb_id": 7, "name": "Celeste", "added_at": 41 }
              ]
            }
        """.trimIndent()

        val backup = WishlistBackupCodec.decode(json)

        assertEquals(WishlistBackup.SCHEMA_VERSION, backup.schemaVersion)
        val item = backup.items.single()
        assertEquals(7L, item.igdbId)
        assertEquals("", item.note)
        assertEquals(emptyList<String>(), item.tags)
    }

    @Test
    fun ignoresUnknownKeysFromNewerExports() {
        val json = """
            {
              "schema_version": 1,
              "exported_at": 42,
              "future_field": { "anything": true },
              "items": [
                { "igdb_id": 7, "name": "Celeste", "added_at": 41, "hype_score": 3.5 }
              ]
            }
        """.trimIndent()

        val backup = WishlistBackupCodec.decode(json)

        assertEquals("Celeste", backup.items.single().name)
    }

    @Test
    fun rejectsTextThatIsNotABackup() {
        assertThrows(SerializationException::class.java) {
            WishlistBackupCodec.decode("this is not json")
        }
    }
}
