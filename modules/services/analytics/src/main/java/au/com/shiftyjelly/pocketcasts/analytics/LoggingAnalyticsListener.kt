package au.com.shiftyjelly.pocketcasts.analytics

import com.automattic.eventhorizon.Trackable
import timber.log.Timber

internal class LoggingAnalyticsListener : AnalyticsListener {
    override fun onEvent(
        event: Trackable,
        trackedEvents: Map<String, TrackedEvent?>,
    ) {
        if (BuildConfig.DEBUG) {
            val realEvents = trackedEvents.filterKeys { id -> id != NoOpTracker.id }
            Timber.tag("Analytics").i(
                buildString {
                    append("\uD83D\uDD35 Event: ")
                    append(event.analyticsName)
                    if (event.analyticsProperties.isNotEmpty()) {
                        append(", Properties: ")
                        append(event.analyticsProperties.toSortedMap())
                    }

                    if (realEvents.isNotEmpty()) {
                        val (usedTrackers, skippedTrackers) = realEvents.toList().partition { (_, trackedEvent) -> trackedEvent != null }
                        append(", ")
                        if (usedTrackers.isNotEmpty()) {
                            append("Used: ")
                            append(usedTrackers.joinToString(prefix = "[", postfix = "]") { (id, _) -> id })
                            if (skippedTrackers.isNotEmpty()) {
                                append(", ")
                            }
                        }
                        if (skippedTrackers.isNotEmpty()) {
                            append("Skipped: ")
                            append(skippedTrackers.joinToString(prefix = "[", postfix = "]") { (id, _) -> id })
                        }
                    }
                },
            )

            realEvents.forEach { (trackerId, trackedEvent) ->
                if (trackedEvent != null) {
                    Timber.tag(trackerId).i(
                        buildString {
                            append("\uD83D\uDD35 Event: ")
                            append(trackedEvent.usedKey)
                            if (trackedEvent.properties.isNotEmpty()) {
                                append(", Properties: ")
                                append(trackedEvent.properties)
                            }
                        },
                    )
                }
            }
        }
    }
}
