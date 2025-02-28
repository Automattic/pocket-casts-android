package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class SuggestedFoldersPaywallViewModel @Inject constructor(
    userManager: UserManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    val signInState = userManager.getSignInState().asFlow()

    fun onDismissed(source: String) {
        analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_PAYWALL_MODAL_MAYBE_LATER_TAPPED, mapOf("source" to source))
        if (source == SuggestedFoldersPaywallBottomSheet.PODCASTS_SOURCE) {
            settings.setDismissedSuggestedFolderPaywallTime()
            settings.updateDismissedSuggestedFolderPaywallCount()
        }
    }

    fun onShown(source: String) {
        analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_PAYWALL_MODAL_SHOWN, mapOf("source" to source))
    }

    fun onUseTheseFolders(source: String) {
        analyticsTracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_PAYWALL_MODAL_USE_THESE_FOLDERS_TAPPED, mapOf("source" to source))
    }
}
