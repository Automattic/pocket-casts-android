package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.dao.AlternateEnclosureDao
import au.com.shiftyjelly.pocketcasts.models.entity.AlternateEnclosureSource
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import au.com.shiftyjelly.pocketcasts.models.type.MediaKind
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AlternateEnclosureManagerImplTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val alternateEnclosureDao = mock<AlternateEnclosureDao>()
    private val manager = AlternateEnclosureManagerImpl(alternateEnclosureDao)

    @Test
    fun `findForEpisode returns the dao enclosures`() = runBlocking {
        val enclosures = listOf(enclosure(position = 0), enclosure(position = 1))
        whenever(alternateEnclosureDao.findByEpisodeUuid("episode-uuid")).thenReturn(enclosures)

        assertEquals(enclosures, manager.findForEpisode("episode-uuid"))
    }

    @Test
    fun `findForEpisode returns empty when the episode has no enclosures`() = runBlocking {
        whenever(alternateEnclosureDao.findByEpisodeUuid("episode-uuid")).thenReturn(emptyList())

        assertEquals(emptyList<EpisodeAlternateEnclosure>(), manager.findForEpisode("episode-uuid"))
    }

    @Test
    fun `an hls enclosure counts as video`() = runBlocking {
        FeatureFlag.setEnabled(Feature.HLS_STREAMING, true)

        assertTrue(hasVideoEnclosure(listOf(enclosure(position = 0))))
    }

    @Test
    fun `a video enclosure counts as video when its flag is on`() = runBlocking {
        FeatureFlag.setEnabled(Feature.VIDEO_ALTERNATE_ENCLOSURES, true)

        assertTrue(hasVideoEnclosure(listOf(videoEnclosure("https://example.com/episode.mp4"))))
    }

    @Test
    fun `a video enclosure the flag hides from playback does not earn the icon`() = runBlocking {
        FeatureFlag.setEnabled(Feature.VIDEO_ALTERNATE_ENCLOSURES, false)

        assertFalse(hasVideoEnclosure(listOf(videoEnclosure("https://example.com/episode.mp4"))))
    }

    @Test
    fun `a video enclosure with no playable source does not earn the icon`() = runBlocking {
        FeatureFlag.setEnabled(Feature.VIDEO_ALTERNATE_ENCLOSURES, true)

        assertFalse(hasVideoEnclosure(listOf(videoEnclosure("ipfs://QmEpisode"))))
    }

    @Test
    fun `an audio enclosure does not earn the icon`() = runBlocking {
        FeatureFlag.setEnabled(Feature.HLS_STREAMING, true)
        FeatureFlag.setEnabled(Feature.VIDEO_ALTERNATE_ENCLOSURES, true)

        val audio = enclosure(position = 0).copy(type = "audio/mpeg", mediaKind = MediaKind.Audio)
        assertFalse(hasVideoEnclosure(listOf(audio)))
    }

    private suspend fun hasVideoEnclosure(enclosures: List<EpisodeAlternateEnclosure>): Boolean {
        whenever(alternateEnclosureDao.observeByEpisodeUuid("episode-uuid")).thenReturn(flowOf(enclosures))
        return manager.hasVideoAlternateEnclosure("episode-uuid").first()
    }

    private fun enclosure(position: Int) = EpisodeAlternateEnclosure(
        episodeUuid = "episode-uuid",
        position = position,
        type = "application/x-mpegURL",
        sources = listOf(AlternateEnclosureSource(uri = "https://example.com/master.m3u8")),
    )

    private fun videoEnclosure(uri: String) = EpisodeAlternateEnclosure(
        episodeUuid = "episode-uuid",
        position = 0,
        type = "video/mp4",
        mediaKind = MediaKind.Video,
        sources = listOf(AlternateEnclosureSource(uri = uri)),
    )
}
