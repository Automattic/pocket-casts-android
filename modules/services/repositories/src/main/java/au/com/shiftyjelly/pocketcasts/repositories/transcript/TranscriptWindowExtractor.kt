package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.CacheControl
import okio.Buffer
import timber.log.Timber

@Singleton
class TranscriptWindowExtractor @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val transcriptService: TranscriptService,
) {
    suspend fun extractWindow(episodeUuid: String, timeSecs: Int, windowSecs: Int = 30): String? {
        return try {
            val transcripts = withTimeoutOrNull(1.minutes) {
                transcriptDao.observeTranscripts(episodeUuid)
                    .filter { it.isNotEmpty() }
                    .firstOrNull()
            }
            val generated = transcripts?.firstOrNull { it.isGenerated } ?: return null

            val body = runCatching { transcriptService.getTranscriptOrThrow(generated.url) }
                .recoverCatching { transcriptService.getTranscriptOrThrow(generated.url, CacheControl.FORCE_CACHE) }
                .getOrNull() ?: return null

            val vttContent = body.use { it.string() }
            parseVttWindow(vttContent, timeSecs, windowSecs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract transcript window for episode $episodeUuid")
            null
        }
    }

    companion object {
        private const val MIN_WORDS = 10

        internal fun parseVttWindow(content: String, timeSecs: Int, windowSecs: Int): String? {
            val entries = WebVttParser().parse(Buffer().writeUtf8(content)).getOrNull() ?: return null
            return windowText(entries, timeSecs, windowSecs)
        }

        private fun windowText(entries: List<TranscriptEntry>, timeSecs: Int, windowSecs: Int): String? {
            val windowStartMs = (timeSecs - windowSecs).coerceAtLeast(0) * 1000L
            val windowEndMs = (timeSecs + windowSecs) * 1000L

            val result = entries
                .filterIsInstance<TranscriptEntry.Text>()
                .filter { it.startTimeMs >= 0 && it.startTimeMs < windowEndMs && it.endTimeMs > windowStartMs }
                .joinToString(" ") { it.value.trim() }
                .trim()

            return result.takeIf { it.split("\\s+".toRegex()).size >= MIN_WORDS }
        }
    }
}
