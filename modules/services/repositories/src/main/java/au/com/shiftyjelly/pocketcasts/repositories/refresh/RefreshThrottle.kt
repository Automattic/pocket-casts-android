package au.com.shiftyjelly.pocketcasts.repositories.refresh

import java.time.Clock

/**
 * Limits how often podcasts are refreshed.
 *
 * An attempt records its start time as soon as it begins. That keeps a background and a manual refresh from running
 * at the same time. A refresh that is cancelled or fails before it gets anything done must [Attempt.abort] its attempt,
 * otherwise it would block the refreshes that follow even though nothing was refreshed.
 */
internal class RefreshThrottle(
    private val clock: Clock,
    private val runNowIntervalMs: Long,
    private val periodicIntervalMs: Long,
) {
    private var lastStartMs = NEVER_MS

    /**
     * Returns an [Attempt] and records its start, or null if the last attempt was too recent.
     */
    @Synchronized
    fun tryStart(runNow: Boolean): Attempt? {
        val nowMs = clock.millis()
        val intervalMs = if (runNow) runNowIntervalMs else periodicIntervalMs
        if (nowMs <= lastStartMs + intervalMs) {
            return null
        }
        val attempt = Attempt(startMs = nowMs, previousStartMs = lastStartMs)
        lastStartMs = nowMs
        return attempt
    }

    /**
     * Runs [block] unless the last attempt was too recent. Returns whether [block] was run.
     *
     * [block] returns true if it refreshed something. If it returns false or throws, its attempt is aborted.
     */
    fun tryRun(runNow: Boolean, block: () -> Boolean): Boolean {
        val attempt = tryStart(runNow) ?: return false
        var refreshed = false
        try {
            refreshed = block()
        } finally {
            if (!refreshed) {
                attempt.abort()
            }
        }
        return true
    }

    @Synchronized
    fun reset() {
        lastStartMs = NEVER_MS
    }

    inner class Attempt internal constructor(
        private val startMs: Long,
        private val previousStartMs: Long,
    ) {
        /**
         * Gives back this attempt's slot so the next attempt isn't throttled by it.
         * Does nothing if a newer attempt has started since, so that attempt keeps its own throttling.
         */
        fun abort() {
            synchronized(this@RefreshThrottle) {
                if (lastStartMs == startMs) {
                    lastStartMs = previousStartMs
                }
            }
        }
    }

    private companion object {
        const val NEVER_MS = -10_000L
    }
}
