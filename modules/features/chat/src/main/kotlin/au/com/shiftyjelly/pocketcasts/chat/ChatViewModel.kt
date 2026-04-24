package au.com.shiftyjelly.pocketcasts.chat

import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.Job
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

    private var sendJob: Job? = null
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
        podcastUuid: String,
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
        observeMessages()
        ensureChatExists(podcastUuid)
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatManager.observeMessages(episodeUuid).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    private fun ensureChatExists(podcastUuid: String) {
        viewModelScope.launch {
            val existing = chatManager.getMessages(episodeUuid)
            if (existing.isEmpty()) {
                val welcomeMsg = ChatMessage(text = welcomeMessageText, role = ChatRole.Assistant)
                chatManager.createChat(episodeUuid, podcastUuid, welcomeMsg)
            }
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun clearChat() {
        sendJob?.cancel()
        _uiState.update { it.copy(isAwaitingReply = false, error = null) }
        viewModelScope.launch {
            val welcomeMsg = ChatMessage(text = welcomeMessageText, role = ChatRole.Assistant)
            chatManager.clearMessages(episodeUuid, welcomeMsg)
        }
    }

    fun onSend() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        _uiState.update { it.copy(inputText = "") }
        performSend(message = text, isRetry = false)
    }

    fun retry() {
        val lastUserMessage = _uiState.value.messages.lastOrNull { it.role == ChatRole.User } ?: return
        performSend(message = lastUserMessage.text, isRetry = true)
    }

    private fun performSend(message: String, isRetry: Boolean) {
        val podcastUuid = _uiState.value.podcastUuid
        val currentMessages = _uiState.value.messages

        _uiState.update { it.copy(isAwaitingReply = true, error = null) }

        sendJob = viewModelScope.launch {
            try {
                chatManager.sendMessage(episodeUuid, podcastUuid, message, currentMessages, isRetry)
            } catch (e: IOException) {
                _uiState.update { it.copy(error = ChatError.NetworkError) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = ChatError.ServerError) }
            } finally {
                _uiState.update { it.copy(isAwaitingReply = false) }
            }
        }
    }
}

data class ChatUiState(
    val inputText: String = "",
    val episodeTitle: String = "",
    val episodeSubtitle: String = "",
    val podcastUuid: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isConnected: Boolean = true,
    val isAwaitingReply: Boolean = false,
    val error: ChatError? = null,
) {
    val canSend: Boolean get() = inputText.isNotBlank() && isConnected && !isAwaitingReply
}
