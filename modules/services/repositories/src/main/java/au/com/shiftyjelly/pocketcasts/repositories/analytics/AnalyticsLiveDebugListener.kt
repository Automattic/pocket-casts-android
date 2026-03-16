package au.com.shiftyjelly.pocketcasts.repositories.analytics

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsListener
import au.com.shiftyjelly.pocketcasts.analytics.TrackedEvent
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.analytics.AnalyticsLiveServiceManager
import au.com.shiftyjelly.pocketcasts.servers.analytics.EventProperties
import au.com.shiftyjelly.pocketcasts.servers.analytics.InputEvent
import com.automattic.eventhorizon.Trackable
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Listens for analytics events and forwards them in batches to a server to help us debug issues.
 */

private const val MAX_BATCH_SIZE = 100
private const val FLUSH_DELAY_MS = 500L
private const val ERROR_BACKOFF_MS = 30_000L

@Singleton
class AnalyticsLiveDebugListener @Inject constructor(
    private val settings: Settings,
    private val analyticsLiveServiceManager: AnalyticsLiveServiceManager,
    private val clock: Clock,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) : AnalyticsListener {
    private val pendingEvents = Channel<InputEvent>(capacity = 1_000, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private var flushJob: Job? = null

    init {
        coroutineScope.launch {
            // Only send events if we turn it on server side, and analytics aren't turned off in the privacy settings.
            combine(
                settings.liveAnalyticsUrl.flow,
                settings.collectAnalytics.flow,
            ) { url, collectAnalytics ->
                if (url.isNotBlank() && collectAnalytics) url else null
            }
                .distinctUntilChanged()
                .collect { url ->
                    if (url != null) {
                        startFlushing(url)
                    } else {
                        stopFlushing()
                    }
                }
        }
    }

    private fun startFlushing(url: String) {
        flushJob?.cancel()
        flushJob = coroutineScope.launch {
            while (isActive) {
                // Send the first event as soon as it comes in.
                val first = pendingEvents.receiveCatching().getOrNull() ?: break
                val batch = mutableListOf(first)
                // Send events in batches if there are more available.
                while (batch.size < MAX_BATCH_SIZE) {
                    val next = pendingEvents.tryReceive().getOrNull() ?: break
                    batch.add(next)
                }
                try {
                    analyticsLiveServiceManager.sendEvents(url, batch)
                    delay(FLUSH_DELAY_MS)
                } catch (exception: Exception) {
                    Timber.e(exception, "Failed to send analytics events")
                    delay(ERROR_BACKOFF_MS)
                }
            }
        }
    }

    private fun stopFlushing() {
        flushJob?.cancel()
        flushJob = null
    }

    override fun onEvent(event: Trackable, trackedEvents: Map<String, TrackedEvent?>) {
        val inputEvent = InputEvent(
            name = event.analyticsName,
            timestamp = clock.instant(),
            properties = EventProperties(event.analyticsProperties),
            platform = "Android",
        )
        pendingEvents.trySend(inputEvent)
    }
}
