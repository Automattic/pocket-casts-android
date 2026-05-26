package au.com.shiftyjelly.pocketcasts.transcripts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.utils.search.SearchCoordinates
import au.com.shiftyjelly.pocketcasts.utils.search.SearchMatches
import au.com.shiftyjelly.pocketcasts.utils.search.kmpSearch
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.Trackable
import com.automattic.eventhorizon.TranscriptErrorEvent
import com.automattic.eventhorizon.TranscriptGeneratedPaywallShownEvent
import com.automattic.eventhorizon.TranscriptSearchNextResultEvent
import com.automattic.eventhorizon.TranscriptSearchPreviousResultEvent
import com.automattic.eventhorizon.TranscriptSearchShownEvent
import com.automattic.eventhorizon.TranscriptShownEvent
import com.automattic.eventhorizon.TranscriptSourceType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel(assistedFactory = TranscriptViewModel.Factory::class)
class TranscriptViewModel @AssistedInject constructor(
    @Assisted private val source: Source,
    private val transcriptManager: TranscriptManager,
    private val episodeManager: EpisodeManager,
    private val userManager: UserManager,
    private val paymentClient: PaymentClient,
    private val eventHorizon: EventHorizon,
    private val sharingClient: TranscriptSharingClient,
    val fingerprintTimingManager: FingerprintTimingManager,
    val playbackManager: PlaybackManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState.Empty)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val subscriptionPlans = paymentClient.loadSubscriptionPlans().getOrNull()
            val trialOffer = subscriptionPlans?.findOfferPlan(SubscriptionTier.Plus, BillingCycle.Monthly, SubscriptionOffer.Trial)
            _uiState.update { state ->
                state.copy(isFreeTrialAvailable = trialOffer != null)
            }
        }
        viewModelScope.launch {
            userManager.getSignInState().asFlow().collect { signInState ->
                _uiState.update { state ->
                    state.copy(isPlusUser = signInState.isSignedInAsPlusOrPatron)
                }
            }
        }
        observePlaybackState()
    }

    private var episodeUuid: String? = null
    private var podcastUuid: String? = null

    private var loadTranscriptJob: Job? = null
    private var searchJob: Job? = null

    fun loadTranscript(episodeUuid: String) {
        loadTranscriptJob?.cancel()
        syncedStateJob?.cancel()
        loadTranscriptJob = viewModelScope.launch {
            searchJob?.cancelAndJoin()

            _uiState.update { state ->
                state.copy(
                    transcriptState = TranscriptState.Loading,
                    searchState = SearchState.Empty,
                    syncedState = FingerprintTimingManager.State.Idle,
                )
            }

            updateEpisodeMetadata(episodeUuid)
            val transcriptState = when (val transcript = transcriptManager.loadTranscript(episodeUuid)) {
                is Transcript.Text -> if (transcript.entries.isNotEmpty()) {
                    TranscriptState.Loaded(transcript)
                } else {
                    track { source, podcastUuid, episodeUuid ->
                        TranscriptErrorEvent(
                            podcastUuid = podcastUuid,
                            episodeUuid = episodeUuid,
                            source = source,
                        )
                    }
                    TranscriptState.NoContent
                }

                is Transcript.Web -> {
                    TranscriptState.Loaded(transcript)
                }

                null -> {
                    track { source, podcastUuid, episodeUuid ->
                        TranscriptErrorEvent(
                            podcastUuid = podcastUuid,
                            episodeUuid = episodeUuid,
                            source = source,
                        )
                    }
                    TranscriptState.Failure
                }
            }
            _uiState.update { state -> state.copy(transcriptState = transcriptState) }

            if (transcriptState is TranscriptState.Loaded) {
                trackTranscriptShown(transcriptState.transcript)
            }

            if (transcriptState is TranscriptState.Loaded && transcriptState.transcript is Transcript.Text) {
                val currentPlayingUuid = playbackManager.getCurrentEpisode()?.uuid
                if (currentPlayingUuid == episodeUuid) {
                    fingerprintTimingManager.prepareForCurrentEpisode()
                    _uiState.update { state -> state.copy(syncedState = fingerprintTimingManager.state) }
                    observeSyncedState()
                }
            }
        }
    }

    private var syncedStateJob: Job? = null

    private fun observeSyncedState() {
        syncedStateJob?.cancel()
        syncedStateJob = viewModelScope.launch {
            fingerprintTimingManager.stateFlow.collect { syncedState ->
                _uiState.update { state -> state.copy(syncedState = syncedState) }
            }
        }
    }

    private fun observePlaybackState() {
        viewModelScope.launch {
            playbackManager.playbackStateFlow.collect { playbackState ->
                _uiState.update { state ->
                    state.copy(playingEpisodeUuid = playbackState.episodeUuid.ifEmpty { null })
                }
            }
        }
    }

    fun seekToTranscriptEntry(entry: TranscriptEntry) {
        val textEntry = entry as? TranscriptEntry.Text ?: return
        if (textEntry.startTimeMs < 0) return
        val currentState = _uiState.value
        if (!currentState.isSyncedActive) return

        val refTimeSec = textEntry.startTimeMs / 1000.0
        val seekTimeMs = fingerprintTimingManager.playbackTimeMs(forReferenceTime = refTimeSec) ?: return
        playbackManager.seekToTimeMs(seekTimeMs)
    }

    override fun onCleared() {
        super.onCleared()
        loadTranscriptJob?.cancel()
        syncedStateJob?.cancel()
    }

    fun reloadTranscript() {
        if (loadTranscriptJob?.isActive == true) {
            return
        }

        episodeUuid?.let { uuid ->
            transcriptManager.resetInvalidTranscripts(uuid)
            loadTranscript(uuid)
        }
    }

    fun openSearch() {
        track { source, podcastUuid, episodeUuid ->
            TranscriptSearchShownEvent(
                podcastUuid = podcastUuid,
                episodeUuid = episodeUuid,
                source = source,
            )
        }
        _uiState.update { state ->
            state.copy(searchState = state.searchState.copy(isSearchOpen = true))
        }
    }

    fun hideSearch() {
        viewModelScope.launch {
            searchJob?.cancelAndJoin()
            _uiState.update { state ->
                state.copy(searchState = SearchState.Empty)
            }
        }
    }

    fun searchInTranscript(searchTerm: String) {
        _uiState.update { state ->
            state.copy(searchState = state.searchState.copy(searchTerm = searchTerm))
        }

        val loadedTranscript = uiState.value.transcriptState as? TranscriptState.Loaded ?: return
        val transcript = loadedTranscript.transcript as? Transcript.Text ?: return

        searchJob?.cancel()
        searchJob = viewModelScope.launch(Dispatchers.Default) {
            delay(300)

            val textLines = transcript.entries.map { entry ->
                when (entry) {
                    is TranscriptEntry.Speaker -> entry.name
                    is TranscriptEntry.Text -> entry.value
                }
            }
            val matchingCoordinates = textLines.kmpSearch(searchTerm)
            val firstCooridantes = matchingCoordinates.entries.firstOrNull()?.let { (line, matches) ->
                matches.firstOrNull()?.let { match -> SearchCoordinates(line, match) }
            }
            val searchMatches = SearchMatches(
                selectedCoordinate = firstCooridantes,
                matchingCoordinates = matchingCoordinates,
            )

            _uiState.update { state ->
                val searchState = state.searchState.copy(
                    isSearchOpen = true,
                    matches = searchMatches,
                )
                state.copy(searchState = searchState)
            }
        }
    }

    fun clearSearch() {
        viewModelScope.launch {
            searchJob?.cancelAndJoin()
            _uiState.update { state ->
                val isSearchOpen = state.searchState.isSearchOpen
                state.copy(searchState = SearchState.Empty.copy(isSearchOpen = isSearchOpen))
            }
        }
    }

    fun selectPreviousSearchMatch() {
        track { source, podcastUuid, episodeUuid ->
            TranscriptSearchPreviousResultEvent(
                podcastUuid = podcastUuid,
                episodeUuid = episodeUuid,
                source = source,
            )
        }
        _uiState.update { state ->
            val previousMatches = state.searchState.matches.previous()
            state.copy(searchState = state.searchState.copy(matches = previousMatches))
        }
    }

    fun selectNextSearchMatch() {
        track { source, podcastUuid, episodeUuid ->
            TranscriptSearchNextResultEvent(
                podcastUuid = podcastUuid,
                episodeUuid = episodeUuid,
                source = source,
            )
        }
        _uiState.update { state ->
            val nextMatches = state.searchState.matches.next()
            state.copy(searchState = state.searchState.copy(matches = nextMatches))
        }
    }

    fun shareTranscript() {
        val transcriptState = uiState.value.transcriptState as? TranscriptState.Loaded ?: return
        val transcript = transcriptState.transcript as? Transcript.Text ?: return

        viewModelScope.launch(Dispatchers.Default) {
            val text = transcript.buildString()
            if (text.isBlank()) return@launch

            val episode = episodeManager.findByUuid(transcript.episodeUuid)

            val sourceView = when (source) {
                Source.Episode -> SourceView.EPISODE_DETAILS
                Source.Player -> SourceView.PLAYER
            }

            val request = SharingRequest
                .transcript(
                    podcastUuid = episode?.podcastUuid.orEmpty(),
                    episodeUuid = transcript.episodeUuid,
                    episodeTitle = episode?.title.orEmpty(),
                    transcript = text,
                    source = sourceView,
                )
                .build()

            sharingClient.shareTranscript(request)
        }
    }

    private fun trackTranscriptShown(transcript: Transcript) {
        val isPaywallVisible = !_uiState.value.isPlusUser && transcript.isGenerated
        if (isPaywallVisible) {
            track { source, podcastUuid, episodeUuid ->
                TranscriptGeneratedPaywallShownEvent(
                    podcastUuid = podcastUuid,
                    episodeUuid = episodeUuid,
                    source = source,
                )
            }
        } else {
            track { source, podcastUuid, episodeUuid ->
                TranscriptShownEvent(
                    type = transcript.type.analyticsValue,
                    showAsWebpage = transcript is Transcript.Web,
                    podcastUuid = podcastUuid,
                    episodeUuid = episodeUuid,
                    source = source,
                )
            }
        }
    }

    fun track(event: (TranscriptSourceType, podcastUuid: String, episodeUuid: String) -> Trackable) {
        val podcastUuid = podcastUuid ?: AnalyticsTracker.INVALID_OR_NULL_VALUE
        val episodeUuid = episodeUuid ?: AnalyticsTracker.INVALID_OR_NULL_VALUE
        val event = event(source.analyticsValue, podcastUuid, episodeUuid)
        eventHorizon.track(event)
    }

    private suspend fun updateEpisodeMetadata(episodeUuid: String) {
        if (this.episodeUuid != episodeUuid) {
            this.episodeUuid = episodeUuid
            this.podcastUuid = episodeManager.findByUuid(episodeUuid)?.podcastUuid
        }
    }

    enum class Source(
        val analyticsValue: TranscriptSourceType,
    ) {
        Episode(
            analyticsValue = TranscriptSourceType.Episode,
        ),
        Player(
            analyticsValue = TranscriptSourceType.Player,
        ),
    }

    @AssistedFactory
    interface Factory {
        fun create(source: Source): TranscriptViewModel
    }
}

