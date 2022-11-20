package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.media.MediaCodec
import android.os.Handler
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer
import com.google.android.exoplayer2.mediacodec.MediaCodecAdapter
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.util.MediaClock
import java.nio.ByteBuffer

class ShiftyAudioRendererV2(
    val customAudio: ShiftyCustomAudio,
    context: Context,
    mediaCodecSelector: MediaCodecSelector,
    eventHandler: Handler?,
    eventListener: AudioRendererEventListener?,
    audioSink: AudioSink,
) : MediaCodecAudioRenderer(context, mediaCodecSelector, eventHandler, eventListener, audioSink), MediaClock {

    private var lastSeenBufferIndex = -1
    private var lastPresentationTimeUs = 0L

    override fun setPlaybackParameters(playbackParameters: PlaybackParameters) {
        super.setPlaybackParameters(playbackParameters)
    }

    @Throws(ExoPlaybackException::class)
    override fun processOutputBuffer(positionUs: Long, elapsedRealtimeUs: Long, codec: MediaCodecAdapter?, buffer: ByteBuffer?, bufferIndex: Int, bufferFlags: Int, sampleCount: Int, bufferPresentationTimeUs: Long, isDecodeOnlyBuffer: Boolean, isLastBuffer: Boolean, format: Format): Boolean {
        if ((isDecodeOnlyBuffer || shouldUseBypass(format)) && bufferFlags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferIndex, bufferFlags, sampleCount, bufferPresentationTimeUs, isDecodeOnlyBuffer, isLastBuffer, format)
        }

        val haveSeenBufferBefore = lastSeenBufferIndex == bufferIndex
        lastSeenBufferIndex = bufferIndex
        if (haveSeenBufferBefore) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferIndex, bufferFlags, sampleCount, bufferPresentationTimeUs, isDecodeOnlyBuffer, isLastBuffer, format)
        }

        // reset lastPresentationTimeUs when a skip back or forward occurs
        if (lastPresentationTimeUs > bufferPresentationTimeUs || bufferPresentationTimeUs - lastPresentationTimeUs > 100000) {
            lastPresentationTimeUs = 0
        }

        val currentSegmentTimeUs = if (lastPresentationTimeUs > 0) bufferPresentationTimeUs - lastPresentationTimeUs else 0
        lastPresentationTimeUs = bufferPresentationTimeUs

        if (customAudio.playbackSpeed == 1f) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferIndex, bufferFlags, sampleCount, bufferPresentationTimeUs, isDecodeOnlyBuffer, isLastBuffer, format)
        }

        val renderedBeforeCall = decoderCounters.renderedOutputBufferCount
        val returnVal = super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferIndex, bufferFlags, sampleCount, bufferPresentationTimeUs, isDecodeOnlyBuffer, isLastBuffer, format)
        if (decoderCounters.renderedOutputBufferCount > renderedBeforeCall) {
            customAudio.addVariableSpeedTime(currentSegmentTimeUs, customAudio.playbackSpeed)
        }

        return returnVal
    }
}