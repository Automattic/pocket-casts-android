package au.com.shiftyjelly.pocketcasts.chat

import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatAnswerRating
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatInputType
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatManager
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatQuoteSeeker
import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatSendFailure
import au.com.shiftyjelly.pocketcasts.repositories.chat.ClearedChat
import au.com.shiftyjelly.pocketcasts.repositories.chat.answerRating
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.eventhorizon.EpisodeChatBetaSheetDismissedEvent
import com.automattic.eventhorizon.EpisodeChatBetaSheetShownEvent
import com.automattic.eventhorizon.EpisodeChatBetaSheetSignInTappedEvent
import com.automattic.eventhorizon.EpisodeChatClearUndoneEvent
import com.automattic.eventhorizon.EpisodeChatClearedEvent
import com.automattic.eventhorizon.EpisodeChatDeleteUndoneEvent
import com.automattic.eventhorizon.EpisodeChatDeletedEvent
import com.automattic.eventhorizon.EpisodeChatDismissedEvent
import com.automattic.eventhorizon.EpisodeChatErrorType
import com.automattic.eventhorizon.EpisodeChatHistoryRowTappedEvent
import com.automattic.eventhorizon.EpisodeChatHistoryShownEvent
import com.automattic.eventhorizon.EpisodeChatInputType
import com.automattic.eventhorizon.EpisodeChatMessageFailedEvent
import com.automattic.eventhorizon.EpisodeChatMessageSentEvent
import com.automattic.eventhorizon.EpisodeChatQuotePlayTappedEvent
import com.automattic.eventhorizon.EpisodeChatQuotePlayUndoneEvent
import com.automattic.eventhorizon.EpisodeChatQuoteSourceType
import com.automattic.eventhorizon.EpisodeChatResponseRatedEvent
import com.automattic.eventhorizon.EpisodeChatResponseRatingClearedEvent
import com.automattic.eventhorizon.EpisodeChatResponseReceivedEvent
import com.automattic.eventhorizon.EpisodeChatSentimentType
import com.automattic.eventhorizon.EpisodeChatShownEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.Trackable
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.time.Clock
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.time.TimeSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val networkConnectionWatcher: NetworkConnectionWatcher,
    private val chatManager: ChatManager,
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val quoteSeeker: ChatQuoteSeeker,
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val timeSource: TimeSource,
    private val clock: Clock,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val _toasts = Channel<ChatToast>(Channel.CONFLATED)
    val toasts = _toasts.receiveAsFlow()

    private val _undoExpired = Channel<Unit>(Channel.CONFLATED)
    val undoExpired = _undoExpired.receiveAsFlow()

    private val _navigation = Channel<ChatNavigation>(Channel.CONFLATED)
    val navigation = _navigation.receiveAsFlow()

    private var sendJob: Job? = null
    private var quotePlaybackJob: Job? = null
    private var ratingWriteJob: Job? = null
    private var clearJob: Job? = null

    @Volatile private var clearedChat: ClearedChatState? = null

    private val chatEpisode = MutableStateFlow<ChatEpisode?>(null)
    private val episodeUuid get() = checkNotNull(chatEpisode.value).uuid
    private val podcastUuid get() = checkNotNull(chatEpisode.value).podcastUuid
    private var playingEpisodeUuid: String? = null
    private var historyItems: List<EpisodeChatHistoryItem>? = null
    private var isHistoryShownPending = false

    @Volatile private var isAdoptingUnansweredQuestion = false

    private var isSigningIn = false

    private val quotePlaybackUndo = AtomicReference<QuotePlaybackUndo?>()
    private val quoteLoadId = AtomicInteger()
    private val warmedUpEpisodeUuids = mutableSetOf<String>()
    private val warmedUpQuoteUuids = mutableSetOf<String>()
    private var lastQuotePlaybackUndoId = 0
    private var transientUserMessage: ChatMessage.User? = null
    private var pendingShownEntryPoint: ChatEntryPoint? = null
    private var hasReceivedAnswer = false
    private var hasSubmittedFeedback = false
    private var starterQuestionsEpisodeUuid: String? = null
    private lateinit var sourceView: SourceView

    init {
        viewModelScope.launch {
            networkConnectionWatcher.networkCapabilities.collect { capabilities ->
                val isConnected = capabilities != null &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val wasConnected = _uiState.value.isConnected
                _uiState.update { it.copy(isConnected = isConnected) }
                val state = _uiState.value
                if (!wasConnected && isConnected && state.areMessagesLoaded && state.messages.isEmpty()) loadStarterQuestions()
            }
        }
    }

    fun setEpisodeInfo(
        episodeUuid: String,
        episodeTitle: String,
        podcastUuid: String,
        podcastTitle: String,
        episodeDurationMs: Int,
        sourceView: SourceView,
        entryPoint: ChatEntryPoint,
        isBeta: Boolean,
        requiresSignIn: Boolean = false,
    ) {
        if (chatEpisode.value != null) return
        this.sourceView = sourceView
        _uiState.update {
            it.copy(
                isBeta = isBeta,
                isBetaNoteVisible = FeatureFlag.isEnabled(Feature.EPISODE_CHAT_FREE_BETA),
                isSignInRequired = requiresSignIn,
            )
        }
        openChat(ChatEpisode(episodeUuid, episodeTitle, podcastUuid, podcastTitle, episodeDurationMs), entryPoint)
        observePlayback()
        observeQuoteWarmUp()
        when {
            !requiresSignIn -> {
                observeMessages()
                observeHistory()
                showBetaSheetIfNeeded(isBeta)
            }

            syncManager.isLoggedIn() -> completeSignIn()

            else -> showBetaSheet()
        }
    }

    fun onSignInClick() {
        if (!_uiState.value.isSignInRequired || isSigningIn) return
        isSigningIn = true
        eventHorizon.track(
            EpisodeChatBetaSheetSignInTappedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        )
        _navigation.trySend(ChatNavigation.SignIn)
    }

    fun refreshSignIn() {
        isSigningIn = false
        if (!_uiState.value.isSignInRequired || !syncManager.isLoggedIn()) return
        completeSignIn()
    }

    private fun completeSignIn() {
        settings.episodeChatBetaSheetSeen.set(true, updateModifiedAt = false)
        _uiState.update {
            it.copy(
                isBeta = settings.cachedSubscription.value == null,
                isSignInRequired = false,
                isBetaSheetVisible = false,
            )
        }
        pendingShownEntryPoint = ChatEntryPoint.SignIn
        observeMessages()
        observeHistory()
        viewModelScope.launch {
            settings.cachedSubscription.flow.collect { subscription ->
                _uiState.update { it.copy(isBeta = subscription == null) }
            }
        }
    }

    private fun openChat(episode: ChatEpisode, entryPoint: ChatEntryPoint) {
        sendJob?.cancel()
        transientUserMessage = null
        starterQuestionsEpisodeUuid = null
        isAdoptingUnansweredQuestion = true
        quoteLoadId.incrementAndGet()
        _uiState.update {
            it.copy(
                loadingQuoteUuid = null,
                episodeUuid = episode.uuid,
                episodeTitle = episode.title,
                podcastUuid = episode.podcastUuid,
                podcastTitle = episode.podcastTitle,
                episodeDurationMs = episode.durationMs,
                inputText = "",
                restoredMessageUuids = emptySet(),
                messages = emptyList(),
                areMessagesLoaded = false,
                isAwaitingReply = false,
                error = null,
                answerRatings = emptyMap(),
                starterQuestions = emptyList(),
            )
        }
        trackShownIfPending(hasHistory = null)
        pendingShownEntryPoint = entryPoint.takeUnless { _uiState.value.isSignInRequired }
        chatEpisode.value = episode
        createChat(episode)
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatEpisode.filterNotNull().flatMapLatest { episode ->
                chatManager.observeMessages(episode.uuid).map { messages -> episode.uuid to messages }
            }.collect { (messagesEpisodeUuid, messages) ->
                val unansweredQuestion = adoptUnansweredQuestion(messagesEpisodeUuid, messages)
                _uiState.update { state ->
                    if (state.episodeUuid != messagesEpisodeUuid) return@update state
                    state.copy(
                        messages = messages.withTransientUserMessage().withQuotePlaybackState(
                            episodeDurationMs = state.episodeDurationMs,
                        ),
                        areMessagesLoaded = true,
                        answerRatings = if (state.areMessagesLoaded) state.answerRatings else messages.storedAnswerRatings(),
                        error = unansweredQuestion?.let { it.failure.toChatError() } ?: state.error,
                    )
                }
                if (messagesEpisodeUuid == chatEpisode.value?.uuid) {
                    trackShownIfPending(hasHistory = messages.isNotEmpty())
                    if (messages.isEmpty()) loadStarterQuestions()
                }
            }
        }
    }

    private fun loadStarterQuestions() {
        val episode = chatEpisode.value ?: return
        if (starterQuestionsEpisodeUuid == episode.uuid) return
        starterQuestionsEpisodeUuid = episode.uuid
        viewModelScope.launch {
            val questions = chatManager.getStarterQuestions(episode.uuid, episode.podcastUuid)
            if (questions.isEmpty() && starterQuestionsEpisodeUuid == episode.uuid) {
                starterQuestionsEpisodeUuid = null
            }
            _uiState.update { state ->
                if (state.episodeUuid == episode.uuid) state.copy(starterQuestions = questions) else state
            }
        }
    }

    private fun adoptUnansweredQuestion(messagesEpisodeUuid: String, messages: List<ChatMessage>): ChatMessage.User? {
        val state = _uiState.value
        if (!isAdoptingUnansweredQuestion || state.episodeUuid != messagesEpisodeUuid) return null
        isAdoptingUnansweredQuestion = false
        if (state.isAwaitingReply || transientUserMessage != null) return null
        val question = messages.lastOrNull() as? ChatMessage.User ?: return null
        transientUserMessage = question
        return question
    }

    private fun ChatSendFailure?.toChatError() = when (this) {
        ChatSendFailure.Server -> ChatError.ServerError
        ChatSendFailure.Network, null -> ChatError.NetworkError
    }

    private fun observePlayback() {
        viewModelScope.launch {
            combine(
                playbackManager.playbackStateFlow,
                chatEpisode.filterNotNull().flatMapLatest { episodeManager.findEpisodeByUuidFlow(it.uuid) },
                playbackManager.playbackStateFlow
                    .map { it.loadedEpisodeUuid }
                    .distinctUntilChanged()
                    .map { uuid -> uuid?.takeIf { it != episodeUuid }?.let { episodeManager.findEpisodeByUuid(it) as? UserEpisode } },
            ) { playbackState, episode, playingUserEpisode ->
                playingEpisodeUuid = playbackState.loadedEpisodeUuid
                when (playbackState.loadedEpisodeUuid) {
                    null -> null to ChatPlayback(
                        isPlaying = false,
                        positionMs = episode.playedUpToMs,
                        durationMs = episode.durationMs,
                    )

                    episode.uuid -> null to ChatPlayback(
                        isPlaying = playbackState.isPlaying,
                        positionMs = playbackState.positionMs,
                        durationMs = playbackState.durationMs.takeIf { it > 0 } ?: episode.durationMs,
                    )

                    else -> ChatPlayingEpisode(
                        episodeTitle = playbackState.title,
                        podcastUuid = playbackState.podcast?.uuid.orEmpty(),
                        podcastTitle = playbackState.podcast?.title.orEmpty(),
                        userEpisode = playingUserEpisode?.takeIf { it.uuid == playbackState.loadedEpisodeUuid },
                    ) to ChatPlayback(
                        isPlaying = playbackState.isPlaying,
                        positionMs = playbackState.positionMs,
                        durationMs = playbackState.durationMs.coerceAtLeast(0),
                    )
                }
            }.distinctUntilChanged().collect { (otherPlayingEpisode, playback) ->
                _uiState.update { it.copy(otherPlayingEpisode = otherPlayingEpisode, playback = playback) }
            }
        }
        viewModelScope.launch {
            playbackManager.playbackStateFlow
                .map { playbackState -> playbackState.podcast?.takeIf { playbackState.loadedEpisodeUuid != null } }
                .distinctUntilChangedBy { it?.uuid }
                .collect { podcast -> _uiState.update { it.copy(playingPodcast = podcast) } }
        }
    }

    private fun observeQuoteWarmUp() {
        viewModelScope.launch {
            combine(
                _uiState.map { it.episodeUuid to it.messages }.distinctUntilChanged(),
                playbackManager.playbackStateFlow.map { it.loadedEpisodeUuid }.distinctUntilChanged(),
            ) { (chatEpisodeUuid, messages), loadedEpisodeUuid ->
                val quotes = messages.filterIsInstance<ChatMessage.Quote>().filter { it.canPlay }
                chatEpisodeUuid to quotes.takeIf { chatEpisodeUuid == loadedEpisodeUuid }.orEmpty()
            }.collect { (chatEpisodeUuid, quotes) -> warmUpQuotes(chatEpisodeUuid, quotes) }
        }
    }

    private fun warmUpQuotes(episodeUuid: String, quotes: List<ChatMessage.Quote>) {
        if (quotes.isEmpty()) return
        if (warmedUpEpisodeUuids.add(episodeUuid)) {
            quoteSeeker.warmUpFingerprints(episodeUuid)
        }
        quotes.filter { warmedUpQuoteUuids.add(it.uuid) }.forEach { quoteSeeker.warmUpQuote(episodeUuid, it) }
    }

    fun onPlayPauseClick() {
        val episodeUuid = episodeUuid
        val previousJob = quotePlaybackJob
        previousJob?.cancel()
        viewModelScope.launch(ioDispatcher) {
            previousJob?.join()
            val state = playbackManager.playbackStateFlow.first()
            when {
                state.loadedEpisodeUuid != null && state.isPlaying -> playbackManager.pause(sourceView = sourceView)
                state.loadedEpisodeUuid != null -> playbackManager.playQueueSuspend(sourceView = sourceView)
                else -> playbackManager.playNowSuspend(episodeUuid, sourceView = sourceView)
            }
        }
    }

    private fun createChat(episode: ChatEpisode) {
        viewModelScope.launch {
            chatManager.createChat(episode.uuid, episode.podcastUuid)
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            chatManager.observeHistory().collect { items ->
                historyItems = items
                _uiState.update { it.copy(history = items.toHistorySections(clock.instant(), ZoneId.systemDefault())) }
                if (isHistoryShownPending) {
                    isHistoryShownPending = false
                    if (_uiState.value.isHistoryVisible) {
                        trackHistoryShown()
                    }
                }
            }
        }
    }

    fun showHistory() {
        _uiState.update {
            it.copy(
                isHistoryVisible = true,
                history = historyItems.orEmpty().toHistorySections(clock.instant(), ZoneId.systemDefault()),
                useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
            )
        }
        if (historyItems == null) {
            isHistoryShownPending = true
        } else {
            trackHistoryShown()
        }
    }

    private fun trackHistoryShown() {
        eventHorizon.track(
            EpisodeChatHistoryShownEvent(
                source = sourceView.analyticsValue,
                chatCount = _uiState.value.history.sumOf { it.items.size }.toLong(),
            ),
        )
    }

    fun closeHistory() {
        _uiState.update { it.copy(isHistoryVisible = false) }
    }

    fun openHistoryChat(item: EpisodeChatHistoryItem) {
        val row = _uiState.value.history.rowOf(item.episodeUuid)
        eventHorizon.track(
            EpisodeChatHistoryRowTappedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = item.episodeUuid,
                podcastUuid = item.podcastUuid,
                isNowPlaying = item.episodeUuid == playingEpisodeUuid,
                section = row?.first?.analyticsValue,
                position = row?.second,
            ),
        )
        if (item.episodeUuid != episodeUuid) {
            openChat(
                ChatEpisode(
                    uuid = item.episodeUuid,
                    title = item.episodeTitle,
                    podcastUuid = item.podcastUuid,
                    podcastTitle = item.podcastTitle,
                    durationMs = item.episodeDurationMs,
                ),
                ChatEntryPoint.History,
            )
        }
        closeHistory()
    }

    fun deleteHistoryChat(item: EpisodeChatHistoryItem) {
        eventHorizon.track(
            EpisodeChatDeletedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = item.episodeUuid,
                podcastUuid = item.podcastUuid,
            ),
        )
        if (item.episodeUuid == episodeUuid) {
            clearCurrentChat()
        } else {
            takeChat(item.episodeUuid, item.podcastUuid, pendingSend = null)
        }
        showToast(ChatToast.ChatDeleted)
    }

    fun undoDeleteChat() {
        restoreTakenChat { chat ->
            EpisodeChatDeleteUndoneEvent(
                source = sourceView.analyticsValue,
                episodeUuid = chat.episodeUuid,
                podcastUuid = chat.podcastUuid,
            )
        }
    }

    fun onInputTextChange(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun clearChat() {
        val state = _uiState.value
        if (state.messages.isEmpty() && !state.isAwaitingReply) return
        eventHorizon.track(
            EpisodeChatClearedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        )
        clearCurrentChat()
        showToast(ChatToast.ChatCleared)
    }

    fun undoClearChat() {
        restoreTakenChat { chat ->
            EpisodeChatClearUndoneEvent(
                source = sourceView.analyticsValue,
                episodeUuid = chat.episodeUuid,
                podcastUuid = chat.podcastUuid,
            )
        }
    }

    private fun clearCurrentChat() {
        val pendingSend = sendJob
        pendingSend?.cancel()
        transientUserMessage = null
        _uiState.update {
            it.copy(
                messages = emptyList(),
                isAwaitingReply = false,
                error = null,
                answerRatings = emptyMap(),
            )
        }
        takeChat(episodeUuid, podcastUuid, pendingSend)
    }

    private fun takeChat(
        episodeUuid: String,
        podcastUuid: String,
        pendingSend: Job?,
    ) {
        clearedChat = null
        val previousClear = clearJob
        val pendingRatingWrite = ratingWriteJob
        clearJob = applicationScope.launch(ioDispatcher) {
            previousClear?.join()
            pendingSend?.join()
            pendingRatingWrite?.join()
            clearedChat = chatManager.clearMessages(episodeUuid)
                .takeIf { it.messages.isNotEmpty() }
                ?.let { ClearedChatState(it, episodeUuid, podcastUuid) }
        }
    }

    private fun restoreTakenChat(undoneEvent: (ClearedChatState) -> Trackable) {
        val pendingClear = clearJob
        applicationScope.launch(ioDispatcher) {
            pendingClear?.join()
            val cleared = clearedChat ?: return@launch
            clearedChat = null
            if (cleared.episodeUuid == chatEpisode.value?.uuid) {
                _uiState.update { state ->
                    state.copy(
                        answerRatings = cleared.chat.messages.storedAnswerRatings() + state.answerRatings,
                        restoredMessageUuids = state.restoredMessageUuids + cleared.chat.messages.map { it.uuid },
                    )
                }
                isAdoptingUnansweredQuestion = true
            }
            chatManager.restoreMessages(cleared.chat)
            eventHorizon.track(undoneEvent(cleared))
        }
    }

    fun trackDismissed() {
        if (_uiState.value.isSignInRequired) return
        trackShownIfPending(hasHistory = null)
        eventHorizon.track(
            EpisodeChatDismissedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        )
    }

    fun onSend() {
        if (!_uiState.value.canSend) return
        val text = _uiState.value.inputText.trim()

        _uiState.update { it.copy(inputText = "") }
        performSend(message = ChatMessage.User(text = text, inputType = ChatInputType.Typed))
    }

    fun onSuggestionClick(text: String, inputType: ChatInputType) {
        val state = _uiState.value
        if (!state.areMessagesLoaded || state.isAwaitingReply || !state.isConnected) return
        performSend(message = ChatMessage.User(text = text, inputType = inputType))
    }

    fun retry() {
        if (!_uiState.value.isConnected || _uiState.value.isAwaitingReply) return
        val failedUserMessage = transientUserMessage ?: return
        performSend(message = failedUserMessage, isRetry = true)
    }

    fun playQuote(quoteUuid: String) {
        val quote = _uiState.value.messages
            .filterIsInstance<ChatMessage.Quote>()
            .firstOrNull { it.uuid == quoteUuid && it.canPlay }
            ?: return
        if (quoteUuid == _uiState.value.loadingQuoteUuid) return

        val episodeUuid = episodeUuid
        val podcastUuid = podcastUuid
        val isOtherEpisode = _uiState.value.otherPlayingEpisode != null
        eventHorizon.track(
            EpisodeChatQuotePlayTappedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                quoteSource = EpisodeChatQuoteSourceType.InlineTimestamp,
                isOtherEpisode = isOtherEpisode,
            ),
        )
        val loadId = startQuoteLoading(quoteUuid)
        val previousJob = quotePlaybackJob
        previousJob?.cancel()
        quotePlaybackJob = applicationScope.launch(ioDispatcher) {
            try {
                previousJob?.join()
                seekToQuote(quote, episodeUuid, podcastUuid, isOtherEpisode, loadId)
            } finally {
                finishQuoteLoading(loadId)
            }
        }
    }

    private fun startQuoteLoading(quoteUuid: String): Int {
        val loadId = quoteLoadId.incrementAndGet()
        _uiState.update { it.copy(loadingQuoteUuid = quoteUuid) }
        return loadId
    }

    private fun finishQuoteLoading(loadId: Int) {
        _uiState.update { state ->
            if (quoteLoadId.get() == loadId) state.copy(loadingQuoteUuid = null) else state
        }
    }

    private suspend fun seekToQuote(
        quote: ChatMessage.Quote,
        episodeUuid: String,
        podcastUuid: String,
        isOtherEpisode: Boolean,
        loadId: Int,
    ) = coroutineScope {
        val episode = episodeManager.findEpisodeByUuid(episodeUuid)
        if (episode == null || !quote.isWithinEpisode(episode.durationMs)) return@coroutineScope
        val quoteTime = async { quoteSeeker.quoteTime(episodeUuid, quote) }
        val userSeek = async(start = CoroutineStart.UNDISPATCHED) { playbackManager.userSeeks.first() }
        val state = playbackManager.playbackStateFlow.first()
        val episodeSwitch = if (state.loadedEpisodeUuid != episodeUuid) {
            val switch = EpisodeSwitch(
                queue = playbackManager.upNextQueue.queueEpisodes,
                episodes = listOf(SwitchedEpisode(episode)),
            )
            playbackManager.playNowSuspend(episode = episode, sourceView = sourceView)
            if (!awaitPlaybackEpisode(episodeUuid)) {
                quoteTime.cancel()
                userSeek.cancel()
                finishQuoteLoading(loadId)
                showToast(ChatToast.QuotePlayFailed)
                if (playbackManager.upNextQueue.currentEpisode?.uuid == episodeUuid) {
                    restorePlayback(state.toPlaybackPosition(episodeUuid, podcastUuid, switch, isOtherEpisode))
                } else {
                    if (switch.queue.none { it.uuid == episodeUuid }) {
                        playbackManager.upNextQueue.removeEpisode(episode)
                    }
                    restoreSwitchedEpisodes(switch)
                }
                return@coroutineScope
            }
            switch
        } else {
            null
        }
        val seekTimeMs = try {
            quoteSeeker.playbackTimeMs(episode, quoteTime.await())
        } finally {
            userSeek.cancel()
        }
        if (userSeek.isCompleted && !userSeek.isCancelled) return@coroutineScope
        if (playbackManager.playbackStateFlow.first().loadedEpisodeUuid != episodeUuid) return@coroutineScope
        playbackManager.seekToTimeMsSuspend(positionMs = seekTimeMs)
        playbackManager.playQueueSuspend(sourceView = sourceView)
        val undoId = ++lastQuotePlaybackUndoId
        quotePlaybackUndo.updateAndGet { undo ->
            QuotePlaybackUndo(
                id = undoId,
                position = undo?.position?.withLaterSwitch(episodeSwitch, isOtherEpisode)
                    ?: state.toPlaybackPosition(episodeUuid, podcastUuid, episodeSwitch, isOtherEpisode),
            )
        }
        showToast(ChatToast.QuotePlaying(episode.title, undoId))
    }

    fun onQuoteUndoExpired(undoId: Int) {
        quotePlaybackUndo.updateAndGet { undo -> undo?.takeUnless { it.id == undoId } }
    }

    private fun showToast(toast: ChatToast) {
        if (toast !is ChatToast.QuotePlaying) {
            quotePlaybackUndo.set(null)
        }
        _toasts.trySend(toast)
    }

    fun undoQuotePlay() {
        val undo = quotePlaybackUndo.getAndSet(null)?.position ?: return
        eventHorizon.track(
            EpisodeChatQuotePlayUndoneEvent(
                source = sourceView.analyticsValue,
                episodeUuid = undo.quoteEpisodeUuid,
                podcastUuid = undo.quotePodcastUuid,
                isOtherEpisode = undo.isOtherEpisode,
            ),
        )
        val previousJob = quotePlaybackJob
        previousJob?.cancel()
        quotePlaybackJob = applicationScope.launch(ioDispatcher) {
            previousJob?.join()
            restorePlayback(undo)
        }
    }

    private suspend fun restorePlayback(undo: PlaybackPosition) {
        val switch = undo.episodeSwitch
        val previousEpisode = undo.episodeUuid?.let { episodeManager.findEpisodeByUuid(it) }
        if (previousEpisode == null) {
            if (switch == null) {
                playbackManager.pause(sourceView = sourceView)
            } else {
                playbackManager.stop()
                playbackManager.upNextQueue.removeAll()
                restoreSwitchedEpisodes(switch)
            }
            return
        }
        if (undo.hasSwitchedAwayFromPreviousEpisode) {
            playbackManager.playNowSuspend(episode = previousEpisode, sourceView = sourceView)
            val isLoaded = awaitPlaybackEpisode(previousEpisode.uuid)
            if (!undo.isPlaying) {
                playbackManager.pause(sourceView = sourceView)
            }
            if (switch != null) {
                if (playbackManager.upNextQueue.currentEpisode?.uuid == previousEpisode.uuid) {
                    playbackManager.upNextQueue.changeList(switch.queue)
                }
                restoreSwitchedEpisodes(switch)
            }
            if (!isLoaded) return
        }
        playbackManager.seekToTimeMsSuspend(positionMs = undo.positionMs)
        if (!undo.isPlaying) {
            playbackManager.pause(sourceView = sourceView)
        }
    }

    private suspend fun restoreSwitchedEpisodes(switch: EpisodeSwitch) {
        switch.episodes.forEach { switched ->
            val episode = episodeManager.findEpisodeByUuid(switched.uuid) ?: return@forEach
            episodeManager.updatePlayingStatusBlocking(episode, switched.playingStatus)
            episodeManager.updatePlayedUpToBlocking(episode, switched.playedUpTo, forceUpdate = true)
            if (switched.wasArchived && episode is PodcastEpisode) {
                episodeManager.archiveAllInList(listOf(episode), playbackManager = null)
            }
        }
    }

    private suspend fun awaitPlaybackEpisode(episodeUuid: String): Boolean {
        return withTimeoutOrNull(QUOTE_PLAYBACK_EPISODE_TIMEOUT_MS) {
            playbackManager.playbackStateFlow.first { it.loadedEpisodeUuid == episodeUuid }
        } != null
    }

    private fun performSend(message: ChatMessage.User, isRetry: Boolean = false) {
        val inputType = message.inputType.analyticsValue
        val supersededQuestionUuid = transientUserMessage?.uuid?.takeIf { it != message.uuid }
        val unsentMessageUuids = setOfNotNull(transientUserMessage?.uuid, message.uuid)
        transientUserMessage = message
        val currentMessages = _uiState.value.messages.filterNot { it.uuid in unsentMessageUuids }
        val messageIndex = currentMessages.count { it is ChatMessage.User } + 1L
        eventHorizon.track(
            EpisodeChatMessageSentEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                messageLength = message.text.length.toLong(),
                inputType = inputType,
                messageIndex = messageIndex,
                isRetry = isRetry,
            ),
        )

        _uiState.update {
            it.copy(
                isAwaitingReply = true,
                error = null,
                messages = it.messages.filterNot { message -> message.uuid in unsentMessageUuids }.withTransientUserMessage(),
            )
        }

        val pendingClear = clearJob
        sendJob = viewModelScope.launch {
            try {
                pendingClear?.join()
                val cleared = clearedChat
                if (cleared == null || cleared.episodeUuid == episodeUuid) {
                    clearedChat = null
                    _undoExpired.trySend(Unit)
                }
                supersededQuestionUuid?.let { chatManager.discardMessage(it) }
                val sentAt = timeSource.markNow()
                chatManager.sendMessage(episodeUuid, podcastUuid, message, currentMessages)
                val responseDuration = sentAt.elapsedNow()
                transientUserMessage = null
                hasReceivedAnswer = true
                eventHorizon.track(
                    EpisodeChatResponseReceivedEvent(
                        source = sourceView.analyticsValue,
                        episodeUuid = episodeUuid,
                        podcastUuid = podcastUuid,
                        durationMs = responseDuration.inWholeMilliseconds,
                        messageIndex = messageIndex,
                    ),
                )
            } catch (e: IOException) {
                trackMessageFailed(EpisodeChatErrorType.Network, inputType, messageIndex)
                _uiState.update { it.copy(error = ChatError.NetworkError) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                trackMessageFailed(EpisodeChatErrorType.Server, inputType, messageIndex)
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
        if (!isBeta || settings.episodeChatBetaSheetSeen.value) return
        showBetaSheet()
    }

    private fun showBetaSheet() {
        if (_uiState.value.isBetaSheetVisible) return
        _uiState.update { it.copy(isBetaSheetVisible = true) }
        eventHorizon.track(
            EpisodeChatBetaSheetShownEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        )
    }

    fun rateAnswer(answerUuid: String, rating: ChatAnswerRating): ChatAnswerRating? {
        val previousRating = _uiState.value.answerRatings[answerUuid]
        val newRating = rating.takeIf { it != previousRating }
        _uiState.update { state ->
            val ratings = if (newRating == null) {
                state.answerRatings - answerUuid
            } else {
                state.answerRatings + (answerUuid to newRating)
            }
            state.copy(answerRatings = ratings)
        }
        saveAnswerRating(answerUuid, newRating)
        trackAnswerRated(answerUuid, newRating, previousRating)
        if (newRating == ChatAnswerRating.Positive) {
            showToast(ChatToast.FeedbackThanks)
        }
        return newRating
    }

    private fun saveAnswerRating(answerUuid: String, rating: ChatAnswerRating?) {
        val previousJob = ratingWriteJob
        ratingWriteJob = applicationScope.launch(ioDispatcher) {
            previousJob?.join()
            try {
                chatManager.rateAnswer(answerUuid, rating)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                LogBuffer.e(LOG_TAG, e, "Failed to save episode chat answer rating")
            }
        }
    }

    private fun List<ChatMessage>.storedAnswerRatings(): Map<String, ChatAnswerRating> {
        return mapNotNull { message -> message.answerRating?.let { message.uuid to it } }.toMap()
    }

    fun answerMessageIndex(answerUuid: String): Long? {
        val messages = _uiState.value.messages
        val answerIndex = messages.indexOfFirst { it.uuid == answerUuid }
        if (answerIndex < 0) return null
        return messages.subList(0, answerIndex).count { it is ChatMessage.User }.toLong()
    }

    private fun trackAnswerRated(answerUuid: String, rating: ChatAnswerRating?, previousRating: ChatAnswerRating?) {
        val event = when {
            rating != null -> EpisodeChatResponseRatedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                rating = rating.analyticsValue,
                messageIndex = answerMessageIndex(answerUuid),
                previousRating = previousRating?.analyticsValue,
            )

            previousRating != null -> EpisodeChatResponseRatingClearedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                previousRating = previousRating.analyticsValue,
                messageIndex = answerMessageIndex(answerUuid),
            )

            else -> return
        }
        eventHorizon.track(event)
    }

    fun onFeedbackSubmitted() {
        hasSubmittedFeedback = true
        showToast(ChatToast.FeedbackThanks)
    }

    fun consumeSurveyEligibility(): Boolean {
        if (!hasReceivedAnswer || hasSubmittedFeedback || settings.episodeChatSurveySeen.value) return false
        settings.episodeChatSurveySeen.set(true, updateModifiedAt = false)
        return true
    }

    fun dismissBetaSheet() {
        val state = _uiState.value
        if (!state.isBetaSheetVisible) return
        if (!state.isSignInRequired) {
            settings.episodeChatBetaSheetSeen.set(true, updateModifiedAt = false)
        }
        _uiState.update { it.copy(isBetaSheetVisible = false) }
        eventHorizon.track(
            EpisodeChatBetaSheetDismissedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
            ),
        )
        if (state.isSignInRequired) {
            _navigation.trySend(ChatNavigation.Close)
        }
    }

    private fun trackShownIfPending(hasHistory: Boolean?) {
        val entryPoint = pendingShownEntryPoint ?: return
        pendingShownEntryPoint = null
        eventHorizon.track(
            EpisodeChatShownEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                hasHistory = hasHistory,
                entryPoint = entryPoint.analyticsValue,
                isBeta = _uiState.value.isBeta,
            ),
        )
    }

    private fun trackMessageFailed(error: EpisodeChatErrorType, inputType: EpisodeChatInputType, messageIndex: Long) {
        eventHorizon.track(
            EpisodeChatMessageFailedEvent(
                source = sourceView.analyticsValue,
                episodeUuid = episodeUuid,
                podcastUuid = podcastUuid,
                error = error,
                inputType = inputType,
                messageIndex = messageIndex,
            ),
        )
    }

    private fun List<ChatMessage>.withQuotePlaybackState(
        episodeDurationMs: Int,
    ): List<ChatMessage> {
        return map { message ->
            if (message is ChatMessage.Quote) {
                message.copy(canPlay = message.isWithinEpisode(episodeDurationMs))
            } else {
                message
            }
        }
    }

    private fun ChatMessage.Quote.isWithinEpisode(episodeDurationMs: Int): Boolean {
        return startMs >= 0 && (episodeDurationMs <= 0 || startMs < episodeDurationMs)
    }
}

