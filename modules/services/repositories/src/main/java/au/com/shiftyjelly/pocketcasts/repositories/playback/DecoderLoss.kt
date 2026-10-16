package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.media.MediaCodec
import androidx.media3.common.PlaybackException

private const val MAX_CAUSE_DEPTH = 8

internal fun PlaybackException.isDecoderLost(): Boolean {
    if (errorCode == PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED) {
        return true
    }
    return generateSequence(cause) { throwable -> throwable.cause?.takeIf { it !== throwable } }
        .take(MAX_CAUSE_DEPTH)
        .any { it.isLostCodecFailure() }
}

private fun Throwable.isLostCodecFailure(): Boolean {
    return when (this) {
        is MediaCodec.CodecException -> !isTransient
        is IllegalStateException -> stackTrace.firstOrNull()?.className == MediaCodec::class.java.name
        else -> false
    }
}
