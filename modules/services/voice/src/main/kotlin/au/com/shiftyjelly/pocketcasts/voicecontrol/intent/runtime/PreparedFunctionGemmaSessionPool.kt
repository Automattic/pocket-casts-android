package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime

import android.os.SystemClock
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SessionPreparationMetrics(
    val sessionCreateMs: Long,
    val staticPrefillMs: Long,
)

data class PreparedSessionResult<T>(
    val value: T,
    val sessionWaitMs: Long,
)

data class SessionLifecycle(
    val reused: Boolean,
    val reuseCount: Int,
    val rotated: Boolean,
    val rotationCause: String?,
)

class PreparedFunctionGemmaSessionPool internal constructor(
    private val runtime: FunctionGemmaRuntime,
    private val scope: CoroutineScope,
    private val elapsedRealtimeMs: () -> Long,
    private val maxContextTokens: Int,
    private val beforePrepareEngineLock: () -> Unit,
    private val beforeConsumerMutex: () -> Unit = {},
    private val beforeAvailabilityAwait: () -> Unit = {},
    private val afterReplacementPublication: () -> Unit = {},
) : Closeable {
    constructor(
        runtime: FunctionGemmaRuntime,
        scope: CoroutineScope,
        maxContextTokens: Int = 2048,
        elapsedRealtimeMs: () -> Long = SystemClock::elapsedRealtime,
    ) : this(runtime, scope, elapsedRealtimeMs, maxContextTokens, {}, {}, {}, {})

    val backend get() = runtime.backend

    private val consumerMutex = Mutex()
    private val engineLock = ReentrantLock()
    private val stateLock = Any()

    private var lifecycle: Lifecycle = Lifecycle.Open
    private var staticPrefix: String? = null
    private var availability = CompletableDeferred<FunctionGemmaSession>()
    private var preparedSession: FunctionGemmaSession? = null
    private var preparedSessionOwner: Job? = null
    private var replacementJob: Job? = null
    private var activeConsumerThread: Thread? = null

    private var reuseCount: Int = 0
    private var baselineTokenCount: Int = -1

    suspend fun prepare(prefix: String): SessionPreparationMetrics {
        val context = currentCoroutineContext()
        context.ensureActive()
        val targetAvailability = synchronized(stateLock) {
            ensureOpen()
            check(staticPrefix == null) { "FunctionGemma session pool is already prepared" }
            staticPrefix = prefix
            availability
        }

        return try {
            beforePrepareEngineLock()
            engineLock.withLock {
                context.ensureActive()
                synchronized(stateLock) {
                    ensureOpen()
                }
                val (session, metrics) = createPreparedSession(prefix, rotate = false)
                publishPreparedSessionOrClose(session, targetAvailability, owner = null, context::ensureActive)
                metrics
            }
        } catch (error: Throwable) {
            transitionToFailure(error)
            throw error
        }
    }

    suspend fun <T> consume(
        block: (FunctionGemmaSession) -> T,
    ): Pair<PreparedSessionResult<T>, SessionLifecycle> {
        beforeConsumerMutex()
        return consumerMutex.withLock {
            val waitStart = elapsedRealtimeMs()
            beforeAvailabilityAwait()
            val signalledSession = availability.await()
            val sessionWaitMs = elapsedRealtimeMs() - waitStart
            // Check pool is still open before acquiring engineLock.
            // If close() won the race (lifecycle is Closing/Closed/Failed),
            // throw CancellationException so the consumer coroutine is
            // cancelled rather than failing with IllegalStateException.
            ensureOpenOrCancelled()
            engineLock.withLock {
                val session = claimPreparedSession(signalledSession)

                val (value, lifecycle) = executeLease(session, sessionWaitMs, block)

                @Suppress("UNCHECKED_CAST")
                return Pair(PreparedSessionResult(value, sessionWaitMs), lifecycle)
            }
        }
    }

    override fun close() {
        val closeAction = synchronized(stateLock) {
            check(activeConsumerThread !== Thread.currentThread()) {
                "Cannot close FunctionGemma session pool from an active consumer"
            }
            when (val state = lifecycle) {
                Lifecycle.Open,
                is Lifecycle.Failed,
                -> {
                    val completion = CountDownLatch(1)
                    lifecycle = Lifecycle.Closing(completion)
                    CloseAction.Perform(
                        completion = completion,
                        replacementJob = replacementJob.also { replacementJob = null },
                        availability = availability,
                    )
                }

                is Lifecycle.Closing -> CloseAction.Wait(state.completion)

                Lifecycle.Closed -> CloseAction.None
            }
        }

        when (closeAction) {
            CloseAction.None -> return

            is CloseAction.Wait -> {
                closeAction.completion.await()
                return
            }

            is CloseAction.Perform -> performClose(closeAction)
        }
    }

    private fun <T> executeLease(
        session: FunctionGemmaSession,
        sessionWaitMs: Long,
        block: (FunctionGemmaSession) -> T,
    ): Pair<T, SessionLifecycle> {
        synchronized(stateLock) {
            activeConsumerThread = Thread.currentThread()
        }

        var blockFailure: Throwable? = null
        var value: T? = null
        try {
            value = block(session)
        } catch (error: Throwable) {
            blockFailure = error
        }

        // Rotation decision based purely on token budget: if accumulated turns
        // would overflow the context window, close this conversation and let the
        // replacement create a fresh one. The system-instruction baseline is
        // excluded so only conversation turns count against the limit.
        val shouldRotate = if (blockFailure != null || session.tokenCount <= 0) {
            false
        } else if (baselineTokenCount < 0) {
            baselineTokenCount = session.tokenCount
            false
        } else {
            val turnTokens = session.tokenCount - baselineTokenCount
            val available = maxContextTokens - baselineTokenCount
            available > 0 && turnTokens > available * 0.8f
        }

        val lifecycle = if (blockFailure != null) {
            SessionLifecycle(reused = false, reuseCount = reuseCount, rotated = false, rotationCause = null)
        } else if (shouldRotate) {
            SessionLifecycle(reused = false, reuseCount = reuseCount, rotated = true, rotationCause = "token_limit")
        } else {
            SessionLifecycle(reused = true, reuseCount = reuseCount + 1, rotated = false, rotationCause = null)
        }

        if (shouldRotate) {
            reuseCount = 0
            baselineTokenCount = -1 // reset for the new conversation
        } else if (blockFailure == null) {
            reuseCount++
        }

        var closeFailure: Throwable? = null
        try {
            session.close()
        } catch (error: Throwable) {
            closeFailure = error
            transitionToFailure(error)
        } finally {
            synchronized(stateLock) {
                activeConsumerThread = null
            }
        }

        if (blockFailure != null) {
            if (closeFailure == null) {
                scheduleReplacement()
            } else {
                closeFailure.let(blockFailure::addSuppressed)
            }
            throw blockFailure
        }

        if (closeFailure == null) {
            scheduleReplacement(rotate = shouldRotate)
        }

        // value is non-null here because blockFailure was null (block didn't throw).
        // When T is nullable (e.g., VoiceIntent?), value may be null legitimately.
        @Suppress("UNCHECKED_CAST")
        val result = value as T
        return Pair(result, lifecycle)
    }

    private fun performClose(action: CloseAction.Perform) {
        action.replacementJob?.cancel()
        action.availability.cancel(CancellationException("FunctionGemma session pool closed"))

        var closeFailure: Throwable? = null
        try {
            engineLock.withLock {
                val session = synchronized(stateLock) {
                    preparedSession.also {
                        preparedSession = null
                        preparedSessionOwner = null
                        staticPrefix = null
                    }
                }
                try {
                    session?.close()
                } catch (error: Throwable) {
                    closeFailure = error
                }
                try {
                    runtime.close()
                } catch (error: Throwable) {
                    closeFailure?.addSuppressed(error) ?: run { closeFailure = error }
                }
            }
        } finally {
            synchronized(stateLock) {
                lifecycle = Lifecycle.Closed
            }
            action.completion.countDown()
        }
        closeFailure?.let { throw it }
    }

    private fun claimPreparedSession(
        signalledSession: FunctionGemmaSession,
    ): FunctionGemmaSession = synchronized(stateLock) {
        ensureOpen()
        check(preparedSession === signalledSession) { "Prepared FunctionGemma session is no longer available" }
        preparedSession = null
        preparedSessionOwner = null
        availability = CompletableDeferred()
        signalledSession
    }

    private fun scheduleReplacement(rotate: Boolean = false) {
        val targetAvailability: CompletableDeferred<FunctionGemmaSession>
        val prefix: String
        synchronized(stateLock) {
            if (lifecycle !== Lifecycle.Open) return
            prefix = checkNotNull(staticPrefix) { "FunctionGemma session pool has not been prepared" }
            targetAvailability = availability
        }

        lateinit var job: Job
        var replacementFailure: Throwable? = null
        job = scope.launch(start = CoroutineStart.LAZY) {
            try {
                val context = currentCoroutineContext()
                engineLock.withLock {
                    context.ensureActive()
                    synchronized(stateLock) {
                        ensureOpen()
                    }
                    val (session) = createPreparedSession(prefix, rotate)
                    publishPreparedSessionOrClose(session, targetAvailability, job, context::ensureActive)
                    afterReplacementPublication()
                    context.ensureActive()
                }
            } catch (error: Throwable) {
                replacementFailure = error
                if (error is CancellationException) throw error
            }
        }
        job.invokeOnCompletion { cause ->
            synchronized(stateLock) {
                if (replacementJob === job) {
                    replacementJob = null
                }
            }
            val failure = cause ?: replacementFailure
            if (failure != null) {
                closePublishedReplacement(job, targetAvailability, failure)
                transitionToFailure(failure)
            }
        }

        val shouldStart = synchronized(stateLock) {
            if (lifecycle !== Lifecycle.Open) {
                false
            } else {
                replacementJob = job
                true
            }
        }
        if (shouldStart) {
            job.start()
        } else {
            job.cancel()
        }
    }

    /**
     * LiteRT session creation and prefill are synchronous native calls and cannot be preempted while executing.
     * Callers must check cancellation immediately before and after this method.
     */
    private fun createPreparedSession(
        prefix: String,
        rotate: Boolean,
    ): Pair<FunctionGemmaSession, SessionPreparationMetrics> {
        val createStart = elapsedRealtimeMs()
        val session = if (rotate) {
            runtime.createSessionWithNewConversation(prefix)
        } else {
            runtime.createSession()
        }
        val sessionCreateMs = elapsedRealtimeMs() - createStart
        return try {
            if (!rotate) {
                val prefillStart = elapsedRealtimeMs()
                session.prefill(prefix)
                val staticPrefillMs = elapsedRealtimeMs() - prefillStart
                session to SessionPreparationMetrics(
                    sessionCreateMs = sessionCreateMs,
                    staticPrefillMs = staticPrefillMs,
                )
            } else {
                session to SessionPreparationMetrics(
                    sessionCreateMs = sessionCreateMs,
                    staticPrefillMs = sessionCreateMs,
                )
            }
        } catch (error: Throwable) {
            closeAfterFailure(session, error)
            throw error
        }
    }

    private fun publishPreparedSessionOrClose(
        session: FunctionGemmaSession,
        targetAvailability: CompletableDeferred<FunctionGemmaSession>,
        owner: Job?,
        ensureActive: () -> Unit,
    ) {
        try {
            ensureActive()
            synchronized(stateLock) {
                ensureOpen()
                check(availability === targetAvailability) { "Prepared FunctionGemma availability changed" }
                check(preparedSession == null) { "FunctionGemma session pool capacity exceeded" }
                preparedSession = session
                preparedSessionOwner = owner
                targetAvailability.complete(session)
            }
        } catch (error: Throwable) {
            closeAfterFailure(session, error)
            throw error
        }
    }

    private fun closePublishedReplacement(
        owner: Job,
        targetAvailability: CompletableDeferred<FunctionGemmaSession>,
        primaryFailure: Throwable,
    ) {
        engineLock.withLock {
            val session = synchronized(stateLock) {
                if (
                    availability === targetAvailability &&
                    preparedSessionOwner === owner
                ) {
                    preparedSession.also {
                        preparedSession = null
                        preparedSessionOwner = null
                    }
                } else {
                    null
                }
            }
            session?.let { closeAfterFailure(it, primaryFailure) }
        }
    }

    private fun transitionToFailure(error: Throwable): Boolean {
        val targetAvailability = synchronized(stateLock) {
            if (lifecycle !== Lifecycle.Open) return false
            lifecycle = Lifecycle.Failed(error)
            staticPrefix = null
            availability
        }
        targetAvailability.completeExceptionally(error)
        return true
    }

    private fun closeAfterFailure(
        session: FunctionGemmaSession,
        primaryFailure: Throwable,
    ) {
        try {
            session.close()
        } catch (closeFailure: Throwable) {
            primaryFailure.addSuppressed(closeFailure)
        }
    }

    private fun ensureOpen() {
        when (val state = lifecycle) {
            Lifecycle.Open -> Unit
            is Lifecycle.Failed -> throw state.cause
            is Lifecycle.Closing -> error("FunctionGemma session pool is closing")
            Lifecycle.Closed -> error("FunctionGemma session pool is closed")
        }
    }

    /**
     * Like [ensureOpen] but throws [CancellationException] when the pool is closing or closed.
     * Used by [consume] after [availability.await] to fail-fast before acquiring [engineLock],
     * so the consumer's coroutine is properly cancelled instead of failing with an unchecked error.
     */
    private fun ensureOpenOrCancelled() {
        synchronized(stateLock) {
            when (val state = lifecycle) {
                Lifecycle.Open -> Unit
                is Lifecycle.Failed -> throw CancellationException("FunctionGemma session pool failed", state.cause)
                is Lifecycle.Closing, Lifecycle.Closed -> throw CancellationException("FunctionGemma session pool closed")
            }
        }
    }

    private sealed interface Lifecycle {
        data object Open : Lifecycle

        data class Failed(val cause: Throwable) : Lifecycle

        data class Closing(val completion: CountDownLatch) : Lifecycle

        data object Closed : Lifecycle
    }

    private sealed interface CloseAction {
        data object None : CloseAction

        data class Wait(val completion: CountDownLatch) : CloseAction

        data class Perform(
            val completion: CountDownLatch,
            val replacementJob: Job?,
            val availability: CompletableDeferred<FunctionGemmaSession>,
        ) : CloseAction
    }
}
