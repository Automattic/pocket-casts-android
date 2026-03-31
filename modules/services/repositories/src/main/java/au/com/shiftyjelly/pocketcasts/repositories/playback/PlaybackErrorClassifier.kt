package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer.DecoderInitializationException
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class PlaybackErrorClassifier {

    @OptIn(UnstableApi::class)
    fun isConnectionError(event: PlayerEvent.PlayerError): Boolean {
        val cause = event.error?.cause
        return cause is HttpDataSource.HttpDataSourceException &&
            cause !is HttpDataSource.InvalidResponseCodeException
    }

    @OptIn(UnstableApi::class)
    @StringRes
    fun classifyErrorStringId(event: PlayerEvent.PlayerError): Int {
        val error = event.error ?: return LR.string.error_unable_to_play
        val cause = error.cause

        return when {
            cause is HttpDataSource.InvalidResponseCodeException -> classifyHttpError(cause.responseCode)

            cause is HttpDataSource.HttpDataSourceException -> LR.string.player_play_failed_check_internet

            cause is DecoderInitializationException -> LR.string.error_decoder_initialization

            cause is UnrecognizedInputFormatException -> LR.string.error_playing_format

            error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED ||
                error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED -> LR.string.error_audio_output

            error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED -> LR.string.error_playing_format

            error.errorCode == PlaybackException.ERROR_CODE_REMOTE_ERROR -> LR.string.error_unable_to_cast

            else -> LR.string.error_unable_to_play
        }
    }

    @StringRes
    fun classifyHttpError(responseCode: Int): Int {
        return when (responseCode) {
            401, 403 -> LR.string.error_streaming_access_denied
            404 -> LR.string.error_streaming_not_found
            410 -> LR.string.error_streaming_no_longer_available
            in 500..599 -> LR.string.error_streaming_server_error
            else -> LR.string.error_streaming_try_downloading
        }
    }
}
