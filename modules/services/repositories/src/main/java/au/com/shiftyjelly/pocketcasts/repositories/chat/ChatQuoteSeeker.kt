package au.com.shiftyjelly.pocketcasts.repositories.chat

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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.withTimeoutOrNull

@Singleton
class ChatQuoteSeeker @Inject constructor(
    private val transcriptManager: TranscriptManager,
    private val fingerprintTimingManager: Lazy<FingerprintTimingManager>,
    private val appPlatform: AppPlatform,
    private val decodePolicy: FingerprintDecodePolicy,
) {
    suspend fun seekTimeMs(episode: BaseEpisode, quote: ChatMessage.Quote): Int {
        val quoteMs = loadTextTranscript(episode.uuid, isGenerated = quote.isFromGeneratedTranscript)
            ?.findQuoteStartMs(quote)
            ?: quote.startMs
        if (!isFingerprintSeekEnabled()) return quoteMs

        val referenceMs = if (quote.isFromGeneratedTranscript) {
            quoteMs
        } else {
            loadTextTranscript(episode.uuid, isGenerated = true)?.findQuoteStartMs(quote) ?: return quoteMs
        }
        return playbackTime(episode, referenceMs.milliseconds)?.inWholeMilliseconds?.toInt() ?: quoteMs
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

    private fun Transcript.Text.findQuoteStartMs(quote: ChatMessage.Quote): Int? {
        return entries.findQuoteTiming(quote.text, hintMs = quote.startMs.toLong())?.startMs?.toInt()
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
