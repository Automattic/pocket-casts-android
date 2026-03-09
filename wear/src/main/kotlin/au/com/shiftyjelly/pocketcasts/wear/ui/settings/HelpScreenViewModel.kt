package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.shared.WatchMessageSendState
import au.com.shiftyjelly.pocketcasts.shared.WatchPhoneCommunication
import au.com.shiftyjelly.pocketcasts.shared.WatchPhoneCommunicationState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class HelpScreenViewModel @Inject constructor(
    private val watchPhoneCommunication: WatchPhoneCommunication.Watch,
) : ViewModel() {

    data class State(val isPhoneAvailable: Boolean)

    private val _statusMessage = MutableStateFlow(LR.string.settings_help_contact_support_wear_requires_nearby_phone)
    val statusMessage: StateFlow<Int> = _statusMessage.asStateFlow()

    val state: StateFlow<State?> = watchPhoneCommunication.watchPhoneCommunicationStateFlow
        .map {
            State(isPhoneAvailable = it == WatchPhoneCommunicationState.AVAILABLE)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun emailLogsToSupport() {
        viewModelScope.launch {
            val result = watchPhoneCommunication.emailLogsToSupportMessage()
            val message = when (result) {
                WatchMessageSendState.QUEUED -> LR.string.settings_help_contact_support_email_sent_to_phone
                WatchMessageSendState.FAILED_TO_QUEUE -> LR.string.settings_help_phone_unavailable_message
            }

            if (result == WatchMessageSendState.QUEUED) {
                // There is a bit of delay between an item being queued and it being received on the phone,
                // so add a short delay before directing the user to their phone.
                delay(2.seconds)
            }
            _statusMessage.value = message
        }
    }
}
