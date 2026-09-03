package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.lfm

import au.com.shiftyjelly.pocketcasts.voicecontrol.dialog.VoiceDialogManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.LfmPrompt
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.SlotRepair
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceIntent
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.ModelManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognitionContext
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class LfmIntentRouter internal constructor(
    private val dialogManager: VoiceDialogManager,
    private val modelManager: ModelManager,
    private val inference: LfmInference,
) : VoiceRecognizer {
    @Inject constructor(
        dialogManager: VoiceDialogManager,
        modelManager: ModelManager,
    ) : this(dialogManager, modelManager, LfmNativeInference)

    private val mutex = Mutex()
    private var loadedRelease: String? = null

    override suspend fun ensureReady(): Result<Unit> = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (!modelManager.isLfmModelReady()) {
                    return@withContext Result.failure(IllegalStateException("LFM model is unavailable"))
                }
                val release = modelManager.lfmReleaseVersion()
                    ?: return@withContext Result.failure(IllegalStateException("LFM manifest release is unavailable"))
                if (loadedRelease == release) {
                    return@withContext Result.success(Unit)
                }
                inference.release()
                val loaded = inference.load(
                    modelPath = modelManager.lfmModelFile.absolutePath,
                    classifierPath = modelManager.lfmClassifierFile.absolutePath,
                    labelMapPath = modelManager.lfmLabelMapFile.absolutePath,
                )
                if (!loaded) {
                    return@withContext Result.failure(
                        IllegalStateException(inference.lastError().ifBlank { "LFM native load failed" }),
                    )
                }
                loadedRelease = release
                Result.success(Unit)
            } catch (error: Throwable) {
                Timber.e(error, "Failed to initialize LfmIntentRouter")
                Result.failure(error)
            }
        }
    }

    override suspend fun recognize(
        transcript: String,
        context: VoiceRecognitionContext,
    ): VoiceIntent? = withContext(Dispatchers.IO) {
        // Hold one lock across tokenize→classify→generate so KV-cache continuity
        // cannot be poisoned by a concurrent recognize/ensureReady caller.
        mutex.withLock {
            if (transcript.isBlank()) return@withContext null
            if (loadedRelease == null) {
                Timber.w("LFM router not ready — ensureReady() was not called before recognize()")
                return@withContext null
            }

            try {
                val prompt = LfmPrompt.render(
                    transcript = transcript,
                    history = dialogManager.promptHistory(),
                )
                val promptTokenIds = inference.tokenize(prompt, addBos = false)
                    ?: return@withContext null
                val userTokenIds = inference.tokenize(transcript, addBos = false)
                    ?: return@withContext null
                val (poolStart, poolEnd) = LfmTokenSpan.lastUserTokenSpan(promptTokenIds, userTokenIds)
                val label = inference.classify(promptTokenIds, poolStart, poolEnd)
                    ?: return@withContext null
                val (tool, action) = LfmLabel.parse(label)
                if (tool == "no_match") {
                    return@withContext null
                }

                val prefill = LfmCallPrefill.render(tool, action)
                val generated = inference.generate(prefill) ?: return@withContext null
                val repaired = SlotRepair.repair(
                    raw = generated,
                    utterance = transcript,
                    tool = tool,
                    action = action,
                ) ?: return@withContext null

                if (repaired.name == "dialog_control") {
                    return@withContext dialogManager.resolve(
                        transcript = transcript,
                        generated = generated,
                        call = repaired,
                    )
                }
                dialogManager.resolve(repaired)
            } catch (error: Throwable) {
                Timber.w(error, "LFM inference failed")
                null
            } finally {
                inference.reset()
            }
        }
    }

    override fun release() {
        loadedRelease = null
        inference.release()
    }
}
