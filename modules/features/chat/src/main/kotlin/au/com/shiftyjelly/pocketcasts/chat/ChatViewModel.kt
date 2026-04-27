package au.com.shiftyjelly.pocketcasts.chat

import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val networkConnectionWatcher: NetworkConnectionWatcher,
    private val chatManager: ChatManager,
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private var sendJob: Job? = null
    private var quotePlaybackJob: Job? = null
    private var pendingPlaybackSnapshot: PlaybackSnapshot? = null
    private var isQuoteInFlight: Boolean = false
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
                _uiState.update { state ->
                    state.copy(messages = messages.withQuotePlaybackState(state.messages.playingQuoteUuid()))
                }
            }
        }
    }

    private fun ensureChatExists(podcastUuid: String) {
        viewModelScope.launch {
            val existing = chatManager.getMessages(episodeUuid)
            if (existing.isEmpty()) {
                val welcomeMsg = ChatMessage.Assistant(text = welcomeMessageText)
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
            val welcomeMsg = ChatMessage.Assistant(text = welcomeMessageText)
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
        val lastUserMessage = _uiState.value.messages.lastOrNull { it is ChatMessage.User } as? ChatMessage.User ?: return
        performSend(message = lastUserMessage.text, isRetry = true)
    }

    fun playQuote(quoteUuid: String) {
        // Tapping the currently-playing quote toggles it off
        val quote = _uiState.value.messages
            .filterIsInstance<ChatMessage.Quote>()
            .firstOrNull { it.uuid == quoteUuid && it.canPlay }
            ?: return

        if (quote.isPlaying) {
            stopQuote()
            return
        }

        quotePlaybackJob?.cancel()
        // Snapshot before we touch playback so we can restore the user where they were.
        val snapshot = capturePlaybackSnapshot()
        pendingPlaybackSnapshot = snapshot
        isQuoteInFlight = true
        _uiState.update { state ->
            state.copy(messages = state.messages.withQuotePlaybackState(quoteUuid))
        }
        quotePlaybackJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                startQuotePlayback(quote.startMs)
            }
            val reachedEnd = awaitEndOfQuote(quote.startMs, quote.endMs)
            if (reachedEnd) {
                withContext(Dispatchers.IO) {
                    restorePreviousPlayback(snapshot)
                }
            }
            pendingPlaybackSnapshot = null
            isQuoteInFlight = false
            _uiState.update { state ->
                state.copy(messages = state.messages.withQuotePlaybackState(playingQuoteUuid = null))
            }
        }
    }

    private fun stopQuote() {
        if (!isQuoteInFlight) return
        quotePlaybackJob?.cancel()
        val snapshot = pendingPlaybackSnapshot
        pendingPlaybackSnapshot = null
        isQuoteInFlight = false
        _uiState.update { state ->
            state.copy(messages = state.messages.withQuotePlaybackState(playingQuoteUuid = null))
        }
        viewModelScope.launch(Dispatchers.IO) {
            restorePreviousPlayback(snapshot)
        }
    }

    private fun capturePlaybackSnapshot(): PlaybackSnapshot? {
        val state = playbackManager.playbackStateRelay.blockingFirst()
        if (state.isEmpty || state.episodeUuid.isEmpty()) return null
        return PlaybackSnapshot(
            episodeUuid = state.episodeUuid,
            positionMs = state.positionMs,
            wasPlaying = state.isPlaying,
        )
    }

    private suspend fun startQuotePlayback(startMs: Int) {
        val episode = episodeManager.findEpisodeByUuid(episodeUuid) ?: return
        val isAlreadyCurrent = playbackManager.playbackStateRelay.blockingFirst().episodeUuid == episodeUuid
        if (!isAlreadyCurrent) {
            playbackManager.playNowSuspend(episode = episode, sourceView = SourceView.EPISODE_DETAILS)
        }
        playbackManager.seekToTimeMsSuspend(positionMs = startMs)
        if (isAlreadyCurrent && !playbackManager.isPlaying()) {
            playbackManager.playQueueSuspend(sourceView = SourceView.EPISODE_DETAILS)
        }
    }

    private suspend fun awaitEndOfQuote(startMs: Int, endMs: Int): Boolean {
        var sawStart = false
        return playbackManager.playbackStateFlow
            .mapNotNull { state ->
                when {
                    state.episodeUuid != episodeUuid -> false
                    else -> {
                        val pos = state.positionMs
                        if (!sawStart && pos in startMs..endMs) sawStart = true
                        if (sawStart && pos >= endMs) true else null
                    }
                }
            }
            .first()
    }

    private suspend fun restorePreviousPlayback(snapshot: PlaybackSnapshot?) {
        if (snapshot == null) {
            playbackManager.pauseSuspend(sourceView = SourceView.EPISODE_DETAILS)
            return
        }
        val currentEpisodeUuid = playbackManager.playbackStateRelay.blockingFirst().episodeUuid
        if (currentEpisodeUuid != snapshot.episodeUuid) {
            val previous = episodeManager.findEpisodeByUuid(snapshot.episodeUuid) ?: run {
                playbackManager.pauseSuspend(sourceView = SourceView.EPISODE_DETAILS)
                return
            }
            playbackManager.playNowSuspend(episode = previous, sourceView = SourceView.EPISODE_DETAILS)
        }
        playbackManager.seekToTimeMsSuspend(positionMs = snapshot.positionMs)
        if (!snapshot.wasPlaying) {
            playbackManager.pauseSuspend(sourceView = SourceView.EPISODE_DETAILS)
        } else if (!playbackManager.isPlaying()) {
            playbackManager.playQueueSuspend(sourceView = SourceView.EPISODE_DETAILS)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (!isQuoteInFlight) return
        val snapshot = pendingPlaybackSnapshot
        pendingPlaybackSnapshot = null
        isQuoteInFlight = false
        applicationScope.launch(Dispatchers.IO) {
            restorePreviousPlayback(snapshot)
        }
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

    private fun List<ChatMessage>.playingQuoteUuid(): String? {
        return filterIsInstance<ChatMessage.Quote>().firstOrNull { it.isPlaying }?.uuid
    }

    private fun List<ChatMessage>.withQuotePlaybackState(playingQuoteUuid: String?): List<ChatMessage> {
        val isQuotePlaybackEnabled = FeatureFlag.isEnabled(Feature.EPISODE_CHAT_PLAYABLE_QUOTES)
        return map { message ->
            if (message is ChatMessage.Quote) {
                val canPlay = isQuotePlaybackEnabled && message.startMs >= 0 && message.endMs > message.startMs
                message.copy(
                    canPlay = canPlay,
                    isPlaying = canPlay && message.uuid == playingQuoteUuid,
                )
            } else {
                message
            }
        }
    }
}

private data class PlaybackSnapshot(
    val episodeUuid: String,
    val positionMs: Int,
    val wasPlaying: Boolean,
)

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
