package au.com.shiftyjelly.pocketcasts.profile.sharing

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class ShareProfileViewModel @Inject constructor() : ViewModel() {

    data class State(
        val displayName: String = "",
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun setDisplayName(name: String) {
        _state.update { it.copy(displayName = name) }
    }
}
