package au.com.shiftyjelly.pocketcasts.views.viewmodels

import androidx.annotation.IdRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@HiltViewModel
class BaseFragmentViewModel
@Inject constructor(
    userManager: UserManager,
    private val endOfYearManager: EndOfYearManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    init {
        viewModelScope.launch {
            combine(
                userManager.getSignInState().asFlow(),
                settings.endOfYearShowBadge2023.flow,
            ) { signInState, showBadge ->
                _uiState.value = UiState(
                    signInState = signInState,
                    showBadgeOnProfileMenu = endOfYearManager.isEligibleForStories() && showBadge,
                )
            }.stateIn(viewModelScope)
        }
    }

    fun onMenuItemTapped(@IdRes resId: Int) {
        when (resId) {
            UR.id.menu_profile -> {
                analyticsTracker.track(AnalyticsEvent.PROFILE_TAB_OPENED)
                settings.endOfYearShowBadge2023.set(false, updateModifiedAt = false)
            }
        }
    }

    data class UiState(
        val signInState: SignInState? = null,
        val showBadgeOnProfileMenu: Boolean = false,
    )
}
