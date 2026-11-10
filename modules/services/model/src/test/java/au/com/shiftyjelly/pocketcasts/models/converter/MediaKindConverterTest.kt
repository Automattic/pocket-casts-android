package au.com.shiftyjelly.pocketcasts.models.converter

import au.com.shiftyjelly.pocketcasts.models.type.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaKindConverterTest {
    private val converter = MediaKindConverter()

    private val allKinds = listOf(MediaKind.Video, MediaKind.Audio, MediaKind.YouTube, MediaKind.Vimeo, MediaKind.Other)

    @Test
    fun `round trips every kind the server defines`() {
        val restored = allKinds.map { converter.toMediaKind(converter.toString(it)) }

        assertEquals(allKinds, restored)
    }

    @Test
    fun `each kind is stored as a different value`() {
        val values = allKinds.map(converter::toString).distinct()

        assertEquals(allKinds.size, values.size)
    }

    @Test
    fun `stores kinds as the value the server sends`() {
        assertEquals("video", converter.toString(MediaKind.Video))
        assertEquals("audio", converter.toString(MediaKind.Audio))
        assertEquals("youtube", converter.toString(MediaKind.YouTube))
        assertEquals("vimeo", converter.toString(MediaKind.Vimeo))
        assertEquals("other", converter.toString(MediaKind.Other))
    }

    @Test
    fun `round trips an unrecognised kind so a later release can still read it`() {
        val unknown = MediaKind.Unknown("hologram")

        assertEquals("hologram", converter.toString(unknown))
        assertEquals(unknown, converter.toMediaKind("hologram"))
    }

    @Test
    fun `null column value stays null rather than becoming other`() {
        assertNull(converter.toMediaKind(null))
        assertNull(converter.toString(null))
    }

    @Test
    fun `an empty column value is kept rather than treated as absent`() {
        assertEquals(MediaKind.Unknown(""), converter.toMediaKind(""))
    }
}
