package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.subscription.FreeTrial
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class UpsellViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val subscriptionManager: SubscriptionManager,
) : ViewModel() {

    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            subscriptionManager.freeTrialForSubscriptionTierFlow(SubscriptionTier.PLUS)
                .stateIn(this)
                .collect { freeTrial ->
                    _state.update {
                        UiState.Loaded(
                            freeTrial = freeTrial,
                            showEarlyAccessMessage = false,
                        )
                    }
                }
        }
    }

    fun onClick(sourceView: SourceView) {
        analyticsTracker.track(
            AnalyticsEvent.BOOKMARKS_UPGRADE_BUTTON_TAPPED,
            mapOf("source" to sourceView.analyticsValue),
        )
    }

    sealed class UiState {
        data object Loading : UiState()
        data class Loaded(
            val freeTrial: FreeTrial,
            val showEarlyAccessMessage: Boolean,
        ) : UiState()
    }
}
