package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.podcasts.BuildConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EpisodeContainerFragmentViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {

    private var initialized = false

    fun onPageSelected(position: Int) {
        // Don't track the initial page selection because that is just the screen loading
        if (initialized) {
            analyticsTracker.track(
                AnalyticsEvent.EPISODE_DETAIL_TAB_CHANGED,
                mapOf(
                    "value" to when (position) {
                        0 -> "details"
                        1 -> "bookmarks"
                        else -> {
                            // This should never happen
                            if (BuildConfig.DEBUG) {
                                throw IllegalStateException("Unknown tab position: $position")
                            } else {
                                "unknown"
                            }
                        }
                    }
                )
            )
        }
        initialized = true
    }
}
