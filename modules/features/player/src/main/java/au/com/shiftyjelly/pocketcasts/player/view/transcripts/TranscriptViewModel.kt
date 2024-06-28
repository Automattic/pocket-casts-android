package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.annotation.OptIn
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.extractor.text.CuesWithTiming
import androidx.media3.extractor.text.SubtitleParser
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
        .map { it.episodeUuid }
        .distinctUntilChanged()
        .flatMapLatest(::createUiStateFlow)
        .stateIn(viewModelScope, SharingStarted.Lazily, UiState())

    private fun createUiStateFlow(episodeId: String) =
        transcriptsManager.observerTranscriptForEpisode(episodeId)
            .map { transcript ->
                val cues = transcript?.let { parseTranscript(it) } ?: emptyList()
                UiState(
                    transcript = transcript,
                    episodeId = episodeId,
                    cues = cues,
                )
            }

    private suspend fun parseTranscript(transcript: Transcript): List<CuesWithTiming> {
        val format = Format.Builder()
            .setSampleMimeType(transcript.type)
            .build()
        return if (subtitleParserFactory.supportsFormat(format).not()) {
            emptyList()
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

    data class UiState(
        val transcript: Transcript? = null,
        val episodeId: String? = null,
        val cues: List<CuesWithTiming> = emptyList(),
    )
}
