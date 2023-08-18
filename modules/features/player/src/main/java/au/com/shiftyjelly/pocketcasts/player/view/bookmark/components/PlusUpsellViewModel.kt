package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlusUpsellViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {

    fun onClick(sourceView: SourceView) {
        analyticsTracker.track(
            AnalyticsEvent.BOOKMARKS_UPGRADE_BUTTON_TAPPED,
            mapOf("source" to sourceView.analyticsValue)
        )
    }
}
