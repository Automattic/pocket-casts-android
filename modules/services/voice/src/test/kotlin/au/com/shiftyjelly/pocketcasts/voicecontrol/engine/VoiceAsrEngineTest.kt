@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@file:Suppress("DEPRECATION")

package au.com.shiftyjelly.pocketcasts.voicecontrol.engine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.AsrBackend
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.AsrCapabilities
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.AsrResult
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.AsrToken
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.ModelSpec
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.TranslationStage
import au.com.shiftyjelly.pocketcasts.voicecontrol.audio.PcmAudioFrame
import au.com.shiftyjelly.pocketcasts.voicecontrol.audio.VoiceAudioProcessor
import au.com.shiftyjelly.pocketcasts.voicecontrol.audio.VoiceSegmenterResult
import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.EarconId
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceIntent
import au.com.shiftyjelly.pocketcasts.voicecontrol.mode.ListeningMode
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognitionContext
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognizer
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AudioRoute
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.MicExposure
import au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.WakeWordDetector
import au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.WakeWordResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq

class VoiceAsrEngineTest {

    private val context = mock<Context>()
    private val audioManager = mock<AudioManager>()
    private val voiceAudioProcessor = mock<VoiceAudioProcessor>()
    private val utteranceFilter = mock<UtteranceFilter>()
    private val intentRecognizer = mock<VoiceRecognizer>()
    private val wakeWordDetector = mock<WakeWordDetector>()
    private val gracePeriodSignal = mock<au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.GracePeriodSignal>()
    private val audioFeedbackRenderer = mock<au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.AudioFeedbackRenderer>()
    private val backend = mock<AsrBackend>()
    private val translationStage = mock<TranslationStage>()

    private var capturedReceiver: BroadcastReceiver? = null

    private val captureFlow: Flow<VoiceSegmenterResult> = MutableStateFlow(VoiceSegmenterResult.Silence)

    private lateinit var engine: VoiceAsrEngine

    private fun CoroutineScope.createEngine() {
        `when`(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager)
        `when`(audioManager.mode).thenReturn(AudioManager.MODE_NORMAL)
        `when`(voiceAudioProcessor.startProcessing()).thenReturn(captureFlow)

        // Default: wake word not detected (allows segments through during grace)
        kotlinx.coroutines.runBlocking {
            `when`(wakeWordDetector.detect(any(), any(), any())).thenReturn(
                au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.WakeWordResult(
                    detected = false,
                    confidence = 0f,
                    completionSample = 4000,
                ),
            )
        }

        `when`(
            context.registerReceiver(
                any<BroadcastReceiver>(),
                any<IntentFilter>(),
            ),
        ).thenAnswer { invocation ->
            capturedReceiver = invocation.getArgument(0)
            Intent()
        }

        engine = VoiceAsrEngine(
            voiceAudioProcessor = voiceAudioProcessor,
            utteranceFilter = utteranceFilter,
            intentRecognizer = intentRecognizer,
            wakeWordDetector = wakeWordDetector,
            gracePeriodSignal = gracePeriodSignal,
            audioFeedbackRenderer = audioFeedbackRenderer,
            translationStage = translationStage,
            context = context,
        )
        engine.scope = this
    }

    private fun startEngine(route: AudioRoute, mode: ListeningMode = ListeningMode.Continuous) {
        engine.start(
            backend = backend,
            audioRoute = route,
            listeningMode = mode,
            playbackBufferProvider = { FloatArray(0) },
            micExposureProvider = { MicExposure.Exposed },
            onIntent = {},
        )
    }

    private fun simulateScoState(state: Int) {
        val intent = mock<Intent>()
        `when`(
            intent.getIntExtra(
                AudioManager.EXTRA_SCO_AUDIO_STATE,
                AudioManager.SCO_AUDIO_STATE_ERROR,
            ),
        ).thenReturn(state)
        capturedReceiver?.onReceive(context, intent)
    }

    // ── Speaker / WiredHeadset routes: no SCO ──────────────────────────

    @Test
    fun `start with Speaker route skips SCO and starts capture`() = runTest {
        createEngine()
        startEngine(AudioRoute.Speaker)
        advanceUntilIdle()

        verify(audioManager, never()).startBluetoothSco()
        verify(voiceAudioProcessor).startProcessing()

        engine.stop()
    }

