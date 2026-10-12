package com.ludicindex.app.data.remote

import com.ludicindex.app.data.prefs.ImageQuality

/**
 * IGDB image URLs look like https://images.igdb.com/igdb/image/upload/t_{size}/{image_id}.jpg
 * Sizes come from the api-docs image table; appending _2x doubles the pixel size.
 */
object IgdbImage {

    private const val BASE = "https://images.igdb.com/igdb/image/upload"

    const val COVER_SMALL = "cover_small"
    const val COVER_BIG = "cover_big"
    const val SCREENSHOT_MED = "screenshot_med"
    const val SCREENSHOT_BIG = "screenshot_big"
    const val SCREENSHOT_HUGE = "screenshot_huge"
    const val THUMB = "thumb"

    fun url(imageId: String, size: String): String = "$BASE/t_$size/$imageId.jpg"

    fun coverUrl(imageId: String?, quality: ImageQuality): String? =
        imageId?.takeIf { it.isNotBlank() }?.let { url(it, quality.coverSize) }

    fun screenshotUrl(imageId: String?, quality: ImageQuality): String? =
        imageId?.takeIf { it.isNotBlank() }?.let { url(it, quality.screenshotSize) }

    /** YouTube thumbnails for IGDB`s game videos. */
    fun videoThumbnailUrl(videoId: String): String =
        "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

    fun videoWatchUrl(videoId: String): String = "https://www.youtube.com/watch?v=$videoId"

    fun websiteUrl(url: String): String =
        if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url"
}
