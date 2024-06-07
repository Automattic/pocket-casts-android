package au.com.shiftyjelly.pocketcasts.preferences.model

import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkConfigurationTest {
    @Test
    fun `all elements are enabled by default`() {
        val config = ArtworkConfiguration(useEpisodeArtwork = true)

        val value = Element.entries.map(config::useEpisodeArtwork)

        assertEquals(true, value.all { it })
    }

    @Test
    fun `elements can be initialized as disabled`() {
        val config = ArtworkConfiguration(
            useEpisodeArtwork = true,
            enabledElements = emptySet(),
        )

        val value = Element.entries.map(config::useEpisodeArtwork)

        assertEquals(true, value.none { it })
    }

    @Test
    fun `elements can be disabled`() {
        val config = ArtworkConfiguration(useEpisodeArtwork = true).disable(Element.Filters)

        val value = config.useEpisodeArtwork(Element.Filters)

        assertEquals(false, value)
    }

    @Test
    fun `elements can be enabled`() {
        val config = ArtworkConfiguration(
            useEpisodeArtwork = true,
            enabledElements = emptySet(),
        ).enable(Element.Starred)

        val value = config.useEpisodeArtwork(Element.Starred)

        assertEquals(true, value)
    }

    @Test
    fun `elements are enabled only if global property is enabled`() {
        val config = ArtworkConfiguration(
            useEpisodeArtwork = false,
            enabledElements = emptySet(),
        ).enable(Element.Starred)

        val value = config.useEpisodeArtwork(Element.Starred)
        assertEquals(false, value)
    }
}
