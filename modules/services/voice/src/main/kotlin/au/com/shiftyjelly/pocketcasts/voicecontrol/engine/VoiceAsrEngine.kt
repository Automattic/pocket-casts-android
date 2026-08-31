package au.com.shiftyjelly.pocketcasts.voicecontrol.engine

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import androidx.annotation.RequiresPermission
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.AsrBackend
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.AsrResult
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.TranslationStage
import au.com.shiftyjelly.pocketcasts.voicecontrol.audio.VoiceAudioProcessor
import au.com.shiftyjelly.pocketcasts.voicecontrol.audio.VoiceSegmenterResult
import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.AudioFeedbackRenderer
import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.EarconId
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.GracePeriodSignal
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceIntent
import au.com.shiftyjelly.pocketcasts.voicecontrol.mode.ListeningMode
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognitionContext
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognizer
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AudioRoute
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.MicExposure
import au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.WakeTranscriptTrimmer
import au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.WakeWordDetector
import au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.WakeWordSegmentCapture
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

@Singleton
class VoiceAsrEngine @Inject constructor(
    private val voiceAudioProcessor: VoiceAudioProcessor,
    private val utteranceFilter: UtteranceFilter,
    private val intentRecognizer: VoiceRecognizer,
    private val wakeWordDetector: WakeWordDetector,
    private val gracePeriodSignal: GracePeriodSignal,
    private val audioFeedbackRenderer: AudioFeedbackRenderer,
    private val translationStage: TranslationStage,
    @ApplicationContext private val context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    internal var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var processingJob: Job? = null
    private var scoStarted = false
    private var savedAudioMode: Int? = null
    private var playbackBufferProvider: (() -> FloatArray)? = null

    private var backend: AsrBackend? = null
    private var onIntent: ((VoiceIntent) -> Unit)? = null
    private var micExposureProvider: (() -> MicExposure)? = null

    @Volatile
    private var currentMode: ListeningMode = ListeningMode.Off

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun start(
        backend: AsrBackend,
        audioRoute: AudioRoute,
        listeningMode: ListeningMode,
        playbackBufferProvider: () -> FloatArray,
        micExposureProvider: () -> MicExposure,
        onIntent: (VoiceIntent) -> Unit,
    ) {
        this.backend = backend
        this.currentMode = listeningMode
        this.playbackBufferProvider = playbackBufferProvider
        this.micExposureProvider = micExposureProvider
        this.onIntent = onIntent
        utteranceFilter.reset()

        processingJob = scope.launch {
            if (audioRoute is AudioRoute.BluetoothA2dpOnly) {
                awaitBluetoothSco()
            }
            try {
                voiceAudioProcessor.startProcessing().collect { result ->
                    when (result) {
                        is VoiceSegmenterResult.SpeechStarted -> { /* utterance started */ }

                        is VoiceSegmenterResult.SpeechContinuing -> { /* accumulating frames */ }

                        is VoiceSegmenterResult.SpeechEnded -> {
                            val totalSamples = result.frames.sumOf { it.samples.size }
                            val durationMs = totalSamples * 1000L / 16000
                            Timber.i("VAD: speech ended (%d samples, ~%dms)", totalSamples, durationMs)

                            val request = shouldTranscribe(result)
                            if (request != null) {
                                transcribeSegment(result, request)
                            }
                        }

                        is VoiceSegmenterResult.Rejected ->
                            Timber.w("VAD: rejected - %s", result.reason)

                        VoiceSegmenterResult.Silence -> { /* no speech */ }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Voice audio processing failed")
            }
        }
        Timber.i("VoiceAsrEngine started (backend=%s, mode=%s)", backend::class.simpleName, listeningMode)
    }

    fun updateListeningMode(mode: ListeningMode) {
        currentMode = mode
        Timber.i("VoiceAsrEngine mode updated to %s", mode)
    }

    /**
     * Runs wake-word detection on every utterance in both listening modes,
     * then decides whether to transcribe. Per spec:
     *
     * - Positive detection (either mode): emits WAKE_WORD earcon, opens/resets
     *   grace, and forwards the complete VAD segment to ASR. Wake-positive
     *   time-band trim happens on timed ASR tokens after ASR, not by cutting audio.
     * - Negative outside grace (WakeWord): drops the segment.
     * - Negative during grace (Continuous): forwards the full segment.
     * - Wake-only is decided after ASR: empty leftover after time-band trim plays ERROR.
     */
    private data class TranscribeRequest(
        val samples: FloatArray,
        val wakePositive: Boolean,
        val completionSample: Int = 0,
    )

    private suspend fun shouldTranscribe(segment: VoiceSegmenterResult.SpeechEnded): TranscribeRequest? {
        // Build float samples from the segment
        val totalSamples = segment.frames.sumOf { it.samples.size }
        val floatSamples = FloatArray(totalSamples)
        var offset = 0
        for (frame in segment.frames) {
            for (i in frame.samples.indices) {
                floatSamples[offset + i] = frame.samples[i].toFloat() / 32768f
            }
            offset += frame.samples.size
        }

        // Always run the wake-word detector — it's lightweight and must observe
        // every utterance so wake-word audio is always stripped and every
        // detection is acknowledged.
        val wwResult = runCatching {
            wakeWordDetector.detect(
                segment = floatSamples,
                sampleRateHz = segment.frames.firstOrNull()?.sampleRateHz ?: 16000,
                speechOnsetSample = segment.speechOnsetSample,
            )
        }.getOrElse { e ->
            Timber.w(e, "Wake word detection failed, dropping segment")
            return null
        }

        // Debug instrumentation: behind WAKE_WORD_DEBUG_CAPTURE, dump every
        // VAD segment (raw WAV + log-Mel PNG) named by timestamp + score.
        WakeWordSegmentCapture.capture(
            context,
            floatSamples,
            segment.frames.firstOrNull()?.sampleRateHz ?: 16000,
            wwResult.confidence,
        )

        val mode = currentMode

        if (wwResult.detected) {
            Timber.i("Wake word detected (confidence=%.2f)", wwResult.confidence)

            // Open or reset the conversation grace period. This causes
            // ListeningModePolicy to switch to Continuous for subsequent utterances.
            gracePeriodSignal.onWakeWordDetected()

            // Always acknowledge detection with the WAKE_WORD earcon
            audioFeedbackRenderer.playEarcon(EarconId.WAKE_WORD)

            return TranscribeRequest(
                samples = floatSamples,
                wakePositive = true,
                completionSample = wwResult.completionSample,
            )
        }

        // Negative detection
        return when (mode) {
            ListeningMode.Continuous -> {
                TranscribeRequest(samples = floatSamples, wakePositive = false)
            }

            ListeningMode.WakeWord -> {
                // Outside grace: drop — wake word is required
                null
            }

            ListeningMode.Off -> null
        }
    }

    private suspend fun transcribeSegment(
        segment: VoiceSegmenterResult.SpeechEnded,
        request: TranscribeRequest,
    ) {
        val b = backend ?: return
        val sampleRateHz = segment.frames.firstOrNull()?.sampleRateHz ?: 16000
        val floatSamples = request.samples

        // Filter out playback bleed before transcribing
        val playbackBuffer = playbackBufferProvider?.invoke() ?: FloatArray(0)
        if (!utteranceFilter.shouldProcess(floatSamples, false, 0, playbackBuffer)) {
            Timber.i("Utterance rejected by bleed filter")
            return
        }

        // ASR
        val asrResult = b.transcribe(floatSamples, sampleRateHz)
        val durationMs = (floatSamples.size * 1000L / sampleRateHz).toInt()
        val transcript = WakeTranscriptTrimmer.commandText(
            result = asrResult,
            wakePositive = request.wakePositive,
            completionSample = request.completionSample,
            sampleRateHz = sampleRateHz,
            utteranceDurationMs = durationMs,
        )
        if (transcript.isBlank()) {
            if (request.wakePositive) {
                Timber.i("Wake-only utterance — no command remainder after ASR")
                audioFeedbackRenderer.playEarcon(EarconId.ERROR)
            } else {
                Timber.i("ASR returned empty transcript")
            }
            return
        }
        // Translate to English when the ASR backend did not already translate and
        // the detected language is not English (the SenseVoice CJK path).
        // Use the wake-trimmed transcript from LFM's WakeTranscriptTrimmer.
        val trimmedResult = asrResult.copy(text = transcript)
        val finalResult = maybeTranslate(trimmedResult, b)
        processUtterance(finalResult)
    }

    private suspend fun processUtterance(result: AsrResult) {
        val recognizer = intentRecognizer
        val handler = onIntent ?: return

        val ready = recognizer.ensureReady()
        if (ready.isFailure) {
            Timber.e(ready.exceptionOrNull(), "Intent recognizer not ready")
            return
        }

        val t0 = System.currentTimeMillis()
        val ctx = VoiceRecognitionContext(
            listeningMode = currentMode,
            micExposure = micExposureProvider?.invoke() ?: MicExposure.Exposed,
        )
        val intent = recognizer.recognize(result.text, ctx)
        val elapsedMs = System.currentTimeMillis() - t0

        if (intent != null) {
            Timber.i("Intent: %s (%dms)", intent::class.simpleName, elapsedMs)
            handler(intent)
        } else {
            Timber.i("No intent (%dms)", elapsedMs)
        }
    }

    private suspend fun maybeTranslate(result: AsrResult, backend: AsrBackend): AsrResult {
        val detected = result.detectedLanguage?.lowercase() ?: return result
        if (detected == "en") return result
        if (backend.capabilities.canTranslateToEnglish) return result

        val ready = translationStage.ensureReady(detected)
        if (ready.isFailure) {
            Timber.w("Translation stage not ready for %s, using native transcript", detected)
            return result
        }
        val translated = translationStage.translate(result.text, detected)
        if (translated.isBlank()) {
            Timber.w("Translation returned blank for %s, using native transcript", detected)
            return result
        }
        Timber.i("Translated %s -> en: '%s'", detected, translated)
        return result.copy(text = translated, detectedLanguage = "en")
    }

    fun stop() {
        processingJob?.cancel()
        processingJob = null
        backend?.release()
        backend = null
        closeBluetoothSco()
        Timber.i("VoiceAsrEngine stopped")
    }

    @Suppress("DEPRECATION") // startBluetoothSco + SCO broadcast deprecated in API 33; no replacement
    private suspend fun awaitBluetoothSco() {
        if (scoStarted) return
        suspendCancellableCoroutine { cont ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    val state = intent.getIntExtra(
                        AudioManager.EXTRA_SCO_AUDIO_STATE,
                        AudioManager.SCO_AUDIO_STATE_ERROR,
                    )
                    Timber.i("SCO state: %d", state)
                    if (state == AudioManager.SCO_AUDIO_STATE_CONNECTED ||
                        state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED
                    ) {
                        context.unregisterReceiver(this)
                        if (cont.isActive) cont.resumeWith(Result.success(Unit))
                    }
                }
            }
            context.registerReceiver(
                receiver,
                IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED),
            )
            savedAudioMode = audioManager.mode
            audioManager.mode = AudioManager.MODE_NORMAL
            audioManager.startBluetoothSco()
            scoStarted = true
            Timber.i("SCO requested, waiting for connection")

            cont.invokeOnCancellation {
                try {
                    context.unregisterReceiver(receiver)
                } catch (_: Exception) {}
            }
        }
    }

    @Suppress("DEPRECATION") // stopBluetoothSco deprecated in API 33; no replacement
    private fun closeBluetoothSco() {
        if (!scoStarted) return
        try {
            audioManager.stopBluetoothSco()
            savedAudioMode?.let { audioManager.mode = it }
        } catch (e: Exception) {
            Timber.w(e, "Failed to stop Bluetooth SCO")
        } finally {
            scoStarted = false
        }
    }
}
