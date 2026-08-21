package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServiceManager
import com.pocketcasts.service.api.OnDemandTranscriptEnablement
import com.pocketcasts.service.api.OnDemandTranscriptOutcome
import com.pocketcasts.service.api.OnDemandTranscriptReason
import com.pocketcasts.service.api.OnDemandTranscriptResponse
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class OnDemandTranscriptRepositoryTest {
    private val syncManager = mock<SyncManager>()
    private val syncServiceManager = mock<SyncServiceManager>()
    private val podcastCacheServiceManager = mock<PodcastCacheServiceManager>()
    private val episodeManager = mock<EpisodeManager>()
    private val showNotesManager = mock<ShowNotesManager>()
    private val repository = OnDemandTranscriptRepositoryImpl(
        syncManager = syncManager,
        syncServiceManager = syncServiceManager,
        podcastCacheServiceManager = podcastCacheServiceManager,
        episodeManager = episodeManager,
        showNotesManager = showNotesManager,
    )

    @Test
    fun `episode access errors are not eligible`() {
        listOf(403, 404).forEach { statusCode ->
            assertEquals(
                OnDemandTranscriptRepository.Outcome.NotEligible,
                statusCode.toOnDemandTranscriptOutcome(),
            )
        }
    }

    @Test
    fun `authentication errors are transient`() {
        assertEquals(
            OnDemandTranscriptRepository.Outcome.TransientFailure,
            401.toOnDemandTranscriptOutcome(),
        )
    }

    @Test
    fun `malformed requests are unknown`() {
        assertEquals(
            OnDemandTranscriptRepository.Outcome.Unknown,
            400.toOnDemandTranscriptOutcome(),
        )
    }

    @Test
    fun `rate limiting is throttled`() {
        assertEquals(
            OnDemandTranscriptRepository.Outcome.Throttled,
            429.toOnDemandTranscriptOutcome(),
        )
    }

    @Test
    fun `server and proxy errors are transient`() {
        listOf(500, 502, 503, 504).forEach { statusCode ->
            assertEquals(
                OnDemandTranscriptRepository.Outcome.TransientFailure,
                statusCode.toOnDemandTranscriptOutcome(),
            )
        }
    }

    @Test
    fun `unexpected status is unknown`() {
        assertEquals(
            OnDemandTranscriptRepository.Outcome.Unknown,
            418.toOnDemandTranscriptOutcome(),
        )
    }

    @Test
    fun `request maps protobuf outcomes`() = runTest {
        val outcomes = mapOf(
            OnDemandTranscriptOutcome.QUEUED to OnDemandTranscriptRepository.Outcome.Queued,
            OnDemandTranscriptOutcome.IN_PROGRESS to OnDemandTranscriptRepository.Outcome.InProgress,
            OnDemandTranscriptOutcome.AVAILABLE to OnDemandTranscriptRepository.Outcome.Available,
            OnDemandTranscriptOutcome.NOT_ELIGIBLE to OnDemandTranscriptRepository.Outcome.NotEligible,
            OnDemandTranscriptOutcome.TRANSIENT_FAILURE to OnDemandTranscriptRepository.Outcome.TransientFailure,
            OnDemandTranscriptOutcome.THROTTLED to OnDemandTranscriptRepository.Outcome.Throttled,
            OnDemandTranscriptOutcome.OUTCOME_UNSPECIFIED to OnDemandTranscriptRepository.Outcome.Unknown,
            OnDemandTranscriptOutcome.UNRECOGNIZED to OnDemandTranscriptRepository.Outcome.Unknown,
        )

        outcomes.forEach { (protobufOutcome, domainOutcome) ->
            val outcomeValue = if (protobufOutcome == OnDemandTranscriptOutcome.UNRECOGNIZED) {
                Int.MAX_VALUE
            } else {
                protobufOutcome.number
            }
            val response = OnDemandTranscriptResponse.newBuilder()
                .setOutcomeValue(outcomeValue)
                .setReason(OnDemandTranscriptReason.REASON_UNSPECIFIED)
                .setEnablement(OnDemandTranscriptEnablement.ENABLEMENT_UNSPECIFIED)
                .setNewlyQueuedCount(2)
                .build()
            whenever(syncManager.getCacheTokenOrLogin<OnDemandTranscriptResponse>(any())).thenReturn(response)

            val result = repository.request(PODCAST_UUID, EPISODE_UUID)

            assertEquals(domainOutcome, result.outcome)
            assertEquals("reason_unspecified", result.reason)
            assertEquals("enablement_unspecified", result.enablement)
            assertEquals(2, result.newlyQueuedCount)
        }
    }

    @Test
    fun `request rethrows cancellation`() = runTest {
        whenever(syncManager.getCacheTokenOrLogin<OnDemandTranscriptResponse>(any()))
            .thenThrow(CancellationException("cancelled"))

        var wasCancelled = false
        try {
            repository.request(PODCAST_UUID, EPISODE_UUID)
        } catch (_: CancellationException) {
            wasCancelled = true
        }

        assertTrue(wasCancelled)
    }

    @Test
    fun `refresh updates generated transcript flag before refreshing show notes`() = runTest {
        val localEpisode = podcastEpisode(hasGeneratedTranscript = false)
        val remotePodcast = Podcast(uuid = PODCAST_UUID).apply {
            episodes += podcastEpisode(hasGeneratedTranscript = true)
        }
        whenever(podcastCacheServiceManager.getPodcastAndEpisode(PODCAST_UUID, EPISODE_UUID))
            .thenReturn(remotePodcast)
        whenever(episodeManager.findByUuid(EPISODE_UUID)).thenReturn(localEpisode)

        repository.refreshMetadata(PODCAST_UUID, EPISODE_UUID)

        inOrder(episodeManager, showNotesManager).apply {
            verify(episodeManager).updateHasGeneratedTranscript(EPISODE_UUID, true)
            verify(showNotesManager).refreshTranscriptMetadata(PODCAST_UUID, EPISODE_UUID)
        }
        verify(episodeManager, never()).update(any())
    }

    @Test
    fun `refresh skips writes and show notes while transcript is unavailable`() = runTest {
        val remotePodcast = Podcast(uuid = PODCAST_UUID).apply {
            episodes += podcastEpisode(hasGeneratedTranscript = false)
        }
        whenever(podcastCacheServiceManager.getPodcastAndEpisode(PODCAST_UUID, EPISODE_UUID))
            .thenReturn(remotePodcast)
        whenever(episodeManager.findByUuid(EPISODE_UUID))
            .thenReturn(podcastEpisode(hasGeneratedTranscript = false))

        repository.refreshMetadata(PODCAST_UUID, EPISODE_UUID)

        verify(episodeManager, never()).updateHasGeneratedTranscript(any(), any())
        verify(showNotesManager, never()).refreshTranscriptMetadata(any(), any())
    }

    @Test
    fun `refresh no-ops when remote episode is missing`() = runTest {
        whenever(podcastCacheServiceManager.getPodcastAndEpisode(PODCAST_UUID, EPISODE_UUID))
            .thenReturn(Podcast(uuid = PODCAST_UUID))

        repository.refreshMetadata(PODCAST_UUID, EPISODE_UUID)

        verify(episodeManager, never()).findByUuid(any())
        verify(showNotesManager, never()).refreshTranscriptMetadata(any(), any())
    }

    @Test
    fun `refresh no-ops when local episode is missing`() = runTest {
        val remotePodcast = Podcast(uuid = PODCAST_UUID).apply {
            episodes += podcastEpisode(hasGeneratedTranscript = true)
        }
        whenever(podcastCacheServiceManager.getPodcastAndEpisode(PODCAST_UUID, EPISODE_UUID))
            .thenReturn(remotePodcast)
        whenever(episodeManager.findByUuid(EPISODE_UUID)).thenReturn(null)

        repository.refreshMetadata(PODCAST_UUID, EPISODE_UUID)

        verify(episodeManager, never()).updateHasGeneratedTranscript(any(), any())
        verify(showNotesManager, never()).refreshTranscriptMetadata(any(), any())
    }

    private fun podcastEpisode(hasGeneratedTranscript: Boolean) = PodcastEpisode(
        uuid = EPISODE_UUID,
        publishedDate = Date(),
        podcastUuid = PODCAST_UUID,
        hasGeneratedTranscript = hasGeneratedTranscript,
    )

    private companion object {
        const val PODCAST_UUID = "podcast-uuid"
        const val EPISODE_UUID = "episode-uuid"
    }
}
