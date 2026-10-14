package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/**
 * Publishes the PCM the player is already decoding, stamped with its media-time position, so
 * fingerprinting can reuse it instead of downloading and decoding the episode a second time.
 * Producer callbacks are all invoked from the playback thread.
 */
@OptIn(UnstableApi::class)
@Singleton
class FingerprintPcmTap @Inject constructor() {

    /** [data] is pooled and may be longer than [size]; call [recycle] once it has been read. */
    inner class PcmChunk internal constructor(
        val data: ByteArray,
        val size: Int,
        val encoding: Int,
        val sampleRate: Int,
        val channelCount: Int,
        val positionSec: Double,
    ) {
        private val isRecycled = AtomicBoolean(false)

        fun recycle() {
            if (isRecycled.compareAndSet(false, true)) arrayPool.offer(data)
        }
    }

    private val arrayPool = ArrayBlockingQueue<ByteArray>(ARRAY_POOL_CAPACITY)
    private val consumer = AtomicReference<Channel<PcmChunk>?>(null)

    /** Single consumer: a new collector replaces the previous one, whose collection is cancelled. */
    val chunks: Flow<PcmChunk> = flow {
        val channel = Channel<PcmChunk>(
            capacity = CHUNK_BUFFER_CAPACITY,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            onUndeliveredElement = { it.recycle() },
        )
        consumer.getAndSet(channel)?.cancel()
        try {
            emitAll(channel)
        } finally {
            consumer.compareAndSet(channel, null)
            channel.cancel()
        }
    }

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

    /** Copies [buffer]'s remaining bytes without moving its position. */
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
        val channel = consumer.get() ?: return
        val size = buffer.remaining()
        val data = obtainArray(size)
        val start = buffer.position()
        buffer.get(data, 0, size)
        buffer.position(start)
        val chunk = PcmChunk(
            data = data,
            size = size,
            encoding = format.encoding,
            sampleRate = format.sampleRate,
            channelCount = format.channelCount,
            positionSec = positionSec,
        )
        if (channel.trySend(chunk).isFailure) chunk.recycle()
    }

    private fun obtainArray(size: Int): ByteArray {
        val pooled = arrayPool.poll()
        return if (pooled != null && pooled.size >= size) pooled else ByteArray(size)
    }

    private fun positionUs(sampleRate: Int): Long = anchorUs + framesSinceAnchor * 1_000_000 / sampleRate

    companion object {
        private const val CHUNK_BUFFER_CAPACITY = 64
        private const val ARRAY_POOL_CAPACITY = 8
        private const val REANCHOR_TOLERANCE_US = 200_000L
    }
}
