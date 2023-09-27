package au.com.shiftyjelly.pocketcasts.repositories.download.task

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.system.ErrnoException
import android.system.OsConstants
import androidx.hilt.work.HiltWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.Worker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.preferences.Settings.NotificationId
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadCallFactory
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadRequestBuilder
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadProgressUpdate
import au.com.shiftyjelly.pocketcasts.repositories.download.ResponseValidationResult
import au.com.shiftyjelly.pocketcasts.repositories.download.toData
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.anyMessageContains
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.await
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.io.RandomAccessFile
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Provider
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private class UnderscoreInHostName : Exception("Download URL is invalid, as it contains an underscore in the hostname. Please contact the podcast author to resolve this.")

@HiltWorker
class DownloadEpisodeTask @AssistedInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    var downloadManager: DownloadManager,
    var episodeManager: EpisodeManager,
    var userEpisodeManager: UserEpisodeManager,
    @DownloadCallFactory private val callFactory: Call.Factory,
    @DownloadRequestBuilder private val requestBuilderProvider: Provider<Request.Builder>
) : Worker(context, params) {

    companion object {
        private const val MAX_RETRIES = 5

        private const val ERROR_FAILED_EPISODE = "This episode may have been moved or deleted. Contact the podcast author."
        private const val ERROR_NO_INTERNET_CONNECTION = "Unable to download podcast episode. Check your internet connection."
        private const val ERROR_SSL_HANDSHAKE = "Failed to create a secure connection to the author's server."
        private const val ERROR_DOWNLOAD_MESSAGE = "This episode may have been moved or deleted. Contact the podcast author. %s"
        private const val ERROR_FILE_NOT_FOUND = "Episode not found, the file may have been moved or deleted. Contact the podcast author."
        private const val ERROR_INVALID_URL = "Episode not available due to an error in the podcast feed. Contact the podcast author."
        private const val ERROR_NO_SPACE_LEFT = "Unable to download podcast episode. Check your storage space."

        private val INVALID_CONTENT_TYPES = arrayOf("application/xml", "text/html", "application/xhtml+xml")

        private const val HTTP_RESUME_SUPPORTED = 206

        // things smaller than 150kbs are suspect, probably text, xml or html error pages
        private const val SUSPECT_EPISODE_SIZE = (150 * 1024).toLong()

        // things smaller than 10kbs are not episodes, way too small and something has gone wrong
        private const val BAD_EPISODE_SIZE = (10 * 1024).toLong()

        // the minimum amount of time between progress reports about the download to the app
        private const val MIN_TIME_BETWEEN_UPDATE_REPORTS: Long = 500 // ms;

        const val INPUT_EPISODE_UUID = "episode_uuid"
        const val INPUT_PATH_TO_SAVE_TO = "path_to_save_to"
        const val INPUT_TEMP_PATH = "input_temp_path"
        const val OUTPUT_ERROR_MESSAGE = "error_message"
        const val OUTPUT_EPISODE_UUID = "episode_uuid"
        const val OUTPUT_CANCELLED = "cancelled"
    }

    private lateinit var episode: BaseEpisode
    private val episodeUUID: String? = inputData.getString(INPUT_EPISODE_UUID)
    private val pathToSaveTo: String? = inputData.getString(INPUT_PATH_TO_SAVE_TO)
    private val tempDownloadPath: String? = inputData.getString(INPUT_TEMP_PATH)

    private var bytesDownloadedSoFar: Long = 0
    private var bytesRemaining: Long = 0

    override fun doWork(): Result {
        if (isStopped) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Cancelling execution of $episodeUUID download because we are already stopped")
            return Result.failure()
        }

        val outputData = Data.Builder().putString(OUTPUT_EPISODE_UUID, episodeUUID).build()

        return try {
            this.episode = runBlocking { episodeManager.findEpisodeByUuid(episodeUUID!!) } ?: return Result.failure()

            if (this.episode.downloadTaskId != id.toString()) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Mismatched download task id for episode ${episode.title}. Cancelling. downloadTaskId: ${this.episode.downloadTaskId} id: $id.")
                return Result.failure()
            }

            if (this.episode.isArchived) {
                // In case the episode was archived again but the cancellation event hasn't come through yet
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Episode ${episode.title} is archived in download task. Cancelling.")
                return Result.success(Data.Builder().putBoolean(OUTPUT_CANCELLED, true).build())
            }

            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Worker Downloading episode ${episode.title} ${episode.uuid}")
            if (!Util.isWearOs(context)) {
                setForegroundAsync(createForegroundInfo())
            }

            runBlocking {
                episodeManager.updateEpisodeStatus(episode, EpisodeStatusEnum.DOWNLOADING)
            }

            download()
                .doOnNext { updateProgress(it) }
                .ignoreElements()
                .blockingAwait()

            if (!isStopped) {
                pathToSaveTo?.let {
                    episodeManager.updateDownloadFilePath(episode, it, false)
                    runBlocking {
                        episodeManager.updateEpisodeStatus(episode, EpisodeStatusEnum.DOWNLOADED)
                    }
                }

                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Downloaded episode ${episode.title} ${episode.uuid}")

                Result.success(outputData)
            } else {
                Result.failure(outputData)
            }
        } catch (e: Exception) {
            val downloadException = e.cause as? DownloadFailed
            if (isStopped) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Downloaded stopped ${episode.title} ${episode.uuid} - ${e.message}")
                Result.failure() // Don't do anything because it's already been handled outside of the task
            } else if (downloadException != null && downloadException.retry && runAttemptCount < MAX_RETRIES) {
                markAsRetry(e)
                Result.retry()
            } else {
                val errorOutputData = markAsFailed(e)
                Result.failure(errorOutputData)
            }
        }
    }

    private fun updateProgress(downloadProgressUpdate: DownloadProgressUpdate) {
        setProgressAsync(downloadProgressUpdate.toData())
    }

    private fun createForegroundInfo(): ForegroundInfo {
        val notification = downloadManager.getNotificationBuilder()
            .build()

        return ForegroundInfo(NotificationId.DOWNLOADING.value, notification)
    }

    private fun markAsRetry(e: Exception? = null) {
        runBlocking {
            val requirements = downloadManager.getRequirementsAndSetStatusAsync(episode)
            val message = e?.toString() ?: "No exception"
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Download stopped, will retry with $requirements ${episode.title} ${episode.uuid} - $message")
        }
    }

    private fun markAsFailed(e: Exception): Data {
        val downloadMessage = (e.cause as? DownloadFailed)?.message

        val outputData = Data.Builder()
            .putString(OUTPUT_ERROR_MESSAGE, downloadMessage ?: e.message)
            .putString(OUTPUT_EPISODE_UUID, episode.uuid)
            .build()

        runBlocking {
            episodeManager.updateEpisodeStatus(episode, EpisodeStatusEnum.DOWNLOAD_FAILED)
        }
        val message = if (downloadMessage.isNullOrBlank()) "Download Failed" else downloadMessage
        episodeManager.updateDownloadErrorDetails(episode, message)

        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, e, "Download failed ${episode.title} ${episode.uuid} - $message")

        return outputData
    }

    fun download(): Observable<DownloadProgressUpdate> {
        return Observable.create { emitter ->
            try {
                // check to see if they've already downloaded the full file
                val path = pathToSaveTo ?: throw Exception("Download episode path not set.")
                val fullDownloadFile = File(path)
                if (fullDownloadFile.exists() && fullDownloadFile.length() > 0) {
                    // don't try to get the files duration as in rare cases the mp3 extractor can fatally crash the app
                    if (!emitter.isDisposed) {
                        emitter.onComplete()
                    }
                } else {
                    downloadFile(tempDownloadPath!!, callFactory, 1, emitter)
                    if (!emitter.isDisposed) {
                        emitter.onComplete()
                    }
                }
            } catch (interrupt: InterruptedIOException) {
            } catch (e: Exception) {
                Timber.e(e)
                if (!emitter.isDisposed) {
                    emitter.onError(e)
                }
            }
        }
    }

    private fun downloadFile(tempDownloadPath: String, httpClient: Call.Factory, tryCount: Int, emitter: ObservableEmitter<DownloadProgressUpdate>) {
        if (emitter.isDisposed || isStopped || pathToSaveTo == null) {
            return
        }

        val episode = this.episode
        var call: Call? = null
        var response: Response? = null

        var localFileSize: Long = 0
        var previousLastModified: String? = null
        var errorMessage: String? = null
        var exception: Exception? = null
        var retry = true

        try {
            var downloadUrl = episode.downloadUrl?.toHttpUrlOrNull()
            if (downloadUrl == null && episode is UserEpisode) {
                downloadUrl = runBlocking { userEpisodeManager.getPlaybackUrl(episode).await()?.toHttpUrlOrNull() }
            }

            if (downloadUrl == null) throw IllegalStateException("Episode is missing url to download")
            if (downloadUrl.host.contains("_")) {
                throw UnderscoreInHostName()
            }

            val requestBuilder = requestBuilderProvider.get()
                .url(downloadUrl)
                .header("User-Agent", "Pocket Casts")

            // check to see if they've tried to download this episode before
            val tempDownloadFile = File(tempDownloadPath)
            val tempDownloadMetaDataFile = File(tempDownloadPath + "_meta")
            if (tempDownloadFile.exists() && tempDownloadMetaDataFile.exists()) {
                localFileSize = tempDownloadFile.length()
                previousLastModified = readSingleLineStringFromFile(tempDownloadMetaDataFile)
            }

            if (emitter.isDisposed || isStopped) {
                return
            }

            if (localFileSize > 0 && previousLastModified != null) {
                // server must support partial content for resume
                requestBuilder.header("Range", "bytes=$localFileSize-")
                val request = requestBuilder
                    .header("If-Range", previousLastModified)
                    .header("Range", "bytes=$localFileSize-")
                    .header("Accept-Encoding", "identity")
                    .build()
                call = httpClient.newCall(request)
                response = call.blockingEnqueue()

                if (response.code != HTTP_RESUME_SUPPORTED) {
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Resuming ${episode.title} not supported, restarting download.")
                    localFileSize = 0 // Partial not supported, start again
                    tempDownloadFile.delete()
                    tempDownloadMetaDataFile.delete()
                    call.cancel()
                    response = null
                }
            }

            if (emitter.isDisposed || isStopped) {
                return
            }

            if (response == null) {
                val request = requestBuilder.build()
                call = httpClient.newCall(request)
                response = call.blockingEnqueue()
            }

            if (emitter.isDisposed || isStopped) {
                return
            }

            // write out the last modified info for this download. Prefer the ETag if it exists, but use Last-Modified if it doesn't
            val eTag = response.header("ETag")
            if (eTag != null) {
                writeToFile(tempDownloadMetaDataFile, eTag)
            } else {
                val lastModified = response.header("Last-Modified")
                if (lastModified != null) {
                    writeToFile(tempDownloadMetaDataFile, lastModified)
                } else {
                    Timber.i("File had no last modified info, resuming not supported")
                    localFileSize = 0
                }
            }

            val validationResult = validateResponse(response)
            if (validationResult.isAlternateUrlFound) {
                return downloadFile(tempDownloadPath, httpClient, tryCount, emitter)
            }

            if (!validationResult.isValid) {
                call?.cancel()
                if (!emitter.isDisposed) {
                    emitter.onError(
                        DownloadFailed(
                            null,
                            validationResult.errorMessage
                                ?: "",
                            false
                        )
                    )
                }
                return
            }

            response.body?.use { body ->
                bytesRemaining = body.contentLength()
                if (bytesRemaining <= 0) {
                    // okhttp can return -1 if unknown so try to find it manually
                    val contentLength = response.header("Content-Length", null)
                    if (contentLength != null) {
                        try {
                            bytesRemaining = java.lang.Long.parseLong(contentLength)
                        } catch (nfe: NumberFormatException) {
                        }
                    }
                }
                val contentType = body.contentType()

                // basic sanity checks to make sure the file looks big enough and it's content type isn't text
                if (bytesRemaining > 0 && bytesRemaining < BAD_EPISODE_SIZE || bytesRemaining > 0 && bytesRemaining < SUSPECT_EPISODE_SIZE && contentType != null && contentType.toString()
                    .lowercase().contains("text")
                ) {
                    if (!emitter.isDisposed) {
                        emitter.onError(DownloadFailed(FileNotFoundException(), "File not found. The podcast author may have moved or deleted this episode file.", false))
                    }
                    return
                }

                body.byteStream().use { inputStream ->
                    RandomAccessFile(tempDownloadFile, "rw").use { outFile ->
                        if (localFileSize == 0L) {
                            outFile.setLength(0) // if we're starting a download again, make sure we zero out the file
                        }
                        outFile.seek(localFileSize)

                        var lastReportedProgressTime = System.currentTimeMillis()
                        val buffer = ByteArray(8192)
                        bytesDownloadedSoFar = localFileSize

                        var bytes = inputStream.read(buffer)
                        while (bytes >= 0) {
                            outFile.write(buffer, 0, bytes)
                            bytes = inputStream.read(buffer)

                            if (emitter.isDisposed || isStopped) {
                                return
                            }

                            bytesDownloadedSoFar += bytes.toLong()
                            bytesRemaining -= bytes.toLong()
                            if (System.currentTimeMillis() - lastReportedProgressTime > MIN_TIME_BETWEEN_UPDATE_REPORTS) {
                                try {
                                    fireProgressUpdate(emitter)
                                } catch (e: Exception) {
                                    Timber.e(e)
                                }

                                lastReportedProgressTime = System.currentTimeMillis()
                            }
                        }

                        if (emitter.isDisposed || isStopped) {
                            return
                        }

                        // check to see the file on the file system is the right size
                        val bytesRequired = bytesRemaining + localFileSize
                        if (bytesRemaining > 0 && bytesRequired > tempDownloadFile.length()) {
                            if (!emitter.isDisposed) {
                                emitter.onError(DownloadFailed(null, "Download failed, only part of the episode was downloaded", true))
                            }
                            return
                        }
                    }

                    tempDownloadMetaDataFile.delete() // at this point we have the file, don't need the metadata about it anymore
                    val fullDownloadFile = File(pathToSaveTo)
                    try {
                        FileUtil.copyFile(tempDownloadFile, fullDownloadFile)
                    } catch (exception: IOException) {
                        LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, exception, "Could not move download temp ${tempDownloadFile.path} to $pathToSaveTo. SavePathFileExists: ${fullDownloadFile.exists()}")

                        // the move failed, so delete the temp file
                        if (tempDownloadFile.exists()) {
                            tempDownloadFile.delete()
                        }

                        if (!emitter.isDisposed) {
                            emitter.onError(DownloadFailed(null, "An error occurred saving your download. Try again, if the error persists there might be an issue with your device.", true))
                        }
                        return
                    }

                    tempDownloadFile.delete()

                    if (episode.sizeInBytes != fullDownloadFile.length()) {
                        episodeManager.updateSizeInBytes(episode, fullDownloadFile.length())
                    }

                    if (!emitter.isDisposed) {
                        emitter.onComplete()
                    }
                    fixMissingDuration()
                    fixInvalidContentType(body.contentType())
                    return
                }
            }
        } catch (e: SocketTimeoutException) {
            exception = e
            errorMessage = createErrorMessage(e, "The podcast author's server timed out.")
            retry = true
        } catch (e: InterruptedIOException) {
            return // Cancelled
        } catch (e: IllegalArgumentException) {
            exception = e
            errorMessage = ERROR_INVALID_URL
            retry = false
        } catch (e: RuntimeException) {
            exception = e
            errorMessage = createErrorMessage(e)
            retry = true
        } catch (e: SocketException) {
            exception = e
            errorMessage = if (e.anyMessageContains("chtbl.com")) {
                context.resources.getString(LR.string.error_chartable)
            } else {
                createErrorMessage(e)
            }
            retry = true
        } catch (e: UnknownHostException) {
            exception = e
            errorMessage = ERROR_NO_INTERNET_CONNECTION
            retry = true
        } catch (e: SSLHandshakeException) {
            exception = e
            errorMessage = ERROR_SSL_HANDSHAKE
            retry = true
        } catch (e: UnderscoreInHostName) {
            exception = e
            errorMessage = e.message
            retry = false
        } catch (e: IOException) {
            exception = e
            errorMessage = ERROR_NO_INTERNET_CONNECTION
            retry = true
            if (e.cause is ErrnoException) {
                val erroNo = (e.cause as ErrnoException).errno
                if (erroNo == OsConstants.ENOSPC) {
                    errorMessage = ERROR_NO_SPACE_LEFT
                    retry = false
                }
            }
        } catch (e: Exception) {
            exception = e
            errorMessage = createErrorMessage(e)
            retry = false
        } finally {
            call?.cancel()
        }
        if (exception != null) {
            LogBuffer.logException(LogBuffer.TAG_BACKGROUND_TASKS, exception, "Download failed %s", this.episode.downloadUrl ?: "")
        }

        if (!emitter.isDisposed) {
            emitter.onError(DownloadFailed(exception, errorMessage ?: "", retry))
        }
    }

    private fun createErrorMessage(e: Throwable): String {
        return createErrorMessage(e, ERROR_FAILED_EPISODE)
    }

    private fun createErrorMessage(e: Throwable, blankMessage: String): String {
        val message = e.message
        if (message == null) {
            return blankMessage
        } else if (message.lowercase().contains("enospc")) {
            return "You seem to be running low on space, take a look at your storage settings."
        }

        return blankMessage
    }

    private fun fixMissingDuration() {
        try {
            val pathToSaveTo = pathToSaveTo ?: return
            val extractor = MediaExtractor()
            extractor.setDataSource(pathToSaveTo)
            val numTracks = extractor.trackCount
            for (i in 0 until numTracks) {
                val format = extractor.getTrackFormat(i)
                if (!format.containsKey(MediaFormat.KEY_DURATION)) {
                    continue
                }
                val duration = format.getLong(MediaFormat.KEY_DURATION)
                if (duration <= 0) {
                    continue
                }

                val durationInSecs = (duration / 1000000).toDouble()
                episodeManager.updateDuration(episode, durationInSecs, true)

                return
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fix missing duration.")
        }
    }

    private fun fixInvalidContentType(contentType: MediaType?) {
        contentType ?: return
        if ((episode.fileType.isNullOrBlank() && (contentType.type == "audio" || contentType.type == "video")) || contentType.type == "video") {
            episodeManager.updateFileType(episode, contentType.toString())
            episode.fileType = contentType.toString()
        }
    }

    private fun validateResponse(response: Response): ResponseValidationResult {
        // check for a valid status code
        val statusCode = response.code
        if (statusCode in 400..599) {
            val responseReason = response.message
            val message = if (statusCode == 404) ERROR_FILE_NOT_FOUND else String.format(
                ERROR_DOWNLOAD_MESSAGE, if (responseReason.isBlank()) "" else "(error $statusCode $responseReason)"
            )
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Invalid response returned for episode download. ${response.code} $responseReason ${response.request.url}")
            return ResponseValidationResult(isValid = false, errorMessage = message)
        }
        // check the content type is valid
        response.header("Content-Type")?.let { contentType ->
            if (INVALID_CONTENT_TYPES.any { it == contentType }) {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Invalid content type returned for episode download. $contentType ${response.request.url}")
                return ResponseValidationResult(isValid = false, errorMessage = ERROR_FAILED_EPISODE)
            }
        }

        return ResponseValidationResult(isValid = true)
    }

    private fun fireProgressUpdate(emitter: ObservableEmitter<DownloadProgressUpdate>) {
        val downloadProgressUpdate = getDownloadProgress()
        if (!emitter.isDisposed && downloadProgressUpdate != null) {
            emitter.onNext(downloadProgressUpdate)
        }
    }

    private fun getDownloadProgress(): DownloadProgressUpdate? {
        val totalSize = (bytesRemaining + bytesDownloadedSoFar).toFloat()
        if (totalSize <= 0f) return null

        val progress = bytesDownloadedSoFar.toFloat() / totalSize
        val total = bytesRemaining + bytesDownloadedSoFar
        val podcastUuid = (episode as? PodcastEpisode)?.podcastUuid

        return DownloadProgressUpdate(
            episode.uuid,
            podcastUuid,
            null,
            progress,
            bytesDownloadedSoFar,
            total
        )
    }

    private fun readSingleLineStringFromFile(file: File): String? {
        try {
            FileInputStream(file).use { fileInputStream ->
                return getFirstLineFromStream(fileInputStream)
            }
        } catch (e: Exception) {
        }
        return null
    }

    private fun getFirstLineFromStream(inputStream: InputStream): String? {
        try {
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                return reader.readLine()
            }
        } catch (e: Exception) {
        }
        return null
    }

    private fun writeToFile(file: File, data: String) {
        try {
            FileWriter(file, false).use { writer ->
                writer.write(data)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    class DownloadFailed(val exception: Exception?, message: String, val retry: Boolean) : Exception(message)
}

/**
 * Have to use enqueue for high bandwidth requests on the watch app
 * See https://github.com/google/horologist/blob/7bd044a4766e379f85ee3f5a01272853eec3155d/network-awareness/src/main/java/com/google/android/horologist/networks/okhttp/impl/HighBandwidthCall.kt#L93-L92
 */
private fun Call.blockingEnqueue(): Response =
    runBlocking {
        suspendCoroutine { cont ->
            this@blockingEnqueue.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    cont.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    cont.resume(response)
                }
            })
        }
    }
