package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.os.Handler
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.audio.AudioCapabilities
import com.google.android.exoplayer2.audio.AudioRendererEventListener
import com.google.android.exoplayer2.audio.AudioSink
import com.google.android.exoplayer2.audio.DefaultAudioSink
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector

class ShiftyRenderersFactory(context: Context?, statsManager: StatsManager, private var boostVolume: Boolean) : DefaultRenderersFactory(context!!) {
    private var playbackSpeed = 0f
    private var internalRenderer: ShiftyAudioRendererV2? = null
    private var audioSink: AudioSink? = null
    private var processorChain: ShiftyAudioProcessorChain? = null
    private val customAudio = ShiftyCustomAudio(statsManager)

    fun setRemoveSilence(trimMode: TrimMode) {
        processorChain?.applyTrimModeForNextUpdate(trimMode)
        audioSink?.skipSilenceEnabled = trimMode != TrimMode.OFF
    }

    fun setBoostVolume(boostVolume: Boolean) {
        this.boostVolume = boostVolume
        internalRenderer?.customAudio?.setBoostVolume(boostVolume)
    }

    fun setPlaybackSpeed(playbackSpeed: Float) {
        this.playbackSpeed = playbackSpeed
        internalRenderer?.customAudio?.playbackSpeed = playbackSpeed
    }

    override fun buildAudioSink(context: Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean, enableOffload: Boolean): AudioSink {
        processorChain = ShiftyAudioProcessorChain(customAudio)
        return DefaultAudioSink.Builder()
            .setAudioCapabilities(AudioCapabilities.getCapabilities(context))
            .setAudioProcessorChain(processorChain!!)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setOffloadMode(if (enableOffload) DefaultAudioSink.OFFLOAD_MODE_ENABLED_GAPLESS_REQUIRED else DefaultAudioSink.OFFLOAD_MODE_DISABLED)
            .build()
    }

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>
    ) {
        this.audioSink = audioSink
        internalRenderer = ShiftyAudioRendererV2(
            customAudio,
            context,
            mediaCodecSelector,
            eventHandler,
            eventListener,
            audioSink
        ).apply {
            customAudio.setBoostVolume(boostVolume)
            customAudio.playbackSpeed = playbackSpeed
            out.add(this)
        }
    }

    fun onAudioSessionId(audioSessionId: Int) {
        customAudio.setupVolumeBoost(audioSessionId)
    }
}
