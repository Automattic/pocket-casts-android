package au.com.shiftyjelly.pocketcasts.repositories.download

import androidx.annotation.WorkerThread
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import dagger.Lazy
import java.io.File
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Source
import okio.buffer
import okio.sink

/**
 * **Note:** This class is intended to be used exclusively from a `WorkManager` `Worker`’s
 * `doWork()` method. It must not be called from any other part of the codebase.
 *
 * This downloader is deliberately implemented using synchronous, blocking logic rather
 * than coroutines or suspend functions. `WorkManager` invokes `Worker.doWork()` on a
 * background thread supplied by its own executor, and the method is expected to run
 * synchronously until the work completes.
 *
 * By keeping `EpisodeDownloader` fully synchronous, all execution remains confined to
 * `WorkManager`’s executor and completes within the lifetime of `doWork()`. This avoids
 * launching additional asynchronous or parallel work that would be invisible to
 * `WorkManager`.
 *
 * In a coroutine-heavy codebase, this design is intentional and respects `WorkManager`’s
 * threading and concurrency guarantees. For example, when `WorkManager` is configured
 * with a single-threaded executor, a blocking implementation ensures tasks execute
 * strictly sequentially. Additionally, the `Worker`’s success or failure accurately
 * reflects the completion of the episode download, since no coroutine jobs outlive the
 * `doWork()` call.
 */
internal class EpisodeDownloader(
    private val httpClient: Lazy<OkHttpClient>,
    private val progressCache: DownloadProgressCache,
) {
    @WorkerThread
    fun download(episode: BaseEpisode, downloadFile: File, tempFile: File): Result {
        val result = try {
            downloadOrThrow(episode, downloadFile, tempFile)
        } catch (error: Throwable) {
            Result.ExceptionFailure(error)
        } finally {
            progressCache.clearProgress(episode.uuid)
            runCatching { tempFile.delete() }
        }
        if (result is Result.Failure) {
            runCatching { downloadFile.delete() }
        }
        return result
    }

    private fun downloadOrThrow(episode: BaseEpisode, downloadFile: File, tempFile: File): Result {
        val downloadUrl = episode.downloadUrl?.toHttpUrlOrNull()
        if (downloadUrl == null) {
            return Result.InvalidDownloadUrl(episode.downloadUrl)
        }

        val request = Request.Builder().url(downloadUrl).build()
        val call = httpClient.get().newCall(request)

        progressCache.updateProgress(episode.uuid, downloadedByteCount = 0L, contentLength = null)
        return call.execute().use { response ->
            if (response.isSuccessful) {
                response.downloadProgressSource(episode).readTo(tempFile)
                tempFile.copyTo(downloadFile, overwrite = true)
                Result.Success(downloadFile)
            } else {
                Result.UnsuccessfulHttpCall(response.code)
            }
        }
    }

    private fun Response.downloadProgressSource(episode: BaseEpisode): Source {
        val contentLength = body.contentLength().takeIf { it > 0 } ?: episode.sizeInBytes.takeIf { it > 0 }
        val source = if (contentLength != null) {
            body.source().withReadListener { byteCount ->
                progressCache.updateProgress(episode.uuid, byteCount, contentLength)
            }
        } else {
            body.source()
        }
        return source
    }

    private fun Source.readTo(tempFile: File) {
        buffer().use { source -> tempFile.sink().use { sink -> source.readAll(sink) } }
    }

    sealed interface Result {
        data class Success(val file: File) : Result

        sealed interface Failure : Result

        data class InvalidDownloadUrl(val url: String?) : Failure

        data class UnsuccessfulHttpCall(val code: Int) : Failure

        class ExceptionFailure(val throwable: Throwable) : Failure
    }
}
