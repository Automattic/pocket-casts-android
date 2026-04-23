package au.com.shiftyjelly.pocketcasts.chat

import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val networkConnectionWatcher: NetworkConnectionWatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkConnectionWatcher.networkCapabilities.collect { capabilities ->
                val isConnected = capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                _uiState.update { it.copy(isConnected = isConnected) }
            }
        }
    }

    private lateinit var welcomeMessage: ChatMessage

    fun setEpisodeInfo(episodeTitle: String, episodeSubtitle: String, podcastUuid: String?, welcomeMessage: String) {
        this.welcomeMessage = ChatMessage(text = welcomeMessage, role = ChatRole.Ai)
        _uiState.update {
            it.copy(
                episodeTitle = episodeTitle,
                episodeSubtitle = episodeSubtitle,
                podcastUuid = podcastUuid,
                messages = listOf(this.welcomeMessage),
            )
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun clearChat() {
        _uiState.update { it.copy(messages = listOf(welcomeMessage)) }
    }

    fun onSend() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val userMessage = ChatMessage(text = text, role = ChatRole.User)
        // TODO: Send message to backend
        _uiState.update { it.copy(inputText = "", messages = it.messages + userMessage) }
    }
}

data class ChatUiState(
    val inputText: String = "",
    val episodeTitle: String = "",
    val episodeSubtitle: String = "",
    val podcastUuid: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isConnected: Boolean = true,
) {
    val canSend: Boolean get() = inputText.isNotBlank() && isConnected
}

data class ChatMessage(
    val text: String,
    val role: ChatRole,
)

enum class ChatRole {
    Ai,
    User,
}
