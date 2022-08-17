package au.com.shiftyjelly.pocketcasts

import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.repositories.playback.PODCASTS_ROOT
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackService
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoMediaId
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import com.nhaarman.mockitokotlin2.any
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeoutException

@RunWith(AndroidJUnit4::class)
class AutoPlaybackServiceTest {
    @get:Rule
    val serviceRule = ServiceTestRule()

    lateinit var service: AutoPlaybackService

    @Before
    fun setup() {
        val serviceIntent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            AutoPlaybackService::class.java
        )

        val binder: IBinder = serviceRule.bindService(serviceIntent)
        service = (binder as PlaybackService.LocalBinder).service as AutoPlaybackService
    }

    @Test
    @Throws(TimeoutException::class)
    fun testReturnsCorrectTabs() {
        val children = service.loadRootChildren()
        assertTrue("There are 3 tabs", children.size == 3)
        assertTrue("The first tab should be podcasts", children[0].mediaId == PODCASTS_ROOT)
        assertTrue("The second should be episode filters", children[1].mediaId == FILTERS_ROOT)
        assertTrue("The v tab should be discover", children[2].mediaId == DISCOVER_ROOT)
    }

    @Test
    fun testLoadDiscover() {
        runBlocking {
            val discover = service.loadDiscoverRoot()
            assertTrue("Discover should have content", !discover.isNullOrEmpty())
        }
    }

    @Test
    fun testLoadFilters() {
        runBlocking {
            val playlist = Playlist(uuid = UUID.randomUUID().toString(), title = "Test title", iconId = 0)
            service.playlistManager = mock<PlaylistManager> { on { findAll() }.doReturn(listOf(playlist)) }

            val filtersRoot = service.loadFiltersRoot()
            assertTrue("Filters should not be empty", !filtersRoot.isNullOrEmpty())
            assertTrue("Filter uuid should be equal", filtersRoot[0].mediaId == playlist.uuid)
            assertTrue("Filter title should be correct", filtersRoot[0].description.title == playlist.title)
            assertTrue("Filter should have an icon", filtersRoot[0].description.iconUri != null)
        }
    }

    @Test
    fun testLoadPodcasts() {
        val podcast = Podcast(UUID.randomUUID().toString(), title = "Test podcast")
        val podcastManager = mock<PodcastManager> { on { runBlocking { findSubscribedSorted() } }.doReturn(listOf(podcast)) }
        service.podcastManager = podcastManager
        val subscriptionManager = mock<SubscriptionManager> { on { getCachedStatus() }.doReturn(SubscriptionStatus.Free()) }
        service.subscriptionManager = subscriptionManager

        runBlocking {
            val podcastsRoot = service.loadPodcastsChildren()
            assertTrue("Podcasts should not be empty", !podcastsRoot.isNullOrEmpty())
            assertTrue("Podcast uuid should be equal", podcastsRoot[0].mediaId == podcast.uuid)
            assertTrue("Podcast title should be correct", podcastsRoot[0].description.title == podcast.title)
        }
    }

    @Test
    fun testLoadPodcastEpisodes() {
        runBlocking {
            val podcast = Podcast(UUID.randomUUID().toString(), title = "Test podcast")
            val episode = Episode(UUID.randomUUID().toString(), title = "Test episode", publishedDate = Date())

            service.playlistManager = mock { on { findByUuid(any()) }.doReturn(null) }
            service.podcastManager = mock { on { runBlocking { findPodcastByUuidSuspend(any()) } }.doReturn(podcast) }
            service.episodeManager = mock { on { findEpisodesByPodcastOrdered(any()) }.doReturn(listOf(episode)) }

            val episodes = service.loadEpisodeChildren(podcast.uuid)
            assertTrue("Episodes should have content", !episodes.isNullOrEmpty())
            assertTrue("Episode uuid should be equal", episodes[0].mediaId == AutoMediaId(episode.uuid, podcast.uuid).toMediaId())
            assertTrue("Episode title should be correct", episodes[0].description.title == episode.title)
        }
    }
}