    @Test
    fun `start with WiredHeadset route skips SCO`() = runTest {
        createEngine()
        startEngine(AudioRoute.Headset(hasMicrophone = true))
        advanceUntilIdle()

        verify(audioManager, never()).startBluetoothSco()
        verify(voiceAudioProcessor).startProcessing()

        engine.stop()
    }

    // ── Bluetooth route: SCO await ─────────────────────────────────────

    @Test
    fun `start with BluetoothA2dpOnly awaits SCO connected before capture`() = runTest {
        createEngine()
        startEngine(AudioRoute.BluetoothA2dpOnly)
        advanceUntilIdle()

        verify(audioManager).startBluetoothSco()
        verify(voiceAudioProcessor, never()).startProcessing()
        assertTrue("Expected receiver registered", capturedReceiver != null)

        simulateScoState(AudioManager.SCO_AUDIO_STATE_CONNECTED)
        advanceUntilIdle()

        verify(voiceAudioProcessor).startProcessing()

        engine.stop()
    }

    @Test
    fun `start with BluetoothA2dpOnly starts capture even when SCO disconnects`() = runTest {
        createEngine()
        startEngine(AudioRoute.BluetoothA2dpOnly)
        advanceUntilIdle()

        verify(audioManager).startBluetoothSco()
        verify(voiceAudioProcessor, never()).startProcessing()

        simulateScoState(AudioManager.SCO_AUDIO_STATE_DISCONNECTED)
        advanceUntilIdle()

        verify(voiceAudioProcessor).startProcessing()

        engine.stop()
    }

    // ── Cancellation / Stop ────────────────────────────────────────────

    @Test
    fun `stop during SCO await cancels wait and unregisters receiver`() = runTest {
        createEngine()
        startEngine(AudioRoute.BluetoothA2dpOnly)
        advanceUntilIdle()

        verify(audioManager).startBluetoothSco()
        assertTrue("Expected receiver registered", capturedReceiver != null)

        engine.stop()
        advanceUntilIdle()

        verify(context).unregisterReceiver(any<BroadcastReceiver>())
        verify(voiceAudioProcessor, never()).startProcessing()
    }

    @Test
    fun `stop after capture started closes SCO and releases backend`() = runTest {
        createEngine()
        startEngine(AudioRoute.BluetoothA2dpOnly)
        advanceUntilIdle()

        simulateScoState(AudioManager.SCO_AUDIO_STATE_CONNECTED)
        advanceUntilIdle()
        verify(voiceAudioProcessor).startProcessing()

        engine.stop()
        advanceUntilIdle()

        verify(audioManager).stopBluetoothSco()
        verify(backend).release()
    }

    @Test
    fun `transcription initializes recognizer before matching intent`() = runTest {
        val recognizer = RecordingRecognizer(VoiceIntent.Playback.Pause)
        `when`(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager)
        `when`(audioManager.mode).thenReturn(AudioManager.MODE_NORMAL)
        `when`(voiceAudioProcessor.startProcessing()).thenReturn(
            flowOf(
                VoiceSegmenterResult.SpeechEnded(
                    listOf(PcmAudioFrame(shortArrayOf(100, 200, 300, 400), 16000)),
                    speechOnsetSample = 2,
                ),
            ),
        )
        `when`(utteranceFilter.shouldProcess(any(), any(), any(), any())).thenReturn(true)

        // Wake word not detected: full segment flows through during grace
        `when`(wakeWordDetector.detect(any(), any(), any())).thenReturn(
            au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.WakeWordResult(
                detected = false,
                confidence = 0f,
                completionSample = 4000,
            ),
        )

        engine = VoiceAsrEngine(
            voiceAudioProcessor = voiceAudioProcessor,
            utteranceFilter = utteranceFilter,
            intentRecognizer = recognizer,
            wakeWordDetector = wakeWordDetector,
            gracePeriodSignal = gracePeriodSignal,
            audioFeedbackRenderer = audioFeedbackRenderer,
            translationStage = translationStage,
            context = context,
        )
        engine.scope = this
        val handledIntents = mutableListOf<VoiceIntent>()

        engine.start(
            backend = FakeAsrBackend("pause"),
            audioRoute = AudioRoute.Speaker,
            listeningMode = ListeningMode.Continuous,
            playbackBufferProvider = { FloatArray(0) },
            micExposureProvider = { MicExposure.Exposed },
            onIntent = { handledIntents += it },
        )
        advanceUntilIdle()

        assertEquals(listOf("ensureReady", "recognize:pause"), recognizer.calls)
        assertEquals(listOf(VoiceIntent.Playback.Pause), handledIntents)
        verify(wakeWordDetector).detect(any(), eq(16000), eq(2))

        engine.stop()
    }

