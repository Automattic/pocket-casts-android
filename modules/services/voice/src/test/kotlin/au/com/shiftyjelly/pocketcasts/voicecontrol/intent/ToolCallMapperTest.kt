package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ToolCallMapperTest {
    private val mapper = ToolCallMapper()

    @Test
    fun `no_match returns null`() {
        val result = mapper.map(ToolCall("no_match", "", emptyMap()))
        assertNull(result)
    }

    @Test
    fun `unknown tool returns null`() {
        val result = mapper.map(ToolCall("unknown_tool", "action", emptyMap()))
        assertNull(result)
    }

    @Test
    fun `playback pause`() {
        val result = mapper.map(ToolCall("playback", "pause", emptyMap()))
        assertEquals(VoiceIntent.Playback.Pause, result)
    }

    @Test
    fun `playback seek relative`() {
        val result = mapper.map(ToolCall("playback", "seek_relative", mapOf("seconds" to 30)))
        assertEquals(VoiceIntent.Playback.SeekRelative(30_000), result)
    }

    @Test
    fun `playback seek relative negative`() {
        val result = mapper.map(ToolCall("playback", "seek_relative", mapOf("seconds" to -15)))
        assertEquals(VoiceIntent.Playback.SeekRelative(-15_000), result)
    }

    @Test
    fun `playback seek to`() {
        val result = mapper.map(ToolCall("playback", "seek_to", mapOf("seconds" to 120)))
        assertEquals(VoiceIntent.Playback.SeekAbsolute(120_000), result)
    }

    @Test
    fun `playback seek relative without seconds returns null`() {
        val result = mapper.map(ToolCall("playback", "seek_relative", emptyMap()))
        assertNull(result)
    }

    @Test
    fun `effects set speed`() {
        val result = mapper.map(ToolCall("effects", "set_speed", mapOf("speed" to 2.0)))
        assertEquals(VoiceIntent.Effects.SetSpeed(2.0), result)
    }

    @Test
    fun `effects adjust speed`() {
        val result = mapper.map(ToolCall("effects", "adjust_speed", mapOf("delta" to 0.5)))
        assertEquals(VoiceIntent.Effects.AdjustSpeed(0.5), result)
    }

    @Test
    fun `effects set trim mode`() {
        val result = mapper.map(ToolCall("effects", "set_trim_mode", mapOf("mode" to "high")))
        assertEquals(VoiceIntent.Effects.SetTrimMode("high"), result)
    }

    @Test
    fun `effects set volume boost`() {
        val result = mapper.map(ToolCall("effects", "set_volume_boost", mapOf("enabled" to true)))
        assertEquals(VoiceIntent.Effects.SetVolumeBoost(true), result)
    }

    @Test
    fun `volume set volume`() {
        val result = mapper.map(ToolCall("volume", "set_volume", mapOf("volume" to 75)))
        assertEquals(VoiceIntent.Volume.SetVolume(75), result)
    }

    @Test
    fun `volume adjust volume`() {
        val result = mapper.map(ToolCall("volume", "adjust_volume", mapOf("delta" to -10)))
        assertEquals(VoiceIntent.Volume.AdjustVolume(-10), result)
    }

    @Test
    fun `sleep set`() {
        val result = mapper.map(ToolCall("sleep", "set", mapOf("minutes" to 30)))
        assertEquals(VoiceIntent.Sleep.Set(30), result)
    }

    @Test
    fun `sleep end of episode`() {
        val result = mapper.map(ToolCall("sleep", "end_of_episode", emptyMap()))
        assertEquals(VoiceIntent.Sleep.EndOfEpisode, result)
    }

    @Test
    fun `sleep cancel`() {
        val result = mapper.map(ToolCall("sleep", "cancel", emptyMap()))
        assertEquals(VoiceIntent.Sleep.Cancel, result)
    }

    @Test
    fun `chapter next`() {
        val result = mapper.map(ToolCall("chapter", "next", emptyMap()))
        assertEquals(VoiceIntent.Chapter.NextChapter, result)
    }

    @Test
    fun `chapter by title`() {
        val result = mapper.map(ToolCall("chapter", "by_title", mapOf("query" to "interview")))
        assertNotNull(result)
        result as VoiceIntent.Chapter.ByTitle
        assertEquals("interview", result.normalizedQuery)
    }

    @Test
    fun `bookmark add with title`() {
        val result = mapper.map(ToolCall("bookmark", "add", mapOf("title" to "highlight")))
        assertEquals(VoiceIntent.Bookmark.Add("highlight"), result)
    }

    @Test
    fun `bookmark add without title`() {
        val result = mapper.map(ToolCall("bookmark", "add", emptyMap()))
        assertEquals(VoiceIntent.Bookmark.Add(null), result)
    }

    @Test
    fun `playback query whats playing`() {
        val result = mapper.map(ToolCall("playback_query", "whats_playing", emptyMap()))
        assertEquals(VoiceIntent.PlaybackQuery.WhatsPlaying, result)
    }

    @Test
    fun `cloud route maps to CloudRoute intent`() {
        val result = mapper.map(
            ToolCall(
                "cloud_route",
                "route",
                mapOf("request" to "summarize this episode", "tier" to "premium"),
            ),
        )
        assertEquals(
            VoiceIntent.CloudRoute("summarize this episode", VoiceIntent.CloudTier.Premium),
            result,
        )
    }

    @Test
    fun `unknown tool name returns null`() {
        val result = mapper.map(ToolCall("unknown_tool", "do_something", emptyMap()))
        assertNull(result)
    }

    @Test
    fun `tool call parse from json`() {
        val json = """{"name": "playback", "action": "pause"}"""
        val call = ToolCall.parse(json)
        assertNotNull(call)
        assertEquals("playback", call!!.name)
        assertEquals("pause", call.action)
    }

    @Test
    fun `tool call parse with parameters`() {
        val json = """{"name": "effects", "action": "set_speed", "parameters": {"speed": 1.5}}"""
        val call = ToolCall.parse(json)
        assertNotNull(call)
        assertEquals("effects", call!!.name)
        assertEquals("set_speed", call.action)
        assertEquals(1.5, call.doubleParam("speed")!!, 0.001)
    }

    @Test
    fun `tool call parse no match`() {
        val json = """{"name": "no_match"}"""
        val call = ToolCall.parse(json)
        assertNotNull(call)
        assertEquals("no_match", call!!.name)
        assertNull(mapper.map(call))
    }

    @Test
    fun `tool call parse from fenced json`() {
        val response = """
            ```json
            {"name": "playback", "action": "pause"}
            ```
        """.trimIndent()

        val call = ToolCall.parse(response)

        assertNotNull(call)
        assertEquals("playback", call!!.name)
        assertEquals("pause", call.action)
    }

    @Test
    fun `tool call parse from response with text prefix`() {
        val response = """The matching call is {"name": "playback", "action": "resume"}."""

        val call = ToolCall.parse(response)

        assertNotNull(call)
        assertEquals("playback", call!!.name)
        assertEquals("resume", call.action)
    }

    @Test
    fun `tool call parse from function call sentinel`() {
        val response = """▎{"name": "volume", "action": "adjust_volume", "parameters": {"delta": 10}}"""

        val call = ToolCall.parse(response)

        assertNotNull(call)
        assertEquals("volume", call!!.name)
        assertEquals("adjust_volume", call.action)
        assertEquals(10, call.intParam("delta"))
    }

    @Test
    fun `tool call parse from legacy escaped function-call output`() {
        val response =
            """
            <start_function_call>call:sleep{action:<escape>set</escape>,minutes:30}<end_function_call>
            """.trimIndent()

        val call = ToolCall.parse(response)

        assertNotNull(call)
        assertEquals("sleep", call!!.name)
        assertEquals("set", call.action)
        assertEquals(30, call.intParam("minutes"))
    }
}