private const val QUOTE_PLAYBACK_EPISODE_TIMEOUT_MS = 10_000L
private const val LOG_TAG = "EpisodeChat"

private val PlaybackState.loadedEpisodeUuid
    get() = episodeUuid.takeIf { it.isNotEmpty() && !isEmpty && !isStopped && !isError }

private class ClearedChatState(
    val chat: ClearedChat,
    val episodeUuid: String,
    val podcastUuid: String,
)

private data class ChatEpisode(
    val uuid: String,
    val title: String,
    val podcastUuid: String,
    val podcastTitle: String,
    val durationMs: Int,
)

private class QuotePlaybackUndo(
    val id: Int,
    val position: PlaybackPosition,
)

private class PlaybackPosition(
    val quoteEpisodeUuid: String,
    val quotePodcastUuid: String,
    val episodeUuid: String?,
    val positionMs: Int,
    val isPlaying: Boolean,
    val episodeSwitch: EpisodeSwitch?,
    val isOtherEpisode: Boolean,
) {
    val hasSwitchedAwayFromPreviousEpisode get() = episodeSwitch?.episodes.orEmpty().any { it.uuid != episodeUuid }

    fun withLaterSwitch(laterSwitch: EpisodeSwitch?, isLaterOtherEpisode: Boolean): PlaybackPosition {
        val knownUuids = episodeSwitch?.episodes.orEmpty().map { it.uuid }.toSet()
        val newEpisodes = laterSwitch?.episodes.orEmpty().filterNot { it.uuid in knownUuids }
        val switch = if (newEpisodes.isEmpty()) {
            episodeSwitch
        } else {
            episodeSwitch?.let { it.copy(episodes = it.episodes + newEpisodes) } ?: laterSwitch
        }
        return PlaybackPosition(
            quoteEpisodeUuid = quoteEpisodeUuid,
            quotePodcastUuid = quotePodcastUuid,
            episodeUuid = episodeUuid,
            positionMs = positionMs,
            isPlaying = isPlaying,
            episodeSwitch = switch,
            isOtherEpisode = isOtherEpisode || isLaterOtherEpisode,
        )
    }
}

