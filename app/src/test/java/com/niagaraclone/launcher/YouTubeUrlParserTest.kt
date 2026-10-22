package com.niagaraclone.launcher

import com.niagaraclone.launcher.ui.components.parseTimestampSeconds
import com.niagaraclone.launcher.ui.components.parseYouTubeUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class YouTubeUrlParserTest {

    @Test
    fun testStandardWatchUrl() {
        val result = parseYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
        assertEquals(0, result?.second)
    }

    @Test
    fun testStandardWatchUrlWithExtraParams() {
        val result = parseYouTubeUrl("https://www.youtube.com/watch?feature=share&v=dQw4w9WgXcQ&si=123456")
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
        assertEquals(0, result?.second)
    }

    @Test
    fun testShortenedYouTuBeUrl() {
        val result = parseYouTubeUrl("https://youtu.be/dQw4w9WgXcQ")
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
        assertEquals(0, result?.second)
    }

    @Test
    fun testShortenedYouTuBeWithTimestamp() {
        val result = parseYouTubeUrl("https://youtu.be/dQw4w9WgXcQ?t=1m30s")
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
        assertEquals(90, result?.second)
    }

    @Test
    fun testSchemelessUrls() {
        val res1 = parseYouTubeUrl("youtu.be/dQw4w9WgXcQ?t=45")
        assertEquals("dQw4w9WgXcQ", res1?.first)
        assertEquals(45, res1?.second)

        val res2 = parseYouTubeUrl("youtube.com/watch?v=dQw4w9WgXcQ")
        assertEquals("dQw4w9WgXcQ", res2?.first)

        val res3 = parseYouTubeUrl("m.youtube.com/watch?v=dQw4w9WgXcQ")
        assertEquals("dQw4w9WgXcQ", res3?.first)
    }

    @Test
    fun testYouTubeShortsUrl() {
        val result = parseYouTubeUrl("https://www.youtube.com/shorts/dQw4w9WgXcQ?feature=share")
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
    }

    @Test
    fun testYouTubeLiveUrl() {
        val result = parseYouTubeUrl("https://www.youtube.com/live/dQw4w9WgXcQ?si=live123")
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
    }

    @Test
    fun testYouTubeMusicUrl() {
        val result = parseYouTubeUrl("https://music.youtube.com/watch?v=dQw4w9WgXcQ&list=RDAMVM")
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
    }

    @Test
    fun testYouTubeEmbedUrl() {
        val result = parseYouTubeUrl("https://www.youtube.com/embed/dQw4w9WgXcQ")
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
    }

    @Test
    fun testAttributionLink() {
        val result = parseYouTubeUrl("https://www.youtube.com/attribution_link?a=xyz&u=%2Fwatch%3Fv%3DdQw4w9WgXcQ%26feature%3Dshare")
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
    }

    @Test
    fun testRawElevenCharId() {
        val result = parseYouTubeUrl("dQw4w9WgXcQ")
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
        assertEquals(0, result?.second)
    }

    @Test
    fun testTextSharedFromApps() {
        val result = parseYouTubeUrl("Look at this great video: https://youtu.be/dQw4w9WgXcQ?si=share123 via YouTube")
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
    }

    @Test
    fun testTimestampVariations() {
        assertEquals(90, parseTimestampSeconds("90"))
        assertEquals(90, parseTimestampSeconds("90s"))
        assertEquals(90, parseTimestampSeconds("1m30s"))
        assertEquals(3600, parseTimestampSeconds("1h"))
        assertEquals(3665, parseTimestampSeconds("1h1m5s"))
        assertEquals(180, parseTimestampSeconds("3m"))
    }

    @Test
    fun testInvalidInputs() {
        assertNull(parseYouTubeUrl(null))
        assertNull(parseYouTubeUrl(""))
        assertNull(parseYouTubeUrl("   "))
        assertNull(parseYouTubeUrl("https://google.com"))
        assertNull(parseYouTubeUrl("https://youtube.com/about"))
    }
}
