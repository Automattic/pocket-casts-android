package au.com.shiftyjelly.pocketcasts.analytics

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AnalyticsTrackerTracks @Inject constructor(
    @ApplicationContext private val appContext: Context,
) : Tracker
