package au.com.shiftyjelly.pocketcasts.transcripts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionOffer
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.payment.getOrNull
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.utils.search.SearchCoordinates
import au.com.shiftyjelly.pocketcasts.utils.search.SearchMatches
import au.com.shiftyjelly.pocketcasts.utils.search.kmpSearch
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
    private val analyticsTracker: AnalyticsTracker,
    private val sharingClient: TranscriptSharingClient,
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
    }

    private var episodeUuid: String? = null
    private var podcastUuid: String? = null

    private var loadTranscriptJob: Job? = null
    private var searchJob: Job? = null

    fun loadTranscript(episodeUuid: String) {
        loadTranscriptJob?.cancel()
        loadTranscriptJob = viewModelScope.launch {
            searchJob?.cancelAndJoin()

            _uiState.update { state ->
                state.copy(
                    transcriptState = TranscriptState.Loading,
                    searchState = SearchState.Empty,
                )
            }

            updateEpisodeMetadata(episodeUuid)
            val transcriptState = when (val transcript = transcriptManager.loadTranscript(episodeUuid)) {
                is Transcript.Text -> if (transcript.entries.isNotEmpty()) {
                    TranscriptState.Loaded(transcript)
                } else {
                    track(AnalyticsEvent.TRANSCRIPT_ERROR)
                    TranscriptState.NoContent
                }

                is Transcript.Web -> {
                    TranscriptState.Loaded(transcript)
                }

                null -> {
                    track(AnalyticsEvent.TRANSCRIPT_ERROR)
                    TranscriptState.Failure
                }
            }
            _uiState.update { state -> state.copy(transcriptState = transcriptState) }
        }
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
        track(AnalyticsEvent.TRANSCRIPT_SEARCH_SHOWN)
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
        track(AnalyticsEvent.TRANSCRIPT_SEARCH_PREVIOUS_RESULT)
        _uiState.update { state ->
            val previousMatches = state.searchState.matches.previous()
            state.copy(searchState = state.searchState.copy(matches = previousMatches))
        }
    }

    fun selectNextSearchMatch() {
        track(AnalyticsEvent.TRANSCRIPT_SEARCH_NEXT_RESULT)
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
                    podcastUuid = episode?.podcastUuid,
                    episodeUuid = transcript.episodeUuid,
                    episodeTitle = episode?.title.orEmpty(),
                    transcript = text,
                )
                .setSourceView(sourceView)
                .build()

            sharingClient.shareTranscript(request)
        }
    }

    fun track(
        event: AnalyticsEvent,
        additionalProperties: Map<String, Any> = emptyMap(),
    ) {
        analyticsTracker.track(
            event,
            buildMap {
                putAll(additionalProperties)
                put("source", source.analyticsValue)
                episodeUuid?.let { uuid -> put("episode_uuid", uuid) }
                podcastUuid?.let { uuid -> put("podcast_uuid", uuid) }
            },
        )
    }

    private suspend fun updateEpisodeMetadata(episodeUuid: String) {
        if (this.episodeUuid != episodeUuid) {
            this.episodeUuid = episodeUuid
            this.podcastUuid = episodeManager.findByUuid(episodeUuid)?.podcastUuid
        }
    }

    enum class Source(
        val analyticsValue: String,
    ) {
        Episode(
            analyticsValue = "episode",
        ),
        Player(
            analyticsValue = "player",
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
) {
    val isPaywallVisible get() = !isPlusUser && (transcriptState as? TranscriptState.Loaded)?.transcript?.isGenerated == true

    val isTextTranscriptLoaded get() = (transcriptState as? TranscriptState.Loaded)?.transcript is Transcript.Text

    val transcriptEpisodeUuid get() = (transcriptState as? TranscriptState.Loaded)?.transcript?.episodeUuid

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
