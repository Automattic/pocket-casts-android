package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.media.MediaCodec
import androidx.annotation.OptIn
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer.DecoderInitializationException

private const val MAX_CAUSE_DEPTH = 8
private const val RELEASED_CODEC_MESSAGE = "Released state"

@OptIn(UnstableApi::class)
internal fun PlaybackException.isDecoderLost(): Boolean {
    if (errorCode == PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED) {
        return true
    }
    val causes = generateSequence(cause) { throwable -> throwable.cause?.takeIf { it !== throwable } }
        .take(MAX_CAUSE_DEPTH)
        .toList()
    return causes.none { it is DecoderInitializationException } && causes.any { it.isReclaimedCodecFailure() }
}

private fun Throwable.isReclaimedCodecFailure(): Boolean {
    return when (this) {
        is MediaCodec.CodecException -> errorCode == MediaCodec.CodecException.ERROR_RECLAIMED

        is IllegalStateException -> stackTrace.firstOrNull()?.className == MediaCodec::class.java.name &&
            message.orEmpty().contains(RELEASED_CODEC_MESSAGE)

        else -> false
    }
}
