package com.niagaraclone.launcher

import com.niagaraclone.launcher.data.model.AppInfo
import com.niagaraclone.launcher.data.model.LauncherSettings
import com.niagaraclone.launcher.data.model.YouTubeQueueItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Wave2Wave3FeaturesTest {

    @Test
    fun testYouTubeQueueItemDataModel() {
        val item = YouTubeQueueItem(
            id = "dQw4w9WgXcQ",
            title = "Deliberate Learning",
            url = "https://youtu.be/dQw4w9WgXcQ",
            timestamp = 1700000000000L
        )

        assertEquals("dQw4w9WgXcQ", item.id)
        assertEquals("Deliberate Learning", item.title)
        assertEquals("https://youtu.be/dQw4w9WgXcQ", item.url)
        assertEquals(1700000000000L, item.timestamp)

        val updated = item.copy(title = "Updated Deliberate Learning")
        assertEquals("Updated Deliberate Learning", updated.title)
        assertEquals("dQw4w9WgXcQ", updated.id)
        assertEquals("https://youtu.be/dQw4w9WgXcQ", updated.url)
    }

    @Test
    fun testCustomAppNameSerializationLogic() {
        val customMap = mapOf(
            "com.twitter.android/com.twitter.android.MainActivity_0" to "X (Bird)",
            "com.google.android.youtube/com.google.android.youtube.HomeActivity_0" to "Video Hub"
        )

        val serialized = customMap.entries.joinToString(":::") { "${it.key}=${it.value}" }
        assertTrue(serialized.contains("X (Bird)"))
        assertTrue(serialized.contains("Video Hub"))

        val restored = mutableMapOf<String, String>()
        serialized.split(":::").forEach { entry ->
            val parts = entry.split("=", limit = 2)
            if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                restored[parts[0]] = parts[1]
            }
        }

        assertEquals(2, restored.size)
        assertEquals("X (Bird)", restored["com.twitter.android/com.twitter.android.MainActivity_0"])
        assertEquals("Video Hub", restored["com.google.android.youtube/com.google.android.youtube.HomeActivity_0"])
    }

    @Test
    fun testAppInfoSectionLetterComputation() {
        assertEquals('A', AppInfo.computeSectionLetter("Audible"))
        assertEquals('A', AppInfo.computeSectionLetter("amazon shopping"))
        assertEquals('Z', AppInfo.computeSectionLetter("Zoom"))
        assertEquals('#', AppInfo.computeSectionLetter("1Password"))
        assertEquals('#', AppInfo.computeSectionLetter("@Voice"))
        assertEquals('#', AppInfo.computeSectionLetter("   #Twitter"))
        assertEquals('#', AppInfo.computeSectionLetter(""))
    }

    @Test
    fun testLauncherSettingsDefaults() {
        val settings = LauncherSettings()
        assertTrue("Double-tap to lock must default to true", settings.doubleTapToLock)
        assertTrue("Show daily quote must default to true", settings.showDailyQuote)
        assertTrue("Intercept YouTube must default to true", settings.interceptYouTube)
    }

    @Test
    fun testScrubberAlphabetCompleteness() {
        val alphabet = ('A'..'Z').map { it.toString() } + "#"
        assertEquals(27, alphabet.size)
        assertEquals("A", alphabet.first())
        assertEquals("#", alphabet.last())
        assertTrue(alphabet.contains("M"))
        assertTrue(alphabet.contains("Z"))
    }
}
