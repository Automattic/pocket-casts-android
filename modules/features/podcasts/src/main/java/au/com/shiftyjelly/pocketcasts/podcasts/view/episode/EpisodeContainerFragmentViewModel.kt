package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EpisodeContainerFragmentViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private var initialized = false

    fun onPageSelected(pageKey: String) {
        // Don't track the initial page selection because that is just the screen loading
        if (initialized) {
            analyticsTracker.track(
                AnalyticsEvent.EPISODE_DETAIL_TAB_CHANGED,
                mapOf("value" to pageKey),
            )
        }
        initialized = true
    }
}
