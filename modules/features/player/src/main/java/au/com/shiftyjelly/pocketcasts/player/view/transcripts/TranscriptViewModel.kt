package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.annotation.OptIn
import androidx.annotation.VisibleForTesting
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
                    val result = buildSubtitleCues(transcript, forceRefresh)
                    UiState.TranscriptLoaded(
                        transcript = transcript,
                        podcastAndEpisode = podcastAndEpisode,
                        cuesWithTimingSubtitle = result,
                    )
                } catch (e: UnsupportedOperationException) {
                    UiState.Error(TranscriptError.NotSupported(transcript.type), transcript, podcastAndEpisode)
                } catch (e: NoNetworkException) {
                    UiState.Error(TranscriptError.NoNetwork, transcript, podcastAndEpisode)
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
                    val newText = TranscriptRegexFilters.htmlFilters.filter(content)
                    // Html content is added as single large cue
                    ImmutableList.of(
                        CuesWithTiming(
                            ImmutableList.of(Cue.Builder().setText(newText).build()),
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
                        val parser = subtitleParserFactory.create(format)
                        parser.parse(
                            data,
                            SubtitleParser.OutputOptions.allCues(),
                        ) { element: CuesWithTiming? ->
                            element?.let {
                                result.add(
                                    CuesWithTiming(
                                        modifiedCues(it),
                                        it.startTimeUs,
                                        it.endTimeUs,
                                    ),
                                )
                            }
                        }
                    }
                    result.build()
                }
            }
        }
    }

    private fun clearErrorsIfFound(transcript: Transcript) {
        if (_uiState.value is UiState.Error) {
            _uiState.value = UiState.TranscriptFound(
                podcastAndEpisode = _uiState.value.podcastAndEpisode,
                transcript = transcript,
            )
        }
    }

    /**
     * Modifies the cues in the given [CuesWithTiming] object.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun modifiedCues(cuesWithTiming: CuesWithTiming) =
        cuesWithTiming.cues.map { cue ->
            val cueBuilder = cue.buildUpon()
            val newText = TranscriptRegexFilters.transcriptFilters.filter(cue.text.toString())
            cueBuilder.setText(newText)
            cueBuilder.build()
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

    sealed class TranscriptError {
        data class NotSupported(val format: String) : TranscriptError()
        data object FailedToLoad : TranscriptError()
        data object NoNetwork : TranscriptError()
    }
}
