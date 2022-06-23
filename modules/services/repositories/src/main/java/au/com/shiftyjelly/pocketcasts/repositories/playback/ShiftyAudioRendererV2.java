package au.com.shiftyjelly.pocketcasts.repositories.playback;

import android.content.Context;
import android.media.MediaCodec;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.audio.AudioRendererEventListener;
import com.google.android.exoplayer2.audio.AudioSink;
import com.google.android.exoplayer2.audio.MediaCodecAudioRenderer;
import com.google.android.exoplayer2.mediacodec.MediaCodecAdapter;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.util.MediaClock;

import java.nio.ByteBuffer;

public class ShiftyAudioRendererV2 extends MediaCodecAudioRenderer implements MediaClock {
    private final ShiftyCustomAudio customAudio;

    public ShiftyAudioRendererV2(ShiftyCustomAudio customAudio, Context context, MediaCodecSelector mediaCodecSelector, @Nullable Handler eventHandler, @Nullable AudioRendererEventListener eventListener, AudioSink audioSink) {
        super(context, mediaCodecSelector, eventHandler, eventListener, audioSink);
        this.customAudio = customAudio;
    }

    private int lastSeenBufferIndex = -1;
    private long lastPresentationTimeUs = 0;

    @Override
    public void setPlaybackParameters(PlaybackParameters playbackParameters) {
        super.setPlaybackParameters(playbackParameters);
    }

    @Override
    protected boolean processOutputBuffer(long positionUs, long elapsedRealtimeUs, @Nullable MediaCodecAdapter codec, @Nullable ByteBuffer buffer, int bufferIndex, int bufferFlags, int sampleCount, long bufferPresentationTimeUs, boolean isDecodeOnlyBuffer, boolean isLastBuffer, Format format) throws ExoPlaybackException {
        if (isDecodeOnlyBuffer || (shouldUseBypass(format) && (bufferFlags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0)) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferIndex, bufferFlags, sampleCount, bufferPresentationTimeUs, isDecodeOnlyBuffer, isLastBuffer, format);
        }

        final boolean haveSeenBufferBefore = lastSeenBufferIndex == bufferIndex;
        lastSeenBufferIndex = bufferIndex;
        if (haveSeenBufferBefore) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferIndex, bufferFlags, sampleCount, bufferPresentationTimeUs, isDecodeOnlyBuffer, isLastBuffer, format);
        }

        //reset lastPresentationTimeUs when a skip back or forward occurs
        if ((lastPresentationTimeUs > bufferPresentationTimeUs) || (bufferPresentationTimeUs - lastPresentationTimeUs > 100000)) {
            lastPresentationTimeUs = 0;
        }

        final long currentSegmentTimeUs = (lastPresentationTimeUs > 0) ? bufferPresentationTimeUs - lastPresentationTimeUs : 0;
        lastPresentationTimeUs = bufferPresentationTimeUs;

        if (customAudio.getPlaybackSpeed() == 1) {
            return super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferIndex, bufferFlags, sampleCount, bufferPresentationTimeUs, isDecodeOnlyBuffer, isLastBuffer, format);
        }

        final int renderedBeforeCall = decoderCounters.renderedOutputBufferCount;
        final boolean returnVal = super.processOutputBuffer(positionUs, elapsedRealtimeUs, codec, buffer, bufferIndex, bufferFlags, sampleCount, bufferPresentationTimeUs, isDecodeOnlyBuffer, isLastBuffer, format);
        if (decoderCounters.renderedOutputBufferCount > renderedBeforeCall) {
            customAudio.addVariableSpeedTime(currentSegmentTimeUs, customAudio.getPlaybackSpeed());
        }

        return returnVal;
    }

    public ShiftyCustomAudio getCustomAudio() {
        return customAudio;
    }
}
