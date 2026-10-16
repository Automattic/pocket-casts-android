package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.media.MediaCodec
import androidx.media3.common.ParserException
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.ExoPlaybackException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class DecoderLossTest {

    @Test
    fun `resources reclaimed error code is a lost decoder`() {
        val error = playbackException(cause = null, errorCode = PlaybackException.ERROR_CODE_DECODING_RESOURCES_RECLAIMED)

        assertTrue(error.isDecoderLost())
    }

    @Test
    fun `nested fatal codec exception is a lost decoder`() {
        val codecException = codecException(actionCode = ACTION_FATAL)
        val error = playbackException(cause = RuntimeException(IllegalStateException(codecException)))

        assertTrue(error.isDecoderLost())
    }

    @Test
    fun `recoverable codec exception is a lost decoder`() {
        val error = playbackException(cause = codecException(actionCode = ACTION_RECOVERABLE))

        assertTrue(error.isDecoderLost())
    }

    @Test
    fun `transient codec exception is not a lost decoder`() {
        val error = playbackException(cause = codecException(actionCode = ACTION_TRANSIENT))

        assertFalse(error.isDecoderLost())
    }

    @Test
    fun `illegal state thrown by MediaCodec is a lost decoder`() {
        val released = IllegalStateException("flush() is valid only at Executing states; currently at Released state")
        released.stackTrace = arrayOf(StackTraceElement(MediaCodec::class.java.name, "native_flush", null, -2))

        assertTrue(playbackException(cause = released).isDecoderLost())
    }

    @Test
    fun `illegal state thrown elsewhere is not a lost decoder`() {
        val error = playbackException(cause = IllegalStateException("Unexpected"))

        assertFalse(error.isDecoderLost())
    }

    @Test
    fun `http error is not a lost decoder`() {
        val httpError = HttpDataSource.InvalidResponseCodeException(500, null, null, emptyMap(), DataSpec.Builder().setUri("https://example.com").build(), ByteArray(0))

        assertFalse(playbackException(cause = httpError).isDecoderLost())
    }

    @Test
    fun `parser error is not a lost decoder`() {
        val error = playbackException(cause = ParserException.createForMalformedContainer("Malformed", null))

        assertFalse(error.isDecoderLost())
    }

    @Test
    fun `cyclic cause chain terminates`() {
        val first = RuntimeException("first")
        val second = RuntimeException("second", first)
        first.initCause(second)

        assertFalse(playbackException(cause = first).isDecoderLost())
    }

    @Test
    fun `codec exception deeper than the depth limit is ignored`() {
        val deep = (1..10).fold<Int, Throwable>(codecException(actionCode = ACTION_FATAL)) { cause, _ -> RuntimeException(cause) }

        assertFalse(playbackException(cause = deep).isDecoderLost())
    }

    private fun playbackException(
        cause: Throwable?,
        errorCode: Int = PlaybackException.ERROR_CODE_DECODING_FAILED,
    ): PlaybackException {
        return ExoPlaybackException.createForUnexpected(RuntimeException(cause), errorCode)
    }

    private fun codecException(actionCode: Int): MediaCodec.CodecException {
        val constructor = MediaCodec.CodecException::class.java.getDeclaredConstructor(
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType,
            String::class.java,
        )
        constructor.isAccessible = true
        return constructor.newInstance(MediaCodec.CodecException.ERROR_RECLAIMED, actionCode, "Released by resource manager")
    }

    private companion object {
        const val ACTION_FATAL = 0
        const val ACTION_TRANSIENT = 1
        const val ACTION_RECOVERABLE = 2
    }
}
