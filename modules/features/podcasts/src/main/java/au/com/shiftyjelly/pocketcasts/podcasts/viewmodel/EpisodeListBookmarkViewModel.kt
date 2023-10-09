package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EpisodeListBookmarkViewModel
@Inject constructor(
    private val settings: Settings,
) : ViewModel() {

    private var _stateFlow: MutableStateFlow<State> = MutableStateFlow(State())
    val stateFlow: StateFlow<State> = _stateFlow

    init {
        viewModelScope.launch {
            settings.cachedSubscriptionStatus.flow
                .stateIn(viewModelScope).collect { cachedSubscriptionStatus ->
                    _stateFlow.update { state ->
                        val userTier = (cachedSubscriptionStatus as? SubscriptionStatus.Paid)?.tier?.toUserTier() ?: UserTier.Free
                        state.copy(
                            isBookmarkFeatureAvailable = FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED) &&
                                Feature.isAvailable(Feature.BOOKMARKS_ENABLED, userTier)
                        )
                    }
                }
        }
    }

    data class State(
        val isBookmarkFeatureAvailable: Boolean = false,
    )
}
