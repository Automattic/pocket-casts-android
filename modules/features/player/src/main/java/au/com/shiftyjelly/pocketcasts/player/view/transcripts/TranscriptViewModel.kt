package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptsManager
import au.com.shiftyjelly.pocketcasts.utils.UrlUtil
import com.google.common.collect.ImmutableList
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@OptIn(UnstableApi::class)
@HiltViewModel
class TranscriptViewModel @Inject constructor(
    private val transcriptsManager: TranscriptsManager,
    playbackManager: PlaybackManager,
    private val urlUtil: UrlUtil,
    private val subtitleParserFactory: SubtitleParser.Factory,
) : ViewModel() {

    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> = playbackManager.playbackStateFlow
        .map { PodcastAndEpisode(it.podcast, it.episodeUuid) }
        .distinctUntilChanged()
        .flatMapLatest(::createUiStateFlow)
        .stateIn(viewModelScope, SharingStarted.Lazily, UiState.Empty())

    private fun createUiStateFlow(podcastAndEpisode: PodcastAndEpisode) =
        transcriptsManager.observerTranscriptForEpisode(podcastAndEpisode.episodeUuid)
            .map { transcript ->
                if (transcript == null) {
                    UiState.Empty(podcastAndEpisode.podcast)
                } else {
                    try {
                        UiState.Success(
                            transcript = transcript,
                            podcast = podcastAndEpisode.podcast,
                            cues = parseTranscript(transcript),
                        )
                    } catch (e: UnsupportedOperationException) {
                        UiState.Error(TranscriptError.NotSupported(transcript.type), podcastAndEpisode.podcast)
                    } catch (e: Exception) {
                        UiState.Error(TranscriptError.FailedToLoad, podcastAndEpisode.podcast)
                    }
                }
            }

    private suspend fun parseTranscript(transcript: Transcript): List<CuesWithTiming> {
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
            return result.build()
        }
    }

    data class PodcastAndEpisode(
        val podcast: Podcast?,
        val episodeUuid: String,
    )

    sealed class UiState {
        abstract val podcast: Podcast?

        data class Empty(
            override val podcast: Podcast? = null,
        ) : UiState()

        data class Success(
            val transcript: Transcript?,
            override val podcast: Podcast?,
            val cues: List<CuesWithTiming> = emptyList(),
        ) : UiState()

        data class Error(
            val error: TranscriptError,
            override val podcast: Podcast?,
        ) : UiState()
    }

    sealed class TranscriptError {
        data class NotSupported(val format: String) : TranscriptError()
        data object FailedToLoad : TranscriptError()
    }
}