private fun PlaybackState.toPlaybackPosition(
    quoteEpisodeUuid: String,
    quotePodcastUuid: String,
    episodeSwitch: EpisodeSwitch?,
    isOtherEpisode: Boolean,
) = PlaybackPosition(
    quoteEpisodeUuid = quoteEpisodeUuid,
    quotePodcastUuid = quotePodcastUuid,
    episodeUuid = episodeUuid.takeUnless { isEmpty || it.isEmpty() },
    positionMs = positionMs,
    isPlaying = isPlaying,
    episodeSwitch = episodeSwitch,
    isOtherEpisode = isOtherEpisode,
)

private data class EpisodeSwitch(
    val queue: List<BaseEpisode>,
    val episodes: List<SwitchedEpisode>,
)

private class SwitchedEpisode(
    val uuid: String,
    val wasArchived: Boolean,
    val playingStatus: EpisodePlayingStatus,
    val playedUpTo: Double,
) {
    constructor(episode: BaseEpisode) : this(episode.uuid, episode.isArchived, episode.playingStatus, episode.playedUpTo)
}

sealed interface ChatNavigation {
    data object SignIn : ChatNavigation
    data object Close : ChatNavigation
}

sealed interface ChatToast {
    data object FeedbackThanks : ChatToast
    data object ChatCleared : ChatToast
    data object ChatDeleted : ChatToast
    data class QuotePlaying(val episodeTitle: String, val undoId: Int) : ChatToast
    data object QuotePlayFailed : ChatToast
}

