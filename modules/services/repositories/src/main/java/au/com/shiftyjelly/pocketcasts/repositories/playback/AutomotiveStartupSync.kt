package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import androidx.annotation.VisibleForTesting
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsThread
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Runs the Android Automotive start-up sync once per car start, so the first play can wait for
 * it and resume the episode and position the account was last at rather than whatever the car
 * had locally.
 *
 * The car app only refreshes once at process start. Cars usually start the app before their
 * data connection is up, so that attempt fails in the first second and nothing retries until
 * the user taps Refresh now in Settings. Playback then starts from the stale local state and
 * pushes that position back to the account, which is the long-running Automotive sync issue.
 *
 * One attempt per process start, bounded by [NETWORK_TIMEOUT] and [REFRESH_TIMEOUT]. If the
 * network never comes up or the refresh fails, the held play goes ahead from the local state as
 * it does today. Signed-out users are never held, since there is nothing to sync.
 *
 * The sync runs in its own job on the application scope, so a caller that stops waiting (for
 * example a held play that the user cancelled with pause) never cancels the sync itself.
 *
 * On other platforms this is a no-op: [isPending] is always false.
 */
@Singleton
class AutomotiveStartupSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: Settings,
    private val syncManager: SyncManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val isAutomotive = Util.isAutomotive(context)
    private val mutex = Mutex()

    @VisibleForTesting
    internal var networkTimeout = NETWORK_TIMEOUT

    @VisibleForTesting
    internal var refreshTimeout = REFRESH_TIMEOUT

    @Volatile
    private var done = !isAutomotive

    @Volatile
    private var processStartMs = System.currentTimeMillis()

    private var syncJob: Deferred<Unit>? = null

    /** True while the start-up sync has not yet run for this process. */
    val isPending: Boolean
        get() = !done

    /** Called when the Automotive playback service is created, so each car start gets one sync. */
    fun reset() {
        if (!isAutomotive) return
        processStartMs = System.currentTimeMillis()
        syncJob = null
        done = false
    }

    /** The network and refresh state at this moment, for the log line that records a held play. */
    fun describeState(): String = "networkConnected=${Network.isConnected(context)}, " +
        "refreshState=${settings.getRefreshState()}, lastRefresh=${settings.getLastRefreshDate()}"

    /**
     * Waits for the start-up sync, starting it if nothing else has. Cancelling the caller stops
     * the wait but not the sync.
     */
    suspend fun awaitSynced() {
        if (done) return
        val job = mutex.withLock {
            if (done) return
            syncJob ?: applicationScope.async {
                try {
                    runStartupSync()
                } finally {
                    done = true
                }
            }.also { syncJob = it }
        }
        job.await()
    }

    private suspend fun runStartupSync() {
        if (!syncManager.isLoggedIn()) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Automotive start-up sync: skipped, not signed in")
            return
        }
        if (settings.getLastRefreshTime() >= processStartMs && settings.getRefreshState() is RefreshState.Success) {
            LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Automotive start-up sync: skipped, already refreshed since start")
            return
        }

        val startMs = System.currentTimeMillis()
        if (!waitForNetwork()) {
            LogBuffer.i(
                LogBuffer.TAG_PLAYBACK,
                "Automotive start-up sync: no network after ${networkTimeout.inWholeSeconds} s, playing from local state",
            )
            return
        }
        LogBuffer.i(LogBuffer.TAG_PLAYBACK, "Automotive start-up sync: network up after ${System.currentTimeMillis() - startMs} ms, refreshing")

        // The start-up attempt may have reached the network check and set the run-now throttle
        // before its sync failed; clear it so this refresh isn't skipped as "too soon".
        RefreshPodcastsThread.clearLastRefreshTime()
        // The refresh runs on the application scope, not as a child of the timeout: the refresh
        // thread blocks and ignores cancellation, so a child would make the timeout wait for the
        // whole refresh, and cancelling it could abort an account sync partway through. The
        // timeout only bounds how long the held play waits.
        val refresh = applicationScope.async { RefreshPodcastsTask.runNowSync(context, applicationScope) }
        val finished = withTimeoutOrNull(refreshTimeout) {
            refresh.await()
            true
        } ?: false
        LogBuffer.i(
            LogBuffer.TAG_PLAYBACK,
            "Automotive start-up sync: refresh ${if (finished) "finished" else "still running"} after ${System.currentTimeMillis() - startMs} ms, " +
                "state=${settings.getRefreshState()}",
        )
    }

    private suspend fun waitForNetwork(): Boolean {
        val deadlineMs = System.currentTimeMillis() + networkTimeout.inWholeMilliseconds
        while (!Network.isConnected(context)) {
            if (System.currentTimeMillis() >= deadlineMs) return false
            delay(NETWORK_POLL_INTERVAL)
        }
        return true
    }

    companion object {
        private val NETWORK_TIMEOUT = 15.seconds
        private val REFRESH_TIMEOUT = 20.seconds
        private val NETWORK_POLL_INTERVAL = 500.milliseconds
    }
}
