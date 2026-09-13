package au.com.shiftyjelly.pocketcasts.repositories.download.task

import android.content.Context
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.impl.utils.taskexecutor.SerialExecutor
import androidx.work.impl.utils.taskexecutor.TaskExecutor
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import com.squareup.moshi.Moshi
import io.reactivex.Completable
import io.reactivex.Maybe
import java.util.Date
import java.util.concurrent.Executor
import kotlinx.coroutines.Dispatchers
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

class UploadEpisodeTaskTest {
    private val userEpisodeManager = mock<UserEpisodeManager>()
    private val playbackManager = mock<PlaybackManager>()
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
        verify(userEpisodeManager, never()).performUploadToServerRxCompletable(any(), any())
    }

    @Test
    fun `succeeds when the upload completes`() {
        givenEpisode(userEpisode)
        givenUpload(Completable.complete())

        val result = runTask()

        assertEquals(Result.success(successOutput()), result)
        verify(userEpisodeManager).performUploadToServerRxCompletable(userEpisode, playbackManager)
    }

    @Test
    fun `fails without retrying on a 400`() {
        givenEpisode(userEpisode)
        givenUpload(Completable.error(httpException(400)))

        val result = runTask(runAttemptCount = 0)

        assertEquals(Result.failure(errorOutput(EPISODE_UUID, "Unable to upload, unsupported file")), result)
    }

    @Test
    fun `uses the server error message when the response has one`() {
        givenEpisode(userEpisode)
        val body = """{"errorMessage":"Server message","errorMessageId":"unknown_id"}"""
        givenUpload(Completable.error(httpException(400, body)))

        val result = runTask()

        assertEquals(Result.failure(errorOutput(EPISODE_UUID, "Server message")), result)
    }

    @Test
    fun `retries other http errors while attempts remain`() {
        givenEpisode(userEpisode)
        givenUpload(Completable.error(httpException(500)))

        val result = runTask(runAttemptCount = 2)

        assertEquals(Result.retry(), result)
    }

    @Test
    fun `fails other http errors once attempts run out`() {
        givenEpisode(userEpisode)
        givenUpload(Completable.error(httpException(500)))

        val result = runTask(runAttemptCount = 3)

        assertEquals(Result.failure(errorOutput(EPISODE_UUID, CONNECTION_ERROR)), result)
    }

    @Test
    fun `retries non http upload errors while attempts remain`() {
        givenEpisode(userEpisode)
        givenUpload(Completable.error(IllegalStateException("Upload failed")))

        val result = runTask(runAttemptCount = 0)

        assertEquals(Result.retry(), result)
    }

    @Test
    fun `fails non http upload errors once attempts run out`() {
        givenEpisode(userEpisode)
        givenUpload(Completable.error(IllegalStateException("Upload failed")))

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
    fun `stopping the worker disposes the upload`() {
        var disposed = false
        givenEpisode(userEpisode)
        givenUpload(Completable.never().doOnDispose { disposed = true })

        val future = createTask().startWork()
        future.cancel(true)

        assertTrue(future.isCancelled)
        assertTrue(disposed)
    }

    private fun givenEpisode(episode: UserEpisode?) {
        val maybe = if (episode == null) Maybe.empty() else Maybe.just(episode)
        whenever(userEpisodeManager.findEpisodeByUuidRxMaybe(EPISODE_UUID)) doReturn maybe
        whenever { userEpisodeManager.findEpisodeByUuid(EPISODE_UUID) } doReturn episode
    }

    private fun givenEpisodeLookupFails(error: RuntimeException) {
        whenever(userEpisodeManager.findEpisodeByUuidRxMaybe(EPISODE_UUID)) doReturn Maybe.error(error)
        whenever { userEpisodeManager.findEpisodeByUuid(EPISODE_UUID) } doThrow error
    }

    private fun givenUpload(upload: Completable) {
        whenever(userEpisodeManager.performUploadToServerRxCompletable(userEpisode, playbackManager)) doReturn upload
    }

    private fun runTask(episodeUuid: String? = EPISODE_UUID, runAttemptCount: Int = 0): Result {
        return createTask(episodeUuid, runAttemptCount).startWork().get()
    }

    private fun createTask(episodeUuid: String? = EPISODE_UUID, runAttemptCount: Int = 0): UploadEpisodeTask {
        val inputData = Data.Builder().putString(UploadEpisodeTask.INPUT_EPISODE_UUID, episodeUuid).build()
        val directExecutor = Executor { it.run() }
        val serialExecutor = object : SerialExecutor {
            override fun execute(command: Runnable) = command.run()
            override fun hasPendingTasks() = false
        }
        val taskExecutor = mock<TaskExecutor> {
            on { mainThreadExecutor } doReturn directExecutor
            on { serialTaskExecutor } doReturn serialExecutor
        }
        val params = mock<WorkerParameters> {
            on { this.inputData } doReturn inputData
            on { this.runAttemptCount } doReturn runAttemptCount
            on { backgroundExecutor } doReturn directExecutor
            on { this.taskExecutor } doReturn taskExecutor
            on { workerContext } doReturn Dispatchers.Unconfined
        }
        return UploadEpisodeTask(context, params, userEpisodeManager, playbackManager, Moshi.Builder().build())
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
