package au.com.shiftyjelly.pocketcasts.referrals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class ReferralsViewModel @Inject constructor(
    private val userManager: UserManager,
    private val settings: Settings,
) : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            userManager.getSignInState().asFlow()
                .stateIn(viewModelScope)
                .collect { signInState ->
                    val showIcon = FeatureFlag.isEnabled(Feature.REFERRALS) && signInState.isSignedInAsPlusOrPatron
                    _state.update {
                        it.copy(
                            showIcon = showIcon,
                            showTooltip = showIcon && settings.showReferralsTooltip.value,
                        )
                    }
                }
        }
    }

    fun onIconClick() {
        if (settings.showReferralsTooltip.value) {
            settings.showReferralsTooltip.set(false, updateModifiedAt = false)
        }
        _state.update {
            it.copy(
                badgeCount = if (it.badgeCount > 0) it.badgeCount - 1 else 3,
                showTooltip = false,
            )
        }
    }

    data class UiState(
        val showIcon: Boolean = false,
        val showTooltip: Boolean = false,
        val badgeCount: Int = 3,
    ) {
        val showBadge: Boolean
            get() = showIcon && badgeCount > 0
    }
}
