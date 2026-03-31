package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.Uri
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer.DecoderInitializationException
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class PlaybackErrorClassificationTest {

    private val errorClassifier = PlaybackErrorClassifier()

    @Test
    fun `HTTP 401 maps to access denied`() {
        val event = createHttpErrorEvent(401)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_streaming_access_denied, stringId)
    }

    @Test
    fun `HTTP 403 maps to access denied`() {
        val event = createHttpErrorEvent(403)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_streaming_access_denied, stringId)
    }

    @Test
    fun `HTTP 404 maps to not found`() {
        val event = createHttpErrorEvent(404)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_streaming_not_found, stringId)
    }

    @Test
    fun `HTTP 410 maps to no longer available`() {
        val event = createHttpErrorEvent(410)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_streaming_no_longer_available, stringId)
    }

    @Test
    fun `HTTP 500 maps to server error`() {
        val event = createHttpErrorEvent(500)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_streaming_server_error, stringId)
    }

    @Test
    fun `HTTP 503 maps to server error`() {
        val event = createHttpErrorEvent(503)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_streaming_server_error, stringId)
    }

    @Test
    fun `generic HTTP error maps to try downloading`() {
        val event = createHttpErrorEvent(418)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_streaming_try_downloading, stringId)
    }

    @Test
    fun `HttpDataSourceException maps to check internet`() {
        val cause = HttpDataSource.HttpDataSourceException(
            "Connection failed",
            DataSpec(Uri.parse("https://example.com/audio.mp3")),
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            HttpDataSource.HttpDataSourceException.TYPE_OPEN,
        )
        val error = PlaybackException(
            "Source error",
            cause,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        )
        val event = PlayerEvent.PlayerError("Connection failed", error)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.player_play_failed_check_internet, stringId)
    }

    @Test
    fun `decoder initialization error maps to decoder initialization`() {
        val cause = DecoderInitializationException(
            Format.Builder().setSampleMimeType("audio/unknown").build(),
            null,
            false,
            0,
        )
        val error = PlaybackException(
            "Decoder init failed",
            cause,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        )
        val event = PlayerEvent.PlayerError("Decoder init failed", error)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_decoder_initialization, stringId)
    }

    @Test
    fun `audio track init failed maps to audio output`() {
        val error = PlaybackException(
            "Audio track init failed",
            null,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED,
        )
        val event = PlayerEvent.PlayerError("Audio track init failed", error)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_audio_output, stringId)
    }

    @Test
    fun `audio track write failed maps to audio output`() {
        val error = PlaybackException(
            "Audio track write failed",
            null,
            PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED,
        )
        val event = PlayerEvent.PlayerError("Audio track write failed", error)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_audio_output, stringId)
    }

    @Test
    fun `decoding format unsupported maps to playing format`() {
        val error = PlaybackException(
            "Format unsupported",
            null,
            PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        )
        val event = PlayerEvent.PlayerError("Format unsupported", error)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_playing_format, stringId)
    }

    @Test
    fun `unrecognized input format maps to playing format`() {
        val cause = UnrecognizedInputFormatException(
            "Unrecognized format",
            Uri.parse("https://example.com/audio.mp3"),
            emptyList(),
        )
        val error = PlaybackException(
            "Source error",
            cause,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
        )
        val event = PlayerEvent.PlayerError("Unrecognized format", error)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_playing_format, stringId)
    }

    @Test
    fun `null error maps to unable to play`() {
        val event = PlayerEvent.PlayerError("Unknown error", null)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_unable_to_play, stringId)
    }

    @Test
    fun `unknown error code maps to unable to play`() {
        val error = PlaybackException(
            "Unknown",
            null,
            PlaybackException.ERROR_CODE_UNSPECIFIED,
        )
        val event = PlayerEvent.PlayerError("Unknown", error)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_unable_to_play, stringId)
    }

    @Test
    fun `remote error maps to unable to cast`() {
        val error = PlaybackException(
            "Cast playback error",
            null,
            PlaybackException.ERROR_CODE_REMOTE_ERROR,
        )
        val event = PlayerEvent.PlayerError("Cast playback error", error)
        val stringId = errorClassifier.classifyErrorStringId(event)
        assertEquals(LR.string.error_unable_to_cast, stringId)
    }

    @Test
    fun `HttpDataSourceException is classified as connection error`() {
        val cause = HttpDataSource.HttpDataSourceException(
            "Connection failed",
            DataSpec(Uri.parse("https://example.com/audio.mp3")),
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            HttpDataSource.HttpDataSourceException.TYPE_OPEN,
        )
        val error = PlaybackException(
            "Source error",
            cause,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        )
        val event = PlayerEvent.PlayerError("Connection failed", error)
        assertTrue(errorClassifier.isConnectionError(event))
    }

    @Test
    fun `InvalidResponseCodeException is not classified as connection error`() {
        val event = createHttpErrorEvent(404)
        assertFalse(errorClassifier.isConnectionError(event))
    }

    @Test
    fun `HTTP 401 is not classified as connection error`() {
        val event = createHttpErrorEvent(401)
        assertFalse(errorClassifier.isConnectionError(event))
    }

    @Test
    fun `null error is not classified as connection error`() {
        val event = PlayerEvent.PlayerError("Unknown error", null)
        assertFalse(errorClassifier.isConnectionError(event))
    }

    @Test
    fun `non-network error is not classified as connection error`() {
        val error = PlaybackException(
            "Decoder init failed",
            null,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        )
        val event = PlayerEvent.PlayerError("Decoder init failed", error)
        assertFalse(errorClassifier.isConnectionError(event))
    }

    private fun createHttpErrorEvent(responseCode: Int): PlayerEvent.PlayerError {
        val cause = HttpDataSource.InvalidResponseCodeException(
            responseCode,
            null,
            null,
            emptyMap(),
            DataSpec(Uri.parse("https://example.com/audio.mp3")),
            ByteArray(0),
        )
        val error = PlaybackException(
            "HTTP error $responseCode",
            cause,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        )
        return PlayerEvent.PlayerError("HTTP error $responseCode", error)
    }
}
