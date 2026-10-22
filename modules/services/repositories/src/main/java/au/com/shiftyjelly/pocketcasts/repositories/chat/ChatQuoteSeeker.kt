package au.com.shiftyjelly.pocketcasts.repositories.chat

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.ChapterSeekResult
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.fingerprint.FingerprintDecodePolicy
import au.com.shiftyjelly.pocketcasts.utils.fingerprint.FingerprintPolicy
import dagger.Lazy
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class ChatQuoteSeeker @Inject constructor(
    private val transcriptManager: TranscriptManager,
    private val fingerprintTimingManager: Lazy<FingerprintTimingManager>,
    private val appPlatform: AppPlatform,
    private val decodePolicy: FingerprintDecodePolicy,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val pendingTranscripts = ConcurrentHashMap<Pair<String, Boolean>, Deferred<Transcript.Text?>>()

    data class QuoteTime(
        val timeMs: Int,
        val referenceTimeMs: Int?,
    )

    fun warmUpQuote(episodeUuid: String, quote: ChatMessage.Quote) {
        quoteTranscripts(episodeUuid, quote)
    }

    fun warmUpFingerprints(episodeUuid: String) {
        if (!isFingerprintSeekEnabled()) return
        fingerprintTimingManager.get().onChatQuoteShown(episodeUuid)
    }

    suspend fun quoteTime(episodeUuid: String, quote: ChatMessage.Quote): QuoteTime {
        val (sourceTranscript, generatedTranscript) = quoteTranscripts(episodeUuid, quote)
        val timeMs = sourceTranscript.await()?.findQuoteStartMs(quote, hintMs = quote.startMs) ?: quote.startMs
        val referenceTimeMs = if (quote.isFromGeneratedTranscript) {
            timeMs
        } else {
            generatedTranscript?.await()?.findQuoteStartMs(quote, hintMs = timeMs, maxHintDistanceMs = Long.MAX_VALUE)
        }
        return QuoteTime(timeMs, referenceTimeMs)
    }

    private fun quoteTranscripts(
        episodeUuid: String,
        quote: ChatMessage.Quote,
    ): Pair<Deferred<Transcript.Text?>, Deferred<Transcript.Text?>?> {
        val sourceTranscript = pendingTranscript(episodeUuid, isGenerated = quote.isFromGeneratedTranscript)
        val generatedTranscript = if (!quote.isFromGeneratedTranscript && isFingerprintSeekEnabled()) {
            pendingTranscript(episodeUuid, isGenerated = true)
        } else {
            null
        }
        return sourceTranscript to generatedTranscript
    }

    private fun pendingTranscript(episodeUuid: String, isGenerated: Boolean): Deferred<Transcript.Text?> {
        val key = episodeUuid to isGenerated
        return pendingTranscripts.computeIfAbsent(key) {
            scope.async(start = CoroutineStart.LAZY) {
                try {
                    loadTextTranscript(episodeUuid, isGenerated)
                } finally {
                    pendingTranscripts.remove(key)
                }
            }
        }.also { it.start() }
    }

    suspend fun playbackTimeMs(episode: BaseEpisode, quoteTime: QuoteTime): Int {
        val referenceTimeMs = quoteTime.referenceTimeMs?.takeIf { it >= 0 && isFingerprintSeekEnabled() }
            ?: return quoteTime.timeMs
        val playbackTime = withTimeoutOrNull(FINGERPRINT_RESOLVE_TIMEOUT) {
            playbackTime(episode, referenceTimeMs.milliseconds)
        }
        return playbackTime?.inWholeMilliseconds?.toInt() ?: quoteTime.timeMs
    }

    private suspend fun loadTextTranscript(episodeUuid: String, isGenerated: Boolean): Transcript.Text? {
        return withTimeoutOrNull(TRANSCRIPT_LOAD_TIMEOUT) {
            if (isGenerated) {
                transcriptManager.loadGeneratedTranscript(episodeUuid)
            } else {
                transcriptManager.loadTranscript(episodeUuid) as? Transcript.Text
            }
        }?.takeIf { it.isGenerated == isGenerated }
    }

    private fun Transcript.Text.findQuoteStartMs(
        quote: ChatMessage.Quote,
        hintMs: Int,
        maxHintDistanceMs: Long = QUOTE_MAX_HINT_DISTANCE_MS,
    ): Int? {
        return entries.findQuoteStartMs(quote.text, hintMs.toLong(), maxHintDistanceMs)?.toInt()
    }

    private suspend fun playbackTime(episode: BaseEpisode, referenceTime: Duration): Duration? {
        val manager = fingerprintTimingManager.get()
        manager.densePlaybackTime(episode.uuid, referenceTime)?.let { return it }
        if (decodePolicy.current() != FingerprintPolicy.PLATFORM) return null
        return (manager.resolvePlaybackTime(episode, referenceTime) as? ChapterSeekResult.Resolved)?.playbackTime
    }

    private fun isFingerprintSeekEnabled() = appPlatform == AppPlatform.Phone &&
        decodePolicy.current() != FingerprintPolicy.DISABLED &&
        FeatureFlag.isEnabled(Feature.SYNCED_TRANSCRIPTS)
}

private val TRANSCRIPT_LOAD_TIMEOUT = 5.seconds

private val FINGERPRINT_RESOLVE_TIMEOUT = 5.seconds
