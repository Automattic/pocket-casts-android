package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.CuesWithTimingSubtitle
import androidx.media3.extractor.text.SubtitleParser
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptsManager
import au.com.shiftyjelly.pocketcasts.utils.UrlUtil
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
    private val urlUtil: UrlUtil,
    private val subtitleParserFactory: SubtitleParser.Factory,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private var _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Empty())
    val uiState: StateFlow<UiState> = _uiState

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
            .map { transcript ->
                transcript?.let {
                    UiState.TranscriptFound(podcastAndEpisode, transcript)
                } ?: UiState.Empty(podcastAndEpisode)
            }

    fun parseAndLoadTranscript() {
        _uiState.value.transcript?.let { transcript ->
            viewModelScope.launch {
                val podcastAndEpisode = _uiState.value.podcastAndEpisode
                _uiState.value = try {
                    val result = parseTranscript(transcript)
                    UiState.TranscriptLoaded(
                        transcript = transcript,
                        podcastAndEpisode = podcastAndEpisode,
                        cuesWithTimingSubtitle = result,
                    )
                } catch (e: UnsupportedOperationException) {
                    UiState.Error(TranscriptError.NotSupported(transcript.type), podcastAndEpisode)
                } catch (e: Exception) {
                    UiState.Error(TranscriptError.FailedToLoad, podcastAndEpisode)
                }
            }
        }
    }

    private suspend fun parseTranscript(transcript: Transcript) = withContext(ioDispatcher) {
        val format = Format.Builder()
            .setSampleMimeType(transcript.type)
            .build()
        if (subtitleParserFactory.supportsFormat(format).not()) {
            throw UnsupportedOperationException("Unsupported MIME type: ${transcript.type}")
        } else {
            val result = ImmutableList.builder<CuesWithTiming>()
            urlUtil.contentBytes(transcript.url)?.let { data ->
                val parser = subtitleParserFactory.create(format)
                parser.parse(
                    data,
                    SubtitleParser.OutputOptions.allCues(),
                ) { element: CuesWithTiming? ->
                    element?.let { result.add(it) }
                }
            }
            CuesWithTimingSubtitle(result.build())
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
            val cuesWithTimingSubtitle: CuesWithTimingSubtitle,
        ) : UiState()

        data class Error(
            val error: TranscriptError,
            override val podcastAndEpisode: PodcastAndEpisode? = null,
        ) : UiState()
    }

    sealed class TranscriptError {
        data class NotSupported(val format: String) : TranscriptError()
        data object FailedToLoad : TranscriptError()
    }
}
