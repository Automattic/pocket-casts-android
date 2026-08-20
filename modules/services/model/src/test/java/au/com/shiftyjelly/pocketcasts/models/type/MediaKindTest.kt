package au.com.shiftyjelly.pocketcasts.models.type

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaKindTest {
    @Test
    fun `maps every kind the server defines`() {
        assertEquals(MediaKind.Video, MediaKind.fromServer("video"))
        assertEquals(MediaKind.Audio, MediaKind.fromServer("audio"))
        assertEquals(MediaKind.YouTube, MediaKind.fromServer("youtube"))
        assertEquals(MediaKind.Vimeo, MediaKind.fromServer("vimeo"))
        assertEquals(MediaKind.Other, MediaKind.fromServer("other"))
    }

    @Test
    fun `an absent kind stays absent rather than becoming other`() {
        assertNull(MediaKind.fromServer(null))
    }

    @Test
    fun `an unrecognised kind keeps its raw value`() {
        assertEquals(MediaKind.Unknown("hologram"), MediaKind.fromServer("hologram"))
        assertEquals("hologram", MediaKind.fromServer("hologram")?.stringValue)
    }

    @Test
    fun `every kind round trips through its string value`() {
        val kinds = listOf(MediaKind.Video, MediaKind.Audio, MediaKind.YouTube, MediaKind.Vimeo, MediaKind.Other, MediaKind.Unknown("hologram"))

        assertEquals(kinds, kinds.map { MediaKind.fromServer(it.stringValue) })
    }

    @Test
    fun `kinds are matched case sensitively as the server sends them lowercase`() {
        assertEquals(MediaKind.Unknown("Video"), MediaKind.fromServer("Video"))
    }
}
