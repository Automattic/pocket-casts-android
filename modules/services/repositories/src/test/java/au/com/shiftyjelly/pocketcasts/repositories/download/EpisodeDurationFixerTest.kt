package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import java.io.File
import java.util.Date
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class EpisodeDurationFixerTest {
    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var mediaDurationExtractor: MediaDurationExtractor

    private val downloadFile = File("episode.mp3")

    private lateinit var fixer: EpisodeDurationFixer

    @Before
    fun setUp() {
        fixer = EpisodeDurationFixer(episodeManager, mediaDurationExtractor)
    }

    @Test
    fun `skips extraction when episode duration is already populated`() {
        val episode = PodcastEpisode(uuid = "abc", duration = 300.0, publishedDate = Date())

        fixer.fixMissingDuration(episode, downloadFile)

        verifyNoInteractions(mediaDurationExtractor)
        verify(episodeManager, never()).updateDurationBlocking(any(), any(), any())
    }

    @Test
    fun `skips update when extractor returns null`() {
        val episode = PodcastEpisode(uuid = "abc", duration = 0.0, publishedDate = Date())
        whenever(mediaDurationExtractor.extractDurationInSeconds(downloadFile)).thenReturn(null)

        fixer.fixMissingDuration(episode, downloadFile)

        verify(episodeManager, never()).updateDurationBlocking(any(), any(), any())
    }

    @Test
    fun `updates duration when extractor returns valid value`() {
        val episode = PodcastEpisode(uuid = "abc", duration = 0.0, publishedDate = Date())
        whenever(mediaDurationExtractor.extractDurationInSeconds(downloadFile)).thenReturn(123.45)

        fixer.fixMissingDuration(episode, downloadFile)

        verify(episodeManager).updateDurationBlocking(eq(episode), eq(123.45), eq(true))
    }

    @Test
    fun `updates duration for user episodes too`() {
        val episode = UserEpisode(uuid = "user-abc", duration = 0.0, publishedDate = Date())
        whenever(mediaDurationExtractor.extractDurationInSeconds(downloadFile)).thenReturn(99.0)

        fixer.fixMissingDuration(episode, downloadFile)

        verify(episodeManager).updateDurationBlocking(eq(episode), eq(99.0), eq(true))
    }
}
