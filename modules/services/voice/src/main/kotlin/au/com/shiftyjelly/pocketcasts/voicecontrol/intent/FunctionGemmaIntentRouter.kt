package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.voicecontrol.dialog.VoiceDialogManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.FunctionGemmaBackend
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.FunctionGemmaRuntimeFactory
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.MonotonicClock
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.PreparedFunctionGemmaSessionPool
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.ModelManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognitionContext
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class FunctionGemmaIntentRouter @Inject constructor(
    private val dialogManager: VoiceDialogManager,
    private val modelManager: ModelManager,
    private val runtimeFactory: FunctionGemmaRuntimeFactory,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val clock: MonotonicClock,
    private val metrics: FunctionGemmaMetrics,
) : VoiceRecognizer {
    private val transitionMutex = Mutex()
    private val stateMutex = Mutex()
    private var activeState: ActiveState? = null
    private var transitionInProgress: CompletableDeferred<Unit>? = null

    internal var beforePoolInvalidation: () -> Unit = {}

    override suspend fun ensureReady(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureCurrentRelease()
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            error.throwIfFatal()
            Timber.e(error, "Failed to initialize FunctionGemmaIntentRouter")
            Result.failure(error)
        }
    }

    override suspend fun recognize(
        transcript: String,
        context: VoiceRecognitionContext,
    ): VoiceIntent? = withContext(Dispatchers.IO) {
        if (transcript.isBlank()) return@withContext null
        val state = stateMutex.withLock { activeState }
        if (state == null) {
            Timber.w("FunctionGemma pool not ready — ensureReady() was not called before recognize()")
            return@withContext null
        }

        try {
            consumeAndResolve(state, transcript)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            error.throwIfFatal()
            if (state.pool.backend != FunctionGemmaBackend.GPU) {
                invalidatePool(state)
                logRuntimeWarning("FunctionGemma CPU inference failed", error)
                return@withContext null
            }
            recoverOnCpuOnce(state, transcript, error)
        }
    }

    private suspend fun ensureCurrentRelease() {
        while (true) {
            when (val action = planReadinessTransition()) {
                ReadinessAction.Ready -> return

                is ReadinessAction.Wait -> action.completion.await()

                is ReadinessAction.Prepare -> {
                    executeReadinessTransition(action)
                    return
                }
            }
        }
    }

    private suspend fun planReadinessTransition(): ReadinessAction = transitionMutex.withLock {
        transitionInProgress?.let { return@withLock ReadinessAction.Wait(it) }
        check(modelManager.isFunctionGemmaModelReady()) {
            "FunctionGemma model or manifest is unavailable"
        }
        val release = requireNotNull(modelManager.functionGemmaReleaseVersion()) {
            "FunctionGemma manifest release is unavailable"
        }
        val current = stateMutex.withLock { activeState }
        if (current?.release == release) return@withLock ReadinessAction.Ready

        val completion = CompletableDeferred<Unit>()
        transitionInProgress = completion
        stateMutex.withLock {
            if (activeState === current) activeState = null
        }
        ReadinessAction.Prepare(current, release, completion)
    }

    private suspend fun executeReadinessTransition(action: ReadinessAction.Prepare) {
        try {
            action.previous?.pool?.close()
            val prepared = createGpuFirstPool(action.release)
            transitionMutex.withLock {
                check(transitionInProgress === action.completion)
                stateMutex.withLock {
                    activeState = prepared
                }
                transitionInProgress = null
                action.completion.complete(Unit)
            }
        } catch (error: Throwable) {
            failTransition(action.completion, error)
            throw error
        }
    }

    private suspend fun createGpuFirstPool(release: String): ActiveState {
        return runCatching {
            ActiveState(
                release = release,
                pool = createPreparedPool(FunctionGemmaBackend.GPU, release),
                fallbackReason = null,
            )
        }.getOrElse { gpuError ->
            metrics.backendFallback(FALLBACK_GPU_INIT, gpuError)
            logRuntimeWarning("FunctionGemma GPU initialization failed; using CPU", gpuError)
            ActiveState(
                release = release,
                pool = createPreparedPool(FunctionGemmaBackend.CPU, release),
                fallbackReason = FALLBACK_GPU_INIT,
            )
        }
    }

    private suspend fun createPreparedPool(
        backend: FunctionGemmaBackend,
        release: String,
    ): PreparedFunctionGemmaSessionPool {
        val engineStart = clock.elapsedRealtimeMs()
        val runtime = runtimeFactory.create(
            modelPath = modelManager.functionGemmaModelFile.absolutePath,
            cacheDir = modelManager.functionGemmaDir.absolutePath,
            backend = backend,
        )
        val engineInitMs = clock.elapsedRealtimeMs() - engineStart
        val pool = PreparedFunctionGemmaSessionPool(
            runtime = runtime,
            scope = applicationScope,
            elapsedRealtimeMs = clock::elapsedRealtimeMs,
        )
        try {
            val preparation = pool.prepare(FunctionGemmaPrompt.staticPrefix)
            metrics.prepared(
                backend = backend,
                modelRelease = release,
                engineInitMs = engineInitMs,
                sessionCreateMs = preparation.sessionCreateMs,
                staticPrefillMs = preparation.staticPrefillMs,
            )
            return pool
        } catch (error: Throwable) {
            try {
                pool.close()
            } catch (closeFailure: Throwable) {
                error.addSuppressed(closeFailure)
            }
            throw error
        }
    }

    private suspend fun consumeAndResolve(
        state: ActiveState,
        transcript: String,
    ): VoiceIntent? {
        val totalStart = clock.elapsedRealtimeMs()
        var requestPrefillMs = 0L
        var decodeMs = 0L
        var parseResolveMs = 0L
        var inputCharacters = 0
        var outputCharacters = 0
        val (result, lifecycle) = state.pool.consume { session ->
            val suffix = buildRequestSuffixSafely(transcript) ?: return@consume null
            inputCharacters = suffix.length
            val prefillStart = clock.elapsedRealtimeMs()
            session.prefill(suffix)
            requestPrefillMs = clock.elapsedRealtimeMs() - prefillStart
            val decodeStart = clock.elapsedRealtimeMs()
            val generated = session.decode().trim { it <= ' ' }
            decodeMs = clock.elapsedRealtimeMs() - decodeStart
            outputCharacters = generated.length
            val parseStart = clock.elapsedRealtimeMs()
            resolveGeneratedSafely(transcript, generated).also {
                parseResolveMs = clock.elapsedRealtimeMs() - parseStart
            }
        }
        metrics.inference(
            FunctionGemmaInferenceMetrics(
                backend = state.pool.backend,
                modelRelease = state.release,
                sessionWaitMs = result.sessionWaitMs,
                requestPrefillMs = requestPrefillMs,
                decodeMs = decodeMs,
                parseResolveMs = parseResolveMs,
                totalMs = clock.elapsedRealtimeMs() - totalStart,
                inputCharacters = inputCharacters,
                outputCharacters = outputCharacters,
                fallbackReason = state.fallbackReason,
                conversationReused = lifecycle.reused,
                reuseCount = lifecycle.reuseCount,
                conversationRotated = lifecycle.rotated,
                rotationCause = lifecycle.rotationCause,
            ),
        )
        return result.value
    }

    private fun buildRequestSuffixSafely(transcript: String): String? {
        return try {
            FunctionGemmaPrompt.requestSuffix(
                transcript = transcript,
                history = dialogManager.promptHistory(),
            ).also { suffix ->
                check(!suffix.contains("<start_function_declaration>")) {
                    "FunctionGemma request suffix contains static declarations"
                }
            }
        } catch (error: Throwable) {
            error.throwIfFatalOrCancellation()
            logRuntimeWarning("FunctionGemma request construction failed", error)
            null
        }
    }

    private fun resolveGeneratedSafely(
        transcript: String,
        generated: String,
    ): VoiceIntent? {
        return try {
            val call = ToolCall.parse(generated) ?: return null
            dialogManager.resolve(transcript, generated, call)
        } catch (error: Throwable) {
            error.throwIfFatalOrCancellation()
            logRuntimeWarning("FunctionGemma output resolution failed", error)
            null
        }
    }

    private suspend fun recoverOnCpuOnce(
        failedGpuState: ActiveState,
        transcript: String,
        gpuFailure: Throwable,
    ): VoiceIntent? {
        metrics.backendFallback(FALLBACK_GPU_INFERENCE, gpuFailure)
        logRuntimeWarning("FunctionGemma GPU inference failed; retrying on CPU", gpuFailure)
        val cpuState = replaceFailedGpuWithCpu(failedGpuState) ?: return null

        return try {
            consumeAndResolve(cpuState, transcript)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            error.throwIfFatal()
            invalidatePool(cpuState)
            logRuntimeWarning("FunctionGemma CPU retry failed", error)
            null
        }
    }

    private suspend fun replaceFailedGpuWithCpu(failedState: ActiveState): ActiveState? {
        while (true) {
            when (val action = planGpuFallback(failedState)) {
                is GpuFallbackAction.UseCpu -> return action.state
                GpuFallbackAction.Stale -> return null
                is GpuFallbackAction.Wait -> action.completion.await()
                is GpuFallbackAction.Replace -> return executeGpuFallback(action)
            }
        }
    }

    private suspend fun planGpuFallback(failedState: ActiveState): GpuFallbackAction = transitionMutex.withLock {
        transitionInProgress?.let { return@withLock GpuFallbackAction.Wait(it) }
        val current = stateMutex.withLock { activeState }
        if (current?.pool?.backend == FunctionGemmaBackend.CPU) {
            return@withLock GpuFallbackAction.UseCpu(current)
        }
        if (current !== failedState) return@withLock GpuFallbackAction.Stale

        val completion = CompletableDeferred<Unit>()
        transitionInProgress = completion
        stateMutex.withLock {
            if (activeState === failedState) activeState = null
        }
        GpuFallbackAction.Replace(failedState, completion)
    }

    private suspend fun executeGpuFallback(action: GpuFallbackAction.Replace): ActiveState? {
        return try {
            action.failed.pool.close()
            val cpuPool = createPreparedPool(FunctionGemmaBackend.CPU, action.failed.release)
            val replacement = ActiveState(
                release = action.failed.release,
                pool = cpuPool,
                fallbackReason = FALLBACK_GPU_INFERENCE,
            )
            transitionMutex.withLock {
                check(transitionInProgress === action.completion)
                stateMutex.withLock {
                    activeState = replacement
                }
                transitionInProgress = null
                action.completion.complete(Unit)
            }
            replacement
        } catch (error: CancellationException) {
            failTransition(action.completion, error)
            throw error
        } catch (error: Throwable) {
            error.throwIfFatal()
            failTransition(action.completion, error)
            logRuntimeWarning("FunctionGemma CPU fallback initialization failed", error)
            null
        }
    }

    private suspend fun invalidatePool(state: ActiveState) {
        while (true) {
            val action = transitionMutex.withLock {
                transitionInProgress?.let { return@withLock InvalidationAction.Wait(it) }
                beforePoolInvalidation()
                val shouldClose = stateMutex.withLock {
                    if (activeState === state) {
                        activeState = null
                        true
                    } else {
                        false
                    }
                }
                if (shouldClose) InvalidationAction.Close else InvalidationAction.Stale
            }
            when (action) {
                InvalidationAction.Close -> {
                    state.pool.close()
                    return
                }

                InvalidationAction.Stale -> return

                is InvalidationAction.Wait -> action.completion.await()
            }
        }
    }

    private suspend fun failTransition(
        completion: CompletableDeferred<Unit>,
        error: Throwable,
    ) {
        transitionMutex.withLock {
            if (transitionInProgress === completion) {
                transitionInProgress = null
                completion.completeExceptionally(error)
            }
        }
    }

    private fun logRuntimeWarning(
        message: String,
        error: Throwable,
    ) {
        Timber.w(
            "%s (%s: %s)",
            message,
            error::class.java.simpleName,
            error.message.orEmpty().take(MAX_LOGGED_ERROR_CHARS),
        )
    }

    private fun Throwable.throwIfFatalOrCancellation() {
        if (this is CancellationException) throw this
        throwIfFatal()
    }

    private fun Throwable.throwIfFatal() {
        if (this is VirtualMachineError || this is ThreadDeath) throw this
    }

    override fun release() {
        val pool = runBlocking {
            while (true) {
                val transition = transitionMutex.withLock {
                    transitionInProgress
                }
                if (transition == null) {
                    return@runBlocking transitionMutex.withLock {
                        stateMutex.withLock {
                            activeState.also { activeState = null }
                        }?.pool
                    }
                }
                runCatching { transition.await() }
            }
            @Suppress("UNREACHABLE_CODE")
            null
        }
        pool?.close()
    }

    private data class ActiveState(
        val release: String,
        val pool: PreparedFunctionGemmaSessionPool,
        val fallbackReason: String?,
    )

    private sealed interface ReadinessAction {
        data object Ready : ReadinessAction

        data class Wait(val completion: CompletableDeferred<Unit>) : ReadinessAction

        data class Prepare(
            val previous: ActiveState?,
            val release: String,
            val completion: CompletableDeferred<Unit>,
        ) : ReadinessAction
    }

    private sealed interface GpuFallbackAction {
        data class UseCpu(val state: ActiveState) : GpuFallbackAction

        data object Stale : GpuFallbackAction

        data class Wait(val completion: CompletableDeferred<Unit>) : GpuFallbackAction

        data class Replace(
            val failed: ActiveState,
            val completion: CompletableDeferred<Unit>,
        ) : GpuFallbackAction
    }

    private sealed interface InvalidationAction {
        data object Close : InvalidationAction

        data object Stale : InvalidationAction

        data class Wait(val completion: CompletableDeferred<Unit>) : InvalidationAction
    }

    private companion object {
        const val MAX_LOGGED_ERROR_CHARS = 200
        const val FALLBACK_GPU_INIT = "gpu_init"
        const val FALLBACK_GPU_INFERENCE = "gpu_inference"
    }
}
