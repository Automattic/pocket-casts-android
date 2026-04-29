package au.com.shiftyjelly.pocketcasts.chat

import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
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
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private var sendJob: Job? = null
    private var quotePlaybackSession: QuotePlaybackSession? = null
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
        val quote = _uiState.value.messages
            .filterIsInstance<ChatMessage.Quote>()
            .firstOrNull { it.uuid == quoteUuid && it.canPlay }
            ?: return

        if (quote.isPlaying) {
            stopQuote()
        } else {
            startQuote(quote)
        }
    }

    private fun startQuote(quote: ChatMessage.Quote) {
        val previousSession = quotePlaybackSession
        val previousJob = previousSession?.job
        previousJob?.cancel()

        val session = previousSession
            ?.takeIf { it.isSnapshotCaptured }
            ?.copyForNextQuote()
            ?: QuotePlaybackSession()

        quotePlaybackSession = session
        updatePlayingQuote(quote.uuid)

        session.job = viewModelScope.launch {
            try {
                previousJob?.join()
                if (quotePlaybackSession !== session) return@launch

                session.captureSnapshotIfNeeded()
                session.hasStartedPlayback = true
                val didStartPlayback = withContext(Dispatchers.IO) {
                    startQuotePlayback(quote.startMs)
                }
                session.hasStartedPlayback = didStartPlayback
                if (!didStartPlayback) {
                    finishQuotePlayback(session)
                    return@launch
                }

                if (awaitEndOfQuote(quote.startMs, quote.endMs)) {
                    finishQuotePlayback(session)
                } else {
                    clearQuotePlaybackState(session)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (quotePlaybackSession === session) {
                    finishQuotePlayback(session)
                }
            }
        }
    }

    private fun stopQuote() {
        val session = quotePlaybackSession ?: return
        session.job?.cancel()
        val snapshot = session.snapshot
        val shouldRestorePlayback = session.hasStartedPlayback

        clearQuotePlaybackState(session)
        if (shouldRestorePlayback) {
            viewModelScope.launch(Dispatchers.IO) {
                restorePreviousPlayback(snapshot)
            }
        }
    }

    private suspend fun QuotePlaybackSession.captureSnapshotIfNeeded() {
        if (isSnapshotCaptured) return
        snapshot = withContext(Dispatchers.IO) {
            capturePlaybackSnapshot()
        }
        isSnapshotCaptured = true
    }

    private suspend fun finishQuotePlayback(session: QuotePlaybackSession) {
        val snapshot = session.snapshot
        val shouldRestorePlayback = session.hasStartedPlayback

        val wasCurrentSession = clearQuotePlaybackState(session)
        if (wasCurrentSession && shouldRestorePlayback) {
            withContext(Dispatchers.IO) {
                restorePreviousPlayback(snapshot)
            }
        }
    }

    private fun clearQuotePlaybackState(session: QuotePlaybackSession): Boolean {
        if (quotePlaybackSession !== session) return false
        session.job = null
        quotePlaybackSession = null
        updatePlayingQuote(playingQuoteUuid = null)
        return true
    }

    private fun updatePlayingQuote(playingQuoteUuid: String?) {
        _uiState.update { state ->
            state.copy(messages = state.messages.withQuotePlaybackState(playingQuoteUuid))
        }
    }

    private suspend fun capturePlaybackSnapshot(): PlaybackSnapshot? {
        val state = playbackManager.playbackStateFlow.first()
        if (state.isEmpty || state.episodeUuid.isEmpty()) return null
        return PlaybackSnapshot(
            episodeUuid = state.episodeUuid,
            positionMs = state.positionMs,
            wasPlaying = state.isPlaying,
        )
    }

    private suspend fun startQuotePlayback(startMs: Int): Boolean {
        var state = playbackManager.playbackStateFlow.first()
        val isAlreadyCurrent = state.episodeUuid == episodeUuid
        if (!isAlreadyCurrent) {
            playbackManager.playNowSuspend(episodeUuid = episodeUuid, sourceView = SourceView.EPISODE_DETAILS)
            state = playbackManager.playbackStateFlow.first()
            if (state.episodeUuid != episodeUuid) return false
        }
        playbackManager.seekToTimeMsSuspend(positionMs = startMs)
        if (isAlreadyCurrent && !state.isPlaying) {
            playbackManager.playQueueSuspend(sourceView = SourceView.EPISODE_DETAILS)
        }
        return true
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
        val currentEpisodeUuid = playbackManager.playbackStateFlow.first().episodeUuid
        if (currentEpisodeUuid != snapshot.episodeUuid) {
            playbackManager.playNowSuspend(episodeUuid = snapshot.episodeUuid, sourceView = SourceView.EPISODE_DETAILS)
            if (playbackManager.playbackStateFlow.first().episodeUuid != snapshot.episodeUuid) {
                playbackManager.pauseSuspend(sourceView = SourceView.EPISODE_DETAILS)
                return
            }
        }
        playbackManager.seekToTimeMsSuspend(positionMs = snapshot.positionMs)
        if (!snapshot.wasPlaying) {
            playbackManager.pauseSuspend(sourceView = SourceView.EPISODE_DETAILS)
        } else if (!playbackManager.playbackStateFlow.first().isPlaying) {
            playbackManager.playQueueSuspend(sourceView = SourceView.EPISODE_DETAILS)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val session = quotePlaybackSession ?: return
        session.job?.cancel()
        quotePlaybackSession = null
        if (session.hasStartedPlayback) {
            applicationScope.launch(Dispatchers.IO) {
                restorePreviousPlayback(session.snapshot)
            }
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
            } catch (e: CancellationException) {
                throw e
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

private class QuotePlaybackSession(
    var job: Job? = null,
    var snapshot: PlaybackSnapshot? = null,
    var isSnapshotCaptured: Boolean = false,
    var hasStartedPlayback: Boolean = false,
) {
    fun copyForNextQuote() = QuotePlaybackSession(
        snapshot = snapshot,
        isSnapshotCaptured = isSnapshotCaptured,
        hasStartedPlayback = hasStartedPlayback,
    )
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
