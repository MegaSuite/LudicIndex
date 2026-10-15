package com.ludicindex.app.data.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2: adds the game-library tables (subscription catalogs first, owned accounts later).
 *
 * The SQL mirrors exactly what Room generates for [com.ludicindex.app.data.local.entity.LibrarySourceEntity]
 * and [com.ludicindex.app.data.local.entity.LibraryItemEntity]; if the entities change, this must too.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `library_sources` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`source_key` TEXT NOT NULL, " +
                "`kind` TEXT NOT NULL, " +
                "`platform` TEXT NOT NULL, " +
                "`label` TEXT NOT NULL, " +
                "`external_account_id` TEXT, " +
                "`region` TEXT, " +
                "`enabled` INTEGER NOT NULL, " +
                "`last_synced_at` INTEGER, " +
                "`created_at` INTEGER NOT NULL)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_library_sources_source_key` " +
                "ON `library_sources` (`source_key`)",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `library_items` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`source_id` INTEGER NOT NULL, " +
                "`external_id` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`cover_url` TEXT, " +
                "`platforms` TEXT NOT NULL, " +
                "`release_date` INTEGER, " +
                "`igdb_id` INTEGER, " +
                "`match_kind` TEXT NOT NULL, " +
                "`owned_at` INTEGER, " +
                "`leaving_at` INTEGER, " +
                "`added_at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`source_id`) REFERENCES `library_sources`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_library_items_source_id` " +
                "ON `library_items` (`source_id`)",
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_library_items_igdb_id` " +
                "ON `library_items` (`igdb_id`)",
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_library_items_source_id_external_id` " +
                "ON `library_items` (`source_id`, `external_id`)",
        )
    }
}

/**
 * v2 → v3: drops the wishlist `status` column (想玩 / 在玩 / 已玩).
 *
 * SQLite on minSdk 26 cannot `ALTER TABLE ... DROP COLUMN`, so the table is rebuilt. `entry_tags`
 * references `wishlist_entries(id)` with `ON DELETE CASCADE`, so dropping the parent table would
 * cascade away every tag link when foreign keys are enforced; the links are therefore stashed in a
 * temporary table and restored afterwards. All ids are preserved, so the foreign keys stay valid.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `wishlist_entries_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`igdb_id` INTEGER NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`cover_image_id` TEXT, " +
                "`release_date` INTEGER, " +
                "`rating` REAL, " +
                "`summary` TEXT, " +
                "`note` TEXT NOT NULL, " +
                "`added_at` INTEGER NOT NULL, " +
                "`updated_at` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO `wishlist_entries_new` " +
                "(`id`, `igdb_id`, `name`, `cover_image_id`, `release_date`, `rating`, `summary`, " +
                "`note`, `added_at`, `updated_at`) " +
                "SELECT `id`, `igdb_id`, `name`, `cover_image_id`, `release_date`, `rating`, " +
                "`summary`, `note`, `added_at`, `updated_at` FROM `wishlist_entries`",
        )
        db.execSQL("CREATE TABLE IF NOT EXISTS `entry_tags_backup` AS SELECT * FROM `entry_tags`")
        db.execSQL("DELETE FROM `entry_tags`")
        db.execSQL("DROP TABLE `wishlist_entries`")
        db.execSQL("ALTER TABLE `wishlist_entries_new` RENAME TO `wishlist_entries`")
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_wishlist_entries_igdb_id` " +
                "ON `wishlist_entries` (`igdb_id`)",
        )
        db.execSQL(
            "INSERT OR IGNORE INTO `entry_tags` (`entry_id`, `tag_id`) " +
                "SELECT `entry_id`, `tag_id` FROM `entry_tags_backup`",
        )
        db.execSQL("DROP TABLE `entry_tags_backup`")
    }
}

/** v3 → v4: adds the per-entry `platform` (the game's chosen store link, e.g. "Steam"). */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `wishlist_entries` ADD COLUMN `platform` TEXT")
    }
}
