package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Format
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptFormat
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptsManager
import au.com.shiftyjelly.pocketcasts.utils.exception.NoNetworkException
import au.com.shiftyjelly.pocketcasts.utils.extensions.splitIgnoreEmpty
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.common.collect.ImmutableList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.toImmutableList

@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@OptIn(UnstableApi::class)
@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val transcriptsManager: TranscriptsManager,
    private val playbackManager: PlaybackManager,
    private val subtitleParserFactory: SubtitleParser.Factory,
    private val analyticsTracker: AnalyticsTracker,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val transcriptJsonParser: TranscriptJsonParser,
) : ViewModel() {
    private var _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Empty())
    val uiState: StateFlow<UiState> = _uiState
    private var _isRefreshing: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    init {
        viewModelScope.launch {
            playbackManager.playbackStateFlow
                .map { PodcastAndEpisode(it.podcast, it.episodeUuid) }
                .distinctUntilChanged { t1, t2 -> t1.episodeUuid == t2.episodeUuid }
                .stateIn(viewModelScope)
                .flatMapLatest(::transcriptFlow)
                .collect { _uiState.value = it }
        }
    }

    private fun transcriptFlow(podcastAndEpisode: PodcastAndEpisode) =
        transcriptsManager.observerTranscriptForEpisode(podcastAndEpisode.episodeUuid)
            .distinctUntilChanged { t1, t2 -> t1?.episodeUuid == t2?.episodeUuid }
            .map { transcript ->
                transcript?.let {
                    UiState.TranscriptFound(podcastAndEpisode, transcript)
                } ?: UiState.Empty(podcastAndEpisode)
            }

    fun parseAndLoadTranscript(
        isTranscriptViewOpen: Boolean,
        pulledToRefresh: Boolean = false,
        retryOnFail: Boolean = false,
    ) {
        if (isTranscriptViewOpen.not()) return
        val podcastAndEpisode = _uiState.value.podcastAndEpisode
        if (pulledToRefresh) {
            _isRefreshing.value = true
            podcastAndEpisode?.let { track(AnalyticsEvent.TRANSCRIPT_PULLED_TO_REFRESH, it) }
        }
        _uiState.value.transcript?.let { transcript ->
            clearErrorsIfFound(transcript)
            viewModelScope.launch {
                _uiState.value = try {
                    val forceRefresh = pulledToRefresh || retryOnFail
                    val cuesInfo = buildSubtitleCues(transcript, forceRefresh)

                    val displayInfo = buildDisplayInfo(
                        cuesInfo = cuesInfo,
                        transcriptFormat = TranscriptFormat.fromType(transcript.type),
                    )

                    if (!pulledToRefresh) {
                        podcastAndEpisode?.let { track(AnalyticsEvent.TRANSCRIPT_SHOWN, it) }
                    }

                    UiState.TranscriptLoaded(
                        transcript = transcript,
                        podcastAndEpisode = podcastAndEpisode,
                        displayInfo = displayInfo,
                        cuesInfo = cuesInfo,
                    )
                } catch (e: Exception) {
                    track(AnalyticsEvent.TRANSCRIPT_ERROR, podcastAndEpisode, mapOf("error" to e.message.orEmpty()))
                    when (e) {
                        is UnsupportedOperationException ->
                            UiState.Error(TranscriptError.NotSupported(transcript.type), transcript, podcastAndEpisode)

                        is NoNetworkException ->
                            UiState.Error(TranscriptError.NoNetwork, transcript, podcastAndEpisode)

                        is TranscriptParsingException ->
                            UiState.Error(TranscriptError.FailedToParse, transcript, podcastAndEpisode)

                        else -> {
                            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, e, "Failed to load transcript: ${transcript.url}")
                            UiState.Error(TranscriptError.FailedToLoad, transcript, podcastAndEpisode)
                        }
                    }
                }
                if (pulledToRefresh) _isRefreshing.value = false
            }
        }
    }

    private suspend fun buildSubtitleCues(transcript: Transcript, forceRefresh: Boolean) = withContext(ioDispatcher) {
        when (transcript.type) {
            TranscriptFormat.HTML.mimeType -> {
                val content = transcriptsManager.loadTranscript(transcript.url, forceRefresh = forceRefresh)?.string() ?: ""
                if (content.trim().isEmpty()) {
                    emptyList()
                } else {
                    // Html content is added as single large cue
                    ImmutableList.of(
                        CuesWithTiming(
                            ImmutableList.of(Cue.Builder().setText(content).build()),
                            0,
                            0,
                        ).toTranscriptCuesInfo(),
                    )
                }
            }

            TranscriptFormat.JSON_PODCAST_INDEX.mimeType -> {
                val jsonString = transcriptsManager.loadTranscript(transcript.url, forceRefresh = forceRefresh)?.string() ?: ""
                if (jsonString.trim().isEmpty()) {
                    emptyList()
                } else {
                    // Parse json following PodcastIndex.org transcript json spec: https://github.com/Podcastindex-org/podcast-namespace/blob/main/transcripts/transcripts.md#json
                    val transcriptCues = transcriptJsonParser.parse(jsonString)
                    transcriptCues.map { cue ->
                        val startTimeUs = cue.startTime?.toMicroSeconds ?: 0
                        val endTimeUs = cue.endTime?.toMicroSeconds ?: 0
                        CuesWithTiming(
                            ImmutableList.of(Cue.Builder().setText(cue.body ?: "").build()),
                            startTimeUs,
                            endTimeUs - startTimeUs,
                        ).toTranscriptCuesInfo(
                            cuesAdditionalInfo = CuesAdditionalInfo(speaker = cue.speaker),
                        )
                    }.toImmutableList()
                }
            }

            else -> {
                val format = Format.Builder()
                    .setSampleMimeType(transcript.type)
                    .build()
                if (subtitleParserFactory.supportsFormat(format).not()) {
                    throw UnsupportedOperationException("Unsupported MIME type: ${transcript.type}")
                } else {
                    val result = ImmutableList.builder<CuesWithTiming>()
                    transcriptsManager.loadTranscript(transcript.url, forceRefresh = forceRefresh)?.bytes()?.let { data ->
                        try {
                            val parser = subtitleParserFactory.create(format)
                            parser.parse(
                                data,
                                SubtitleParser.OutputOptions.allCues(),
                            ) { element: CuesWithTiming? ->
                                element?.let { result.add(it) }
                            }
                        } catch (e: Exception) {
                            val message = "Failed to parse transcript: ${transcript.url}"
                            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, e, message)
                            throw TranscriptParsingException(message)
                        }
                    }
                    result.build().map { it.toTranscriptCuesInfo() }
                }
            }
        }
    }

    private suspend fun buildDisplayInfo(
        cuesInfo: List<TranscriptCuesInfo>,
        transcriptFormat: TranscriptFormat?,
    ) = withContext(ioDispatcher) {
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
            items.add(DisplayItem(currentItem, isSpeaker, currentItemStartIndex, currentItemStartIndex + currentItem.length))
        }
        DisplayInfo(
            text = formattedText,
            items = items,
        )
    }

    private fun clearErrorsIfFound(transcript: Transcript) {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.TranscriptFound(
                podcastAndEpisode = _uiState.value.podcastAndEpisode,
                transcript = transcript,
            )
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

    private val Double.toMicroSeconds: Long
        get() = toDuration(DurationUnit.SECONDS).inWholeMicroseconds

    data class PodcastAndEpisode(
        val podcast: Podcast?,
        val episodeUuid: String,
    )

    sealed class UiState {
        open val transcript: Transcript? = null
        open val podcastAndEpisode: PodcastAndEpisode? = null

        data class Empty(
            override val podcastAndEpisode: PodcastAndEpisode? = null,
        ) : UiState()

        data class TranscriptFound(
            override val podcastAndEpisode: PodcastAndEpisode? = null,
            override val transcript: Transcript,
        ) : UiState()

        data class TranscriptLoaded(
            override val podcastAndEpisode: PodcastAndEpisode? = null,
            override val transcript: Transcript,
            val displayInfo: DisplayInfo,
            val cuesInfo: List<TranscriptCuesInfo>,
        ) : UiState() {
            val isTranscriptEmpty: Boolean = cuesInfo.isEmpty()
        }

        data class Error(
            val error: TranscriptError,
            override val transcript: Transcript,
            override val podcastAndEpisode: PodcastAndEpisode? = null,
        ) : UiState()
    }

    data class TranscriptCuesInfo(
        val cuesWithTiming: CuesWithTiming,
        val cuesAdditionalInfo: CuesAdditionalInfo? = null,
    )

    private fun CuesWithTiming.toTranscriptCuesInfo(
        cuesAdditionalInfo: CuesAdditionalInfo? = null,
    ) = TranscriptCuesInfo(this, cuesAdditionalInfo)

    data class CuesAdditionalInfo(
        val speaker: String?,
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
    }

    class TranscriptParsingException(message: String) : Exception(message)
}
