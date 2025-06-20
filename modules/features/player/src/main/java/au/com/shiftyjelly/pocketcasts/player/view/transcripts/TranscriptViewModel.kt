package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@OptIn(UnstableApi::class)
@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val transcriptManager: TranscriptManager,
    private val playbackManager: PlaybackManager,
    private val analyticsTracker: AnalyticsTracker,
    private val settings: Settings,
) : ViewModel() {
    private var _uiState = MutableStateFlow<UiState>(UiState.Empty)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playbackManager.playbackStateFlow
                .distinctUntilChangedBy { it.episodeUuid }
                .collectLatest { playbackState ->
                    reloadJob?.cancelAndJoin()
                    loadTranscripts(episodeUuid = playbackState.episodeUuid)
                }
        }

        viewModelScope.launch {
            settings.cachedSubscription.flow.collect { subscription ->
                _uiState.update { value -> value.copy(subscriptionTier = subscription?.tier) }
            }
        }
    }

    private var reloadJob: Job? = null

    fun reloadTranscripts() {
        if (reloadJob?.isActive == true) {
            return
        }
        reloadJob = viewModelScope.launch {
            val episodeUuid = playbackManager.getCurrentEpisode()?.uuid
            if (episodeUuid != null) {
                transcriptManager.resetInvalidTranscripts(episodeUuid)
                loadTranscripts(episodeUuid)
            }
        }
    }

    private suspend fun loadTranscripts(episodeUuid: String) {
        _uiState.update { value -> value.copy(transcriptState = TranscriptState.Loading) }
        val transcript = transcriptManager.loadTranscript(episodeUuid)
        val state = if (transcript == null) {
            track(AnalyticsEvent.TRANSCRIPT_ERROR)
            TranscriptState.Failure(TranscriptError.FailedToLoad)
        } else {
            withContext(Dispatchers.Default) {
                when (transcript) {
                    is Transcript.Text -> if (transcript.entries.isEmpty()) {
                        TranscriptState.Failure(TranscriptError.Empty)
                    } else {
                        TranscriptState.Loaded(transcript)
                    }

                    is Transcript.Web -> TranscriptState.Loaded(transcript)
                }
            }
        }
        _uiState.update { value -> value.copy(transcriptState = state) }
    }

    fun track(
        event: AnalyticsEvent,
        analyticsProp: Map<String, String> = emptyMap(),
    ) {
        val loadedState = (_uiState.value.transcriptState as? TranscriptState.Loaded)

        analyticsTracker.track(
            event,
            analyticsProp
                .plus("episode_uuid" to loadedState?.transcript?.episodeUuid.orEmpty())
                .plus("podcast_uuid" to loadedState?.transcript?.podcastUuid.orEmpty()),
        )
    }

    data class UiState(
        val subscriptionTier: SubscriptionTier?,
        val transcriptState: TranscriptState,
    ) {
        private val isSubscriptionRequired = when (transcriptState) {
            is TranscriptState.Loaded -> transcriptState.transcript.isGenerated && subscriptionTier == null
            is TranscriptState.Loading, is TranscriptState.Failure -> false
        }

        val showPaywall = (transcriptState as? TranscriptState.Loaded)?.isTranscriptEmpty == false && isSubscriptionRequired

        val showSearch = (transcriptState as? TranscriptState.Loaded)?.showAsWebPage == false && !isSubscriptionRequired

        companion object {
            val Empty = UiState(
                subscriptionTier = null,
                transcriptState = TranscriptState.Loading,
            )
        }
    }

    sealed interface TranscriptState {
        data object Loading : TranscriptState

        data class Loaded(
            val transcript: Transcript,
        ) : TranscriptState {
            val displayInfo = transcript.toDisplayInfo()

            val isTranscriptEmpty
                get() = when (transcript) {
                    is Transcript.Text -> transcript.entries.isEmpty()
                    is Transcript.Web -> false
                }

            val showAsWebPage get() = transcript is Transcript.Web
        }

        data class Failure(
            val error: TranscriptError,
        ) : TranscriptState
    }

    data class DisplayInfo(
        val text: String,
        val items: List<DisplayItem> = emptyList(),
    )

    data class DisplayItem(
        val text: String,
        val isSpeaker: Boolean = false,
        val startIndex: Int,
        val endIndex: Int,
    )

    enum class TranscriptError {
        Empty,
        FailedToLoad,
    }
}

private fun Transcript.toDisplayInfo(): TranscriptViewModel.DisplayInfo {
    return when (this) {
        is Transcript.Text -> {
            var accumulatedTextLength = 0
            TranscriptViewModel.DisplayInfo(
                text = entries.fold(StringBuilder()) { acc, entry ->
                    val entryText = when (entry) {
                        is TranscriptEntry.Speaker -> entry.name
                        is TranscriptEntry.Text -> entry.value
                    }
                    acc.append(entryText)
                }.toString(),
                items = entries.map { entry ->
                    val entryText = when (entry) {
                        is TranscriptEntry.Speaker -> entry.name
                        is TranscriptEntry.Text -> entry.value
                    }
                    TranscriptViewModel.DisplayItem(
                        text = entryText,
                        isSpeaker = entry is TranscriptEntry.Speaker,
                        startIndex = accumulatedTextLength,
                        endIndex = accumulatedTextLength + entryText.length,
                    ).also { accumulatedTextLength += entryText.length }
                },
            )
        }

        is Transcript.Web -> TranscriptViewModel.DisplayInfo(
            text = "",
            items = emptyList(),
        )
    }
}
