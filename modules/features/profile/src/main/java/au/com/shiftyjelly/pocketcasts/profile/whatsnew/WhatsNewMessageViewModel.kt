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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = WhatsNewMessageViewModel.Factory::class)
class WhatsNewMessageViewModel @AssistedInject constructor(
    @Assisted private val messageId: String,
    private val manager: WhatsNewManager,
    settings: Settings,
) : ViewModel() {
    private val isMissing = MutableStateFlow(false)

    private var shownMessage: WhatsNewMessage? = null

    internal val uiState: StateFlow<UiState> = combine(
        manager.feedMessages.map { messages ->
            messages.firstOrNull { it.id == messageId }?.also { shownMessage = it } ?: shownMessage
        },
        isMissing,
    ) { message, isMissing ->
        when {
            message != null -> UiState.Loaded(message)
            isMissing -> UiState.Missing
            else -> UiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    val bottomInset: StateFlow<Int> = settings.bottomInset
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        viewModelScope.launch {
            manager.refreshIfNeeded()
            if (manager.feedMessages.first().none { it.id == messageId }) {
                isMissing.value = true
            }
        }
    }

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
