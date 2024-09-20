package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.annotation.OptIn
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessorChain
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import timber.log.Timber

@OptIn(UnstableApi::class)
class ShiftyAudioProcessorChain(private val customAudio: ShiftyCustomAudio) : AudioProcessorChain {
    private val lowProcessor = ShiftyTrimSilenceProcessor(
        416000.microseconds,
        291000.microseconds,
        ShiftyTrimSilenceProcessor.DEFAULT_SILENCE_THRESHOLD_LEVEL,
        ::onSkippedFrames,
    )
    private val mediumProcessor =
        ShiftyTrimSilenceProcessor(
            300000.microseconds,
            225000.microseconds,
            ShiftyTrimSilenceProcessor.DEFAULT_SILENCE_THRESHOLD_LEVEL,
            ::onSkippedFrames,
        )
    private val highProcessor = ShiftyTrimSilenceProcessor(
        83000.microseconds,
        0.microseconds,
        ShiftyTrimSilenceProcessor.DEFAULT_SILENCE_THRESHOLD_LEVEL,
        ::onSkippedFrames,
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
            (audioProcessor as? ShiftyTrimSilenceProcessor)?.enabled = false
        }
        if (trimMode != TrimMode.OFF) {
            val index = trimMode.ordinal - 1
            (audioProcessors[index] as ShiftyTrimSilenceProcessor).enabled = true
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

    private fun onSkippedFrames(duration: Duration) {
        if (duration == Duration.ZERO) return
        Timber.d("Skipped ${duration.inWholeMilliseconds}ms")
        customAudio.addSilenceSkippedTime(duration.inWholeMicroseconds)
    }
}
