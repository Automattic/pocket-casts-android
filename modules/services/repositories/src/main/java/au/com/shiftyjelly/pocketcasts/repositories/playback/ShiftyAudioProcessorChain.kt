package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.audio.AudioProcessor
import com.google.android.exoplayer2.audio.DefaultAudioSink.AudioProcessorChain
import com.google.android.exoplayer2.audio.SonicAudioProcessor
import timber.log.Timber

internal class ShiftyAudioProcessorChain(private val customAudio: ShiftyCustomAudio) : AudioProcessorChain {
    private val lowProcessor = ShiftyTrimSilenceProcessor(
        this::onSkippedFrames,
        416000,
        291000,
        ShiftyTrimSilenceProcessor.DEFAULT_SILENCE_THRESHOLD_LEVEL
    )
    private val mediumProcessor =
        ShiftyTrimSilenceProcessor(
            this::onSkippedFrames,
            300000,
            225000,
            ShiftyTrimSilenceProcessor.DEFAULT_SILENCE_THRESHOLD_LEVEL
        )
    private val highProcessor = ShiftyTrimSilenceProcessor(
        this::onSkippedFrames,
        83000,
        0,
        ShiftyTrimSilenceProcessor.DEFAULT_SILENCE_THRESHOLD_LEVEL
    )
    private val sonicAudioProcessor = SonicAudioProcessor()

    private val audioProcessors = arrayOf(lowProcessor, mediumProcessor, highProcessor, sonicAudioProcessor)
    private var trimMode = TrimMode.OFF
    override fun getAudioProcessors(): Array<AudioProcessor> {
        return audioProcessors
    }

    override fun applyPlaybackParameters(playbackParameters: PlaybackParameters): PlaybackParameters {
        val speed = playbackParameters.speed
        val pitch = playbackParameters.pitch
        sonicAudioProcessor.setSpeed(speed)
        sonicAudioProcessor.setPitch(pitch)
        return PlaybackParameters(speed, pitch)
    }

    override fun applySkipSilenceEnabled(skipSilenceEnabled: Boolean): Boolean {
        for (audioProcessor in audioProcessors) {
            (audioProcessor as? ShiftyTrimSilenceProcessor)?.setEnabled(false)
        }
        if (trimMode != TrimMode.OFF) {
            val index = trimMode.ordinal - 1
            (audioProcessors[index] as ShiftyTrimSilenceProcessor).setEnabled(true)
        }
        return trimMode != TrimMode.OFF
    }

    override fun getMediaDuration(playoutDuration: Long): Long {
        return sonicAudioProcessor.getMediaDuration(playoutDuration)
    }

    override fun getSkippedOutputFrameCount(): Long {
        return lowProcessor.skippedFrames + mediumProcessor.skippedFrames + highProcessor.skippedFrames
    }

    fun applyTrimModeForNextUpdate(trimMode: TrimMode) {
        this.trimMode = trimMode
    }

    private fun onSkippedFrames(durationUs: Long) {
        if (durationUs == 0L) return
        Timber.d("Skipped ${durationUs / 1000}ms")
        customAudio.addSilenceSkippedTime(durationUs)
    }
}
