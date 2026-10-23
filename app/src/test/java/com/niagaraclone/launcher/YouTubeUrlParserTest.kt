package com.niagaraclone.launcher

import com.niagaraclone.launcher.ui.components.YouTubeParseResult
import com.niagaraclone.launcher.ui.components.parseTimestampSeconds
import com.niagaraclone.launcher.ui.components.parseYouTubeInput
import com.niagaraclone.launcher.ui.components.parseYouTubeUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun testChatGPTMarkdownLink() {
        // [Title](url)
        val result1 = parseYouTubeUrl("[Rick Astley - Never Gonna Give You Up](https://www.youtube.com/watch?v=dQw4w9WgXcQ)")
        assertNotNull(result1)
        assertEquals("dQw4w9WgXcQ", result1?.first)

        val result2 = parseYouTubeUrl("[Check this out](https://youtu.be/dQw4w9WgXcQ?si=abcdef123)")
        assertNotNull(result2)
        assertEquals("dQw4w9WgXcQ", result2?.first)
    }

    @Test
    fun testChatGPTBacktickLink() {
        val result1 = parseYouTubeUrl("`https://www.youtube.com/watch?v=dQw4w9WgXcQ`")
        assertNotNull(result1)
        assertEquals("dQw4w9WgXcQ", result1?.first)

        val result2 = parseYouTubeUrl("`https://youtu.be/dQw4w9WgXcQ`")
        assertNotNull(result2)
        assertEquals("dQw4w9WgXcQ", result2?.first)
    }

    @Test
    fun testTrailingPunctuationFromChat() {
        // Trailing dot from a sentence
        val resDot = parseYouTubeUrl("Check out this video: https://youtu.be/dQw4w9WgXcQ.")
        assertEquals("dQw4w9WgXcQ", resDot?.first)

        // Trailing comma
        val resComma = parseYouTubeUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ,")
        assertEquals("dQw4w9WgXcQ", resComma?.first)

        // Parentheses
        val resParen = parseYouTubeUrl("(https://www.youtube.com/watch?v=dQw4w9WgXcQ)")
        assertEquals("dQw4w9WgXcQ", resParen?.first)

        // Quotes
        val resQuotes = parseYouTubeUrl("\"https://youtu.be/dQw4w9WgXcQ\"")
        assertEquals("dQw4w9WgXcQ", resQuotes?.first)

        // Exclamation mark
        val resExcl = parseYouTubeUrl("https://youtu.be/dQw4w9WgXcQ!")
        assertEquals("dQw4w9WgXcQ", resExcl?.first)
    }

    @Test
    fun testIframeHtmlEmbed() {
        val html = "<iframe width=\"560\" height=\"315\" src=\"https://www.youtube.com/embed/dQw4w9WgXcQ\" frameborder=\"0\"></iframe>"
        val result = parseYouTubeUrl(html)
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
    }

    @Test
    fun testRedirectUrls() {
        // Google redirect
        val google = "https://www.google.com/url?q=https://www.youtube.com/watch%3Fv%3DdQw4w9WgXcQ&sa=U"
        val resGoogle = parseYouTubeUrl(google)
        assertEquals("dQw4w9WgXcQ", resGoogle?.first)

        // ChatGPT redirect
        val chatgpt = "https://chatgpt.com/redirect?url=https%3A%2F%2Fyoutu.be%2FdQw4w9WgXcQ"
        val resChatGPT = parseYouTubeUrl(chatgpt)
        assertEquals("dQw4w9WgXcQ", resChatGPT?.first)

        // Reddit redirect
        val reddit = "https://out.reddit.com/url?url=https%3A%2F%2Fwww.youtube.com%2Fwatch%3Fv%3DdQw4w9WgXcQ"
        val resReddit = parseYouTubeUrl(reddit)
        assertEquals("dQw4w9WgXcQ", resReddit?.first)
    }

    @Test
    fun testPlaylistWatchUrl() {
        val result = parseYouTubeUrl("https://www.youtube.com/watch?list=PL123456&v=dQw4w9WgXcQ&index=1")
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
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
    fun testMultilineChatText() {
        val multiLine = """
            Here is the recommendation from ChatGPT:
            Title: Never Gonna Give You Up
            Link: https://youtu.be/dQw4w9WgXcQ?si=share123
            Enjoy watching!
        """.trimIndent()
        val result = parseYouTubeUrl(multiLine)
        assertNotNull(result)
        assertEquals("dQw4w9WgXcQ", result?.first)
    }

    @Test
    fun testSearchQueryInput() {
        val queryResult = parseYouTubeInput("Huberman Lab Sleep Protocol")
        assertTrue(queryResult is YouTubeParseResult.SearchQuery)
        assertEquals("Huberman Lab Sleep Protocol", (queryResult as YouTubeParseResult.SearchQuery).query)

        val searchUrlResult = parseYouTubeInput("https://www.youtube.com/results?search_query=focus+lofi+beats")
        assertTrue(searchUrlResult is YouTubeParseResult.SearchQuery)
        assertEquals("focus lofi beats", (searchUrlResult as YouTubeParseResult.SearchQuery).query)
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
