package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptCuesInfo
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptFormat
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptsManager
import au.com.shiftyjelly.pocketcasts.utils.exception.EmptyDataException
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.exception.ParsingException
import au.com.shiftyjelly.pocketcasts.utils.extensions.splitIgnoreEmpty
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@OptIn(UnstableApi::class)
@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val transcriptsManager: TranscriptsManager,
    private val playbackManager: PlaybackManager,
    private val analyticsTracker: AnalyticsTracker,
    private val settings: Settings,
) : ViewModel() {
    private var _uiState = MutableStateFlow<UiState>(UiState.Empty)
    val uiState = _uiState.asStateFlow()
    private var _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            playbackManager.playbackStateFlow
                .distinctUntilChangedBy { it.episodeUuid }
                .flatMapLatest { playbackState ->
                    transcriptsManager
                        .observeTranscriptForEpisode(playbackState.episodeUuid)
                        .map { transcript -> transcript to playbackState }
                }
                .distinctUntilChangedBy { (transcript, _) -> transcript?.url }
                .collect { (transcript, playbackState) ->
                    val state = if (transcript != null) {
                        TranscriptState.Found(transcript)
                    } else {
                        TranscriptState.Empty
                    }
                    _uiState.update { value ->
                        value.copy(
                            podcastAndEpisode = PodcastAndEpisode(playbackState.podcast, playbackState.episodeUuid),
                            transcriptState = state,
                        )
                    }
                }
        }

        viewModelScope.launch {
            settings.cachedSubscription.flow.collect { subscription ->
                _uiState.update { value -> value.copy(subscriptionTier = subscription?.tier) }
            }
        }
    }

    fun parseAndLoadTranscript(
        pulledToRefresh: Boolean = false,
        retryOnFail: Boolean = false,
    ) {
        val podcastAndEpisode = _uiState.value.podcastAndEpisode
        if (pulledToRefresh) {
            _isRefreshing.value = true
            podcastAndEpisode?.let { track(AnalyticsEvent.TRANSCRIPT_PULLED_TO_REFRESH, it) }
        }
        _uiState.value.transcriptState.transcript?.let { transcript ->
            clearErrorsIfFound(transcript)

            viewModelScope.launch {
                val newTranscriptState = try {
                    val forceRefresh = pulledToRefresh || retryOnFail
                    val cuesInfo = transcriptsManager.loadTranscriptCuesInfo(
                        podcastUuid = podcastAndEpisode?.podcast?.uuid.orEmpty(),
                        transcript = transcript,
                        forceRefresh = forceRefresh,
                    )

                    val displayInfo = buildDisplayInfo(
                        cuesInfo = cuesInfo,
                        transcriptFormat = TranscriptFormat.fromType(transcript.type),
                    )

                    val loaded = TranscriptState.Loaded(
                        transcript = transcript,
                        displayInfo = displayInfo,
                        cuesInfo = cuesInfo,
                    )

                    loaded
                } catch (e: Exception) {
                    track(AnalyticsEvent.TRANSCRIPT_ERROR, podcastAndEpisode, mapOf("error" to e.message.orEmpty()))
                    when (e) {
                        is EmptyDataException ->
                            TranscriptState.Error(TranscriptError.Empty, transcript)

                        is UnsupportedOperationException ->
                            TranscriptState.Error(TranscriptError.NotSupported(transcript.type), transcript)

                        is NoNetworkException ->
                            TranscriptState.Error(TranscriptError.NoNetwork, transcript)

                        is ParsingException ->
                            TranscriptState.Error(TranscriptError.FailedToParse, transcript)

                        else -> {
                            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, e, "Failed to load transcript: ${transcript.url}")
                            TranscriptState.Error(TranscriptError.FailedToLoad, transcript)
                        }
                    }
                }
                _uiState.update { value -> value.copy(transcriptState = newTranscriptState) }

                val uiState = _uiState.value
                if (!pulledToRefresh && !uiState.showPaywall && uiState.transcriptState is TranscriptState.Loaded) {
                    podcastAndEpisode?.let {
                        track(
                            event = AnalyticsEvent.TRANSCRIPT_SHOWN,
                            podcastAndEpisode = it,
                            analyticsProp = buildMap {
                                put("type", transcript.type)
                                put("show_as_webpage", uiState.transcriptState.showAsWebPage.toString())
                            },
                        )
                    }
                }

                if (pulledToRefresh) {
                    _isRefreshing.value = false
                }
            }
        }
    }

    private suspend fun buildDisplayInfo(
        cuesInfo: List<TranscriptCuesInfo>,
        transcriptFormat: TranscriptFormat?,
    ) = withContext(Dispatchers.Default) {
        var previousSpeaker = ""
        val speakerIndices = mutableListOf<Int>()
        val formattedText = buildString {
            cuesInfo.forEach {
                it.cuesWithTiming.cues.forEach { cue ->
                    // Extract speaker
                    cue.text?.let { cueText ->
                        val speaker = it.cuesAdditionalInfo?.speaker ?: TranscriptRegexFilters.extractSpeaker(cueText.toString(), transcriptFormat)
                        speaker?.let {
                            if (previousSpeaker != speaker) {
                                append("\n\n")
                                append(speaker)
                                append("\n\n")
                                speakerIndices.add(length - speaker.length - 2)
                                previousSpeaker = speaker
                            }
                        }
                        val filters = if (transcriptFormat == TranscriptFormat.HTML) {
                            TranscriptRegexFilters.htmlFilters
                        } else {
                            TranscriptRegexFilters.transcriptFilters
                        }
                        val newText = filters.filter(cueText.toString())
                        append(newText)
                    }
                }
            }
        }
        val items = mutableListOf<DisplayItem>()
        formattedText.splitIgnoreEmpty("\n\n").filter { it.isNotEmpty() }.mapIndexed { index, currentItem ->
            val previousItemEndIndex = if (index == 0) 0 else items[index - 1].endIndex
            val currentItemStartIndex = formattedText.indexOf(currentItem, previousItemEndIndex)
            val isSpeaker = currentItemStartIndex in speakerIndices
            // As we are removing new lines, add a space after each item so copying the text doesn't merge items together.
            val text = "$currentItem "
            items.add(DisplayItem(text, isSpeaker, currentItemStartIndex, currentItemStartIndex + currentItem.length))
        }
        DisplayInfo(
            text = formattedText,
            items = items,
        )
    }

    private fun clearErrorsIfFound(transcript: Transcript) {
        if (_uiState.value.transcriptState is TranscriptState.Error) {
            _uiState.update { value ->
                value.copy(
                    transcriptState = TranscriptState.Found(transcript = transcript),
                )
            }
        }
    }

    fun track(
        event: AnalyticsEvent,
        podcastAndEpisode: PodcastAndEpisode?,
        analyticsProp: Map<String, String> = emptyMap(),
    ) {
        analyticsTracker.track(
            event,
            analyticsProp
                .plus("episode_uuid" to podcastAndEpisode?.episodeUuid.orEmpty())
                .plus("podcast_uuid" to podcastAndEpisode?.podcast?.uuid.orEmpty()),
        )
    }

    data class UiState(
        val subscriptionTier: SubscriptionTier?,
        val podcastAndEpisode: PodcastAndEpisode?,
        val transcriptState: TranscriptState,
    ) {
        private val isSubscriptionRequired = if (transcriptState.transcript?.isGenerated == true) {
            subscriptionTier == null
        } else {
            false
        }

        val showPaywall = (transcriptState as? TranscriptState.Loaded)?.isTranscriptEmpty == false && isSubscriptionRequired

        val showSearch = (transcriptState as? TranscriptState.Loaded)?.showAsWebPage == false && !isSubscriptionRequired

        companion object {
            val Empty = UiState(
                subscriptionTier = null,
                podcastAndEpisode = null,
                transcriptState = TranscriptState.Empty,
            )
        }
    }

    sealed interface TranscriptState {
        val transcript: Transcript?

        data object Empty : TranscriptState {
            override val transcript get() = null
        }

        data class Found(
            override val transcript: Transcript,
        ) : TranscriptState

        data class Loaded(
            override val transcript: Transcript,
            val displayInfo: DisplayInfo,
            val cuesInfo: List<TranscriptCuesInfo>,
        ) : TranscriptState {
            val isTranscriptEmpty: Boolean = cuesInfo.isEmpty()

            val showAsWebPage: Boolean
                get() = transcript.type == TranscriptFormat.HTML.mimeType &&
                    cuesInfo.isNotEmpty() && cuesInfo[0].cuesWithTiming.cues.any {
                        it.text?.contains("<script type=\"text/javascript\">") ?: false
                    }
        }

        data class Error(
            val error: TranscriptError,
            override val transcript: Transcript,
        ) : TranscriptState
    }

    data class PodcastAndEpisode(
        val podcast: Podcast?,
        val episodeUuid: String,
    )

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

    sealed class TranscriptError {
        data class NotSupported(val format: String) : TranscriptError()
        data object FailedToLoad : TranscriptError()
        data object NoNetwork : TranscriptError()
        data object FailedToParse : TranscriptError()
        data object Empty : TranscriptError()
    }
}
