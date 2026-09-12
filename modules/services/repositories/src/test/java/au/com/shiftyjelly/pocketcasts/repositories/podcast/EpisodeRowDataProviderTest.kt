package au.com.shiftyjelly.pocketcasts.repositories.podcast

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.ReadSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadProgressCache
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import io.reactivex.Observable
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

private const val EPISODE_UUID = "episode-uuid"

class EpisodeRowDataProviderTest {
    private val episodeManager = mock<EpisodeManager>()
    private val userEpisodeManager = mock<UserEpisodeManager>()
    private val downloadProgressCache = DownloadProgressCache()

    private val playbackManager = mock<PlaybackManager> {
        on { playbackStateFlow } doReturn emptyFlow()
    }

    private val upNextQueue = mock<UpNextQueue> {
        on { changesObservable } doReturn Observable.empty()
    }

    private val bookmarkManager = mock<BookmarkManager> {
        on { hasBookmarksFlow(any()) } doReturn emptyFlow()
    }

    private val alternateEnclosureManager = mock<AlternateEnclosureManager> {
        on { hasHlsAlternateEnclosure(any()) } doReturn emptyFlow()
    }

    private val settings = mock<Settings> {
        val cachedSubscription = mock<ReadSetting<Subscription?>> {
            on { flow } doReturn MutableStateFlow(null)
        }
        on { this.cachedSubscription } doReturn cachedSubscription
    }

    private val provider = EpisodeRowDataProvider(
        episodeManager = episodeManager,
        downloadProgressCache = downloadProgressCache,
        playbackManager = playbackManager,
        upNextQueue = upNextQueue,
        bookmarkManager = bookmarkManager,
        userEpisodeManager = userEpisodeManager,
        alternateEnclosureManager = alternateEnclosureManager,
        settings = settings,
    )

    @Test
    fun `episode row data emits nothing for an unknown episode`() = runTest {
        episodeManager.stub {
            on { findEpisodeByUuid(EPISODE_UUID) } doReturn null
        }

        val rowData = provider.episodeRowDataFlow(EPISODE_UUID).toList()

        assertTrue(rowData.isEmpty())
    }

    @Test
    fun `episode row data emits once every source has a value`() = runTest {
        episodeManager.stub {
            on { findEpisodeByUuid(EPISODE_UUID) } doReturn PodcastEpisode(uuid = EPISODE_UUID, publishedDate = Date())
        }

        provider.episodeRowDataFlow(EPISODE_UUID).test {
            val rowData = awaitItem()

            assertEquals(0, rowData.downloadProgress)
            assertEquals(PlaybackState(episodeUuid = EPISODE_UUID), rowData.playbackState)
            assertEquals(false, rowData.isInUpNext)
            assertEquals(false, rowData.hasBookmarks)
            assertEquals(false, rowData.hasHlsAlternateEnclosure)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `episode row data follows download progress`() = runTest {
        episodeManager.stub {
            on { findEpisodeByUuid(EPISODE_UUID) } doReturn PodcastEpisode(uuid = EPISODE_UUID, publishedDate = Date())
        }

        provider.episodeRowDataFlow(EPISODE_UUID).test {
            assertEquals(0, awaitItem().downloadProgress)

            downloadProgressCache.updateProgress(EPISODE_UUID, downloadedByteCount = 25, contentLength = 100)

            assertEquals(25, awaitItem().downloadProgress)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `user episode row data emits nothing while the episode is missing`() = runTest {
        userEpisodeManager.stub {
            on { episodeFlow(EPISODE_UUID) } doReturn flowOf(null)
        }

        // real time, because the provider emits on Dispatchers.IO and would outrun the test scheduler
        val rowData = withContext(Dispatchers.Default) {
            withTimeoutOrNull(300) { provider.userEpisodeRowDataFlow(EPISODE_UUID).first() }
        }

        assertNull(rowData)
    }

    @Test
    fun `user episode row data emits the first upload progress immediately and then the latest per window`() = runTest {
        val episode = UserEpisode(uuid = EPISODE_UUID, publishedDate = Date())
        userEpisodeManager.stub {
            on { episodeFlow(EPISODE_UUID) } doReturn flowOf(episode)
        }

        try {
            provider.userEpisodeRowDataFlow(EPISODE_UUID).test(timeout = 10.seconds) {
                assertEquals(0, awaitItem().uploadProgress)

                UploadProgressManager.pushProgress(EPISODE_UUID, 0.1f)

                assertEquals(10, awaitItem().uploadProgress)

                UploadProgressManager.pushProgress(EPISODE_UUID, 0.2f)
                UploadProgressManager.pushProgress(EPISODE_UUID, 0.3f)

                // 20 is dropped, the window only lets the latest value through
                assertEquals(30, awaitItem().uploadProgress)
                cancelAndIgnoreRemainingEvents()
            }
        } finally {
            UploadProgressManager.clearProgress(EPISODE_UUID)
        }
    }

    @Test
    fun `user episode row data emits once every source has a value`() = runTest {
        val episode = UserEpisode(uuid = EPISODE_UUID, publishedDate = Date())
        userEpisodeManager.stub {
            on { episodeFlow(EPISODE_UUID) } doReturn flowOf(episode)
        }

        provider.userEpisodeRowDataFlow(EPISODE_UUID).test {
            val rowData = awaitItem()

            assertEquals(episode, rowData.episode)
            assertEquals(0, rowData.downloadProgress)
            assertEquals(0, rowData.uploadProgress)
            assertEquals(PlaybackState(episodeUuid = EPISODE_UUID), rowData.playbackState)
            assertEquals(false, rowData.isInUpNext)
            assertEquals(false, rowData.hasBookmarks)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
