package au.com.shiftyjelly.pocketcasts.repositories.sync

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.PodcastDao
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.android.tracks.crashlogging.CrashLogging
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class PodcastRefresherImplTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    lateinit var episodeManager: EpisodeManager

    @Mock
    lateinit var appDatabase: AppDatabase

    @Mock
    lateinit var podcastDao: PodcastDao

    @Mock
    lateinit var cacheServiceManager: PodcastCacheServiceManager

    @Mock
    lateinit var crashLogging: CrashLogging

    private lateinit var podcastRefresher: PodcastRefresherImpl

    @Before
    fun setUp() {
        whenever(appDatabase.podcastDao()).thenReturn(podcastDao)
        podcastRefresher = PodcastRefresherImpl(
            episodeManager = episodeManager,
            appDatabase = appDatabase,
            cacheServiceManager = cacheServiceManager,
            crashLogging = crashLogging,
        )
    }

    @Test
    fun `updatePodcastIfRequired does not update when no field changes`() = runTest {
        val podcast = createPodcast()
        val updatedPodcast = podcast.copy()

        podcastRefresher.updatePodcastIfRequired(podcast, updatedPodcast)

        verify(podcastDao, never()).updateRefresh(
            uuid = podcast.uuid,
            title = podcast.title,
            author = podcast.author,
            podcastCategory = podcast.podcastCategory,
            podcastDescription = podcast.podcastDescription,
            estimatedNextEpisode = podcast.estimatedNextEpisode,
            episodeFrequency = podcast.episodeFrequency,
            refreshAvailable = podcast.refreshAvailable,
            fundingUrl = podcast.fundingUrl,
        )
    }

    @Test
    fun `updatePodcastIfRequired updates when the title changes`() = runTest {
        val existingPodcast = createPodcast(title = "Old Title")
        val updatedPodcast = existingPodcast.copy(title = "New Title")

        podcastRefresher.updatePodcastIfRequired(existingPodcast, updatedPodcast)

        verify(podcastDao).updateRefresh(
            uuid = existingPodcast.uuid,
            title = "New Title",
            author = existingPodcast.author,
            podcastCategory = existingPodcast.podcastCategory,
            podcastDescription = existingPodcast.podcastDescription,
            estimatedNextEpisode = existingPodcast.estimatedNextEpisode,
            episodeFrequency = existingPodcast.episodeFrequency,
            refreshAvailable = existingPodcast.refreshAvailable,
            fundingUrl = existingPodcast.fundingUrl,
        )
    }

    @Test
    fun `updatePodcastIfRequired updates when the author changes`() = runTest {
        val existingPodcast = createPodcast(author = "Old Author")
        val updatedPodcast = existingPodcast.copy(author = "New Author")

        podcastRefresher.updatePodcastIfRequired(existingPodcast, updatedPodcast)

        verify(podcastDao).updateRefresh(
            uuid = existingPodcast.uuid,
            title = existingPodcast.title,
            author = "New Author",
            podcastCategory = existingPodcast.podcastCategory,
            podcastDescription = existingPodcast.podcastDescription,
            estimatedNextEpisode = existingPodcast.estimatedNextEpisode,
            episodeFrequency = existingPodcast.episodeFrequency,
            refreshAvailable = existingPodcast.refreshAvailable,
            fundingUrl = existingPodcast.fundingUrl,
        )
    }

    @Test
    fun `updatePodcastIfRequired updates when the category changes`() = runTest {
        val existingPodcast = createPodcast(podcastCategory = "Technology")
        val updatedPodcast = existingPodcast.copy(podcastCategory = "News")

        podcastRefresher.updatePodcastIfRequired(existingPodcast, updatedPodcast)

        verify(podcastDao).updateRefresh(
            uuid = existingPodcast.uuid,
            title = existingPodcast.title,
            author = existingPodcast.author,
            podcastCategory = "News",
            podcastDescription = existingPodcast.podcastDescription,
            estimatedNextEpisode = existingPodcast.estimatedNextEpisode,
            episodeFrequency = existingPodcast.episodeFrequency,
            refreshAvailable = existingPodcast.refreshAvailable,
            fundingUrl = existingPodcast.fundingUrl,
        )
    }

    @Test
    fun `updatePodcastIfRequired updates when the description changes`() = runTest {
        val existingPodcast = createPodcast(podcastDescription = "Old description")
        val updatedPodcast = existingPodcast.copy(podcastDescription = "New description")

        podcastRefresher.updatePodcastIfRequired(existingPodcast, updatedPodcast)

        verify(podcastDao).updateRefresh(
            uuid = existingPodcast.uuid,
            title = existingPodcast.title,
            author = existingPodcast.author,
            podcastCategory = existingPodcast.podcastCategory,
            podcastDescription = "New description",
            estimatedNextEpisode = existingPodcast.estimatedNextEpisode,
            episodeFrequency = existingPodcast.episodeFrequency,
            refreshAvailable = existingPodcast.refreshAvailable,
            fundingUrl = existingPodcast.fundingUrl,
        )
    }

    @Test
    fun `updatePodcastIfRequired updates when the estimatedNextEpisode changes`() = runTest {
        val oldDate = Date(System.currentTimeMillis())
        val newDate = Date(System.currentTimeMillis() + 86400000) // 1 day later
        val existingPodcast = createPodcast(estimatedNextEpisode = oldDate)
        val updatedPodcast = existingPodcast.copy(estimatedNextEpisode = newDate)

        podcastRefresher.updatePodcastIfRequired(existingPodcast, updatedPodcast)

        verify(podcastDao).updateRefresh(
            uuid = existingPodcast.uuid,
            title = existingPodcast.title,
            author = existingPodcast.author,
            podcastCategory = existingPodcast.podcastCategory,
            podcastDescription = existingPodcast.podcastDescription,
            estimatedNextEpisode = newDate,
            episodeFrequency = existingPodcast.episodeFrequency,
            refreshAvailable = existingPodcast.refreshAvailable,
            fundingUrl = existingPodcast.fundingUrl,
        )
    }

    @Test
    fun `updatePodcastIfRequired updates when the episodeFrequency changes`() = runTest {
        val existingPodcast = createPodcast(episodeFrequency = "weekly")
        val updatedPodcast = existingPodcast.copy(episodeFrequency = "daily")

        podcastRefresher.updatePodcastIfRequired(existingPodcast, updatedPodcast)

        verify(podcastDao).updateRefresh(
            uuid = existingPodcast.uuid,
            title = existingPodcast.title,
            author = existingPodcast.author,
            podcastCategory = existingPodcast.podcastCategory,
            podcastDescription = existingPodcast.podcastDescription,
            estimatedNextEpisode = existingPodcast.estimatedNextEpisode,
            episodeFrequency = "daily",
            refreshAvailable = existingPodcast.refreshAvailable,
            fundingUrl = existingPodcast.fundingUrl,
        )
    }

    @Test
    fun `updatePodcastIfRequired updates when the refreshAvailable changes`() = runTest {
        val existingPodcast = createPodcast(refreshAvailable = false)
        val updatedPodcast = existingPodcast.copy(refreshAvailable = true)

        podcastRefresher.updatePodcastIfRequired(existingPodcast, updatedPodcast)

        verify(podcastDao).updateRefresh(
            uuid = existingPodcast.uuid,
            title = existingPodcast.title,
            author = existingPodcast.author,
            podcastCategory = existingPodcast.podcastCategory,
            podcastDescription = existingPodcast.podcastDescription,
            estimatedNextEpisode = existingPodcast.estimatedNextEpisode,
            episodeFrequency = existingPodcast.episodeFrequency,
            refreshAvailable = true,
            fundingUrl = existingPodcast.fundingUrl,
        )
    }

    @Test
    fun `updatePodcastIfRequired updates when the fundingUrl changes`() = runTest {
        val existingPodcast = createPodcast(fundingUrl = "https://old.com")
        val updatedPodcast = existingPodcast.copy(fundingUrl = "https://new.com")

        podcastRefresher.updatePodcastIfRequired(existingPodcast, updatedPodcast)

        verify(podcastDao).updateRefresh(
            uuid = existingPodcast.uuid,
            title = existingPodcast.title,
            author = existingPodcast.author,
            podcastCategory = existingPodcast.podcastCategory,
            podcastDescription = existingPodcast.podcastDescription,
            estimatedNextEpisode = existingPodcast.estimatedNextEpisode,
            episodeFrequency = existingPodcast.episodeFrequency,
            refreshAvailable = existingPodcast.refreshAvailable,
            fundingUrl = "https://new.com",
        )
    }

    @Test
    fun `updatePodcastIfRequired updates when multiple fields change`() = runTest {
        val existingPodcast = createPodcast(
            title = "Old Title",
            author = "Old Author",
            podcastCategory = "Technology",
            refreshAvailable = false,
        )
        val updatedPodcast = existingPodcast.copy(
            title = "New Title",
            author = "New Author",
            podcastCategory = "News",
            refreshAvailable = true,
        )

        podcastRefresher.updatePodcastIfRequired(existingPodcast, updatedPodcast)

        verify(podcastDao).updateRefresh(
            uuid = existingPodcast.uuid,
            title = "New Title",
            author = "New Author",
            podcastCategory = "News",
            podcastDescription = existingPodcast.podcastDescription,
            estimatedNextEpisode = existingPodcast.estimatedNextEpisode,
            episodeFrequency = existingPodcast.episodeFrequency,
            refreshAvailable = true,
            fundingUrl = existingPodcast.fundingUrl,
        )
    }

    @Test
    fun `updatePodcastIfRequired does not update when only non-tracked fields change`() = runTest {
        val existingPodcast = createPodcast()
        val updatedPodcast = existingPodcast.copy(
            thumbnailUrl = "https://new-thumbnail.com",
            isSubscribed = !existingPodcast.isSubscribed,
            playbackSpeed = 1.5,
        )

        podcastRefresher.updatePodcastIfRequired(existingPodcast, updatedPodcast)

        verify(podcastDao, never()).updateRefresh(
            uuid = existingPodcast.uuid,
            title = existingPodcast.title,
            author = existingPodcast.author,
            podcastCategory = existingPodcast.podcastCategory,
            podcastDescription = existingPodcast.podcastDescription,
            estimatedNextEpisode = existingPodcast.estimatedNextEpisode,
            episodeFrequency = existingPodcast.episodeFrequency,
            refreshAvailable = existingPodcast.refreshAvailable,
            fundingUrl = existingPodcast.fundingUrl,
        )
    }

    private fun createPodcast(
        uuid: String = "test-uuid",
        title: String = "Test Podcast",
        author: String = "Test Author",
        podcastCategory: String = "Technology",
        podcastDescription: String = "Test Description",
        estimatedNextEpisode: Date? = null,
        episodeFrequency: String? = "weekly",
        refreshAvailable: Boolean = false,
        fundingUrl: String? = null,
    ) = Podcast(
        uuid = uuid,
        title = title,
        author = author,
        podcastCategory = podcastCategory,
        podcastDescription = podcastDescription,
        estimatedNextEpisode = estimatedNextEpisode,
        episodeFrequency = episodeFrequency,
        refreshAvailable = refreshAvailable,
        fundingUrl = fundingUrl,
    )
}
