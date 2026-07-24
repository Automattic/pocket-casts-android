package au.com.shiftyjelly.pocketcasts.repositories.playback

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
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

    private data class EventSource(
        val source: Any,
        val token: Token,
    )

    private val mutex = Mutex()
    private val version = AtomicLong()
    private val eventSource = AtomicReference<EventSource?>()

    fun beginTransition(): Token = Token(version.incrementAndGet())

    fun isCurrent(token: Token): Boolean = token.version == version.get()

    fun bindEventSource(source: Any, token: Token) {
        eventSource.set(EventSource(source, token))
    }

    fun tokenForEventSource(source: Any): Token? {
        return eventSource.get()?.takeIf { it.source === source }?.token
    }

    fun clearEventSource(source: Any) {
        eventSource.updateAndGet { current ->
            current?.takeUnless { it.source === source }
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
}
