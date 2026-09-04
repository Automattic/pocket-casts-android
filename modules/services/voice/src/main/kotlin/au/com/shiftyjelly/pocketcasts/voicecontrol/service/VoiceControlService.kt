package au.com.shiftyjelly.pocketcasts.voicecontrol.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresPermission
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.AsrBackendSelector
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.CanaryFlashBackend
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.SenseVoiceBackend
import au.com.shiftyjelly.pocketcasts.voicecontrol.asr.WhisperCppBackend
import au.com.shiftyjelly.pocketcasts.voicecontrol.engine.PlaybackBufferRecorder
import au.com.shiftyjelly.pocketcasts.voicecontrol.engine.VoiceAsrEngine
import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.AudioFeedbackRenderer
import au.com.shiftyjelly.pocketcasts.voicecontrol.feedback.EarconId
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.LiveConditionMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlGate
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.ModelsReadyCondition
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.GracePeriodSignal
import au.com.shiftyjelly.pocketcasts.voicecontrol.mode.ListeningMode
import au.com.shiftyjelly.pocketcasts.voicecontrol.mode.ListeningModePolicy
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognitionContext
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognizer
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackContextMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.VoicePlaybackIntentExecutor
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.AndroidAudioRouteMonitor
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.MicExposure
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.toMicExposure
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
class VoiceControlService : Service() {

    @Inject lateinit var gate: VoiceControlGate

    @Inject lateinit var notificationManager: VoiceControlNotificationManager

    @Inject lateinit var voiceRecognizer: VoiceRecognizer

    @Inject lateinit var voicePlaybackIntentExecutor: VoicePlaybackIntentExecutor

    @Inject lateinit var playbackContextMonitor: PlaybackContextMonitor

    @Inject lateinit var audioRouteMonitor: AndroidAudioRouteMonitor

    @Inject lateinit var listeningModePolicy: ListeningModePolicy

    @Inject lateinit var liveConditionMonitor: LiveConditionMonitor

    @Inject lateinit var voiceAsrEngine: dagger.Lazy<VoiceAsrEngine>

    @Inject lateinit var asrBackendSelector: AsrBackendSelector

    @Inject lateinit var playbackBufferRecorder: PlaybackBufferRecorder

    @Inject lateinit var modelManager: au.com.shiftyjelly.pocketcasts.voicecontrol.model.ModelManager

    @Inject lateinit var modelsReadyCondition: ModelsReadyCondition

    @Inject lateinit var audioFeedbackRenderer: AudioFeedbackRenderer

    @Inject lateinit var gracePeriodSignal: GracePeriodSignal

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var modeJob: Job? = null
    private var lastIntentType: String? = null
    private var lastCommandTime: Long = 0L
    private var engineStarted = false
    private var captureWasEverActive = false
    private var acquisitionLogged = false
    private var mediaSession: MediaSession? = null
    private var currentMode: ListeningMode = ListeningMode.Off

