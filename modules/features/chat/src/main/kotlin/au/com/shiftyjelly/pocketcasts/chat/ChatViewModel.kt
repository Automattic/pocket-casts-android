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

    fun setEpisodeInfo(episodeTitle: String, episodeSubtitle: String, podcastUuid: String?) {
        _uiState.update { it.copy(episodeTitle = episodeTitle, episodeSubtitle = episodeSubtitle, podcastUuid = podcastUuid) }
    }

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun onSend() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        // TODO: Send message to backend
        _uiState.update { it.copy(inputText = "") }
    }
}

data class ChatUiState(
    val inputText: String = "",
    val episodeTitle: String = "",
    val episodeSubtitle: String = "",
    val podcastUuid: String? = null,
)
