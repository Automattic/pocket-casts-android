package au.com.shiftyjelly.pocketcasts.repositories.refresh

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.dao.PendingEpisodeTaskDao
import au.com.shiftyjelly.pocketcasts.models.entity.PendingEpisodeTask
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast.AutoAddUpNext
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.download.AutoDownloadEpisodeProvider
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadType
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

class PendingEpisodeTaskProcessorTest {
    private val now = Instant.parse("2026-09-22T10:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private val pendingEpisodeTaskDao = mock<PendingEpisodeTaskDao>()
    private val episodeManager = mock<EpisodeManager>()
    private val podcastManager = mock<PodcastManager>()
    private val playbackManager = mock<PlaybackManager>()
    private val autoDownloadProvider = mock<AutoDownloadEpisodeProvider>()
    private val downloadQueue = mock<DownloadQueue>()

    private val processor = PendingEpisodeTaskProcessor(
        pendingEpisodeTaskDao = pendingEpisodeTaskDao,
        episodeManager = episodeManager,
        podcastManager = podcastManager,
        playbackManager = playbackManager,
        autoDownloadProvider = autoDownloadProvider,
        downloadQueue = downloadQueue,
        clock = clock,
    )

    @Test
    fun `remove tasks that are older than a week`() = runTest {
        processor.removeExpiredTasks()

        verify(pendingEpisodeTaskDao).deleteCreatedBefore(now.minusSeconds(7 * 24 * 60 * 60))
    }

    @Test
    fun `auto download the episodes left behind by an interrupted refresh`() = runTest {
        givenPendingTasks(PendingEpisodeTask.Type.AUTO_DOWNLOAD, "interrupted-episode", "new-episode")
        autoDownloadProvider.stub {
            on { getAll(listOf("interrupted-episode", "new-episode")) } doReturn setOf("interrupted-episode", "new-episode")
        }
        givenEnqueueSucceeds()

        processor.processAutoDownload(listOf("new-episode"))

        verify(downloadQueue).enqueueAll(
            setOf("interrupted-episode", "new-episode"),
            DownloadType.Automatic(bypassAutoDownloadStatus = false),
            SourceView.AUTO_DOWNLOAD,
        )
    }

    @Test
    fun `remove the auto download tasks once they are enqueued`() = runTest {
        givenPendingTasks(PendingEpisodeTask.Type.AUTO_DOWNLOAD, "episode-1")
        autoDownloadProvider.stub { on { getAll(any()) } doReturn emptySet() }
        givenEnqueueSucceeds()

        processor.processAutoDownload(emptyList())

        verify(pendingEpisodeTaskDao).delete(PendingEpisodeTask.Type.AUTO_DOWNLOAD, listOf("episode-1"))
    }

    @Test
    fun `keep the auto download tasks when the enqueue is cancelled`() = runTest {
        givenPendingTasks(PendingEpisodeTask.Type.AUTO_DOWNLOAD, "episode-1")
        autoDownloadProvider.stub { on { getAll(any()) } doReturn emptySet() }
        val cancelledJob = Job().apply { cancel() }
        downloadQueue.stub { on { enqueueAll(any(), any(), any()) } doReturn cancelledJob }

        processor.processAutoDownload(emptyList())

        verify(pendingEpisodeTaskDao, never()).delete(eq(PendingEpisodeTask.Type.AUTO_DOWNLOAD), any())
    }

    @Test
    fun `add the pending episodes to up next with the podcast's current mode`() = runTest {
        givenPendingTasks(PendingEpisodeTask.Type.UP_NEXT, "episode-1")
        val episode = episode("episode-1", podcastUuid = "podcast-1")
        givenEpisodes(episode)
        givenPodcast("podcast-1", AutoAddUpNext.PLAY_NEXT)

        processor.processUpNext(emptyList())

        verify(playbackManager).addEpisodes(listOf(AutoAddUpNext.PLAY_NEXT to episode))
        verify(pendingEpisodeTaskDao).delete(PendingEpisodeTask.Type.UP_NEXT, listOf("episode-1"))
    }

    @Test
    fun `skip an episode whose podcast no longer adds to up next`() = runTest {
        givenPendingTasks(PendingEpisodeTask.Type.UP_NEXT, "episode-1")
        givenEpisodes(episode("episode-1", podcastUuid = "podcast-1"))
        givenPodcast("podcast-1", AutoAddUpNext.OFF)

        processor.processUpNext(emptyList())

        verify(playbackManager).addEpisodes(emptyList())
        verify(pendingEpisodeTaskDao).delete(PendingEpisodeTask.Type.UP_NEXT, listOf("episode-1"))
    }

    @Test
    fun `skip an episode that was archived or played before the task ran`() = runTest {
        givenPendingTasks(PendingEpisodeTask.Type.UP_NEXT, "archived", "played", "unplayed")
        val unplayed = episode("unplayed", podcastUuid = "podcast-1")
        givenEpisodes(
            episode("archived", podcastUuid = "podcast-1").apply { isArchived = true },
            episode("played", podcastUuid = "podcast-1").apply { playingStatus = EpisodePlayingStatus.COMPLETED },
            unplayed,
        )
        givenPodcast("podcast-1", AutoAddUpNext.PLAY_LAST)

        processor.processUpNext(emptyList())

        verify(playbackManager).addEpisodes(listOf(AutoAddUpNext.PLAY_LAST to unplayed))
        verify(pendingEpisodeTaskDao).delete(PendingEpisodeTask.Type.UP_NEXT, listOf("archived", "played", "unplayed"))
    }

    @Test
    fun `do nothing when there are no pending up next tasks`() = runTest {
        givenPendingTasks(PendingEpisodeTask.Type.UP_NEXT)

        processor.processUpNext(emptyList())

        verifyNoInteractions(playbackManager)
    }

    private fun givenPendingTasks(task: PendingEpisodeTask.Type, vararg episodeUuids: String) {
        pendingEpisodeTaskDao.stub {
            on { findEpisodeUuids(task) } doReturn episodeUuids.toList()
        }
    }

    private fun givenEpisodes(vararg episodes: PodcastEpisode) {
        episodeManager.stub {
            on { findByUuids(any()) } doReturn episodes.toList()
        }
    }

    private fun givenPodcast(uuid: String, autoAddToUpNext: AutoAddUpNext) {
        podcastManager.stub {
            on { findPodcastByUuid(uuid) } doReturn Podcast(uuid = uuid, autoAddToUpNext = autoAddToUpNext)
        }
    }

    private fun givenEnqueueSucceeds() {
        downloadQueue.stub {
            on { enqueueAll(any(), any(), any()) } doReturn CompletableDeferred(Unit)
        }
    }

    private fun episode(uuid: String, podcastUuid: String) = PodcastEpisode(
        uuid = uuid,
        podcastUuid = podcastUuid,
        publishedDate = Date(),
    )
}
