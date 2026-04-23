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
    private val chatManager: ChatManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var episodeUuid: String
    private lateinit var welcomeMessageText: String

    init {
        viewModelScope.launch {
            networkConnectionWatcher.networkCapabilities.collect { capabilities ->
                val isConnected = capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                _uiState.update { it.copy(isConnected = isConnected) }
            }
        }
    }

    fun setEpisodeInfo(
        episodeUuid: String,
        episodeTitle: String,
        episodeSubtitle: String,
        podcastUuid: String?,
        welcomeMessage: String,
    ) {
        this.episodeUuid = episodeUuid
        this.welcomeMessageText = welcomeMessage
        _uiState.update {
            it.copy(
                episodeTitle = episodeTitle,
                episodeSubtitle = episodeSubtitle,
                podcastUuid = podcastUuid,
            )
        }
        loadChat(podcastUuid)
    }

    private fun loadChat(podcastUuid: String?) {
        viewModelScope.launch {
            val existingMessages = chatManager.getMessages(episodeUuid)
            if (existingMessages.isNotEmpty()) {
                _uiState.update { it.copy(messages = existingMessages) }
            } else {
                val welcomeMsg = ChatMessage(text = welcomeMessageText, role = ChatRole.Ai)
                _uiState.update { it.copy(messages = listOf(welcomeMsg)) }
                chatManager.createChat(episodeUuid, podcastUuid, welcomeMsg)
            }
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun clearChat() {
        val welcomeMsg = ChatMessage(text = welcomeMessageText, role = ChatRole.Ai)
        _uiState.update { it.copy(messages = listOf(welcomeMsg)) }
        viewModelScope.launch {
            chatManager.clearMessages(episodeUuid, welcomeMsg)
        }
    }

    fun onSend() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        val userMessage = ChatMessage(text = text, role = ChatRole.User)
        _uiState.update { it.copy(inputText = "", messages = it.messages + userMessage) }
        viewModelScope.launch {
            chatManager.saveMessage(episodeUuid, userMessage)
        }
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
