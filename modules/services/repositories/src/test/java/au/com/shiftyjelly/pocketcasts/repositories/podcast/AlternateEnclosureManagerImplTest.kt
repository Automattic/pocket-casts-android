package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.dao.AlternateEnclosureDao
import au.com.shiftyjelly.pocketcasts.models.entity.AlternateEnclosureSource
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class AlternateEnclosureManagerImplTest {
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
    fun `hasHlsAlternateEnclosure queries the dao with the HLS mime types`() = runBlocking {
        whenever(alternateEnclosureDao.hasHlsEnclosure("episode-uuid", BaseEpisode.HLS_MIME_TYPES)).thenReturn(flowOf(true))

        assertTrue(manager.hasHlsAlternateEnclosure("episode-uuid").first())
    }

    private fun enclosure(position: Int) = EpisodeAlternateEnclosure(
        episodeUuid = "episode-uuid",
        position = position,
        type = "application/x-mpegURL",
        sources = listOf(AlternateEnclosureSource(uri = "https://example.com/master.m3u8")),
    )
}
