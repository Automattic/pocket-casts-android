package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.shared.WatchPhoneCommunication
import au.com.shiftyjelly.pocketcasts.shared.WatchPhoneCommunicationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HelpScreenViewModel @Inject constructor(
    private val watchPhoneCommunication: WatchPhoneCommunication.Watch,
) : ViewModel() {

    data class State(val isPhoneAvailable: Boolean)

    val state: StateFlow<State?> = watchPhoneCommunication.watchPhoneCommunicationStateFlow
        .map {
            State(isPhoneAvailable = it == WatchPhoneCommunicationState.AVAILABLE)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun emailLogsToSupport() {
        viewModelScope.launch {
            watchPhoneCommunication.emailLogsToSupportMessage()
        }
    }
}
