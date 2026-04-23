package au.com.shiftyjelly.pocketcasts.chat

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class ChatViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    fun setEpisodeInfo(episodeTitle: String, episodeSubtitle: String, podcastUuid: String?, welcomeMessage: String) {
        _uiState.update {
            it.copy(
                episodeTitle = episodeTitle,
                episodeSubtitle = episodeSubtitle,
                podcastUuid = podcastUuid,
                messages = listOf(ChatMessage(text = welcomeMessage, role = ChatRole.Ai)),
            )
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
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
)

data class ChatMessage(
    val text: String,
    val role: ChatRole,
)

enum class ChatRole {
    Ai,
    User,
}
