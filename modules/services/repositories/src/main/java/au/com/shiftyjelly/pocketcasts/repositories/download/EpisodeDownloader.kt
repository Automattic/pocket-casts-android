package au.com.shiftyjelly.pocketcasts.repositories.download

import androidx.annotation.WorkerThread
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import dagger.Lazy
import java.io.File
import java.io.IOException
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
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
    private val httpClient: Lazy<Call.Factory>,
    private val progressCache: DownloadProgressCache,
    private val minContentLength: Long = SUSPICIOUS_FILE_SIZE,
    private val onResponse: (Response) -> Unit = {},
    private val onComplete: (DownloadProgress?, fileSize: Long) -> Unit = { _, _ -> },
) {
    @WorkerThread
    fun download(episode: BaseEpisode, downloadFile: File, tempFile: File): Result {
        val result = try {
            downloadOrThrow(episode, downloadFile, tempFile)
        } catch (error: Throwable) {
            Result.ExceptionFailure(error)
        }

        val progress = progressCache.progressFlow.value[episode.uuid]
        onComplete(progress, tempFile.length())

        runCatching { tempFile.delete() }
        if (result is Result.Failure) {
            progressCache.clearProgress(episode.uuid)
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
        return call.blockingEnqueue().use { response ->
            onResponse(response)

            if (!response.isSuccessful) {
                return Result.UnsuccessfulHttpCall(response.code)
            }

            val contentType = response.header("Content-Type")
            if (contentType.isInvalidContentType()) {
                return Result.InvalidContentType(contentType)
            }

            response.downloadProgressSource(episode).readTo(tempFile)
            val fileSize = tempFile.length()
            if (fileSize < minContentLength) {
                return Result.SuspiciousFileSize(fileSize)
            }

            tempFile.copyTo(downloadFile, overwrite = true)
            Result.Success(downloadFile)
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

        data class InvalidContentType(val contentType: String) : Failure

        data class SuspiciousFileSize(val bytes: Long) : Failure

        class ExceptionFailure(val throwable: Throwable) : Failure
    }
}

/**
 * Have to use enqueue for high bandwidth requests on the watch app
 * See https://github.com/google/horologist/blob/7bd044a4766e379f85ee3f5a01272853eec3155d/network-awareness/src/main/java/com/google/android/horologist/networks/okhttp/impl/HighBandwidthCall.kt#L93-L92
 */
private fun Call.blockingEnqueue(): Response = runBlocking {
    suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        })
        continuation.invokeOnCancellation { this@blockingEnqueue.cancel() }
    }
}

@OptIn(ExperimentalContracts::class)
private fun String?.isInvalidContentType(): Boolean {
    contract {
        returns(true) implies (this@isInvalidContentType != null)
    }
    return if (this != null) {
        INVALID_CONTENT_TYPES.any { invalid -> startsWith(invalid, ignoreCase = true) }
    } else {
        false
    }
}

private val INVALID_CONTENT_TYPES = setOf(
    "text/",
    "image/",
    "application/json",
    "application/xml",
    "application/xhtml+xml",
    "application/rss+xml",
    "application/atom+xml",
    "application/x-www-form-urlencoded",
    "application/javascript",
    "application/pdf",
)

// Things smaller than 150kbs are suspect, probably text, xml or html error pages
private const val SUSPICIOUS_FILE_SIZE = (150 * 1024).toLong()
