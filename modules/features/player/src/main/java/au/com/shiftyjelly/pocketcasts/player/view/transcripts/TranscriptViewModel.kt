package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Format
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
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

@kotlin.OptIn(ExperimentalCoroutinesApi::class)
@OptIn(UnstableApi::class)
@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val transcriptsManager: TranscriptsManager,
    private val playbackManager: PlaybackManager,
    private val subtitleParserFactory: SubtitleParser.Factory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
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

    fun parseAndLoadTranscript(isTranscriptViewOpen: Boolean, forceRefresh: Boolean = false) {
        if (isTranscriptViewOpen.not()) return
        if (forceRefresh) _isRefreshing.value = true
        _uiState.value.transcript?.let { transcript ->
            clearErrorsIfFound(transcript)
            val podcastAndEpisode = _uiState.value.podcastAndEpisode
            viewModelScope.launch {
                _uiState.value = try {
                    val cuesWithTimingSubtitle = buildSubtitleCues(transcript, forceRefresh)

                    val displayInfo = buildDisplayInfo(
                        cuesWithTimingSubtitle = cuesWithTimingSubtitle,
                        transcriptFormat = TranscriptFormat.fromType(transcript.type),
                    )

                    UiState.TranscriptLoaded(
                        transcript = transcript,
                        podcastAndEpisode = podcastAndEpisode,
                        displayInfo = displayInfo,
                        cuesWithTimingSubtitle = cuesWithTimingSubtitle,
                    )
                } catch (e: UnsupportedOperationException) {
                    UiState.Error(TranscriptError.NotSupported(transcript.type), transcript, podcastAndEpisode)
                } catch (e: NoNetworkException) {
                    UiState.Error(TranscriptError.NoNetwork, transcript, podcastAndEpisode)
                } catch (e: TranscriptParsingException) {
                    UiState.Error(TranscriptError.FailedToParse, transcript, podcastAndEpisode)
                } catch (e: Exception) {
                    UiState.Error(TranscriptError.FailedToLoad, transcript, podcastAndEpisode)
                }
                if (forceRefresh) _isRefreshing.value = false
            }
        }
    }

    private suspend fun buildSubtitleCues(transcript: Transcript, forceRefresh: Boolean) = withContext(ioDispatcher) {
        when (transcript.type) {
            TranscriptFormat.HTML.mimeType -> {
                val content = transcriptsManager.loadTranscript(transcript.url, forceRefresh = forceRefresh)?.string() ?: ""
                if (content.trim().isEmpty()) {
                    emptyList<CuesWithTiming>()
                } else {
                    // Html content is added as single large cue
                    ImmutableList.of(
                        CuesWithTiming(
                            ImmutableList.of(Cue.Builder().setText(content).build()),
                            0,
                            0,
                        ),
                    )
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
                    result.build()
                }
            }
        }
    }

    private suspend fun buildDisplayInfo(
        cuesWithTimingSubtitle: List<CuesWithTiming>,
        transcriptFormat: TranscriptFormat?,
    ) = withContext(ioDispatcher) {
        var previousSpeaker = ""
        val speakerIndices = mutableListOf<Int>()
        val formattedText = buildString {
            cuesWithTimingSubtitle
                .flatMap { it.cues }
                .forEach { cue ->
                    // Extract speaker
                    val markupText =  if (transcriptFormat == TranscriptFormat.VTT) cue.markup else cue.text
                    markupText?.let {
                        TranscriptRegexFilters.extractSpeaker(it.toString(), transcriptFormat)?.let { speaker ->
                            if (previousSpeaker != speaker) {
                                append("\n\n$speaker\n\n")
                                speakerIndices.add(length - speaker.length - 2)
                                previousSpeaker = speaker
                            }
                        }
                    }
                    val filters = if (transcriptFormat == TranscriptFormat.HTML) {
                        TranscriptRegexFilters.htmlFilters
                    } else {
                        TranscriptRegexFilters.transcriptFilters
                    }
                    val newText = filters.filter(cue.text.toString())
                    append(newText)
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
            val cuesWithTimingSubtitle: List<CuesWithTiming>,
        ) : UiState() {
            val isTranscriptEmpty: Boolean = cuesWithTimingSubtitle.isEmpty()
        }

        data class Error(
            val error: TranscriptError,
            override val transcript: Transcript,
            override val podcastAndEpisode: PodcastAndEpisode? = null,
        ) : UiState()
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

    sealed class TranscriptError {
        data class NotSupported(val format: String) : TranscriptError()
        data object FailedToLoad : TranscriptError()
        data object NoNetwork : TranscriptError()
        data object FailedToParse : TranscriptError()
    }

    class TranscriptParsingException(message: String) : Exception(message)
}
