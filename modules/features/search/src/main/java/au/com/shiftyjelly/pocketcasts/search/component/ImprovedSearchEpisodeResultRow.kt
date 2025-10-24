package au.com.shiftyjelly.pocketcasts.search.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton
import au.com.shiftyjelly.pocketcasts.views.helper.PlayButtonListener
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.rx2.asFlow

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel()
class ImprovedEpisodeRowViewModel @Inject constructor(
    private val podcastManager: PodcastManager,
    playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
) : ViewModel() {

    private val episodePlaybackFlow = playbackManager.playbackStateFlow

    private data class EpisodeKey(
        val episodeUuid: String,
        val podcastUuid: String,
    )

    private val episodeFlowCache = mutableMapOf<EpisodeKey, StateFlow<RowState>>()

    fun getEpisodeFlow(episodeUuid: String, podcastUuid: String): StateFlow<RowState> {
        return episodeFlowCache.getOrPut(EpisodeKey(episodeUuid, podcastUuid)) {
            val flow = combine<PodcastEpisode, PlaybackState, RowState>(
                podcastManager.findOrDownloadPodcastRxSingle(podcastUuid)
                    .toObservable().asFlow()
                    .flatMapLatest { ep ->
                        flow { emit(checkNotNull(episodeManager.findByUuid(episodeUuid))) }
                    },
                episodePlaybackFlow.map {
                    if (it.episodeUuid == episodeUuid) {
                        it
                    } else {
                        PlaybackState()
                    }
                }.distinctUntilChanged(),
            ) { episode, playbackState ->
                val changedEpisode = episode.copy(
                    playedUpToModified = if (playbackState.episodeUuid == episodeUuid) {
                        System.currentTimeMillis()
                    } else {
                        episode.playedUpToModified
                    },
                ).also {
                    val isPlaying = playbackState.episodeUuid == episodeUuid && playbackState.state == PlaybackState.State.PLAYING
                    it.playing = isPlaying
                    it.playedUpToMs = if (playbackState.episodeUuid == episodeUuid) {
                        playbackState.positionMs
                    } else {
                        episode.playedUpToMs
                    }
                    it.durationMs = if (playbackState.episodeUuid == episodeUuid) {
                        playbackState.durationMs
                    } else {
                        episode.durationMs
                    }
                    if (isPlaying) {
                        it.playingStatus = EpisodePlayingStatus.IN_PROGRESS
                    }
                }
                RowState.Loaded(changedEpisode)
            }.catch {
                emit(RowState.Error(it))
            }
            flow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis = 300, replayExpirationMillis = 300), initialValue = RowState.Idle)
        }
    }

    sealed interface RowState {
        data object Idle : RowState
        data class Loaded(
            val episode: BaseEpisode,
        ) : RowState

        data class Error(val error: Throwable?) : RowState
    }
}

@Composable
fun ImprovedSearchEpisodeResultRow(
    item: SearchAutoCompleteItem.Episode,
    onClick: () -> Unit,
    playButtonListener: PlayButton.OnClickListener,
    modifier: Modifier = Modifier,
    viewModel: ImprovedEpisodeRowViewModel = hiltViewModel(),
) {
    ImprovedSearchEpisodeResultRow(
        episodeUuid = item.uuid,
        podcastUuid = item.podcastUuid,
        title = item.title,
        duration = item.duration.seconds,
        publishedAt = item.publishedAt,
        onClick = onClick,
        playButtonListener = playButtonListener,
        rowStateFlow = { episode, podcast -> viewModel.getEpisodeFlow(episode, podcast) },
        modifier = modifier,
    )
}

@Composable
fun ImprovedSearchEpisodeResultRow(
    episode: ImprovedSearchResultItem.EpisodeItem,
    onClick: () -> Unit,
    playButtonListener: PlayButtonListener,
    modifier: Modifier = Modifier,
    viewModel: ImprovedEpisodeRowViewModel = hiltViewModel(),
) {
    ImprovedSearchEpisodeResultRow(
        episodeUuid = episode.uuid,
        podcastUuid = episode.podcastUuid,
        title = episode.title,
        duration = episode.duration,
        publishedAt = episode.publishedDate,
        playButtonListener = playButtonListener,
        onClick = onClick,
        modifier = modifier,
        rowStateFlow = { episode, podcast -> viewModel.getEpisodeFlow(episode, podcast) },
    )
}

