package au.com.shiftyjelly.pocketcasts.sharing

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.clip.Clip
import au.com.shiftyjelly.pocketcasts.utils.toHhMmSs
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.FFmpegSessionCompleteCallback
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal class FFmpegMediaService(
    private val context: Context,
) : MediaService {
    override suspend fun clipAudio(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range): Result<File> = withContext(Dispatchers.IO) {
        val fileName = "${podcast.title} - ${episode.title} - ${clipRange.start.toHhMmSs()}–${clipRange.end.toHhMmSs()}".replace("""\W+""".toRegex(), "_")
        val outputFile = File(context.cacheDir, "$fileName.mp3")
        if (outputFile.exists()) {
            return@withContext Result.success(outputFile)
        }

        val ffmpegFile = File(context.cacheDir, "ffmpeg-audio-clip.mp3")
        val command = buildString {
            val audioSource = episode.downloadedFilePath?.let(::File)?.takeIf(File::exists)?.toString() ?: episode.downloadUrl
            if (audioSource == null) {
                return@withContext Result.failure(IOException("No stream found for ${episode.uuid} episode"))
            }

            append("-i $audioSource ") // Audio stream source
            val coverFile = convertCoverToJpeg(episode) // Convert covers to JPEG because MP3 doesn't support embedding WebP
            if (coverFile != null) {
                append("-i $coverFile ")
            }
            append("-ss ${clipRange.startInSeconds} ") // Clip start, must be in seconds or HH:MM:SS.xxx
            append("-to ${clipRange.endInSeconds} ") // Clip end, must be in seconds or HH:MM:SS.xxx
            append("-q:a 0 ") // Max audio quality
            append("-map 0:a:0 ") // Include only the first audio stream, videos can have multiple audio streams
            if (coverFile != null) {
                append("-map 1:0 ") // Include the cover stream
                append("-c:1 copy ") // Copy codec for the cover stream
            }
            append("-user_agent 'Pocket Casts'") // Add User-Agent
            append("-y ") // Overwrite output file if it already exists
            append("$ffmpegFile") // Output file
        }

        executeAsyncCommand(command).mapCatching {
            if (!ffmpegFile.renameTo(outputFile)) {
                throw IOException("Failed to rename clip file to output file")
            }
            outputFile
        }
    }

    private suspend fun convertCoverToJpeg(episode: PodcastEpisode): File? {
        val outputFile = File(context.cacheDir, "ffmpeg-converted-cover.jpeg")
        val coverPath = episode.imageUrl ?: "${BuildConfig.SERVER_STATIC_URL}/discover/images/960/${episode.podcastUuid}.jpg"
        val command = "-i $coverPath -user_agent 'Pocket Casts' -y $outputFile"
        return executeAsyncCommand(command).map { outputFile }.getOrNull()
    }

    private suspend fun executeAsyncCommand(command: String): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val session = FFmpegKit.executeAsync(
            command,
            object : FFmpegSessionCompleteCallback {
                override fun apply(session: FFmpegSession) {
                    when {
                        ReturnCode.isSuccess(session.returnCode) -> {
                            continuation.resume(Result.success(Unit))
                        }

                        ReturnCode.isCancel(session.returnCode) -> {
                            continuation.cancel()
                        }

                        else -> {
                            continuation.resume(Result.failure(IOException(session.failStackTrace)))
                        }
                    }
                }
            },
        )
        continuation.invokeOnCancellation { session.cancel() }
    }
}