data class ChatUiState(
    val inputText: String = "",
    val episodeUuid: String = "",
    val episodeTitle: String = "",
    val podcastUuid: String = "",
    val podcastTitle: String = "",
    val episodeDurationMs: Int = 0,
    val messages: List<ChatMessage> = emptyList(),
    val areMessagesLoaded: Boolean = false,
    val isConnected: Boolean = true,
    val isAwaitingReply: Boolean = false,
    val error: ChatError? = null,
    val isBeta: Boolean = false,
    val isBetaNoteVisible: Boolean = false,
    val isBetaSheetVisible: Boolean = false,
    val isSignInRequired: Boolean = false,
    val playback: ChatPlayback = ChatPlayback(),
    val otherPlayingEpisode: ChatPlayingEpisode? = null,
    val playingPodcast: Podcast? = null,
    val history: List<ChatHistorySection> = emptyList(),
    val isHistoryVisible: Boolean = false,
    val useEpisodeArtwork: Boolean = false,
    val answerRatings: Map<String, ChatAnswerRating> = emptyMap(),
    val restoredMessageUuids: Set<String> = emptySet(),
    val loadingQuoteUuid: String? = null,
    val starterQuestions: List<String> = emptyList(),
) {
    val canSend: Boolean get() = inputText.isNotBlank() && isConnected && !isAwaitingReply

    val followUps: List<String>
        get() {
            if (isAwaitingReply) return emptyList()
            val answerUuid = answerUuidsByLastIndex[messages.lastIndex] ?: return emptyList()
            return messages.firstNotNullOfOrNull { message ->
                (message as? ChatMessage.Assistant)?.takeIf { it.uuid == answerUuid }?.followUps
            }.orEmpty()
        }

    val answerUuidsByLastIndex: Map<Int, String>
        get() = buildMap {
            var answerUuid: String? = null
            messages.forEachIndexed { index, message ->
                if (message is ChatMessage.User) {
                    answerUuid = null
                } else {
                    val uuid = answerUuid ?: message.uuid
                    answerUuid = uuid
                    val next = messages.getOrNull(index + 1)
                    if (next == null || next is ChatMessage.User) {
                        put(index, uuid)
                    }
                }
            }
        }
}

private val ChatAnswerRating.analyticsValue
    get() = when (this) {
        ChatAnswerRating.Positive -> EpisodeChatSentimentType.Positive
        ChatAnswerRating.Negative -> EpisodeChatSentimentType.Negative
    }

private val ChatInputType.analyticsValue
    get() = when (this) {
        ChatInputType.Typed -> EpisodeChatInputType.Typed
        ChatInputType.SummaryPrompt -> EpisodeChatInputType.SummaryPrompt
        ChatInputType.SuggestedPrompt -> EpisodeChatInputType.SuggestedPrompt
        ChatInputType.FollowUp -> EpisodeChatInputType.FollowUp
    }

data class ChatPlayingEpisode(
    val episodeTitle: String,
    val podcastUuid: String,
    val podcastTitle: String,
    val userEpisode: UserEpisode? = null,
)

data class ChatPlayback(
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
) {
    val progress: Float get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
