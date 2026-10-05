package au.com.shiftyjelly.pocketcasts.chat

import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.EpisodeChatBetaSheetDismissedEvent
import com.automattic.eventhorizon.EpisodeChatBetaSheetShownEvent
import com.automattic.eventhorizon.EpisodeChatClearedEvent
import com.automattic.eventhorizon.EpisodeChatDismissedEvent
import com.automattic.eventhorizon.EpisodeChatErrorType
import com.automattic.eventhorizon.EpisodeChatMessageFailedEvent
import com.automattic.eventhorizon.EpisodeChatMessageSentEvent
import com.automattic.eventhorizon.EpisodeChatQuotePlayTappedEvent
import com.automattic.eventhorizon.EpisodeChatQuoteStopTappedEvent
import com.automattic.eventhorizon.EpisodeChatShownEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private var sendJob: Job? = null
    private var quotePlaybackSession: QuotePlaybackSession? = null
    private var transientUserMessage: ChatMessage.User? = null
    private lateinit var episodeUuid: String
    private lateinit var podcastUuid: String
    private lateinit var sourceView: SourceView

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
        podcastTitle: String,
        episodeDurationMs: Int,
        sourceView: SourceView,
        isBeta: Boolean,
    ) {
        this.episodeUuid = episodeUuid
        this.podcastUuid = podcastUuid
        this.sourceView = sourceView
        _uiState.update {
            it.copy(
                episodeTitle = episodeTitle,
                episodeSubtitle = episodeSubtitle,
                podcastUuid = podcastUuid,
                podcastTitle = podcastTitle,
                episodeDurationMs = episodeDurationMs,
                isBeta = isBeta,
            )
        }
        observeMessages()
        observePlayback()
        createChat(podcastUuid)
        trackShown()
        showBetaSheetIfNeeded(isBeta)
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatManager.observeMessages(episodeUuid).collect { messages ->
                _uiState.update { state ->
                    state.copy(
                        messages = messages.withTransientUserMessage().withQuotePlaybackState(
                            playingQuoteUuid = state.messages.playingQuoteUuid(),
                            episodeDurationMs = state.episodeDurationMs,
                        ),
                    )
                }
            }
        }
    }

    private fun observePlayback() {
        viewModelScope.launch {
            combine(
                playbackManager.playbackStateFlow,
                episodeManager.findEpisodeByUuidFlow(episodeUuid),
            ) { playbackState, episode ->
                if (playbackState.episodeUuid == episode.uuid) {
                    ChatPlayback(
                        isPlaying = playbackState.isPlaying,
                        positionMs = playbackState.positionMs,
                        durationMs = playbackState.durationMs.takeIf { it > 0 } ?: episode.durationMs,
                    )
                } else {
                    ChatPlayback(
                        isPlaying = false,
                        positionMs = episode.playedUpToMs,
                        durationMs = episode.durationMs,
                    )
                }
            }.distinctUntilChanged().collect { playback ->
                _uiState.update { it.copy(playback = playback) }
            }
        }
    }

    fun onPlayPauseClick() {
        endQuoteWithoutRestoring()
        viewModelScope.launch {
            val playbackState = playbackManager.playbackStateFlow.first()
            if (playbackState.episodeUuid == episodeUuid && playbackState.isPlaying) {
                playbackManager.pause(sourceView = sourceView)
            } else {
                playbackManager.playNowSuspend(episodeUuid, sourceView = sourceView)
            }
        }
    }

    private fun endQuoteWithoutRestoring() {
        val session = quotePlaybackSession ?: return
        session.job?.cancel()
        clearQuotePlaybackState(session)
    }

    private fun createChat(podcastUuid: String) {
        viewModelScope.launch {
            chatManager.createChat(episodeUuid, podcastUuid)
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun clearChat() {
        sendJob?.cancel()
        transientUserMessage = null
        _uiState.update {
            it.copy(
                messages = emptyList(),
                isAwaitingReply = false,
                error = null,
            )
        }
        eventHorizon.track(
            EpisodeChatClearedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        )
        viewModelScope.launch {
            chatManager.clearMessages(episodeUuid)
        }
    }

    fun trackDismissed() {
        eventHorizon.track(
            EpisodeChatDismissedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        )
    }

    fun onSend() {
        val text = _uiState.value.inputText.trim()
        if (text.isEmpty()) return

        _uiState.update { it.copy(inputText = "") }
        performSend(message = ChatMessage.User(text = text))
    }

    fun retry() {
        val failedUserMessage = transientUserMessage ?: return
        performSend(message = failedUserMessage)
    }

    fun playQuote(quoteUuid: String) {
        val quote = _uiState.value.messages
            .filterIsInstance<ChatMessage.Quote>()
            .firstOrNull { it.uuid == quoteUuid && it.canPlay }
            ?: return

        if (quote.isPlaying) {
            eventHorizon.track(
                EpisodeChatQuoteStopTappedEvent(
                    source = sourceView.analyticsValue,
                    episodeUuid = episodeUuid,
                    podcastUuid = podcastUuid,
                ),
            )
            stopQuote()
        } else {
            eventHorizon.track(
                EpisodeChatQuotePlayTappedEvent(
                    source = sourceView.analyticsValue,
                    episodeUuid = episodeUuid,
                    podcastUuid = podcastUuid,
                ),
            )
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
                if (quoteEpisode == null || !quote.isWithinEpisode(quoteEpisode.durationMs)) {
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
            state.copy(
                messages = state.messages.withQuotePlaybackState(
                    playingQuoteUuid = playingQuoteUuid,
                    episodeDurationMs = state.episodeDurationMs,
                ),
            )
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
            playbackManager.pauseSuspend(sourceView = sourceView)
        }
    }

    private suspend fun startQuotePlayback(episode: BaseEpisode, startMs: Int): Boolean {
        val state = playbackManager.playbackStateFlow.first()
        val isAlreadyCurrent = state.episodeUuid == episode.uuid && !state.isEmpty && !state.isStopped && !state.isError
        if (!isAlreadyCurrent) {
            playbackManager.playNowSuspend(episode = episode, sourceView = sourceView)
            if (!awaitPlaybackEpisode(episode.uuid)) return false
        }
        playbackManager.seekToTimeMsSuspend(positionMs = startMs)
        playbackManager.playQueueSuspend(sourceView = sourceView)
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
            playbackManager.pauseSuspend(sourceView = sourceView)
            return
        }
        val currentState = playbackManager.playbackStateFlow.first()
        val isSnapshotCurrent = currentState.episodeUuid == snapshot.episodeUuid &&
            !currentState.isEmpty &&
            !currentState.isStopped &&
            !currentState.isError
        if (!isSnapshotCurrent) {
            val previous = episodeManager.findEpisodeByUuid(snapshot.episodeUuid) ?: run {
                playbackManager.pauseSuspend(sourceView = sourceView)
                return
            }
            playbackManager.playNowSuspend(episode = previous, sourceView = sourceView)
            if (!awaitPlaybackEpisode(snapshot.episodeUuid)) {
                playbackManager.pauseSuspend(sourceView = sourceView)
                return
            }
        }
        playbackManager.seekToTimeMsSuspend(positionMs = snapshot.positionMs)
        if (snapshot.wasPlaying) {
            playbackManager.playQueueSuspend(sourceView = sourceView)
        } else {
            playbackManager.pauseSuspend(sourceView = sourceView)
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

    private fun performSend(message: ChatMessage.User) {
        transientUserMessage = message
        val currentMessages = _uiState.value.messages.filterNot { it.uuid == message.uuid }

        _uiState.update {
            it.copy(
                isAwaitingReply = true,
                error = null,
                messages = it.messages.withTransientUserMessage(),
            )
        }

        sendJob = viewModelScope.launch {
            try {
                chatManager.sendMessage(episodeUuid, message, currentMessages)
                transientUserMessage = null
                eventHorizon.track(
                    EpisodeChatMessageSentEvent(
                        source = sourceView.analyticsValue,
                        episodeUuid = episodeUuid,
                        podcastUuid = podcastUuid,
                        messageLength = message.text.length.toLong(),
                    ),
                )
            } catch (e: IOException) {
                trackMessageFailed(EpisodeChatErrorType.Network)
                _uiState.update { it.copy(error = ChatError.NetworkError) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                trackMessageFailed(EpisodeChatErrorType.Server)
                _uiState.update { it.copy(error = ChatError.ServerError) }
            } finally {
                _uiState.update { it.copy(isAwaitingReply = false) }
            }
        }
    }

    private fun List<ChatMessage>.withTransientUserMessage(): List<ChatMessage> {
        val message = transientUserMessage ?: return this
        return if (any { it.uuid == message.uuid }) this else this + message
    }

    private fun showBetaSheetIfNeeded(isBeta: Boolean) {
        if (!isBeta || settings.episodeChatBetaSheetSeen.value || _uiState.value.isBetaSheetVisible) return
        _uiState.update { it.copy(isBetaSheetVisible = true) }
        eventHorizon.track(
            EpisodeChatBetaSheetShownEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        )
    }

    fun dismissBetaSheet() {
        if (!_uiState.value.isBetaSheetVisible) return
        settings.episodeChatBetaSheetSeen.set(true, updateModifiedAt = false)
        _uiState.update { it.copy(isBetaSheetVisible = false) }
        eventHorizon.track(
            EpisodeChatBetaSheetDismissedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        )
    }

    private fun trackShown() {
        eventHorizon.track(
            EpisodeChatShownEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        )
    }

    private fun trackMessageFailed(error: EpisodeChatErrorType) {
        eventHorizon.track(
            EpisodeChatMessageFailedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                error = error,
            ),
        )
    }

    private fun List<ChatMessage>.playingQuoteUuid(): String? {
        return filterIsInstance<ChatMessage.Quote>().firstOrNull { it.isPlaying }?.uuid
    }

    private fun List<ChatMessage>.withQuotePlaybackState(
        playingQuoteUuid: String?,
        episodeDurationMs: Int,
    ): List<ChatMessage> {
        val isQuotePlaybackEnabled = FeatureFlag.isEnabled(Feature.EPISODE_CHAT_PLAYABLE_QUOTES)
        return map { message ->
            if (message is ChatMessage.Quote) {
                val canPlay = isQuotePlaybackEnabled && message.isWithinEpisode(episodeDurationMs)
                message.copy(
                    canPlay = canPlay,
                    isPlaying = canPlay && message.uuid == playingQuoteUuid,
                )
            } else {
                message
            }
        }
    }

    private fun ChatMessage.Quote.isWithinEpisode(episodeDurationMs: Int): Boolean {
        val isValidRange = startMs >= 0 && endMs > startMs
        val endsBeforeEpisode = episodeDurationMs <= 0 || endMs < episodeDurationMs
        return isValidRange && endsBeforeEpisode
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
    val podcastTitle: String = "",
    val episodeDurationMs: Int = 0,
    val messages: List<ChatMessage> = emptyList(),
    val isConnected: Boolean = true,
    val isAwaitingReply: Boolean = false,
    val error: ChatError? = null,
    val isBeta: Boolean = false,
    val isBetaSheetVisible: Boolean = false,
    val playback: ChatPlayback = ChatPlayback(),
) {
    val canSend: Boolean get() = inputText.isNotBlank() && isConnected && !isAwaitingReply
}

data class ChatPlayback(
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
) {
    val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
