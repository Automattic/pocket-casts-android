package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.annotation.OptIn
import androidx.annotation.StringRes
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.StuckPlayerException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer.DecoderInitializationException
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class PlaybackErrorClassifier @Inject constructor() {

    fun classifyHelpUrl(responseCode: Int?): String {
        return when (responseCode) {
            401, 403 -> Settings.INFO_EPISODE_ACCESS_ISSUES_URL
            404, 410 -> Settings.INFO_EPISODE_NOT_FOUND_URL
            in 500..599, 400, 405, 408, 409, 429 -> Settings.INFO_EPISODE_SERVER_PROBLEM_URL
            else -> Settings.INFO_DOWNLOAD_AND_PLAYBACK_URL
        }
    }

    @OptIn(UnstableApi::class)
    @StringRes
    fun classifyErrorStringId(event: PlayerEvent.PlayerError): Int {
        val error = event.error ?: return LR.string.error_unable_to_play
        val cause = error.cause

        return when {
            cause is StuckPlayerException -> classifyStuckError(cause.stuckType)

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

    @OptIn(UnstableApi::class)
    @StringRes
    private fun classifyStuckError(stuckType: Int): Int {
        return when (stuckType) {
            StuckPlayerException.STUCK_BUFFERING_NOT_LOADING,
            StuckPlayerException.STUCK_BUFFERING_NO_PROGRESS,
            -> LR.string.player_play_failed_check_internet

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
