package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

object AnalyticsTracker {
    private val trackers: MutableList<Tracker> = mutableListOf()

    fun init(@ApplicationContext appContext: Context) {
        // TODO: Initialization logic
    }

    fun registerTracker(tracker: Tracker?) {
        tracker?.let { trackers.add(tracker) }
    }
}