    // ── Translation stage wiring ───────────────────────────────────────

    @Test
    fun `non-English transcript translated by stage before intent routing when backend cannot translate`() = runTest {
        val recognizer = RecordingRecognizer(VoiceIntent.Playback.Pause)
        `when`(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager)
        `when`(audioManager.mode).thenReturn(AudioManager.MODE_NORMAL)
        `when`(voiceAudioProcessor.startProcessing()).thenReturn(
            flowOf(
                VoiceSegmenterResult.SpeechEnded(
                    listOf(PcmAudioFrame(shortArrayOf(100, 200, 300, 400), 16000)),
                    speechOnsetSample = 2,
                ),
            ),
        )
        `when`(utteranceFilter.shouldProcess(any(), any(), any(), any())).thenReturn(true)
        `when`(wakeWordDetector.detect(any(), any(), any())).thenReturn(
            au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.WakeWordResult(
                detected = false,
                confidence = 0f,
            ),
        )
        `when`(translationStage.ensureReady("zh")).thenReturn(Result.success(Unit))
        `when`(translationStage.translate("你好", "zh")).thenReturn("hello")

        engine = VoiceAsrEngine(
            voiceAudioProcessor = voiceAudioProcessor,
            utteranceFilter = utteranceFilter,
            intentRecognizer = recognizer,
            wakeWordDetector = wakeWordDetector,
            gracePeriodSignal = gracePeriodSignal,
            audioFeedbackRenderer = audioFeedbackRenderer,
            translationStage = translationStage,
            context = context,
        )
        engine.scope = this

        engine.start(
            backend = ResultBackend(AsrResult(text = "你好", detectedLanguage = "zh")),
            audioRoute = AudioRoute.Speaker,
            listeningMode = ListeningMode.Continuous,
            playbackBufferProvider = { FloatArray(0) },
            micExposureProvider = { MicExposure.Exposed },
            onIntent = {},
        )
        advanceUntilIdle()

        verify(translationStage).ensureReady("zh")
        verify(translationStage).translate("你好", "zh")
        assertTrue("Expected translated 'hello' to reach recognizer", recognizer.calls.contains("recognize:hello"))

        engine.stop()
    }

    @Test
    fun `english transcript bypasses translation stage`() = runTest {
        val recognizer = RecordingRecognizer(VoiceIntent.Playback.Pause)
        `when`(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager)
        `when`(audioManager.mode).thenReturn(AudioManager.MODE_NORMAL)
        `when`(voiceAudioProcessor.startProcessing()).thenReturn(
            flowOf(
                VoiceSegmenterResult.SpeechEnded(
                    listOf(PcmAudioFrame(shortArrayOf(100, 200, 300, 400), 16000)),
                    speechOnsetSample = 2,
                ),
            ),
        )
        `when`(utteranceFilter.shouldProcess(any(), any(), any(), any())).thenReturn(true)
        `when`(wakeWordDetector.detect(any(), any(), any())).thenReturn(
            au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword.WakeWordResult(
                detected = false,
                confidence = 0f,
            ),
        )

        engine = VoiceAsrEngine(
            voiceAudioProcessor = voiceAudioProcessor,
            utteranceFilter = utteranceFilter,
            intentRecognizer = recognizer,
            wakeWordDetector = wakeWordDetector,
            gracePeriodSignal = gracePeriodSignal,
            audioFeedbackRenderer = audioFeedbackRenderer,
            translationStage = translationStage,
            context = context,
        )
        engine.scope = this

        engine.start(
            backend = ResultBackend(AsrResult(text = "pause", detectedLanguage = "en")),
            audioRoute = AudioRoute.Speaker,
            listeningMode = ListeningMode.Continuous,
            playbackBufferProvider = { FloatArray(0) },
            micExposureProvider = { MicExposure.Exposed },
            onIntent = {},
        )
        advanceUntilIdle()

        verify(translationStage, never()).translate(any(), any())
        assertTrue("Expected native 'pause' to reach recognizer", recognizer.calls.contains("recognize:pause"))

        engine.stop()
    }

