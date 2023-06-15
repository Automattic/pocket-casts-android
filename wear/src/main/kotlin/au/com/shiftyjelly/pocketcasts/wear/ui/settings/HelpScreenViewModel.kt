package au.com.shiftyjelly.pocketcasts.wear.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.shared.WatchMessageSendState
import au.com.shiftyjelly.pocketcasts.shared.WatchPhoneCommunication
import au.com.shiftyjelly.pocketcasts.shared.WatchPhoneCommunicationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class HelpScreenViewModel @Inject constructor(
    private val watchPhoneCommunication: WatchPhoneCommunication.Watch,
) : ViewModel() {

    data class State(val isPhoneAvailable: Boolean)

    val state: StateFlow<State?> = watchPhoneCommunication.watchPhoneCommunicationStateFlow
        .map {
            State(isPhoneAvailable = it == WatchPhoneCommunicationState.AVAILABLE)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun emailLogsToSupport(context: Context) {
        viewModelScope.launch {
            val result = watchPhoneCommunication.emailLogsToSupportMessage()
            val message = when (result) {
                WatchMessageSendState.QUEUED -> LR.string.settings_help_contact_support_email_sent_to_phone
                WatchMessageSendState.FAILED_TO_QUEUE -> LR.string.settings_help_phone_unavailable_message
            }

            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

    fun sendLogsToPhone(context: Context) {
        viewModelScope.launch {
            val result = watchPhoneCommunication.sendLogsToPhoneMessage()
            val message = when (result) {
                WatchMessageSendState.QUEUED -> LR.string.settings_help_logs_sent_to_phone_check_phone
                WatchMessageSendState.FAILED_TO_QUEUE -> LR.string.settings_help_phone_unavailable_message
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
