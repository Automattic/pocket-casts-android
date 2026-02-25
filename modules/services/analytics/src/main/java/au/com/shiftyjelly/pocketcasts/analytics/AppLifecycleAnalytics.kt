package au.com.shiftyjelly.pocketcasts.analytics

import com.automattic.eventhorizon.ApplicationClosedEvent
import com.automattic.eventhorizon.ApplicationInstalledEvent
import com.automattic.eventhorizon.ApplicationOpenedEvent
import com.automattic.eventhorizon.ApplicationUpdatedEvent
import com.automattic.eventhorizon.EventHorizon
import javax.inject.Inject
import kotlin.time.TimeSource

class AppLifecycleAnalytics @Inject constructor(
    private val eventHorizon: EventHorizon,
    private val timeSource: TimeSource,
) {
    private var timeMark = timeSource.markNow()

    fun onNewApplicationInstall() {
        eventHorizon.track(ApplicationInstalledEvent)
    }

    fun onApplicationUpgrade(previousVersionCode: Int) {
        eventHorizon.track(ApplicationUpdatedEvent(previousVersion = previousVersionCode.toString()))
    }

    fun onApplicationEnterForeground() {
        timeMark = timeSource.markNow()
        eventHorizon.track(ApplicationOpenedEvent)
    }

    fun onApplicationEnterBackground() {
        val elapsedSeconds = timeMark.elapsedNow().inWholeSeconds
        timeMark = timeSource.markNow()
        eventHorizon.track(ApplicationClosedEvent(timeInApp = elapsedSeconds))
    }
}
