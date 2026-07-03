package au.com.shiftyjelly.pocketcasts.voicecontrol.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

class SpokenTemplateResolverTest {
    private lateinit var resolver: SpokenTemplateResolver

    @Before
    fun setUp() {
        val templates = mapOf(
            "effects.set_speed" to "{speed}x speed",
            "effects.adjust_speed" to "{speed}x",
            "effects.set_trim_mode" to "Silence trim {mode}",
            "effects.set_volume_boost" to "Volume boost {on/off}",
            "effects.query" to "Speed {speed}x, trim {mode}, volume boost {on/off}",
            "volume.query" to "Volume {volume} percent",
            "sleep.set" to "Sleep timer set for {minutes} minutes",
            "sleep.end_of_episode" to "Sleep timer set for end of episode",
            "sleep.end_of_chapter" to "Sleep timer set for end of chapter",
            "sleep.add_time" to "{total} minutes remaining",
            "sleep.cancel" to "Sleep timer cancelled",
            "sleep.query" to "Sleep timer {remaining}",
            "bookmark.add" to "Bookmarked at {position}",
            "playback.next_episode" to "Playing {episode title}",
        )
        resolver = SpokenTemplateResolver(templates)
    }

    @Test
    fun `resolve set_speed template`() {
        val text = resolver.resolve("effects.set_speed", mapOf("speed" to "1.5"))
        assertEquals("1.5x speed", text)
    }

    @Test
    fun `resolve sleep set template`() {
        val text = resolver.resolve("sleep.set", mapOf("minutes" to "30"))
        assertEquals("Sleep timer set for 30 minutes", text)
    }

    @Test
    fun `resolve sleep cancel template`() {
        val text = resolver.resolve("sleep.cancel", emptyMap())
        assertEquals("Sleep timer cancelled", text)
    }

    @Test
    fun `missing template key returns empty string`() {
        val text = resolver.resolve("nonexistent.key", emptyMap())
        assertEquals("", text)
    }

    @Test
    fun `all template keys are defined for English`() {
        val requiredKeys = listOf(
            "effects.set_speed", "effects.adjust_speed", "effects.set_trim_mode",
            "effects.set_volume_boost", "effects.query",
            "volume.query",
            "sleep.set", "sleep.end_of_episode", "sleep.end_of_chapter",
            "sleep.add_time", "sleep.cancel", "sleep.query",
            "bookmark.add", "playback.next_episode",
        )
        requiredKeys.forEach { key ->
            val text = resolver.resolve(
                key,
                mapOf(
                    "speed" to "1",
                    "minutes" to "1",
                    "mode" to "off",
                    "position" to "0:00",
                    "total" to "1",
                    "remaining" to "1 minute",
                    "volume" to "50",
                ),
            )
            assertNotEquals("Template $key should not be empty", "", text)
        }
    }
}
