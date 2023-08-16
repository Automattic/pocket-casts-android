package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NoBookmarksViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {

    fun onGoToHeadphoneSettingsClicked(sourceView: SourceView) {
        analyticsTracker.track(
            AnalyticsEvent.BOOKMARKS_EMPTY_GO_TO_HEADPHONE_SETTINGS,
            mapOf("source" to sourceView.analyticsValue)
        )
    }
}