    // ── SCO not reopened for subsequent starts ─────────────────────────

    @Test
    fun `stop then restart on Bluetooth re-opens SCO`() = runTest {
        createEngine()
        startEngine(AudioRoute.BluetoothA2dpOnly)
        advanceUntilIdle()

        simulateScoState(AudioManager.SCO_AUDIO_STATE_CONNECTED)
        advanceUntilIdle()

        engine.stop()
        advanceUntilIdle()
        verify(audioManager).stopBluetoothSco()

        // Restart — stop cleared scoStarted, so SCO must be re-opened
        startEngine(AudioRoute.BluetoothA2dpOnly)
        advanceUntilIdle()

        verify(audioManager, times(2)).startBluetoothSco()

        engine.stop()
    }

    // ── Route switching scenarios ──────────────────────────────────────

    @Test
    fun `restart from Speaker to Bluetooth triggers SCO await`() = runTest {
        createEngine()
        startEngine(AudioRoute.Speaker)
        advanceUntilIdle()
        verify(audioManager, never()).startBluetoothSco()

        engine.stop()
        capturedReceiver = null

        startEngine(AudioRoute.BluetoothA2dpOnly)
        advanceUntilIdle()

        verify(audioManager, times(1)).startBluetoothSco()
        assertTrue("Expected receiver registered", capturedReceiver != null)

        engine.stop()
    }

    @Test
    fun `restart from Bluetooth to Speaker closes SCO`() = runTest {
        createEngine()
        startEngine(AudioRoute.BluetoothA2dpOnly)
        advanceUntilIdle()

        simulateScoState(AudioManager.SCO_AUDIO_STATE_CONNECTED)
        advanceUntilIdle()

        engine.stop()
        advanceUntilIdle()
        verify(audioManager).stopBluetoothSco()

        startEngine(AudioRoute.Speaker)
        advanceUntilIdle()
        verify(voiceAudioProcessor, times(2)).startProcessing()

        verify(audioManager, times(1)).startBluetoothSco()
        verify(audioManager, times(1)).stopBluetoothSco()

        engine.stop()
    }

    // ── Wake-word detection paths ──────────────────────────────────────

    private fun CoroutineScope.createEngineWithSpeech(
        recognizer: VoiceRecognizer,
        wakeWordResult: WakeWordResult,
    ) {
        `when`(context.getSystemService(Context.AUDIO_SERVICE)).thenReturn(audioManager)
        `when`(audioManager.mode).thenReturn(AudioManager.MODE_NORMAL)
        `when`(voiceAudioProcessor.startProcessing()).thenReturn(
            flowOf(
                VoiceSegmenterResult.SpeechEnded(
                    listOf(PcmAudioFrame(shortArrayOf(100, 200, 300, 400), 16000)),
                ),
            ),
        )
        `when`(utteranceFilter.shouldProcess(any(), any(), any(), any())).thenReturn(true)
        kotlinx.coroutines.runBlocking {
            `when`(wakeWordDetector.detect(any(), any(), any())).thenReturn(wakeWordResult)
        }

        engine = VoiceAsrEngine(
            voiceAudioProcessor = voiceAudioProcessor,
            utteranceFilter = utteranceFilter,
            intentRecognizer = recognizer,
            wakeWordDetector = wakeWordDetector,
            gracePeriodSignal = gracePeriodSignal,
            audioFeedbackRenderer = audioFeedbackRenderer,
            translationStage = translationStage,
            context = context,
        )
        engine.scope = this
    }

    @Test
    fun `wake word detection opens grace plays earcon and forwards full segment to ASR`() = runTest {
        val recognizer = RecordingRecognizer(VoiceIntent.Playback.Pause)
        createEngineWithSpeech(
            recognizer = recognizer,
            wakeWordResult = WakeWordResult(
                detected = true,
                confidence = 0.9f,
                completionSample = 4000,
            ),
        )
        val handledIntents = mutableListOf<VoiceIntent>()

        engine.start(
            backend = FakeAsrBackend(
                "Auris skip forward",
                tokens = listOf(
                    AsrToken("Auris", 0, 300),
                    AsrToken(" skip", 500, 800),
                    AsrToken(" forward", 800, 1200),
                ),
            ),
            audioRoute = AudioRoute.Speaker,
            listeningMode = ListeningMode.WakeWord,
            playbackBufferProvider = { FloatArray(0) },
            micExposureProvider = { MicExposure.Exposed },
            onIntent = { handledIntents += it },
        )
        advanceUntilIdle()

        verify(gracePeriodSignal).onWakeWordDetected()
        verify(audioFeedbackRenderer).playEarcon(EarconId.WAKE_WORD)
        assertEquals(listOf("ensureReady", "recognize:skip forward"), recognizer.calls)
        assertEquals(listOf(VoiceIntent.Playback.Pause), handledIntents)

        engine.stop()
    }

