package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptService
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.CacheControl
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

            val body = runCatching { transcriptService.getTranscriptOrThrow(generated.url, CacheControl.FORCE_CACHE) }
                .getOrNull() ?: return null

            val vttContent = body.use { it.string() }
            parseVttWindow(vttContent, timeSecs, windowSecs)
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract transcript window for episode $episodeUuid")
            null
        }
    }

    companion object {
        private val TIMESTAMP_REGEX =
            """(?:(\d{2}):)?(\d{2}):(\d{2})\.\d{3}\s*-->\s*(?:(\d{2}):)?(\d{2}):(\d{2})\.\d{3}""".toRegex()

        internal fun parseVttWindow(content: String, timeSecs: Int, windowSecs: Int): String? {
            val windowStart = (timeSecs - windowSecs).coerceAtLeast(0)
            val windowEnd = timeSecs + windowSecs
            val lines = content.lines()
            val texts = mutableListOf<String>()
            var i = 0

            while (i < lines.size) {
                val match = TIMESTAMP_REGEX.find(lines[i])
                if (match != null) {
                    val sh = match.groupValues[1].takeIf { it.isNotEmpty() }?.toInt() ?: 0
                    val sm = match.groupValues[2].toInt()
                    val ss = match.groupValues[3].toInt()
                    val eh = match.groupValues[4].takeIf { it.isNotEmpty() }?.toInt() ?: 0
                    val em = match.groupValues[5].toInt()
                    val es = match.groupValues[6].toInt()
                    val start = sh * 3600 + sm * 60 + ss
                    val end = eh * 3600 + em * 60 + es

                    if (start < windowEnd && end > windowStart) {
                        i++
                        val cueLines = mutableListOf<String>()
                        while (i < lines.size && lines[i].isNotBlank()) {
                            cueLines.add(lines[i].replace(Regex("<[^>]+>"), "").trim())
                            i++
                        }
                        val text = cueLines.joinToString(" ").trim()
                        if (text.isNotEmpty()) {
                            texts.add(text)
                        }
                    }
                }
                i++
            }

            val result = texts.joinToString(" ")
            return result.takeIf { it.split("\\s+".toRegex()).size >= 10 }
        }
    }
}
