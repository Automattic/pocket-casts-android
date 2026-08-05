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
import au.com.shiftyjelly.pocketcasts.component.TvEmptyState
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionContext
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionsModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeInfoModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeListItem
import au.com.shiftyjelly.pocketcasts.component.rememberTvEpisodeListFocus
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.podcasts.TvPodcastDetailsScreen
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
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
        TvUpNextContent(
            uiState = uiState,
            onNavigateToHome = onNavigateToHome,
            onOpenPodcast = { openedPodcastUuid = it },
            modifier = modifier,
        )
    }
}

@Composable
private fun TvUpNextContent(
    uiState: TvUpNextUiState,
    onNavigateToHome: () -> Unit,
    onOpenPodcast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is TvUpNextUiState.Loading -> {
                LoadingView(color = TvColors.TextPrimary, modifier = Modifier.fillMaxSize())
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
                )
            }
        }
    }
}

@Composable
private fun UpNextList(
    episodes: List<PodcastEpisode>,
    onOpenPodcast: (String) -> Unit,
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
            .padding(start = 32.dp, top = 16.dp),
    ) {
        UpNextHeader(episodes = episodes)
        Spacer(Modifier.height(24.dp))
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
                    onClick = {},
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
            style = TvTextStyles.Title3,
            color = TvColors.TextPrimary,
        )
        Text(
            text = episodeSummaryText(episodes),
            style = TvTextStyles.Caption2,
            color = TvColors.TextSecondary,
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

private const val ROW_WIDTH_FRACTION = 0.75f

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvUpNextLoadedPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.BackgroundSunken)) {
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
                )
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvUpNextEmptyPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.BackgroundSunken)) {
                TvUpNextContent(
                    uiState = TvUpNextUiState.Empty,
                    onNavigateToHome = {},
                    onOpenPodcast = {},
                )
            }
        }
    }
}
