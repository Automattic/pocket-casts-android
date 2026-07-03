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
import au.com.shiftyjelly.pocketcasts.voicecontrol.audio.VoiceAudioProcessor
import au.com.shiftyjelly.pocketcasts.voicecontrol.audio.VoiceSegmenterResult
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceIntent
import au.com.shiftyjelly.pocketcasts.voicecontrol.mode.ListeningMode
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognitionContext
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognizer
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AudioRoute
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.MicExposure
import au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.CommandWindow
import au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.WakeWordDetector
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
    @ApplicationContext private val context: Context,
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    internal var scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val commandWindow = CommandWindow()
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
                        is VoiceSegmenterResult.SpeechStarted -> commandWindow.onActivity()

                        is VoiceSegmenterResult.SpeechContinuing -> { /* accumulating frames */ }

                        is VoiceSegmenterResult.SpeechEnded -> {
                            val totalSamples = result.frames.sumOf { it.samples.size }
                            val durationMs = totalSamples * 1000L / 16000
                            Timber.i("VAD: speech ended (%d samples, ~%dms)", totalSamples, durationMs)

                            val audio = shouldTranscribe(result)
                            if (audio != null) {
                                transcribeSegment(result, overrideAudio = audio)
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

    /** Returns the audio to transcribe, or null if the segment should be dropped. */
    private suspend fun shouldTranscribe(segment: VoiceSegmenterResult.SpeechEnded): FloatArray? {
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

        val mode = currentMode
        return when (mode) {
            ListeningMode.Continuous -> floatSamples

            ListeningMode.WakeWord -> {
                val wwResult = runCatching {
                    wakeWordDetector.detect(floatSamples, segment.frames.firstOrNull()?.sampleRateHz ?: 16000)
                }.getOrElse { e ->
                    Timber.w(e, "Wake word detection failed, dropping segment")
                    return null
                }

                if (wwResult.detected) {
                    Timber.i("Wake word detected (confidence=%.2f)", wwResult.confidence)
                    commandWindow.onWakeWord()
                    wwResult.remainderSamples ?: floatSamples
                } else {
                    if (commandWindow.isActive) floatSamples else null
                }
            }

            ListeningMode.Off -> null
        }
    }

    private suspend fun transcribeSegment(
        segment: VoiceSegmenterResult.SpeechEnded,
        overrideAudio: FloatArray? = null,
    ) {
        val b = backend ?: return
        val sampleRateHz = segment.frames.firstOrNull()?.sampleRateHz ?: 16000

        // Use override audio if provided (e.g., remainder after wake word), else build from frames
        val floatSamples = overrideAudio ?: run {
            val totalSamples = segment.frames.sumOf { it.samples.size }
            val samples = FloatArray(totalSamples)
            var off = 0
            for (frame in segment.frames) {
                for (i in frame.samples.indices) {
                    samples[off + i] = frame.samples[i].toFloat() / 32768f
                }
                off += frame.samples.size
            }
            samples
        }

        // Filter out playback bleed before transcribing
        val playbackBuffer = playbackBufferProvider?.invoke() ?: FloatArray(0)
        if (!utteranceFilter.shouldProcess(floatSamples, false, 0, playbackBuffer)) {
            Timber.i("Utterance rejected by bleed filter")
            return
        }

        // ASR
        val asrResult = b.transcribe(floatSamples, sampleRateHz)
        if (asrResult.text.isBlank()) {
            Timber.i("ASR returned empty transcript")
            return
        }
        processUtterance(asrResult)
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

    fun stop() {
        processingJob?.cancel()
        processingJob = null
        backend?.release()
        backend = null
        closeBluetoothSco()
        commandWindow.reset()
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
