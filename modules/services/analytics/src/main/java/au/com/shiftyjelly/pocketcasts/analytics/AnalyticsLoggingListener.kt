package au.com.shiftyjelly.pocketcasts.analytics

import timber.log.Timber

internal class AnalyticsLoggingListener : AnalyticsTracker.Listener {
    override fun onEvent(
        event: AnalyticsEvent,
        properties: Map<String, Any>,
        trackedEvents: Map<String, TrackedEvent?>,
    ) {
        if (BuildConfig.DEBUG) {
            Timber.tag("Analytics").i(
                buildString {
                    append("\uD83D\uDD35 Event: ")
                    append(event.key)
                    if (properties.isNotEmpty()) {
                        append(", Properties: ")
                        append(properties.toSortedMap())
                    }

                    if (trackedEvents.isNotEmpty()) {
                        val (usedTrackers, skippedTrackers) = trackedEvents.toList().partition { (_, trackedEvent) -> trackedEvent != null }
                        append('\n')
                        if (usedTrackers.isNotEmpty()) {
                            append(" - Used: ")
                            append(usedTrackers.joinToString { (id, _) -> id })
                            if (skippedTrackers.isNotEmpty()) {
                                append('\n')
                            }
                        }
                        if (skippedTrackers.isNotEmpty()) {
                            append(" - Skipped: ")
                            append(skippedTrackers.joinToString { (id, _) -> id })
                        }
                    }
                },
            )

            trackedEvents.forEach { (trackerId, trackedEvent) ->
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
