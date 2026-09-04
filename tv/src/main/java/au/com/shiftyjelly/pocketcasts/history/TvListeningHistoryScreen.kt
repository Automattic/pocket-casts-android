package au.com.shiftyjelly.pocketcasts.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.LocalOpenNowPlaying
import au.com.shiftyjelly.pocketcasts.component.ScrollToTopEffect
import au.com.shiftyjelly.pocketcasts.component.TvEmptyState
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionContext
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionsModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeInfoModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeListItem
import au.com.shiftyjelly.pocketcasts.component.rememberTvEpisodeListFocus
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvListeningHistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: TvListeningHistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val openNowPlaying = LocalOpenNowPlaying.current

    LaunchedEffect(Unit) {
        viewModel.trackShown()
    }

    TvListeningHistoryContent(
        uiState = uiState,
        onPlayEpisode = { episode ->
            viewModel.play(episode)
            openNowPlaying()
        },
        modifier = modifier,
    )
}

@Composable
private fun TvListeningHistoryContent(
    uiState: TvListeningHistoryUiState,
    onPlayEpisode: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is TvListeningHistoryUiState.Loading -> {
                LoadingView(color = MaterialTheme.tvColors.textPrimary, modifier = Modifier.fillMaxSize())
            }

            is TvListeningHistoryUiState.Empty -> {
                TvEmptyState(
                    title = stringResource(LR.string.tv_history_empty_title),
                    subtitle = stringResource(LR.string.tv_history_empty_subtitle),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is TvListeningHistoryUiState.Loaded -> {
                HistoryList(
                    episodes = uiState.episodes,
                    onPlayEpisode = onPlayEpisode,
                )
            }
        }
    }
}

@Composable
private fun HistoryList(
    episodes: List<PodcastEpisode>,
    onPlayEpisode: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormatter = remember(context) { RelativeDateFormatter(context) }
    val listState = rememberLazyListState()
    ScrollToTopEffect { listState.scrollToItem(0) }
    val focus = rememberTvEpisodeListFocus(episodes, listState, requestInitialFocus = true)
    var actionsEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }
    var detailsEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(ROW_WIDTH_FRACTION)
            .padding(start = 42.dp, top = 6.dp),
    ) {
        Text(
            text = stringResource(LR.string.profile_navigation_listening_history),
            style = MaterialTheme.tvTypography.title3,
            color = MaterialTheme.tvColors.textPrimary,
        )
        Spacer(Modifier.height(19.5.dp))
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
            modifier = Modifier.weight(1f),
        ) {
            itemsIndexed(
                items = episodes,
                key = { _, episode -> episode.uuid },
            ) { index, episode ->
                TvEpisodeListItem(
                    episode = episode,
                    dateFormatter = dateFormatter,
                    onClick = { onPlayEpisode(episode) },
                    onOpenActions = {
                        focus.watchForRemoval(episodes, index)
                        actionsEpisode = episode
                    },
                    episodeFocusRequester = focus.requesterFor(episode.uuid),
                )
            }
        }
    }

    actionsEpisode?.let { episode ->
        TvEpisodeActionsModal(
            episode = episode,
            actionContext = TvEpisodeActionContext.ListeningHistory,
            onDismissRequest = { actionsEpisode = null },
            onShowEpisodeDetails = {
                detailsEpisode = episode
                actionsEpisode = null
            },
        )
    }
    detailsEpisode?.let { episode ->
        TvEpisodeInfoModal(
            episode = episode,
            actionContext = TvEpisodeActionContext.ListeningHistory,
            onDismissRequest = { detailsEpisode = null },
        )
    }
}

private const val ROW_WIDTH_FRACTION = 0.75f

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvListeningHistoryLoadedPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvListeningHistoryContent(
                uiState = TvListeningHistoryUiState.Loaded(
                    episodes = List(4) { index ->
                        PodcastEpisode(
                            uuid = "episode-$index",
                            title = "Episode $index title that may span multiple lines to test the layout",
                            duration = 3600.0,
                            playedUpTo = 600.0,
                            publishedDate = Date(0),
                        )
                    },
                ),
                onPlayEpisode = {},
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvListeningHistoryEmptyPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvListeningHistoryContent(
                uiState = TvListeningHistoryUiState.Empty,
                onPlayEpisode = {},
            )
        }
    }
}
