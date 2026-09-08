package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.google.protobuf.int32Value
import com.google.protobuf.int64Value
import com.pocketcasts.service.api.EpisodeResponse
import com.pocketcasts.service.api.EpisodesResponse
import com.pocketcasts.service.api.SyncUserEpisode
import com.pocketcasts.service.api.syncUserEpisode
import java.util.Date
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class EpisodeSyncTest {

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var playbackManager: PlaybackManager

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var syncManager: SyncManager

    private lateinit var episodeSync: EpisodeSync

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(playbackManager.isPlaying()).thenReturn(false)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(null)
        whenever(settings.getPlaybackEpisodePositionChangedOnSyncThresholdSecs()).thenReturn(0L)
        episodeSync = EpisodeSync(episodeManager, podcastManager, playbackManager, settings, syncManager)
    }

    @Test
    fun `fetch and mark played a missing episode from a subscribed podcast`() = runTest {
        val serverEpisode = completedServerEpisode(uuid = "episode1", podcastUuid = "podcast1")

        whenever(episodeManager.findByUuids(any())).thenReturn(emptyList())
        whenever(podcastManager.findSubscribedUuids()).thenReturn(listOf("podcast1"))
        whenever(syncManager.getEpisodesOrThrow(any())).thenReturn(episodesResponse("episode1", "podcast1"))

        episodeSync.processIncrementalResponse(listOf(serverEpisode))

        verify(syncManager).getEpisodesOrThrow(any())
        verify(episodeManager).add(argThat { size == 1 && first().uuid == "episode1" }, eq("podcast1"), eq(false))
        verify(episodeManager).markedAsPlayedExternally(argThat { uuid == "episode1" }, eq(playbackManager), eq(podcastManager))
        verify(episodeManager).updateAllSyncFields(argThat { any { it.uuid == "episode1" && it.playingStatus == EpisodePlayingStatus.COMPLETED } })
    }

    @Test
    fun `do not fetch a missing episode from an unsubscribed podcast`() = runTest {
        val serverEpisode = completedServerEpisode(uuid = "episode1", podcastUuid = "podcast1")

        whenever(episodeManager.findByUuids(any())).thenReturn(emptyList())
        whenever(podcastManager.findSubscribedUuids()).thenReturn(emptyList())

        episodeSync.processIncrementalResponse(listOf(serverEpisode))

        verify(syncManager, never()).getEpisodesOrThrow(any())
        verify(episodeManager, never()).add(any(), any(), any())
        verify(episodeManager, never()).markedAsPlayedExternally(any(), any(), any())
    }

    @Test
    fun `apply the server change to an already present episode without fetching`() = runTest {
        val serverEpisode = completedServerEpisode(uuid = "episode1", podcastUuid = "podcast1")
        val localEpisode = PodcastEpisode(
            uuid = "episode1",
            podcastUuid = "podcast1",
            publishedDate = Date(),
            playingStatus = EpisodePlayingStatus.NOT_PLAYED,
        )

        whenever(episodeManager.findByUuids(any())).thenReturn(listOf(localEpisode))

        episodeSync.processIncrementalResponse(listOf(serverEpisode))

        verify(podcastManager, never()).findSubscribedUuids()
        verify(syncManager, never()).getEpisodesOrThrow(any())
        verify(episodeManager, never()).add(any(), any(), any())
        verify(episodeManager).markedAsPlayedExternally(eq(localEpisode), eq(playbackManager), eq(podcastManager))
    }

    private fun completedServerEpisode(uuid: String, podcastUuid: String): SyncUserEpisode {
        val modified = System.currentTimeMillis()
        return syncUserEpisode {
            this.uuid = uuid
            this.podcastUuid = podcastUuid
            playingStatus = int32Value { value = EpisodePlayingStatus.COMPLETED.toInt() }
            playingStatusModified = int64Value { value = modified }
        }
    }

    private fun episodesResponse(uuid: String, podcastUuid: String): EpisodesResponse {
        return EpisodesResponse.newBuilder()
            .addEpisodes(
                EpisodeResponse.newBuilder()
                    .setUuid(uuid)
                    .setPodcastUuid(podcastUuid)
                    .setTitle("Title $uuid")
                    .setUrl("https://example.com/$uuid")
                    .build(),
            )
            .build()
    }
}
