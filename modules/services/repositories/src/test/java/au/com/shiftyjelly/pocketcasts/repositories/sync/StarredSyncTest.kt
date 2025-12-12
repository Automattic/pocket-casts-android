package au.com.shiftyjelly.pocketcasts.repositories.sync

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import com.pocketcasts.service.api.StarredEpisode
import io.reactivex.Single
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class StarredSyncTest {

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var settings: Settings

    private lateinit var starredSync: StarredSync

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)

        starredSync = StarredSync(episodeManager, podcastManager, settings)
    }

    @Test
    fun `star the local episode when the server episode is starred`() = runTest {
        val now = System.currentTimeMillis()
        val serverEpisode = createServerEpisode("episode1", starredModified = now - TimeUnit.DAYS.toMillis(1))
        val podcast = createPodcast()
        val localEpisode = createEpisode(serverEpisode)

        whenever(settings.getStarredServerModified()).thenReturn(0L)
        whenever(podcastManager.findOrDownloadPodcastRxSingle(serverEpisode.podcastUuid)).thenReturn(Single.just(podcast))
        whenever(episodeManager.findByUuid(serverEpisode.uuid)).thenReturn(localEpisode)

        starredSync.syncStarredEpisodes(
            serverEpisodes = listOf(serverEpisode),
            currentTimeMs = now,
        )

        verify(episodeManager).starEpisodeFromServer(
            episode = localEpisode,
            modified = serverEpisode.starredModified,
        )
    }

    @Test
    fun `ignore the server episode when the local episode starred value has changed`() = runTest {
        val now = System.currentTimeMillis()
        val serverEpisode = createServerEpisode("episode1", starredModified = now - TimeUnit.DAYS.toMillis(1))
        val podcast = createPodcast()
        val localEpisode = createEpisode(serverEpisode).copy(
            isStarred = false,
            lastStarredDate = now,
        )

        whenever(settings.getStarredServerModified()).thenReturn(0L)
        whenever(podcastManager.findOrDownloadPodcastRxSingle(serverEpisode.podcastUuid)).thenReturn(Single.just(podcast))
        whenever(episodeManager.findByUuid(serverEpisode.uuid)).thenReturn(localEpisode)

        starredSync.syncStarredEpisodes(
            serverEpisodes = listOf(serverEpisode),
            currentTimeMs = now,
        )

        verify(episodeManager, never()).starEpisodeFromServer(
            episode = localEpisode,
            modified = serverEpisode.starredModified,
        )
    }

    @Test
    fun `update settings with max starredModified value from multiple episodes`() = runTest {
        val now = System.currentTimeMillis()
        val episode1 = createServerEpisode("episode1", starredModified = now - TimeUnit.DAYS.toMillis(2))
        val episode2 = createServerEpisode("episode2", starredModified = now - TimeUnit.DAYS.toMillis(1)) // Most recent
        val episode3 = createServerEpisode("episode3", starredModified = now - TimeUnit.DAYS.toMillis(3))

        val podcast = createPodcast()
        val localEpisode1 = createEpisode(episode1)
        val localEpisode2 = createEpisode(episode2)
        val localEpisode3 = createEpisode(episode3)

        whenever(settings.getStarredServerModified()).thenReturn(0L)
        whenever(podcastManager.findOrDownloadPodcastRxSingle(episode1.podcastUuid)).thenReturn(Single.just(podcast))
        whenever(podcastManager.findOrDownloadPodcastRxSingle(episode2.podcastUuid)).thenReturn(Single.just(podcast))
        whenever(podcastManager.findOrDownloadPodcastRxSingle(episode3.podcastUuid)).thenReturn(Single.just(podcast))
        whenever(episodeManager.findByUuid(episode1.uuid)).thenReturn(localEpisode1)
        whenever(episodeManager.findByUuid(episode2.uuid)).thenReturn(localEpisode2)
        whenever(episodeManager.findByUuid(episode3.uuid)).thenReturn(localEpisode3)

        starredSync.syncStarredEpisodes(
            serverEpisodes = listOf(episode3, episode1, episode2),
            currentTimeMs = now,
        )

        verify(settings).setStarredServerModified(episode2.starredModified)
    }

    @Test
    fun `only update episodes newer than one week`() = runTest {
        val now = System.currentTimeMillis()
        val recentEpisode = createServerEpisode("recent", starredModified = now - TimeUnit.DAYS.toMillis(1))
        val oldEpisode = createServerEpisode("old", starredModified = now - TimeUnit.DAYS.toMillis(14))

        val podcast = createPodcast()
        val localRecentEpisode = createEpisode(recentEpisode)
        val localOldEpisode = createEpisode(oldEpisode)

        // Set the last starred modified to now to check only the recent episode is updated
        whenever(settings.getStarredServerModified()).thenReturn(now)
        whenever(podcastManager.findOrDownloadPodcastRxSingle(recentEpisode.podcastUuid)).thenReturn(Single.just(podcast))
        whenever(episodeManager.findByUuid(recentEpisode.uuid)).thenReturn(localRecentEpisode)

        starredSync.syncStarredEpisodes(
            serverEpisodes = listOf(recentEpisode, oldEpisode),
            currentTimeMs = now,
        )

        // Verify the recent episode had the star updated
        verify(episodeManager).starEpisodeFromServer(
            episode = localRecentEpisode,
            modified = recentEpisode.starredModified,
        )
        // Verify the old episode didn't have the star updated
        verify(episodeManager, never()).starEpisodeFromServer(
            episode = localOldEpisode,
            modified = oldEpisode.starredModified,
        )
    }

    private fun createServerEpisode(uuid: String, starredModified: Long): StarredEpisode {
        return StarredEpisode.newBuilder()
            .setUuid(uuid)
            .setPodcastUuid("podcast-$uuid")
            .setDuration(1000)
            .setPlayingStatus(1)
            .setPlayedUpTo(0)
            .setIsDeleted(false)
            .setStarredModified(starredModified)
            .build()
    }

    private fun createPodcast(): Podcast {
        return Podcast(
            uuid = "podcast-uuid",
        )
    }

    private fun createEpisode(starredEpisode: StarredEpisode) = PodcastEpisode(
        uuid = starredEpisode.uuid,
        publishedDate = Date(),
        podcastUuid = starredEpisode.podcastUuid,
        isStarred = false,
    )
}
