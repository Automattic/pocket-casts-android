package au.com.shiftyjelly.pocketcasts.upnext

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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.component.LocalOpenNowPlaying
import au.com.shiftyjelly.pocketcasts.component.TvEmptyState
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionContext
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionsModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeInfoModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeListItem
import au.com.shiftyjelly.pocketcasts.component.rememberTvEpisodeListFocus
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.TvPodcastDetailsScreen
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvUpNextScreen(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvUpNextViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var openedPodcastUuid by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.trackUpNextShown()
        viewModel.onShown()
    }

    val podcastUuid = openedPodcastUuid
    if (podcastUuid != null) {
        BackHandler { openedPodcastUuid = null }
        TvPodcastDetailsScreen(
            podcastUuid = podcastUuid,
            source = SourceView.UP_NEXT,
            onClose = { openedPodcastUuid = null },
            modifier = modifier,
        )
    } else {
        val openNowPlaying = LocalOpenNowPlaying.current
        TvUpNextContent(
            uiState = uiState,
            onNavigateToHome = {
                viewModel.trackDiscoverButtonTapped()
                onNavigateToHome()
            },
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
private fun TvUpNextContent(
    uiState: TvUpNextUiState,
    onNavigateToHome: () -> Unit,
    onOpenPodcast: (String) -> Unit,
    onPlayEpisode: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is TvUpNextUiState.Loading -> {
                LoadingView(color = MaterialTheme.tvColors.textPrimary, modifier = Modifier.fillMaxSize())
            }

            is TvUpNextUiState.Empty -> {
                UpNextEmpty(
                    onNavigateToHome = onNavigateToHome,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is TvUpNextUiState.Loaded -> {
                UpNextList(
                    episodes = uiState.episodes,
                    onOpenPodcast = onOpenPodcast,
                    onPlayEpisode = onPlayEpisode,
                )
            }
        }
    }
}

@Composable
private fun UpNextList(
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
            .padding(start = 42.dp, top = 40.dp),
    ) {
        UpNextHeader(episodes = episodes)
        Spacer(Modifier.height(16.dp))
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
            actionContext = TvEpisodeActionContext.UpNext,
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
            actionContext = TvEpisodeActionContext.UpNext,
            onDismissRequest = { detailsEpisode = null },
        )
    }
}

@Composable
private fun UpNextHeader(
    episodes: List<PodcastEpisode>,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(LR.string.up_next),
            style = MaterialTheme.tvTypography.title2,
            color = MaterialTheme.tvColors.textPrimary,
        )
        Text(
            text = episodeSummaryText(episodes),
            style = MaterialTheme.tvTypography.caption2,
            color = MaterialTheme.tvColors.textSecondary,
        )
    }
}

@Composable
private fun episodeSummaryText(episodes: List<PodcastEpisode>): String {
    val context = LocalContext.current
    val countText = pluralStringResource(LR.plurals.episode_count, episodes.size, episodes.size)
    val remainingMs = episodes.sumOf { episode ->
        (episode.durationMs - episode.playedUpToMs).coerceAtLeast(0).toLong()
    }
    val timeLeftText = stringResource(LR.string.time_left, TimeHelper.getTimeDurationShortString(remainingMs, context))
    return "$countText · $timeLeftText"
}

@Composable
private fun UpNextEmpty(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvEmptyState(
        title = stringResource(LR.string.tv_up_next_empty_title),
        subtitle = stringResource(LR.string.tv_up_next_empty_subtitle),
        actionLabel = stringResource(LR.string.tv_up_next_empty_action_title),
        onAction = onNavigateToHome,
        modifier = modifier,
    )
}

private const val ROW_WIDTH_FRACTION = 0.65f

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvUpNextLoadedPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvUpNextContent(
                uiState = TvUpNextUiState.Loaded(
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
                onNavigateToHome = {},
                onOpenPodcast = {},
                onPlayEpisode = {},
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvUpNextEmptyPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvUpNextContent(
                uiState = TvUpNextUiState.Empty,
                onNavigateToHome = {},
                onOpenPodcast = {},
                onPlayEpisode = {},
            )
        }
    }
}
