package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            settings.cachedSubscription.flow
                .stateIn(viewModelScope).collect { subscription ->
                    _stateFlow.update { state ->
                        state.copy(isBookmarkFeatureAvailable = subscription != null)
                    }
                }
        }
    }

    data class State(
        val isBookmarkFeatureAvailable: Boolean = false,
    )
}