data class UiState(
    val transcriptState: TranscriptState,
    val searchState: SearchState,
    val isPlusUser: Boolean,
    val isFreeTrialAvailable: Boolean,
    val syncedState: FingerprintTimingManager.State = FingerprintTimingManager.State.Idle,
    val playingEpisodeUuid: String? = null,
) {
    val isPaywallVisible get() = !isPlusUser && (transcriptState as? TranscriptState.Loaded)?.transcript?.isGenerated == true

    val isTextTranscriptLoaded get() = (transcriptState as? TranscriptState.Loaded)?.transcript is Transcript.Text

    val transcriptEpisodeUuid get() = (transcriptState as? TranscriptState.Loaded)?.transcript?.episodeUuid

    val isSyncedActive get() = syncedState is FingerprintTimingManager.State.Active &&
        transcriptEpisodeUuid != null &&
        transcriptEpisodeUuid == playingEpisodeUuid

    companion object {
        val Empty = UiState(
            transcriptState = TranscriptState.Loading,
            searchState = SearchState.Empty,
            isPlusUser = false,
            isFreeTrialAvailable = false,
        )
    }
}

sealed interface TranscriptState {
    data object Loading : TranscriptState

    data class Loaded(
        val transcript: Transcript,
    ) : TranscriptState

    data object NoContent : TranscriptState

    data object Failure : TranscriptState
}

data class SearchState(
    val isSearchOpen: Boolean,
    val searchTerm: String,
    val matches: SearchMatches,
) {
    companion object {
        val Empty = SearchState(
            isSearchOpen = false,
            searchTerm = "",
            matches = SearchMatches(
                selectedCoordinate = null,
                matchingCoordinates = emptyMap(),
            ),
        )
    }
}
