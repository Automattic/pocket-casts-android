package au.com.shiftyjelly.pocketcasts.voicecontrol.audio

import kotlin.math.sqrt
import timber.log.Timber

class EnergyVoiceAudioSegmenter @javax.inject.Inject constructor() : VoiceAudioSegmenter {
    /** RMS amplitude threshold. Speech frames typically have RMS above this value. */
    private val speechThreshold: Double = 1500.0
    private val minimumSpeechFrames: Int = 1
    private val trailingSilenceFrames: Int = 4
    private val maxSpeechDurationMs: Long = 5_000
    private var cooldownUntilMs: Long = 0L
    private val frames = mutableListOf<PcmAudioFrame>()
    private var speechFrames = 0
    private var silenceFrames = 0
    private var speechStartTimeMs: Long = 0L
    private var debugSampleCount = 0

    override fun process(frame: PcmAudioFrame): VoiceSegmenterResult {
        val now = System.currentTimeMillis()
        if (now < cooldownUntilMs) return VoiceSegmenterResult.Silence

        val rms = rms(frame.samples)
        val isSpeech = rms >= speechThreshold

        // Log RMS for a few frames to understand noise floor
        if (debugSampleCount < 20) {
            Timber.d("RMS=${rms.toInt()} threshold=${speechThreshold.toInt()} isSpeech=$isSpeech speechFrames=$speechFrames")
            debugSampleCount++
        }

        // Check for timeout if we're in speech mode
        if (speechFrames > 0 && now - speechStartTimeMs > maxSpeechDurationMs) {
            val segment = if (speechFrames >= minimumSpeechFrames) frames.toList() else null
            reset()
            return if (segment != null) {
                VoiceSegmenterResult.SpeechEnded(segment)
            } else {
                VoiceSegmenterResult.Rejected(RejectionReason.Timeout)
            }
        }

        if (isSpeech) {
            frames += frame
            speechFrames += 1
            silenceFrames = 0
            if (speechFrames == 1) {
                speechStartTimeMs = System.currentTimeMillis()
            }
            return if (speechFrames == 1) VoiceSegmenterResult.SpeechStarted else VoiceSegmenterResult.SpeechContinuing
        }

        if (speechFrames > 0) {
            frames += frame
            silenceFrames += 1
            if (silenceFrames >= trailingSilenceFrames) {
                val segment = if (speechFrames >= minimumSpeechFrames) frames.toList() else null
                reset()
                return if (segment != null) {
                    VoiceSegmenterResult.SpeechEnded(segment)
                } else {
                    VoiceSegmenterResult.Rejected(RejectionReason.TooShort)
                }
            }
        }

        return VoiceSegmenterResult.Silence
    }

    private fun reset() {
        frames.clear()
        speechFrames = 0
        silenceFrames = 0
        speechStartTimeMs = 0L
        cooldownUntilMs = System.currentTimeMillis() + 4000L
    }

    private fun rms(samples: ShortArray): Double {
        var sumSq = 0L
        for (s in samples) {
            val v = s.toInt()
            sumSq += v.toLong() * v
        }
        return sqrt(sumSq.toDouble() / samples.size)
    }
}