@Composable
private fun ImprovedSearchEpisodeResultRow(
    episodeUuid: String,
    podcastUuid: String,
    title: String,
    duration: Duration,
    publishedAt: Date,
    onClick: () -> Unit,
    playButtonListener: PlayButton.OnClickListener,
    rowStateFlow: (String, String) -> StateFlow<ImprovedEpisodeRowViewModel.RowState>,
    modifier: Modifier = Modifier,
) {
    val state by rowStateFlow(episodeUuid, podcastUuid).collectAsState()

    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        EpisodeImage(
            episode = PodcastEpisode(
                uuid = episodeUuid,
                title = title,
                duration = duration.inWholeSeconds.toDouble(),
                publishedDate = publishedAt,
                podcastUuid = podcastUuid,
            ),
            placeholderType = PocketCastsImageRequestFactory.PlaceholderType.Small,
            useEpisodeArtwork = false,
            corners = 4.dp,
            modifier = Modifier
                .size(56.dp)
                .shadow(1.dp, RoundedCornerShape(4.dp)),
        )
        Column(
            modifier = Modifier.weight(1f),
        ) {
            val context = LocalContext.current
            val formattedDuration =
                remember(duration, context) { TimeHelper.getTimeDurationMediumString(duration.inWholeMilliseconds.toInt(), context) }
            val dateFormatter = RelativeDateFormatter(context)
            val formattedPublishDate = remember(publishedAt, dateFormatter) { dateFormatter.format(publishedAt) }

            TextC70(
                fontSize = 11.sp,
                text = formattedPublishDate,
                maxLines = 1,
            )
            TextH40(
                text = title,
                color = MaterialTheme.theme.colors.primaryText01,
                maxLines = 1,
            )
            TextH60(
                fontSize = 12.sp,
                text = formattedDuration,
                color = MaterialTheme.theme.colors.secondaryText02,
                fontWeight = FontWeight.W600,
                maxLines = 1,
            )
        }
        when (val state = state) {
            is ImprovedEpisodeRowViewModel.RowState.Loaded -> {
                val buttonColor = MaterialTheme.theme.colors.primaryInteractive01.toArgb()
                AndroidView(
                    modifier = Modifier.size(48.dp),
                    factory = {
                        PlayButton(it).apply {
                            listener = playButtonListener
                        }
                    },
                    update = { playButton ->
                        val buttonType = PlayButton.calculateButtonType(state.episode, true)
                        playButton.setButtonType(
                            episode = state.episode,
                            buttonType = buttonType,
                            color = buttonColor,
                            fromListUuid = null,
                        )
                    },
                )
            }

            else -> Unit
        }
    }
}

@Preview
@Composable
private fun PreviewEpisodeResultRow(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        ImprovedSearchEpisodeResultRow(
            episodeUuid = "",
            podcastUuid = "",
            title = "Episode title",
            duration = 340.seconds,
            publishedAt = Date(),
            rowStateFlow = { _, _ -> MutableStateFlow(ImprovedEpisodeRowViewModel.RowState.Idle) },
            playButtonListener = object : PlayButton.OnClickListener {
                override var source: SourceView = SourceView.SEARCH_RESULTS

                override fun onPlayClicked(episodeUuid: String) = Unit

                override fun onPauseClicked() = Unit

                override fun onPlayNext(episodeUuid: String) = Unit

                override fun onPlayLast(episodeUuid: String) = Unit

                override fun onDownload(episodeUuid: String) = Unit

                override fun onStopDownloading(episodeUuid: String) = Unit

                override fun onPlayedClicked(episodeUuid: String) = Unit
            },
            onClick = {},
        )
    }
}
