package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MimeTypes
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.SelectedStream
import au.com.shiftyjelly.pocketcasts.repositories.podcast.AlternateEnclosureManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class StreamSelectorViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val alternateEnclosureManager: AlternateEnclosureManager,
) : ViewModel() {

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState = playbackManager.playbackStateFlow
        .map { it.episodeUuid }
        .distinctUntilChanged()
        .flatMapLatest(::uiStateFlow)
        .stateIn(viewModelScope, SharingStarted.Lazily, UiState())

    private fun uiStateFlow(episodeUuid: String) = combine(
        episodeManager.findEpisodeByUuidFlow(episodeUuid),
        alternateEnclosureManager.observeForEpisode(episodeUuid),
        playbackManager.selectedStreams,
        ::buildState,
    )

    fun selectStream(option: StreamOption) {
        val episodeUuid = uiState.value.episodeUuid ?: return
        playbackManager.selectStream(episodeUuid, SelectedStream(option.uri, option.contentType))
    }

    private fun buildState(
        episode: BaseEpisode,
        enclosures: List<EpisodeAlternateEnclosure>,
        selectedStreams: Map<String, SelectedStream>,
    ): UiState {
        val currentUri = selectedStreams[episode.uuid]?.uri ?: episode.streamUrl
        val options = buildList {
            episode.downloadUrl?.takeIf { it.isNotBlank() }?.let { uri ->
                add(option(uri, episode.fileType, height = null, bitrate = null, currentUri))
            }
            enclosures.forEach { enclosure ->
                val uri = enclosure.sources.firstOrNull { it.uri.startsWith("http", ignoreCase = true) }?.uri
                if (uri != null) {
                    val contentType = enclosure.type ?: enclosure.sources.firstOrNull { it.uri == uri }?.contentType
                    add(option(uri, contentType, enclosure.height, enclosure.bitrate, currentUri))
                }
            }
        }.distinctBy { it.uri }
        return UiState(episodeUuid = episode.uuid, options = options)
    }

    private fun option(
        uri: String,
        contentType: String?,
        height: Int?,
        bitrate: Long?,
        currentUri: String?,
    ) = StreamOption(
        uri = uri,
        contentType = contentType,
        kind = streamKind(uri, contentType),
        height = height,
        bitrate = bitrate,
        isSelected = uri == currentUri,
    )

    private fun streamKind(uri: String, contentType: String?): StreamKind = when {
        BaseEpisode.isHlsUrl(uri) || isHlsMimeType(contentType) -> StreamKind.Hls
        contentType?.startsWith("video/", ignoreCase = true) == true -> StreamKind.Video
        contentType?.startsWith("audio/", ignoreCase = true) == true -> StreamKind.Audio
        else -> StreamKind.Other
    }

    private fun isHlsMimeType(type: String?): Boolean = type.equals(MimeTypes.APPLICATION_M3U8, ignoreCase = true) ||
        type.equals("application/vnd.apple.mpegurl", ignoreCase = true)

    data class UiState(
        val episodeUuid: String? = null,
        val options: List<StreamOption> = emptyList(),
    )

    data class StreamOption(
        val uri: String,
        val contentType: String?,
        val kind: StreamKind,
        val height: Int?,
        val bitrate: Long?,
        val isSelected: Boolean,
    )

    enum class StreamKind { Hls, Video, Audio, Other }
}
