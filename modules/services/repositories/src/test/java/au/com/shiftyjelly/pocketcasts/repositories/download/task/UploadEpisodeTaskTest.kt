package au.com.shiftyjelly.pocketcasts.repositories.download.task

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import com.squareup.moshi.Moshi
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.awaitCancellation
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

class UploadEpisodeTaskTest {
    private val userEpisodeManager = mock<UserEpisodeManager>()
    private val playbackManager = mock<PlaybackManager>()
    private val moshi = Moshi.Builder().build()
    private val context = mock<Context> {
        on { resources } doReturn mock()
    }
    private val userEpisode = UserEpisode(uuid = EPISODE_UUID, publishedDate = Date())

    @Test
    fun `fails when the input has no episode uuid`() {
        val result = runTask(episodeUuid = null)

        assertEquals(Result.failure(errorOutput(null, "Could not find episode null for upload")), result)
    }

    @Test
    fun `succeeds without uploading when the episode is missing`() {
        givenEpisode(null)

        val result = runTask()

        assertEquals(Result.success(successOutput()), result)
        verifyBlocking(userEpisodeManager, never()) { performUploadToServer(any(), any()) }
    }

    @Test
    fun `succeeds when the upload completes`() {
        givenEpisode(userEpisode)
        givenUpload {}

        val result = runTask()

        assertEquals(Result.success(successOutput()), result)
        verifyBlocking(userEpisodeManager) { performUploadToServer(userEpisode, playbackManager) }
    }

    @Test
    fun `fails without retrying on a 400`() {
        givenEpisode(userEpisode)
        givenUpload { throw httpException(400) }

        val result = runTask(runAttemptCount = 0)

        assertEquals(Result.failure(errorOutput(EPISODE_UUID, "Unable to upload, unsupported file")), result)
    }

    @Test
    fun `uses the server error message when the response has one`() {
        givenEpisode(userEpisode)
        val body = """{"errorMessage":"Server message","errorMessageId":"unknown_id"}"""
        givenUpload { throw httpException(400, body) }

        val result = runTask()

        assertEquals(Result.failure(errorOutput(EPISODE_UUID, "Server message")), result)
    }

    @Test
    fun `retries other http errors while attempts remain`() {
        givenEpisode(userEpisode)
        givenUpload { throw httpException(500) }

        val result = runTask(runAttemptCount = 2)

        assertEquals(Result.retry(), result)
    }

    @Test
    fun `fails other http errors once attempts run out`() {
        givenEpisode(userEpisode)
        givenUpload { throw httpException(500) }

        val result = runTask(runAttemptCount = 3)

        assertEquals(Result.failure(errorOutput(EPISODE_UUID, CONNECTION_ERROR)), result)
    }

    @Test
    fun `retries non http upload errors while attempts remain`() {
        givenEpisode(userEpisode)
        givenUpload { throw IllegalStateException("Upload failed") }

        val result = runTask(runAttemptCount = 0)

        assertEquals(Result.retry(), result)
    }

    @Test
    fun `fails non http upload errors once attempts run out`() {
        givenEpisode(userEpisode)
        givenUpload { throw IllegalStateException("Upload failed") }

        val result = runTask(runAttemptCount = 3)

        assertEquals(Result.failure(errorOutput(EPISODE_UUID, CONNECTION_ERROR)), result)
    }

    @Test
    fun `maps an episode lookup error like an upload error`() {
        givenEpisodeLookupFails(IllegalStateException("Database error"))

        val result = runTask(runAttemptCount = 0)

        assertEquals(Result.retry(), result)
    }

    @Test
    fun `stopping the worker cancels the upload`() {
        val started = CountDownLatch(1)
        val cancelled = CountDownLatch(1)
        givenEpisode(userEpisode)
        givenUpload {
            started.countDown()
            try {
                awaitCancellation()
            } finally {
                cancelled.countDown()
            }
        }

        val future = createTask().startWork()
        assertTrue(started.await(5, TimeUnit.SECONDS))
        future.cancel(true)

        assertTrue(future.isCancelled)
        assertTrue(cancelled.await(5, TimeUnit.SECONDS))
    }

    private fun givenEpisode(episode: UserEpisode?) {
        whenever { userEpisodeManager.findEpisodeByUuid(EPISODE_UUID) } doReturn episode
    }

    private fun givenEpisodeLookupFails(error: RuntimeException) {
        whenever { userEpisodeManager.findEpisodeByUuid(EPISODE_UUID) } doThrow error
    }

    private fun givenUpload(upload: suspend () -> Unit) {
        whenever { userEpisodeManager.performUploadToServer(userEpisode, playbackManager) } doSuspendableAnswer { upload() }
    }

    private fun runTask(episodeUuid: String? = EPISODE_UUID, runAttemptCount: Int = 0): Result {
        return createTask(episodeUuid, runAttemptCount).startWork().get()
    }

    private fun createTask(episodeUuid: String? = EPISODE_UUID, runAttemptCount: Int = 0): UploadEpisodeTask {
        val inputData = Data.Builder().putString(UploadEpisodeTask.INPUT_EPISODE_UUID, episodeUuid).build()
        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): UploadEpisodeTask {
                return UploadEpisodeTask(appContext, workerParameters, userEpisodeManager, playbackManager, moshi)
            }
        }
        whenever(context.applicationContext) doReturn context
        return TestListenableWorkerBuilder.from(context, UploadEpisodeTask::class.java)
            .setInputData(inputData)
            .setRunAttemptCount(runAttemptCount)
            .setWorkerFactory(workerFactory)
            .build()
    }

    private fun successOutput() = Data.Builder()
        .putString(UploadEpisodeTask.OUTPUT_EPISODE_UUID, EPISODE_UUID)
        .build()

    private fun errorOutput(episodeUuid: String?, message: String) = Data.Builder()
        .putString(UploadEpisodeTask.OUTPUT_EPISODE_UUID, episodeUuid)
        .putString(UploadEpisodeTask.OUTPUT_ERROR_MESSAGE, message)
        .build()

    private fun httpException(code: Int, body: String = "") = HttpException(
        Response.error<Unit>(code, body.toResponseBody("application/json".toMediaType())),
    )

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val CONNECTION_ERROR = "Unable to upload, please check your internet connection and try again"
    }
}
