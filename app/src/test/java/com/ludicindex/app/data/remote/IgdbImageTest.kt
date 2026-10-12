package com.ludicindex.app.data.remote

import com.ludicindex.app.data.prefs.ImageQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IgdbImageTest {

    @Test
    fun buildsCoverUrlsForEachQuality() {
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/coaarl.jpg",
            IgdbImage.coverUrl("coaarl", ImageQuality.STANDARD),
        )
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_cover_big_2x/coaarl.jpg",
            IgdbImage.coverUrl("coaarl", ImageQuality.HIGH),
        )
    }

    @Test
    fun buildsScreenshotUrlsForEachQuality() {
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_screenshot_big/sctj8u.jpg",
            IgdbImage.screenshotUrl("sctj8u", ImageQuality.STANDARD),
        )
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_screenshot_huge/sctj8u.jpg",
            IgdbImage.screenshotUrl("sctj8u", ImageQuality.HIGH),
        )
    }

    @Test
    fun returnsNullForMissingImageIds() {
        assertNull(IgdbImage.coverUrl(null, ImageQuality.STANDARD))
        assertNull(IgdbImage.coverUrl("   ", ImageQuality.STANDARD))
        assertNull(IgdbImage.screenshotUrl(null, ImageQuality.STANDARD))
    }

    @Test
    fun buildsYouTubeUrls() {
        assertEquals(
            "https://img.youtube.com/vi/yowv6_rspoM/hqdefault.jpg",
            IgdbImage.videoThumbnailUrl("yowv6_rspoM"),
        )
        assertEquals(
            "https://www.youtube.com/watch?v=yowv6_rspoM",
            IgdbImage.videoWatchUrl("yowv6_rspoM"),
        )
    }

    @Test
    fun qualifiesBareWebsiteUrls() {
        assertEquals("https://example.com", IgdbImage.websiteUrl("example.com"))
        assertEquals("https://example.com", IgdbImage.websiteUrl("https://example.com"))
        assertEquals("http://example.com", IgdbImage.websiteUrl("http://example.com"))
    }
}
