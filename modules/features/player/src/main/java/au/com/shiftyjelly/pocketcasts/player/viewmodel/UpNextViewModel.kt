package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.UpNextSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.UpNextSortEvent
import com.automattic.eventhorizon.UpNextSortTooltipClosedEvent
import com.automattic.eventhorizon.UpNextSortTooltipShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class UpNextViewModel @Inject constructor(
    private val userManager: UserManager,
    private val upNextQueue: UpNextQueue,
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
) : ViewModel() {
    private val _isSignedInAsPaidUser = MutableStateFlow(false)
    val isSignedInAsPaidUser: StateFlow<Boolean> get() = _isSignedInAsPaidUser

    // Whether Up Next is the visible foreground, so the tooltip's Compose Popup (its own window) doesn't draw over the player opened above it.
    private val _isUpNextVisible = MutableStateFlow(false)

    val showSortDurationTooltip: StateFlow<Boolean> = combine(
        settings.showUpNextSortDurationTooltip.flow,
        _isUpNextVisible,
    ) { shouldShowTooltip, isUpNextVisible ->
        shouldShowTooltip && isUpNextVisible && FeatureFlag.isEnabled(Feature.UP_NEXT_DURATION)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            userManager.getSignInState().asFlow().collect { signInState ->
                _isSignedInAsPaidUser.value = signInState.isSignedInAsPlusOrPatron
            }
        }
    }

    fun sortUpNext(sortType: UpNextSortType) {
        eventHorizon.track(
            UpNextSortEvent(
                sortType = sortType.analyticsValue,
            ),
        )
        upNextQueue.sortUpNext(sortType)
    }

    fun setUpNextVisible(visible: Boolean) {
        _isUpNextVisible.value = visible
    }

    fun onSortClick() {
        dismissSortDurationTooltip()
    }

    fun onUpNextScrolled() {
        dismissSortDurationTooltip()
    }

    fun onSortTooltipShown() {
        eventHorizon.track(UpNextSortTooltipShownEvent)
    }

    fun onSortTooltipTapped() {
        dismissSortDurationTooltip()
    }

    private fun dismissSortDurationTooltip() {
        eventHorizon.track(UpNextSortTooltipClosedEvent)
        if (settings.showUpNextSortDurationTooltip.value) {
            settings.showUpNextSortDurationTooltip.set(false, updateModifiedAt = false)
        }
    }
}
