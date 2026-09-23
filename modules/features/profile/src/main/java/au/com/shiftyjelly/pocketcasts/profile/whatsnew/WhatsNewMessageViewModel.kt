package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.whatsnew.WhatsNewManager
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessage
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = WhatsNewMessageViewModel.Factory::class)
class WhatsNewMessageViewModel @AssistedInject constructor(
    @Assisted private val messageId: String,
    manager: WhatsNewManager,
    settings: Settings,
) : ViewModel() {
    internal val uiState: StateFlow<UiState> = manager.feedMessages
        .map { messages ->
            val message = messages.firstOrNull { it.id == messageId }
            if (message != null) UiState.Loaded(message) else UiState.Missing
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val bottomInset: StateFlow<Int> = settings.bottomInset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    internal sealed interface UiState {
        data object Loading : UiState

        data object Missing : UiState

        data class Loaded(val message: WhatsNewMessage) : UiState
    }

    @AssistedFactory
    interface Factory {
        fun create(messageId: String): WhatsNewMessageViewModel
    }
}
