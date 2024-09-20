package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class AutoArchiveFragmentViewModel @Inject constructor(
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTracker,
) : ViewModel() {

    private val mutableState = MutableStateFlow(initState())
    val state: StateFlow<State> = mutableState

    fun trackOnViewShownEvent() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_SHOWN)
    }

    fun onStarredChanged(newValue: Boolean) {
        settings.autoArchiveIncludesStarred.set(newValue, updateModifiedAt = true)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_INCLUDE_STARRED_TOGGLED,
            mapOf("enabled" to newValue),
        )
        mutableState.update { it.copy(starredEpisodes = newValue) }
    }

    fun onPlayedEpisodesAfterChanged(newValue: AutoArchiveAfterPlaying) {
        settings.autoArchiveAfterPlaying.set(newValue, updateModifiedAt = true)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_PLAYED_CHANGED,
            mapOf("value" to newValue.analyticsValue),
        )
        mutableState.update { it.copy(archiveAfterPlaying = newValue) }
    }

    fun onInactiveChanged(newValue: AutoArchiveInactive) {
        settings.autoArchiveInactive.set(newValue, updateModifiedAt = true)
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_AUTO_ARCHIVE_INACTIVE_CHANGED,
            mapOf("value" to newValue.analyticsValue),
        )
        mutableState.update { it.copy(archiveInactive = newValue) }
    }

    private fun initState() = State(
        starredEpisodes = settings.autoArchiveIncludesStarred.value,
        archiveAfterPlaying = settings.autoArchiveAfterPlaying.value,
        archiveInactive = settings.autoArchiveInactive.value,
    )

    data class State(
        val starredEpisodes: Boolean,
        val archiveAfterPlaying: AutoArchiveAfterPlaying = AutoArchiveAfterPlaying.Never,
        val archiveInactive: AutoArchiveInactive = AutoArchiveInactive.Never,
    )
}
