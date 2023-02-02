package au.com.shiftyjelly.pocketcasts.views.viewmodels

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.views.fragments.FilterSelectFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FilterSelectViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {

    private var isFragmentChangingConfigurations: Boolean = false

    fun trackFilterChange(source: FilterSelectFragment.Source) {
        if (!isFragmentChangingConfigurations) {
            when (source) {
                FilterSelectFragment.Source.AUTO_DOWNLOAD -> {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_AUTO_DOWNLOAD_FILTERS_CHANGED)
                }
                FilterSelectFragment.Source.PODCAST_SETTINGS -> {
                    // Do not track because the filter_updated event was tracked when the change was persisted
                }
            }
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }
}
