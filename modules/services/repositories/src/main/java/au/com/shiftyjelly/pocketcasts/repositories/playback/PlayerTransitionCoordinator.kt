package au.com.shiftyjelly.pocketcasts.repositories.playback

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orders asynchronous player transitions and serializes access to the active [Player].
 *
 * A transition receives its version when the command is issued. Work that finishes after a newer command is ignored,
 * while current work runs exclusively so a player cannot be replaced while it is being configured or started.
 */
internal class PlayerTransitionCoordinator {
    class Token internal constructor(internal val version: Long)
    class Snapshot internal constructor(internal val version: Long)
    class EventTransition internal constructor(
        val token: Token,
        internal val eventSourceToken: EventSourceToken,
    )
    class EventSourceToken internal constructor(
        internal val source: Any,
        internal val generation: Long,
        internal val transitionVersion: Long,
        internal val isAcceptingEvents: Boolean,
    )

    private val mutex = Mutex()
    private val stateLock = Any()
    private val version = AtomicLong()
    private val settledVersion = AtomicLong()
    private val eventSourceGeneration = AtomicLong()
    private val eventSource = AtomicReference<EventSourceToken?>()
    private val stateRevision = MutableStateFlow(0L)

    fun beginTransition(): Token = Token(version.incrementAndGet())

    fun snapshot(): Snapshot = Snapshot(version.get())

    /**
     * Starts a transition only when no newer transition has begun since [snapshot] was captured.
     */
    fun tryBeginTransition(snapshot: Snapshot): Token? {
        val nextVersion = snapshot.version + 1
        return if (version.compareAndSet(snapshot.version, nextVersion)) {
            Token(nextVersion)
        } else {
            null
        }
    }

    fun isCurrent(token: Token): Boolean = token.version == version.get()

    /**
     * Waits until the newest transition has completed and returns a snapshot that can be claimed without overtaking
     * an in-flight command.
     */
    suspend fun awaitSettledSnapshot(): Snapshot {
        while (true) {
            val observedRevision = stateRevision.value
            val currentVersion = version.get()
            if (settledVersion.get() == currentVersion) {
                return Snapshot(currentVersion)
            }
            stateRevision.first { it != observedRevision }
        }
    }

    /**
     * Starts a new generation for callbacks from [source].
     *
     * Event-source generations change only when a player is created or configured, not when transition preparation
     * begins. This keeps live-player events flowing during preparation while rejecting callbacks queued by an older
     * configuration of the same player.
     */
    fun bindEventSource(source: Any, token: Token) {
        synchronized(stateLock) {
            eventSource.set(
                EventSourceToken(
                    source = source,
                    generation = eventSourceGeneration.incrementAndGet(),
                    transitionVersion = token.version,
                    isAcceptingEvents = true,
                ),
            )
            signalStateChange()
        }
    }

    fun tokenForEventSource(source: Any): EventSourceToken? {
        return eventSource.get()?.takeIf { it.source === source && it.isAcceptingEvents }
    }

    fun hasEventSource(source: Any): Boolean {
        return eventSource.get()?.source === source
    }

    /**
     * Returns whether [source] is still reserved by a terminal event that predates [completingToken].
     *
     * A newer no-op command must preserve that reservation while the terminal handler retries its final player
     * transition. The terminal transition that owns the reservation must still validate the final queue/player state.
     */
    fun hasInactiveEventSourceFromEarlierTransition(
        source: Any,
        completingToken: Token,
    ): Boolean {
        val current = eventSource.get()
        return current?.source === source &&
            !current.isAcceptingEvents &&
            current.transitionVersion != completingToken.version
    }

    fun isCurrentEventSource(token: EventSourceToken): Boolean {
        val current = eventSource.get()
        return current?.source === token.source &&
            current.generation == token.generation &&
            current.isAcceptingEvents
    }

    fun clearEventSource(source: Any) {
        synchronized(stateLock) {
            val previous = eventSource.getAndUpdate { current ->
                current?.takeUnless { it.source === source }
            }
            if (previous?.source === source) {
                signalStateChange()
            }
        }
    }

