package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NoBookmarksViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {

    fun onGoToHeadphoneSettingsClicked() {
        analyticsTracker.track(AnalyticsEvent.BOOKMARKS_EMPTY_GO_TO_HEADPHONE_SETTINGS)
    }
}
