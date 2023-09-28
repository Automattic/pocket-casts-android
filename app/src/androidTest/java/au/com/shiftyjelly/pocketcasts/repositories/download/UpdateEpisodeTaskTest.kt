package au.com.shiftyjelly.pocketcasts.repositories.download

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.download.task.UpdateEpisodeTask
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.never
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.util.Date
import java.util.UUID

@RunWith(AndroidJUnit4ClassRunner::class)
class UpdateEpisodeTaskTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    /**
     * Test the download url is updated when the server has a different url.
     */
    @Test
    fun testDownloadUrlChanged() {
        testDownloadUrl(
            deviceUrl = "https://www.pocketcasts.com/old_url.mp3",
            serverUrl = "https://www.pocketcasts.com/new_url.mp3",
            shouldUpdate = true
        )
    }

    /**
     * Test the download url is not updated when the server and device have the same url.
     */
    @Test
    fun testDownloadUrlSame() {
        testDownloadUrl(
            deviceUrl = "https://www.pocketcasts.com/url.mp3",
            serverUrl = "https://www.pocketcasts.com/url.mp3",
            shouldUpdate = false
        )
    }

    private fun testDownloadUrl(deviceUrl: String, serverUrl: String, shouldUpdate: Boolean) {
        val podcastUuid = UUID.randomUUID().toString()
        val episodeUuid = UUID.randomUUID().toString()
        val deviceEpisode = PodcastEpisode(
            uuid = episodeUuid,
            publishedDate = Date(),
            podcastUuid = podcastUuid,
            downloadUrl = deviceUrl
        )
        val serverEpisode = PodcastEpisode(
            uuid = episodeUuid,
            publishedDate = Date(),
            podcastUuid = podcastUuid,
            downloadUrl = serverUrl
        )
        val serverPodcast = Podcast(uuid = podcastUuid).apply {
            episodes.add(serverEpisode)
        }

        val podcastCacheServerManager = mock<PodcastCacheServerManager> {
            onBlocking { getPodcastAndEpisode(podcastUuid, episodeUuid) } doReturn serverPodcast
        }
        val episodeDao = mock<EpisodeDao> {
            onBlocking { findByUuid(episodeUuid) } doReturn deviceEpisode
        }
        val appDatabase = mock<AppDatabase> {
            on { episodeDao() } doReturn episodeDao
        }
        val inputData = UpdateEpisodeTask.buildInputData(deviceEpisode)
        val worker = TestListenableWorkerBuilder<UpdateEpisodeTask>(context = context, inputData = inputData)
            .setWorkerFactory(TestWorkerFactory(podcastCacheServerManager, appDatabase))
            .build()

        runTest {
            val result = worker.doWork()
            assertTrue("Worker should succeed", result is ListenableWorker.Result.Success)

            if (shouldUpdate) {
                verify(episodeDao, times(1)).updateDownloadUrl(serverUrl, episodeUuid)
                verify(episodeDao, never()).updateDownloadUrl(deviceUrl, episodeUuid)
            } else {
                verify(episodeDao, never()).updateDownloadUrl(serverUrl, episodeUuid)
                verify(episodeDao, never()).updateDownloadUrl(deviceUrl, episodeUuid)
            }
        }
    }

    class TestWorkerFactory(private val podcastCacheServerManager: PodcastCacheServerManager, private val appDatabase: AppDatabase) : WorkerFactory() {
        override fun createWorker(context: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
            return UpdateEpisodeTask(
                context = context,
                params = workerParameters,
                podcastCacheServerManager = podcastCacheServerManager,
                appDatabase = appDatabase
            )
        }
    }
}
