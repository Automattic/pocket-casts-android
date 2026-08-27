package au.com.shiftyjelly.pocketcasts.starred

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import au.com.shiftyjelly.pocketcasts.component.TvEmptyState
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionContext
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionsModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeInfoModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeListItem
import au.com.shiftyjelly.pocketcasts.component.rememberTvEpisodeListFocus
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.TvPodcastDetailsScreen
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvStarredScreen(
    modifier: Modifier = Modifier,
    viewModel: TvStarredViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var openedPodcastUuid by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.trackStarredShown()
        viewModel.onShown()
    }

    val podcastUuid = openedPodcastUuid
    if (podcastUuid != null) {
        BackHandler { openedPodcastUuid = null }
        TvPodcastDetailsScreen(
            podcastUuid = podcastUuid,
            onClose = { openedPodcastUuid = null },
            modifier = modifier,
        )
    } else {
        val openNowPlaying = LocalOpenNowPlaying.current
        TvStarredContent(
            uiState = uiState,
            onOpenPodcast = { openedPodcastUuid = it },
            onPlayEpisode = { episode ->
                viewModel.play(episode)
                openNowPlaying()
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun TvStarredContent(
    uiState: TvStarredUiState,
    onOpenPodcast: (String) -> Unit,
    onPlayEpisode: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is TvStarredUiState.Loading -> {
                LoadingView(color = MaterialTheme.tvColors.textPrimary, modifier = Modifier.fillMaxSize())
            }

            is TvStarredUiState.Empty -> {
                StarredEmpty(modifier = Modifier.fillMaxSize())
            }

            is TvStarredUiState.Loaded -> {
                StarredList(
                    episodes = uiState.episodes,
                    onOpenPodcast = onOpenPodcast,
                    onPlayEpisode = onPlayEpisode,
                )
            }
        }
    }
}

@Composable
private fun StarredList(
    episodes: List<PodcastEpisode>,
    onOpenPodcast: (String) -> Unit,
    onPlayEpisode: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormatter = remember(context) { RelativeDateFormatter(context) }
    val listState = rememberLazyListState()
    val focus = rememberTvEpisodeListFocus(episodes, listState, requestInitialFocus = false)
    var actionsEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }
    var detailsEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }

    Column(
        modifier = modifier
            .fillMaxHeight()
            .fillMaxWidth(ROW_WIDTH_FRACTION)
            .padding(start = 32.dp, top = 8.dp),
    ) {
        Text(
            text = stringResource(LR.string.tv_profile_starred_episodes),
            style = MaterialTheme.tvTypography.title3,
            color = MaterialTheme.tvColors.textPrimary,
        )
        Spacer(Modifier.height(26.dp))
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
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
            actionContext = TvEpisodeActionContext.Starred,
            onDismissRequest = { actionsEpisode = null },
            onShowEpisodeDetails = {
                detailsEpisode = episode
                actionsEpisode = null
            },
            onGoToPodcast = { onOpenPodcast(episode.podcastUuid) },
        )
    }
    detailsEpisode?.let { episode ->
        TvEpisodeInfoModal(
            episode = episode,
            actionContext = TvEpisodeActionContext.Starred,
            onDismissRequest = { detailsEpisode = null },
        )
    }
}

@Composable
private fun StarredEmpty(
    modifier: Modifier = Modifier,
) {
    TvEmptyState(
        title = stringResource(LR.string.tv_starred_empty_title),
        subtitle = stringResource(LR.string.tv_starred_empty_subtitle),
        modifier = modifier,
    )
}

private const val ROW_WIDTH_FRACTION = 0.75f

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvStarredLoadedPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvStarredContent(
                uiState = TvStarredUiState.Loaded(
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
                onOpenPodcast = {},
                onPlayEpisode = {},
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvStarredEmptyPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvStarredContent(
                uiState = TvStarredUiState.Empty,
                onOpenPodcast = {},
                onPlayEpisode = {},
            )
        }
    }
}
