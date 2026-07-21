package au.com.shiftyjelly.pocketcasts.repositories.transcript

import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptService
import dagger.Lazy
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.CacheControl
import okio.Buffer
import timber.log.Timber

@Singleton
class TranscriptWindowExtractor @Inject constructor(
    private val transcriptDao: TranscriptDao,
    private val transcriptService: TranscriptService,
    private val fingerprintTimingManager: Lazy<FingerprintTimingManager>,
    private val playbackManager: Lazy<PlaybackManager>,
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
            parseVttWindow(vttContent, referenceCenterSecs(episodeUuid, timeSecs), windowSecs)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to extract transcript window for episode $episodeUuid")
            null
        }
    }

    // Generated transcripts are in the reference timeline; map the playback-time bookmark onto it.
    private suspend fun referenceCenterSecs(episodeUuid: String, timeSecs: Int): Int {
        val manager = fingerprintTimingManager.get()
        val playbackMs = timeSecs * 1000
        fun refNow() = if (manager.activeEpisodeUuid == episodeUuid) manager.referenceTime(playbackMs)?.roundToInt() else null

        refNow()?.let { return it }
        if (playbackManager.get().getCurrentEpisode()?.uuid != episodeUuid) return timeSecs

        manager.prepareForCurrentEpisode(FingerprintTimingManager.PrepareTrigger.BOOKMARK)
        val ref = withTimeoutOrNull(COVERAGE_TIMEOUT) {
            merge(
                manager.mappingVersion.map { refNow() }.filter { it != null },
                // Bail only on this episode's terminal states; the conflated flow can hold a stale one.
                manager.stateFlow
                    .filter {
                        (it is FingerprintTimingManager.State.Unavailable && it.episodeUuid == episodeUuid) ||
                            (it is FingerprintTimingManager.State.Failed && it.episodeUuid == episodeUuid)
                    }
                    .map { null },
            ).first()
        }
        return ref ?: timeSecs
    }

    companion object {
        private const val MIN_WORDS = 10

        // The tap-built map lags the playhead by a full fingerprint window, so cover that plus slack.
        private val COVERAGE_TIMEOUT = 12.seconds

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
