package au.com.shiftyjelly.pocketcasts.chat

import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
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
import kotlinx.coroutines.withTimeoutOrNull

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
                val quoteEpisode = withContext(Dispatchers.IO) {
                    episodeManager.findEpisodeByUuid(episodeUuid)
                }
                if (quoteEpisode == null) {
                    finishQuotePlayback(session)
                    return@launch
                }

                session.shouldRestorePlayback = true
                val didStartPlayback = withContext(Dispatchers.IO) {
                    pauseCurrentPlayback()
                    startQuotePlayback(quoteEpisode, quote.startMs)
                }
                if (!didStartPlayback) {
                    finishQuotePlayback(session)
                    return@launch
                }

                awaitEndOfQuote(quote.startMs, quote.endMs)
                finishQuotePlayback(session)
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
        val job = session.job
        job?.cancel()
        val snapshot = session.snapshot
        val shouldRestorePlayback = session.shouldRestorePlayback

        clearQuotePlaybackState(session)
        if (shouldRestorePlayback) {
            viewModelScope.launch(Dispatchers.IO) {
                job?.join()
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
        val shouldRestorePlayback = session.shouldRestorePlayback

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
        if (state.episodeUuid.isEmpty() || state.isEmpty || state.isStopped || state.isError) return null
        return PlaybackSnapshot(
            episodeUuid = state.episodeUuid,
            positionMs = state.positionMs,
            wasPlaying = state.isPlaying,
        )
    }

    private suspend fun pauseCurrentPlayback() {
        if (playbackManager.playbackStateFlow.first().isPlaying) {
            playbackManager.pauseSuspend(sourceView = SourceView.EPISODE_DETAILS)
        }
    }

    private suspend fun startQuotePlayback(episode: BaseEpisode, startMs: Int): Boolean {
        val state = playbackManager.playbackStateFlow.first()
        val isAlreadyCurrent = state.episodeUuid == episode.uuid && !state.isEmpty && !state.isStopped && !state.isError
        if (!isAlreadyCurrent) {
            playbackManager.playNowSuspend(episode = episode, sourceView = SourceView.EPISODE_DETAILS)
            if (!awaitPlaybackEpisode(episode.uuid)) return false
        }
        playbackManager.seekToTimeMsSuspend(positionMs = startMs)
        playbackManager.playQueueSuspend(sourceView = SourceView.EPISODE_DETAILS)
        return true
    }

    private suspend fun awaitEndOfQuote(startMs: Int, endMs: Int) {
        var sawStart = false
        playbackManager.playbackStateFlow
            .mapNotNull { state ->
                when {
                    state.episodeUuid != episodeUuid -> true
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
        val currentState = playbackManager.playbackStateFlow.first()
        val isSnapshotCurrent = currentState.episodeUuid == snapshot.episodeUuid &&
            !currentState.isEmpty &&
            !currentState.isStopped &&
            !currentState.isError
        if (!isSnapshotCurrent) {
            val previous = episodeManager.findEpisodeByUuid(snapshot.episodeUuid) ?: run {
                playbackManager.pauseSuspend(sourceView = SourceView.EPISODE_DETAILS)
                return
            }
            playbackManager.playNowSuspend(episode = previous, sourceView = SourceView.EPISODE_DETAILS)
            if (!awaitPlaybackEpisode(snapshot.episodeUuid)) {
                playbackManager.pauseSuspend(sourceView = SourceView.EPISODE_DETAILS)
                return
            }
        }
        playbackManager.seekToTimeMsSuspend(positionMs = snapshot.positionMs)
        if (snapshot.wasPlaying) {
            playbackManager.playQueueSuspend(sourceView = SourceView.EPISODE_DETAILS)
        } else {
            playbackManager.pauseSuspend(sourceView = SourceView.EPISODE_DETAILS)
        }
    }

    private suspend fun awaitPlaybackEpisode(episodeUuid: String): Boolean {
        val state = playbackManager.playbackStateFlow.first()
        if (state.episodeUuid == episodeUuid && !state.isEmpty && !state.isStopped && !state.isError) return true
        return withTimeoutOrNull(QUOTE_PLAYBACK_EPISODE_TIMEOUT_MS) {
            playbackManager.playbackStateFlow.first { playbackState ->
                playbackState.episodeUuid == episodeUuid &&
                    !playbackState.isEmpty &&
                    !playbackState.isStopped &&
                    !playbackState.isError
            }
        } != null
    }

    override fun onCleared() {
        super.onCleared()
        val session = quotePlaybackSession ?: return
        val job = session.job
        job?.cancel()
        quotePlaybackSession = null
        if (session.shouldRestorePlayback) {
            applicationScope.launch(Dispatchers.IO) {
                job?.join()
                restorePreviousPlayback(session.snapshot)
            }
        }
    }

    private fun performSend(message: String, isRetry: Boolean) {
        val currentMessages = _uiState.value.messages

        _uiState.update { it.copy(isAwaitingReply = true, error = null) }

        sendJob = viewModelScope.launch {
            try {
                chatManager.sendMessage(episodeUuid, message, currentMessages, isRetry)
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
    var shouldRestorePlayback: Boolean = false,
) {
    fun copyForNextQuote() = QuotePlaybackSession(
        snapshot = snapshot,
        isSnapshotCaptured = isSnapshotCaptured,
        shouldRestorePlayback = shouldRestorePlayback,
    )
}

private const val QUOTE_PLAYBACK_EPISODE_TIMEOUT_MS = 5_000L

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
