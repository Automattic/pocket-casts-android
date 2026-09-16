package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.HistoryManager
import java.io.IOException
import java.util.Date
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

class SyncHistoryTaskTest {
    private val episodeManager = mock<EpisodeManager> {
        on { findEpisodesForHistorySyncBlocking() } doReturn emptyList()
    }
    private val syncManager = mock<SyncManager>()
    private val settings = mock<Settings>()
    private val historyManager = mock<HistoryManager>()
    private val context = mock<Context> {
        on { resources } doReturn mock()
    }

    @Test
    fun `processes the server response and marks history synced`() {
        val response = HistorySyncResponse(serverModified = 10, lastCleared = 0, changes = emptyList())
        givenServerResponse { response }

        val result = runTask()

        assertEquals(Result.success(), result)
        verifyBlocking(historyManager) { processServerResponse(response, updateServerModified = true) }
        verify(episodeManager).markPlaybackHistorySyncedBlocking()
        verify(episodeManager, never()).clearEpisodePlaybackInteractionDatesBeforeBlocking(any())
    }

    @Test
    fun `clears local history when the server was cleared`() {
        givenServerResponse { HistorySyncResponse(serverModified = 10, lastCleared = 1_000, changes = null) }
        whenever(settings.getClearHistoryTime()) doReturn 500L

        val result = runTask()

        assertEquals(Result.success(), result)
        verify(episodeManager).clearEpisodePlaybackInteractionDatesBeforeBlocking(Date(1_000))
        verify(settings).setClearHistoryTime(0L)
    }

    @Test
    fun `succeeds without processing when the server returns not modified`() {
        givenServerResponse { throw httpException(304) }

        val result = runTask()

        assertEquals(Result.success(), result)
        verifyBlocking(historyManager, never()) { processServerResponse(any(), any()) }
        verify(episodeManager, never()).markPlaybackHistorySyncedBlocking()
    }

    @Test
    fun `fails on other http errors`() {
        givenServerResponse { throw httpException(500) }

        val result = runTask()

        assertEquals(Result.failure(), result)
        verify(episodeManager, never()).markPlaybackHistorySyncedBlocking()
    }

    @Test
    fun `fails on network errors`() {
        givenServerResponse { throw IOException("No network") }

        val result = runTask()

        assertEquals(Result.failure(), result)
        verify(episodeManager, never()).markPlaybackHistorySyncedBlocking()
    }

    private fun givenServerResponse(answer: suspend () -> HistorySyncResponse) {
        whenever { syncManager.historySync(anyOrNull()) } doSuspendableAnswer { answer() }
    }

    private fun runTask(): Result {
        val workerFactory = object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): SyncHistoryTask {
                return SyncHistoryTask(appContext, workerParameters, episodeManager, syncManager, settings, historyManager)
            }
        }
        whenever(context.applicationContext) doReturn context
        return TestListenableWorkerBuilder.from(context, SyncHistoryTask::class.java)
            .setWorkerFactory(workerFactory)
            .build()
            .startWork()
            .get()
    }

    // Response.error(code, body) rejects codes below 400, so a 304 needs a raw response
    private fun httpException(code: Int): HttpException {
        val rawResponse = okhttp3.Response.Builder()
            .code(code)
            .message("HTTP $code")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("https://api.pocketcasts.com/history/sync").build())
            .build()
        return HttpException(Response.error<Unit>("".toResponseBody("application/json".toMediaType()), rawResponse))
    }
}