    @Test
    fun `wake-only detection opens grace plays error earcon after empty command transcript`() = runTest {
        val recognizer = RecordingRecognizer(VoiceIntent.Playback.Pause)
        createEngineWithSpeech(
            recognizer = recognizer,
            wakeWordResult = WakeWordResult(
                detected = true,
                confidence = 0.9f,
                completionSample = 4000,
            ),
        )

        engine.start(
            backend = FakeAsrBackend(
                "Auris",
                tokens = listOf(AsrToken("Auris", 0, 400)),
            ),
            audioRoute = AudioRoute.Speaker,
            listeningMode = ListeningMode.WakeWord,
            playbackBufferProvider = { FloatArray(0) },
            micExposureProvider = { MicExposure.Exposed },
            onIntent = {},
        )
        advanceUntilIdle()

        verify(gracePeriodSignal).onWakeWordDetected()
        verify(audioFeedbackRenderer).playEarcon(EarconId.WAKE_WORD)
        verify(audioFeedbackRenderer).playEarcon(EarconId.ERROR)
        assertTrue("Expected no intent routing, got ${recognizer.calls}", recognizer.calls.isEmpty())

        engine.stop()
    }

    @Test
    fun `negative detection in WakeWord mode drops segment`() = runTest {
        val recognizer = RecordingRecognizer(VoiceIntent.Playback.Pause)
        createEngineWithSpeech(
            recognizer = recognizer,
            wakeWordResult = WakeWordResult(
                detected = false,
                confidence = 0f,
                completionSample = 4000,
            ),
        )

        engine.start(
            backend = FakeAsrBackend("pause"),
            audioRoute = AudioRoute.Speaker,
            listeningMode = ListeningMode.WakeWord,
            playbackBufferProvider = { FloatArray(0) },
            micExposureProvider = { MicExposure.Exposed },
            onIntent = {},
        )
        advanceUntilIdle()

        verify(gracePeriodSignal, never()).onWakeWordDetected()
        verify(audioFeedbackRenderer, never()).playEarcon(any())
        assertTrue("Expected no ASR calls, got ${recognizer.calls}", recognizer.calls.isEmpty())

        engine.stop()
    }

    private class RecordingRecognizer(
        private val intent: VoiceIntent?,
    ) : VoiceRecognizer {
        val calls = mutableListOf<String>()

        override suspend fun ensureReady(): Result<Unit> {
            calls += "ensureReady"
            return Result.success(Unit)
        }

        override suspend fun recognize(
            transcript: String,
            context: VoiceRecognitionContext,
        ): VoiceIntent? {
            calls += "recognize:$transcript"
            return intent
        }

        override fun release() = Unit
    }

    private class FakeAsrBackend(
        private val transcript: String,
        private val tokens: List<AsrToken>? = null,
    ) : AsrBackend {
        override suspend fun ensureReady(): Result<Unit> = Result.success(Unit)

        override suspend fun transcribe(samples: FloatArray, sampleRateHz: Int): AsrResult = AsrResult(transcript, tokens = tokens)

        override val requiredModel: ModelSpec = ModelSpec(files = emptyList(), targetDir = "")

        override val capabilities: AsrCapabilities = AsrCapabilities(supportedLanguages = setOf("en"))

        override fun release() = Unit
    }

    private class ResultBackend(
        private val result: AsrResult,
    ) : AsrBackend {
        override suspend fun ensureReady(): Result<Unit> = Result.success(Unit)

        override suspend fun transcribe(samples: FloatArray, sampleRateHz: Int): AsrResult = result

        override val requiredModel: ModelSpec = ModelSpec(files = emptyList(), targetDir = "")

        override val capabilities: AsrCapabilities = AsrCapabilities(
            supportedLanguages = setOf("zh", "en"),
            canTranslateToEnglish = false,
        )

        override fun release() = Unit
    }
}
