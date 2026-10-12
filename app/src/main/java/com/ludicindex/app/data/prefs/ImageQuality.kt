package com.ludicindex.app.data.prefs

/** Maps to IGDB image size tokens (see api-docs image size table). */
enum class ImageQuality(
    val coverSize: String,
    val screenshotSize: String,
) {
    STANDARD("cover_big", "screenshot_big"),
    HIGH("cover_big_2x", "screenshot_huge");

    companion object {
        fun fromKey(key: String?): ImageQuality =
            entries.firstOrNull { it.name == key } ?: STANDARD
    }
}
