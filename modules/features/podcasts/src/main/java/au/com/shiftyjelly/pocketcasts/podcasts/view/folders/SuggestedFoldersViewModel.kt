package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SuggestedFoldersViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_MODAL_SHOWN)
    }

    fun onDismissed() {
        analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_MODAL_DISMISSED)
    }

    fun onUseTheseFolders() {
        analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_MODAL_USE_THESE_FOLDERS_TAPPED)
    }

    fun onCreateCustomFolders() {
        analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_MODAL_CREATE_CUSTOM_FOLDERS_TAPPED)
    }
}
