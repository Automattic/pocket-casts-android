package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.repositories.di.ProcessLifecycle
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.PlaybackForegroundServiceErrorEvent
import com.automattic.eventhorizon.PlaybackServiceStartErrorEvent
import com.automattic.eventhorizon.PlaybackServiceType
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

// Reports the two ways playback runs with no foreground service: the service failing to start, or to reach foreground.
@Singleton
class PlaybackServiceErrorReporter @Inject constructor(
    private val eventHorizon: EventHorizon,
    @ProcessLifecycle private val processLifecycleOwner: LifecycleOwner,
) {
    private val foregroundFailureCount = AtomicInteger()

    // Call when playback starts and whenever the service reaches the foreground, so occurrence counts from 1 again.
    fun resetFailureCount() {
        foregroundFailureCount.set(0)
    }

    fun trackServiceStartFailed(
        service: PlaybackServiceType,
        error: Throwable,
        source: SourceView?,
    ) {
        eventHorizon.track(
            PlaybackServiceStartErrorEvent(
                service = service,
                appInBackground = isAppInBackground(),
                exception = error.javaClass.name,
                source = source?.analyticsValue,
            ),
        )
    }

    fun trackForegroundStartFailed(
        service: PlaybackServiceType,
        error: Throwable,
        source: SourceView?,
        playbackContinued: Boolean,
    ) {
        eventHorizon.track(
            PlaybackForegroundServiceErrorEvent(
                service = service,
                appInBackground = isAppInBackground(),
                playbackContinued = playbackContinued,
                occurrence = foregroundFailureCount.incrementAndGet().toLong(),
                exception = error.javaClass.name,
                source = source?.analyticsValue,
            ),
        )
    }

    private fun isAppInBackground() = !processLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}
