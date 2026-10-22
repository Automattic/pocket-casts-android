package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Publishes the PCM the player is already decoding, stamped with its media-time position, so
 * fingerprinting can reuse it instead of downloading and decoding the episode a second time.
 * Producer callbacks are all invoked from the playback thread.
 */
@OptIn(UnstableApi::class)
@Singleton
class FingerprintPcmTap @Inject constructor() {

    class PcmChunk(
        val data: ByteArray,
        val encoding: Int,
        val sampleRate: Int,
        val channelCount: Int,
        val positionSec: Double,
    )

    private val chunkFlow = MutableSharedFlow<PcmChunk>(
        extraBufferCapacity = CHUNK_BUFFER_CAPACITY,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val chunks: SharedFlow<PcmChunk> = chunkFlow.asSharedFlow()

    private var pendingAnchorUs = C.TIME_UNSET
    private var anchorUs = C.TIME_UNSET
    private var framesSinceAnchor = 0L

    fun onSinkBuffer(presentationTimeUs: Long) {
        pendingAnchorUs = presentationTimeUs
    }

    fun onSinkFlush() {
        pendingAnchorUs = C.TIME_UNSET
        anchorUs = C.TIME_UNSET
        framesSinceAnchor = 0
    }

    fun onPcm(buffer: ByteBuffer, format: AudioProcessor.AudioFormat) {
        val frames = buffer.remaining() / format.bytesPerFrame
        if (frames == 0) return
        val pending = pendingAnchorUs
        pendingAnchorUs = C.TIME_UNSET
        if (anchorUs == C.TIME_UNSET) {
            if (pending == C.TIME_UNSET) return
            anchorUs = pending
            framesSinceAnchor = 0
        } else if (pending != C.TIME_UNSET) {
            if (abs(pending - positionUs(format.sampleRate)) > REANCHOR_TOLERANCE_US) {
                anchorUs = pending
                framesSinceAnchor = 0
            }
        }
        val positionSec = positionUs(format.sampleRate) / 1_000_000.0
        framesSinceAnchor += frames
        if (chunkFlow.subscriptionCount.value == 0) return
        val data = ByteArray(buffer.remaining())
        buffer.duplicate().get(data)
        chunkFlow.tryEmit(
            PcmChunk(
                data = data,
                encoding = format.encoding,
                sampleRate = format.sampleRate,
                channelCount = format.channelCount,
                positionSec = positionSec,
            ),
        )
    }

    private fun positionUs(sampleRate: Int): Long = anchorUs + framesSinceAnchor * 1_000_000 / sampleRate

    companion object {
        private const val CHUNK_BUFFER_CAPACITY = 64
        private const val REANCHOR_TOLERANCE_US = 200_000L
    }
}
