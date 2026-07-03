package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceIntentTest {
    @Test
    fun `seek relative stores milliseconds`() {
        val intent = VoiceIntent.Playback.SeekRelative(deltaMs = 30_000)

        assertEquals(30_000, intent.deltaMs)
    }

    @Test
    fun `chapter title trims query`() {
        val intent = VoiceIntent.Chapter.ByTitle(query = "  interview  ")

        assertEquals("interview", intent.normalizedQuery)
    }

    @Test
    fun `playback pause is a Playback intent`() {
        val intent: VoiceIntent = VoiceIntent.Playback.Pause

        assert(intent is VoiceIntent.Playback)
    }

    @Test
    fun `sleep set stores minutes`() {
        val intent = VoiceIntent.Sleep.Set(minutes = 30)

        assertEquals(30, intent.minutes)
    }

    @Test
    fun `bookmark add accepts null title`() {
        val intent = VoiceIntent.Bookmark.Add(title = null)

        assertEquals(null, intent.title)
    }
}
