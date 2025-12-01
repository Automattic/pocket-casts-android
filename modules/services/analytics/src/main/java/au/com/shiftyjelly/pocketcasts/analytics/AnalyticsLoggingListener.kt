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
                        append("\n⠀⠀ ") // The first two characters after the new line are not spaces but U+2800 to align the text correctly with the blue dot
                        if (usedTrackers.isNotEmpty()) {
                            append("Used: ")
                            append(usedTrackers.joinToString(prefix = "[", postfix = "]") { (id, _) -> id })
                            if (skippedTrackers.isNotEmpty()) {
                                append(",")
                            }
                            append(" ")
                        }
                        if (skippedTrackers.isNotEmpty()) {
                            append("Skipped: ")
                            append(skippedTrackers.joinToString(prefix = "[", postfix = "]") { (id, _) -> id })
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
