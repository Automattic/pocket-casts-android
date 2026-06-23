package au.com.shiftyjelly.pocketcasts.models.converter

import au.com.shiftyjelly.pocketcasts.models.entity.AlternateEnclosureSource
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Test

class AlternateEnclosureSourcesConverterTest {
    private val converter = AlternateEnclosureSourcesConverter(Moshi.Builder().build())

    @Test
    fun `round trips sources`() {
        val sources = listOf(
            AlternateEnclosureSource(uri = "https://example.com/master.m3u8", contentType = "application/x-mpegURL"),
            AlternateEnclosureSource(uri = "ipfs://QmManifest", contentType = null),
        )

        val restored = converter.toSources(converter.toJsonString(sources))

        assertEquals(sources, restored)
    }

    @Test
    fun `round trips empty list`() {
        assertEquals(emptyList<AlternateEnclosureSource>(), converter.toSources(converter.toJsonString(emptyList())))
    }

    @Test
    fun `null string maps to empty list`() {
        assertEquals(emptyList<AlternateEnclosureSource>(), converter.toSources(null))
    }
}
