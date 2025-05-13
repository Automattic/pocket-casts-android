package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class EpisodeListBookmarkViewModel @Inject constructor(
    private val settings: Settings,
) : ViewModel() {

    private var _stateFlow: MutableStateFlow<State> = MutableStateFlow(State())
    val stateFlow: StateFlow<State> = _stateFlow

    init {
        viewModelScope.launch {
            settings.cachedSubscriptionStatus.flow
                .stateIn(viewModelScope).collect { cachedSubscriptionStatus ->
                    _stateFlow.update { state ->
                        val isPaidUser = (cachedSubscriptionStatus as? SubscriptionStatus.Paid)?.tier?.isPaid == true
                        state.copy(
                            isBookmarkFeatureAvailable = isPaidUser,
                        )
                    }
                }
        }
    }

    data class State(
        val isBookmarkFeatureAvailable: Boolean = false,
    )
}
