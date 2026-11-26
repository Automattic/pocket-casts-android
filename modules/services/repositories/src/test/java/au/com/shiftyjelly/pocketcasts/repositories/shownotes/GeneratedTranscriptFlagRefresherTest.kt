package au.com.shiftyjelly.pocketcasts.repositories.shownotes

import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.servers.podcast.EpisodeInfo
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastInfo
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastResponse
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import java.time.Instant
import java.util.Date
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

class GeneratedTranscriptFlagRefresherTest {
    private val clock = MutableClock(Instant.parse("2026-11-03T10:00:00Z"))
    private val cacheServiceManager = mock<PodcastCacheServiceManager>()
    private val episodeDao = mock<EpisodeDao>()

    private val refresher = GeneratedTranscriptFlagRefresher(
        cacheServiceManager = cacheServiceManager,
        episodeDao = episodeDao,
        clock = clock,
    )

    @Test
    fun `return true without fetching when the flag is already set`() = runTest {
        val episode = createEpisode(hasGeneratedTranscript = true)

        assertTrue(refresher.hasGeneratedTranscript(episode))

        verify(cacheServiceManager, never()).getPodcastResponse(any())
    }

    @Test
    fun `return false without fetching for episodes older than 60 days`() = runTest {
        val episode = createEpisode(publishedDate = Date.from(clock.instant().minusSeconds(61.days.inWholeSeconds)))

        assertFalse(refresher.hasGeneratedTranscript(episode))

        verify(cacheServiceManager, never()).getPodcastResponse(any())
    }

    @Test
    fun `mark episodes the server reports as having a generated transcript`() = runTest {
        val episode = createEpisode()
        whenever(cacheServiceManager.getPodcastResponse("podcast")) doReturn createResponse("episode" to true, "other" to false, "unknown" to null)
        whenever(episodeDao.findByUuid("episode")) doReturn episode.copy(hasGeneratedTranscript = true)

        assertTrue(refresher.hasGeneratedTranscript(episode))

        verify(episodeDao).markHasGeneratedTranscript(listOf("episode"))
    }

    @Test
    fun `return false when the server reports no generated transcript`() = runTest {
        val episode = createEpisode()
        whenever(cacheServiceManager.getPodcastResponse("podcast")) doReturn createResponse("episode" to false)
        whenever(episodeDao.findByUuid("episode")) doReturn episode

        assertFalse(refresher.hasGeneratedTranscript(episode))
    }

    @Test
    fun `fetch each podcast at most once per 30 minutes`() = runTest {
        val episode = createEpisode()
        whenever(cacheServiceManager.getPodcastResponse("podcast")) doReturn createResponse("episode" to false)
        whenever(episodeDao.findByUuid("episode")) doReturn episode

        refresher.hasGeneratedTranscript(episode)
        clock += 29.minutes
        refresher.hasGeneratedTranscript(episode)
        verify(cacheServiceManager, times(1)).getPodcastResponse("podcast")

        clock += 2.minutes
        refresher.hasGeneratedTranscript(episode)
        verify(cacheServiceManager, times(2)).getPodcastResponse("podcast")
    }

    @Test
    fun `throttle podcasts independently`() = runTest {
        val episode = createEpisode()
        val otherEpisode = createEpisode(uuid = "other", podcastUuid = "other_podcast")
        whenever(cacheServiceManager.getPodcastResponse(any())) doReturn createResponse()

        refresher.hasGeneratedTranscript(episode)
        refresher.hasGeneratedTranscript(otherEpisode)

        verify(cacheServiceManager).getPodcastResponse("podcast")
        verify(cacheServiceManager).getPodcastResponse("other_podcast")
    }

    @Test
    fun `return false when the fetch fails`() = runTest {
        val episode = createEpisode()
        whenever(cacheServiceManager.getPodcastResponse("podcast")) doThrow RuntimeException("Network error")
        whenever(episodeDao.findByUuid("episode")) doReturn episode

        assertFalse(refresher.hasGeneratedTranscript(episode))

        verify(episodeDao, never()).markHasGeneratedTranscript(any())
    }

    @Test
    fun `retry right away after a failed fetch`() = runTest {
        val episode = createEpisode()
        whenever(cacheServiceManager.getPodcastResponse("podcast")) doThrow RuntimeException("Network error")
        whenever(episodeDao.findByUuid("episode")) doReturn episode

        refresher.hasGeneratedTranscript(episode)
        refresher.hasGeneratedTranscript(episode)

        verify(cacheServiceManager, times(2)).getPodcastResponse("podcast")
    }

    @Test
    fun `retry right away after an HTTP error`() = runTest {
        val episode = createEpisode()
        whenever(cacheServiceManager.getPodcastResponse("podcast")) doReturn Response.error(503, "".toResponseBody())
        whenever(episodeDao.findByUuid("episode")) doReturn episode

        refresher.hasGeneratedTranscript(episode)
        refresher.hasGeneratedTranscript(episode)

        verify(cacheServiceManager, times(2)).getPodcastResponse("podcast")
        verify(episodeDao, never()).markHasGeneratedTranscript(any())
    }

    private fun createEpisode(
        uuid: String = "episode",
        podcastUuid: String = "podcast",
        hasGeneratedTranscript: Boolean = false,
        publishedDate: Date = Date.from(clock.instant().minusSeconds(1.days.inWholeSeconds)),
    ) = PodcastEpisode(
        uuid = uuid,
        podcastUuid = podcastUuid,
        publishedDate = publishedDate,
        hasGeneratedTranscript = hasGeneratedTranscript,
    )

    private fun createResponse(vararg episodes: Pair<String, Boolean?>) = Response.success(
        PodcastResponse(
            episodeFrequency = "",
            estimatedNextEpisodeAt = null,
            episodeCount = episodes.size,
            hasMoreEpisodes = false,
            hasSeasons = false,
            seasonCount = 0,
            refreshAllowed = true,
            podcastInfo = PodcastInfo(
                uuid = "podcast",
                url = null,
                title = null,
                author = null,
                description = null,
                descriptionHtml = null,
                showType = null,
                category = null,
                audio = true,
                episodes = episodes.map { (uuid, hasGeneratedTranscript) ->
                    EpisodeInfo(
                        uuid = uuid,
                        title = null,
                        season = null,
                        number = null,
                        type = null,
                        url = "",
                        alternateEnclosures = null,
                        fileType = null,
                        fileSize = null,
                        duration = null,
                        published = "2026-11-02T10:00:00Z",
                        slug = null,
                        hasGeneratedTranscript = hasGeneratedTranscript,
                    )
                },
                isPrivate = false,
                fundings = null,
                slug = null,
                explicit = false,
                webFeed = false,
                networkList = null,
            ),
        ),
    )
}