    /**
     * Completes [token] after all earlier player commits have left the serialized section.
     *
     * An unchanged live source adopts the completed transition without changing its generation, so passive callbacks
     * remain valid. A cleared or replaced source is never rebound by completion.
     */
    suspend fun completeTransition(
        token: Token,
        sourceProvider: suspend () -> Any?,
    ): Boolean = mutex.withLock {
        if (!isCurrent(token)) {
            false
        } else {
            val source = sourceProvider()
            if (!isCurrent(token)) {
                false
            } else {
                synchronized(stateLock) {
                    val current = eventSource.get()
                    if (source != null && current?.source === source) {
                        val shouldRemainInactive = !current.isAcceptingEvents &&
                            current.transitionVersion != token.version
                        eventSource.set(
                            EventSourceToken(
                                source = source,
                                generation = current.generation,
                                transitionVersion = token.version,
                                isAcceptingEvents = !shouldRemainInactive,
                            ),
                        )
                    } else {
                        eventSource.set(null)
                    }
                    settledVersion.set(token.version)
                    signalStateChange()
                }
                true
            }
        }
    }

    /**
     * Waits for in-flight commands that have not changed [eventSourceToken]'s source generation, then atomically
     * claims the source for a terminal event. Claiming starts a new generation so callbacks queued beside the
     * terminal event cannot update the next playback state.
     */
    suspend fun beginTransitionForEventSource(eventSourceToken: EventSourceToken): EventTransition? {
        return beginEventTransition(eventSourceToken, expectedToAcceptEvents = true)
    }

    suspend fun retryTransitionForEventSource(eventSourceToken: EventSourceToken): EventTransition? {
        return beginEventTransition(eventSourceToken, expectedToAcceptEvents = false)
    }

    private suspend fun beginEventTransition(
        eventSourceToken: EventSourceToken,
        expectedToAcceptEvents: Boolean,
    ): EventTransition? {
        while (true) {
            val observedRevision = stateRevision.value
            val result = mutex.withLock {
                synchronized(stateLock) {
                    val current = eventSource.get()
                    val isCurrentSource = current?.source === eventSourceToken.source &&
                        current.generation == eventSourceToken.generation &&
                        current.isAcceptingEvents == expectedToAcceptEvents
                    if (!isCurrentSource) {
                        return@synchronized EventTransitionResult.Stale
                    }

                    val currentVersion = version.get()
                    if (current.transitionVersion != currentVersion || settledVersion.get() != currentVersion) {
                        return@synchronized EventTransitionResult.Pending
                    }

                    val nextVersion = currentVersion + 1
                    if (!version.compareAndSet(currentVersion, nextVersion)) {
                        return@synchronized EventTransitionResult.Pending
                    }

                    val eventGeneration = eventSourceGeneration.incrementAndGet()
                    val claimedSource = EventSourceToken(
                        source = eventSourceToken.source,
                        generation = eventGeneration,
                        transitionVersion = nextVersion,
                        isAcceptingEvents = false,
                    )
                    eventSource.set(
                        claimedSource,
                    )
                    signalStateChange()
                    EventTransitionResult.Started(
                        EventTransition(
                            token = Token(nextVersion),
                            eventSourceToken = claimedSource,
                        ),
                    )
                }
            }
            when (result) {
                EventTransitionResult.Pending -> stateRevision.first { it != observedRevision }
                EventTransitionResult.Stale -> return null
                is EventTransitionResult.Started -> return result.transition
            }
        }
    }

    suspend fun runIfEventSourceCurrent(
        token: EventSourceToken,
        block: suspend () -> Unit,
    ): Boolean = mutex.withLock {
        if (!isCurrentEventSource(token)) {
            false
        } else {
            block()
            true
        }
    }

    suspend fun runIfCurrent(
        token: Token,
        block: suspend () -> Unit,
    ): Boolean = mutex.withLock {
        if (!isCurrent(token)) {
            false
        } else {
            block()
            true
        }
    }

    private fun signalStateChange() {
        stateRevision.value = stateRevision.value + 1
    }

    private sealed interface EventTransitionResult {
        data object Pending : EventTransitionResult
        data object Stale : EventTransitionResult
        data class Started(val transition: EventTransition) : EventTransitionResult
    }
}
