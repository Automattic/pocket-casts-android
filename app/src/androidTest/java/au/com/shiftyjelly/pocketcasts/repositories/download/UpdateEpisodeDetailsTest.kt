package au.com.shiftyjelly.pocketcasts.repositories.download

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4ClassRunner::class)
class UpdateEpisodeDetailsTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testRedirect() {
        val testFileSize = 100000000L
        val testFileType = "audio/mp3"

        val server = MockWebServer()

        val firstUrl = server.url("/episodes/1.mp3")
        val secondUrl = server.url("/episodes/v2/1.mp3")

        val redirectResponse = MockResponse().setResponseCode(301).addHeader("Location", secondUrl)
        val finalResponse = MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", testFileType)
            .addHeader("Content-Length", testFileSize.toString())

        server.enqueue(redirectResponse)
        server.enqueue(finalResponse)

        val episode = Episode(uuid = UUID.randomUUID().toString(), publishedDate = Date(), downloadUrl = firstUrl.toString())
        val episodeManager = mock<EpisodeManager> { on { findByUuid(episode.uuid) }.doReturn(episode) }

        val episodeUuids = listOf(episode.uuid).toTypedArray()
        val data = Data.Builder().putStringArray(UpdateEpisodeDetailsTask.INPUT_EPISODE_UUIDS, episodeUuids).build()
        val worker = TestListenableWorkerBuilder<UpdateEpisodeDetailsTask>(context, inputData = data)
            .setWorkerFactory(TestWorkerFactory(episodeManager))
            .build()

        runBlocking {
            val result = worker.doWork()
            assertTrue("Worker should succeed", result is ListenableWorker.Result.Success)

            val firstRequest = server.takeRequest()
            assertTrue("Request was a head request", firstRequest.method == "HEAD")

            val secondRequest = server.takeRequest()
            assertTrue("Url was redirected", secondRequest.requestUrl == secondUrl)
            assertTrue("Second request was still a head request", secondRequest.method == "HEAD")

            val thirdRequest = server.takeRequest(1, TimeUnit.MILLISECONDS)
            assertNull("There shouldn't be a third request", thirdRequest)

            verify(episodeManager, times(1)).updateSizeInBytes(episode, testFileSize)
        }
    }

    class TestWorkerFactory(private val episodeManager: EpisodeManager) : WorkerFactory() {
        override fun createWorker(context: Context, workerClassName: String, workerParameters: WorkerParameters): ListenableWorker? {
            return UpdateEpisodeDetailsTask(context, workerParameters, episodeManager)
        }
    }
}