    companion object {
        private const val COMMAND_DEBOUNCE_MS = 2000L
        internal const val STOP_ACTION = "au.com.shiftyjelly.pocketcasts.voicecontrol.action.STOP"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP_ACTION) {
            stopVoiceControl()
            return START_NOT_STICKY
        }
        startVoiceControl()
        return START_NOT_STICKY
    }

    private fun startVoiceControl() {
        if (engineStarted) {
            Timber.i("Voice control already started")
            return
        }

        if (!hasRequiredPermissions()) {
            Timber.w("Missing permissions, stopping")
            stopSelf()
            return
        }

        try {
            val notification = notificationManager.createDownloadingNotification()
            startForeground(notificationManager.notificationId, notification)
        } catch (e: SecurityException) {
            Timber.e(e, "Cannot start foreground — mic permission not granted")
            stopSelf()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            mediaSession = MediaSession(this, "VoiceControl MediaSession")
            mediaSession?.isActive = true
        }

        // Start monitoring transient conflict conditions
        liveConditionMonitor.start()

        // Handle models first: defer mode observation until readiness completes.
        // This avoids lazy init on first utterance — both whisper and the
        // LFM inference runtime are fully prepared before audio capture.
        serviceScope.launch(Dispatchers.IO) {
            ensureModelsReady()
            launch(Dispatchers.Main) {
                modeJob = kotlinx.coroutines.flow.combine(
                    listeningModePolicy.mode,
                    audioRouteMonitor.route,
                ) { mode, _ -> mode }.onEach { mode ->
                    val route = audioRouteMonitor.route.value
                    val micExposure = route.toMicExposure()
                    when (mode) {
                        ListeningMode.Off -> {
                            if (engineStarted) {
                                // Active capture was ongoing, now stopping
                                val gateState = gate.state.value
                                val blockedRules = gateState.rules
                                    .filter { it.value !is VoiceControlRuleState.Allowed }
                                val reasons = blockedRules.entries
                                    .joinToString(";") { "${it.key}=${it.value}" }
                                val isUserRequested = blockedRules.keys.any {
                                    it == "voice_control_user_enabled"
                                }
                                val msg = "mic_capture_stopped: reasons=%s setup=%b conflicts=%b context=%b micExposure=%s route=%s mode=%s priorCaptureActive=true"
                                if (isUserRequested) {
                                    Timber.i(msg, reasons, gateState.setupPassed, gateState.conflictsPassed, gateState.contextPassed, micExposure, route, mode)
                                } else {
                                    Timber.e(msg, reasons, gateState.setupPassed, gateState.conflictsPassed, gateState.contextPassed, micExposure, route, mode)
                                }
                                stopEngine()
                            } else if (!captureWasEverActive && !acquisitionLogged) {
                                // Capture never started — log once, not on every recomputation
                                acquisitionLogged = true
                                val gateState = gate.state.value
                                val blockedRules = gateState.rules
                                    .filter { it.value !is VoiceControlRuleState.Allowed }
                                val reasons = blockedRules.entries
                                    .joinToString(";") { "${it.key}=${it.value}" }
                                Timber.i(
                                    "mic_acquisition_skipped: reasons=%s setup=%b conflicts=%b context=%b micExposure=%s route=%s mode=%s priorCaptureActive=false",
                                    reasons,
                                    gateState.setupPassed,
                                    gateState.conflictsPassed,
                                    gateState.contextPassed,
                                    micExposure,
                                    route,
                                    mode,
                                )
                            }
                            // else: was Off before, still Off — skip (reactive recomputation)
                            currentMode = ListeningMode.Off
                            updateListeningState()
                        }

                        ListeningMode.Continuous, ListeningMode.WakeWord -> {
                            captureWasEverActive = true
                            if (engineStarted) {
                                if (mode != currentMode) {
                                    voiceAsrEngine.get().updateListeningMode(mode)
                                    // Keep the wake-word hint accurate as grace opens/expires
                                    val notification = notificationManager.createListeningNotification(
                                        wakeWordRequired = mode == ListeningMode.WakeWord,
                                    )
                                    notificationManager.notify(notification)
                                }
                            } else {
                                @Suppress("MissingPermission") // permission checked in hasRequiredPermissions() above
                                startEngine(mode)
                            }
                            currentMode = mode
                            updateListeningState()
                        }
                    }
                }.launchIn(serviceScope)
            }
        }

        Timber.i("Voice control service started, observing listening mode")
    }

    private suspend fun ensureModelsReady() {
        Timber.i("Ensuring models")

        val backend = asrBackendSelector.select()
        Timber.i("Selected ASR backend: %s", backend::class.simpleName)

        val needDownload = !modelManager.isModelReady(backend.requiredModel) ||
            !modelManager.isLfmModelReady()

        if (needDownload) {
            modelsReadyCondition.update(isReady = false)
        }

        if (modelManager.ensureModel(backend.requiredModel).isFailure) {
            Timber.e("ASR model download failed")
            serviceScope.launch(Dispatchers.Main) { stopSelf() }
            return
        }
        wireBackend(backend)

        if (backend.ensureReady().isFailure) {
            Timber.e("Backend not ready")
            serviceScope.launch(Dispatchers.Main) { stopSelf() }
            return
        }

        if (modelManager.ensureLfmModel().isFailure) {
            Timber.e("LFM model download failed")
            serviceScope.launch(Dispatchers.Main) { stopSelf() }
            return
        }

        Timber.i("LFM model ready, initializing intent router")
        val routerResult = voiceRecognizer.ensureReady()
        if (routerResult.isFailure) {
            Timber.e(routerResult.exceptionOrNull(), "Intent router initialization failed")
            serviceScope.launch(Dispatchers.Main) { stopSelf() }
            return
        }
        Timber.i("Intent router initialized")

        modelsReadyCondition.update(isReady = true)
        Timber.i("Voice control models ready")
    }

    private fun wireBackend(backend: au.com.shiftyjelly.pocketcasts.voicecontrol.asr.AsrBackend) {
        val modelDir = java.io.File(filesDir, backend.requiredModel.targetDir)
        val modelFile = backend.requiredModel.files.firstOrNull()?.let {
            java.io.File(modelDir, it.filename)
        }
        if (modelFile != null && backend is WhisperCppBackend) {
            backend.setModelFile(modelFile)
        }
        if (backend is SenseVoiceBackend) {
            backend.setModelDir(modelDir)
        }
        if (backend is CanaryFlashBackend) {
            backend.setModelDir(modelDir)
        }
        Timber.i("ASR model ready")
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startEngine(mode: ListeningMode) {
        if (engineStarted) return
        val backend = asrBackendSelector.select()

        try {
            voiceAsrEngine.get().start(
                backend = backend,
                audioRoute = audioRouteMonitor.route.value,
                listeningMode = mode,
                playbackBufferProvider = playbackBufferRecorder::snapshot,
                micExposureProvider = { audioRouteMonitor.route.value.toMicExposure() },
                onIntent = { intent -> handleIntent(intent) },
            )
            engineStarted = true

            if (mode == ListeningMode.WakeWord) {
                audioFeedbackRenderer.playEarcon(EarconId.LISTENING_START)
            }

            val notification = notificationManager.createListeningNotification(
                wakeWordRequired = mode == ListeningMode.WakeWord,
            )
            notificationManager.notify(notification)
            Timber.i("[VoicePipeline] engine started mode=%s", mode)
        } catch (e: Exception) {
            Timber.e(e, "[VoicePipeline] engine start failed")
        }
    }

    private fun stopEngine() {
        if (!engineStarted) return
        voiceAsrEngine.get().stop()
        engineStarted = false
        Timber.i("[VoicePipeline] engine stopped")
    }

    private fun handleIntent(
        intent: au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceIntent,
    ) {
        val now = System.currentTimeMillis()
        val intentType = intent::class.simpleName ?: intent.toString()
        if (intentType == lastIntentType && (now - lastCommandTime) < COMMAND_DEBOUNCE_MS) {
            Timber.i("[VoicePipeline] debounce %s", intentType)
            return
        }
        lastIntentType = intentType
        lastCommandTime = now

        serviceScope.launch(Dispatchers.IO) {
            val response = voicePlaybackIntentExecutor.execute(intent)
            audioFeedbackRenderer.render(response)
            gracePeriodSignal.onCommandRecognized()
        }
    }

    private fun updateListeningState() {
        val session = mediaSession ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return

        when (currentMode) {
            ListeningMode.Continuous -> {
                session.isActive = true
                session.setPlaybackState(
                    PlaybackState.Builder()
                        .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                        .build(),
                )
            }

            ListeningMode.WakeWord -> {
                session.isActive = true
                session.setPlaybackState(
                    PlaybackState.Builder()
                        .setState(PlaybackState.STATE_PAUSED, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 0.0f)
                        .build(),
                )
            }

            ListeningMode.Off -> {
                session.isActive = false
            }
        }
    }

    private fun stopVoiceControl() {
        Timber.i("Stopping voice control service")
        modeJob?.cancel()
        stopEngine()
        mediaSession?.isActive = false
        mediaSession?.release()
        mediaSession = null
        voiceRecognizer.release()
        liveConditionMonitor.stop()
        audioFeedbackRenderer.release()
        notificationManager.cancelNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.cancel()
        stopSelf()
    }

    private fun hasRequiredPermissions(): Boolean {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_MICROPHONE) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        stopVoiceControl()
    }
}
