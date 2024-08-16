package au.com.shiftyjelly.pocketcasts.sharing

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.sharing.clip.Clip
import au.com.shiftyjelly.pocketcasts.sharing.ui.CardType
import au.com.shiftyjelly.pocketcasts.sharing.ui.VisualCardType
import au.com.shiftyjelly.pocketcasts.utils.toSecondsWithSingleMilli
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
    private val sessionFiles = LinkedHashSet<File>()

    override suspend fun clipAudio(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range): Result<File> = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "${sanitizedFileName(podcast, episode, clipRange)}.mp3")
        if (outputFile in sessionFiles && outputFile.exists()) {
            return@withContext Result.success(outputFile)
        }
        createMp3Clip(episode, clipRange, addCover = true)
            .mapCatching { file ->
                if (!file.renameTo(outputFile)) {
                    throw IOException("Failed to rename clip file to output file")
                }
                outputFile
            }
            .onSuccess { sessionFiles.add(it) }
    }

    override suspend fun clipVideo(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range, cardType: VisualCardType, backgroundFile: File): Result<File> = withContext(Dispatchers.IO) {
        val outputFile = File(context.cacheDir, "${sanitizedFileName(podcast, episode, clipRange, cardType)}.mp4")
        if (outputFile in sessionFiles && outputFile.exists()) {
            return@withContext Result.success(outputFile)
        }

        val ffmpegFile = File(context.cacheDir, "ffmpeg-video-clip.mp4")
        createMp3Clip(episode, clipRange, addCover = false)
            .mapCatching { clipFile ->
                val command = buildString {
                    append("-r 10 ") // Set frame rate to 10 to allow sub-second clips
                    append("-t ${clipRange.duration.toSecondsWithSingleMilli()} ") // Duration must be in S.xxx or HH:MM:SS.xxx format
                    append("-i $backgroundFile ") // Image stream source
                    append("-i $clipFile ") // Audio stream source
                    append("-c:a aac ") // Use aac audio codec, it helps with Quick Time which doesn't handle libmp3lame
                    append("-c:v mpeg4 ") // Use mpeg4 video codec
                    append("-q:v 1 ") // Use the highest video quality
                    append("-pix_fmt yuv420p ") // Use 4:2:0 pixel format
                    val loopCount = (clipRange.duration.inWholeMilliseconds / 100 - 1).coerceAtLeast(1)
                    append("-vf \"loop=$loopCount:1\" ") // Loop the first frame to match clip's length
                    append("-movflags faststart ") // Move metadata to the file's start for faster playback
                    append("-y ") // Overwrite output file if it already exists
                    append("$ffmpegFile") // Output file
                }
                executeAsyncCommand(command).getOrThrow()
            }
            .map {
                if (!ffmpegFile.renameTo(outputFile)) {
                    throw IOException("Failed to rename clip file to output file")
                }
                outputFile
            }
            .onSuccess { sessionFiles.add(it) }
    }

    private suspend fun createMp3Clip(
        episode: PodcastEpisode,
        clipRange: Clip.Range,
        addCover: Boolean,
    ): Result<File> = withContext(Dispatchers.IO) {
        val ffmpegFile = File(context.cacheDir, "ffmpeg-audio-clip.mp3")
        val command = buildString {
            val audioSource = episode.downloadedFilePath?.let(::File)?.takeIf(File::exists)?.toString() ?: episode.downloadUrl
            if (audioSource == null) {
                return@withContext Result.failure(IOException("No stream found for ${episode.uuid} episode"))
            }
            val isWebSource = audioSource == episode.downloadUrl

            append("-ss ${clipRange.start.toSecondsWithSingleMilli()} ") // Clip start, must be in S.xxx or HH:MM:SS.xxx format
            append("-to ${clipRange.end.toSecondsWithSingleMilli()} ") // Clip end, must be in S.xxx or HH:MM:SS.xxx format
            if (isWebSource) {
                append("-user_agent 'Pocket Casts' ") // Set User-Agent header so we don't get blocked by hosts
            }
            append("-i $audioSource ") // Audio stream source
            val coverFile = if (addCover) {
                createJpegCover(episode) // Convert covers to JPEG because MP3 doesn't support embedding WebP
            } else {
                null
            }
            if (coverFile != null) {
                append("-i $coverFile ") // Include cover in the MP3 file
            }
            append("-q:a 0 ") // Max audio quality
            append("-map 0:a:0 ") // Include only the first audio stream, videos can have multiple audio streams
            if (coverFile != null) {
                append("-map 1:0 ") // Include the cover stream
                append("-c:1 copy ") // Copy codec for the cover stream
            }
            append("-y ") // Overwrite output file if it already exists
            append("$ffmpegFile") // Output file
        }
        executeAsyncCommand(command).map { ffmpegFile }
    }

    private suspend fun createJpegCover(episode: PodcastEpisode): File? {
        val outputFile = File(context.cacheDir, "ffmpeg-converted-cover.jpeg")
        val coverPath = episode.imageUrl ?: "${BuildConfig.SERVER_STATIC_URL}/discover/images/960/${episode.podcastUuid}.jpg"
        val command = "-user_agent 'Pocket Casts' -i $coverPath -y $outputFile"
        return executeAsyncCommand(command).map { outputFile }.getOrNull()
    }

    private fun sanitizedFileName(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range): String {
        return "${podcast.title} - ${episode.title} - ${clipRange.start.toSecondsWithSingleMilli()}–${clipRange.end.toSecondsWithSingleMilli()}".replace("""\W+""".toRegex(), "_")
    }

    private fun sanitizedFileName(podcast: Podcast, episode: PodcastEpisode, clipRange: Clip.Range, cardType: VisualCardType): String {
        return "${podcast.title} - ${episode.title} - ${clipRange.start.toSecondsWithSingleMilli()}–${clipRange.end.toSecondsWithSingleMilli()}-${cardType.id}".replace("""\W+""".toRegex(), "_")
    }

    private val VisualCardType.id get() = when (this) {
        CardType.Horizontal -> "h"
        CardType.Square -> "s"
        CardType.Vertical -> "v"
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
