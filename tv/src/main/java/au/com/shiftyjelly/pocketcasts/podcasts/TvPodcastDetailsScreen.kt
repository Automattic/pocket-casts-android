package au.com.shiftyjelly.pocketcasts.podcasts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvArchivedFilterButton
import au.com.shiftyjelly.pocketcasts.component.TvArtworkImage
import au.com.shiftyjelly.pocketcasts.component.TvEmptyState
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionContext
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionsModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeInfoModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeListItem
import au.com.shiftyjelly.pocketcasts.component.TvPodcastInfoModal
import au.com.shiftyjelly.pocketcasts.component.TvSortButton
import au.com.shiftyjelly.pocketcasts.component.rememberTvEpisodeListFocus
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.displayLabel
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.onboarding.createaccount.TvCreateAccountModal
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInUiState
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvDetailsArtworkSize
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvPodcastDetailsScreen(
    podcastUuid: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvPodcastDetailsViewModel = hiltViewModel<TvPodcastDetailsViewModel, TvPodcastDetailsViewModel.Factory>(
        key = podcastUuid,
        creationCallback = { factory -> factory.create(podcastUuid) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState, onClose) {
        if (uiState is TvPodcastDetailsUiState.NotFound) {
            onClose()
        }
    }

    TvPodcastDetailsContent(
        uiState = uiState,
        accountAuthState = viewModel.accountAuthState,
        onToggleSubscribe = viewModel::toggleSubscribe,
        onStartAccountAuth = viewModel::startAccountAuth,
        onStopAccountAuth = viewModel::stopAccountAuth,
        onRetryAccountAuth = viewModel::retryAccountAuth,
        onChangeSortType = viewModel::changeSortType,
        onToggleArchiveFilter = viewModel::toggleArchiveFilter,
        modifier = modifier,
    )
}

@Composable
private fun TvPodcastDetailsContent(
    uiState: TvPodcastDetailsUiState,
    accountAuthState: StateFlow<TvSignInUiState>,
    onToggleSubscribe: () -> Unit,
    onStartAccountAuth: () -> Unit,
    onStopAccountAuth: () -> Unit,
    onRetryAccountAuth: () -> Unit,
    onChangeSortType: (EpisodesSortType) -> Unit,
    onToggleArchiveFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is TvPodcastDetailsUiState.Loading, TvPodcastDetailsUiState.NotFound -> {
                LoadingView(color = TvColors.TextPrimary, modifier = Modifier.fillMaxSize())
            }

            is TvPodcastDetailsUiState.Loaded -> {
                val followFocusRequester = remember { FocusRequester() }
                var isShowingInfoModal by remember { mutableStateOf(false) }
                var isShowingAccountModal by rememberSaveable { mutableStateOf(false) }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(80.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, top = 16.dp, end = 32.dp),
                ) {
                    PodcastInfo(
                        podcast = uiState.podcast,
                        followFocusRequester = followFocusRequester,
                        onFollow = {
                            if (uiState.isLoggedIn || uiState.podcast.isSubscribed) {
                                onToggleSubscribe()
                            } else {
                                isShowingAccountModal = true
                            }
                        },
                        onMoreInfo = { isShowingInfoModal = true },
                        modifier = Modifier.weight(INFO_PANE_WEIGHT),
                    )
                    if (uiState.episodes.isEmpty() && uiState.archivedEpisodeCount == 0) {
                        TvEmptyState(
                            title = stringResource(LR.string.podcast_no_episodes_found),
                            subtitle = stringResource(LR.string.podcast_no_episodes),
                            modifier = Modifier.weight(1f - INFO_PANE_WEIGHT).fillMaxHeight(),
                        )
                    } else {
                        EpisodeList(
                            podcast = uiState.podcast,
                            episodes = uiState.episodes,
                            archivedEpisodeCount = uiState.archivedEpisodeCount,
                            isShowingArchived = uiState.isShowingArchived,
                            onChangeSortType = onChangeSortType,
                            onToggleArchiveFilter = onToggleArchiveFilter,
                            leftFocusRequester = followFocusRequester,
                            modifier = Modifier.weight(1f - INFO_PANE_WEIGHT),
                        )
                    }
                }
                if (isShowingInfoModal) {
                    TvPodcastInfoModal(
                        podcast = uiState.podcast,
                        onDismissRequest = { isShowingInfoModal = false },
                    )
                }
                if (isShowingAccountModal) {
                    val authState by accountAuthState.collectAsStateWithLifecycle()
                    val currentOnStartAccountAuth by rememberUpdatedState(onStartAccountAuth)
                    val currentOnStopAccountAuth by rememberUpdatedState(onStopAccountAuth)
                    DisposableEffect(Unit) {
                        currentOnStartAccountAuth()
                        onDispose { currentOnStopAccountAuth() }
                    }
                    LaunchedEffect(authState) {
                        if (authState is TvSignInUiState.Complete) {
                            isShowingAccountModal = false
                        }
                    }
                    TvCreateAccountModal(
                        uiState = authState,
                        onRetry = onRetryAccountAuth,
                        onDismissRequest = { isShowingAccountModal = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun PodcastInfo(
    podcast: Podcast,
    followFocusRequester: FocusRequester,
    onFollow: () -> Unit,
    onMoreInfo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier,
    ) {
        TvArtworkImage(
            model = PodcastImage.getMediumArtworkUrl(podcast.uuid),
            modifier = Modifier
                .size(TvDetailsArtworkSize)
                .clip(RoundedCornerShape(8.dp)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (podcast.author.isNotBlank()) {
                Text(
                    text = podcast.author,
                    style = TvTextStyles.Caption1,
                    color = TvColors.TextSecondary,
                )
            }
            Text(
                text = podcast.title,
                style = TvTextStyles.Title3,
                color = TvColors.TextPrimary,
            )
            if (podcast.podcastDescription.isNotBlank()) {
                Text(
                    text = podcast.podcastDescription,
                    style = TvTextStyles.Caption1,
                    color = TvColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onFollow,
                colors = TvButtonDefaults.filledButtonColors(),
                modifier = Modifier.focusRequester(followFocusRequester),
            ) {
                Text(stringResource(if (podcast.isSubscribed) LR.string.podcast_subscribed else LR.string.tv_podcast_follow))
            }
            Button(
                onClick = onMoreInfo,
                colors = TvButtonDefaults.filledButtonColors(),
            ) {
                Text(stringResource(LR.string.tv_podcast_more_info))
            }
        }
    }
}

@Composable
private fun EpisodeList(
    podcast: Podcast,
    episodes: List<PodcastEpisode>,
    archivedEpisodeCount: Int,
    isShowingArchived: Boolean,
    onChangeSortType: (EpisodesSortType) -> Unit,
    onToggleArchiveFilter: () -> Unit,
    leftFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormatter = remember(context) { RelativeDateFormatter(context) }
    val listState = rememberLazyListState()
    val focus = rememberTvEpisodeListFocus(episodes, listState, requestInitialFocus = true)
    LaunchedEffect(podcast.episodesSortType) {
        listState.scrollToItem(0)
    }
    var actionsEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }
    var detailsEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            Text(
                text = stringResource(LR.string.search_results_all_episodes),
                style = TvTextStyles.Title3,
                color = TvColors.TextPrimary,
                modifier = Modifier.weight(1f),
            )
            TvArchivedFilterButton(
                isShowingArchived = isShowingArchived,
                onToggleArchiveFilter = onToggleArchiveFilter,
                leftFocusRequester = leftFocusRequester,
            )
            Spacer(Modifier.width(12.dp))
            TvSortButton(
                selected = podcast.episodesSortType,
                options = PodcastSortOptions,
                label = { it.displayLabel() },
                onSelect = onChangeSortType,
            )
        }
        if (episodes.isEmpty()) {
            AllEpisodesArchived(
                episodeCount = archivedEpisodeCount,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                        leftFocusRequester = leftFocusRequester,
                    )
                }
            }
        }
    }
    actionsEpisode?.let { episode ->
        TvEpisodeActionsModal(
            episode = episode,
            actionContext = TvEpisodeActionContext.PodcastDetails,
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
            onDismissRequest = { detailsEpisode = null },
        )
    }
}

@Composable
private fun AllEpisodesArchived(
    episodeCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Text(
            text = pluralStringResource(LR.plurals.tv_podcast_all_archived, episodeCount, episodeCount),
            style = MaterialTheme.typography.bodyLarge,
            color = TvColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 400.dp),
        )
    }
}

private const val INFO_PANE_WEIGHT = 0.35f

private val PodcastSortOptions = listOf(
    EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC,
    EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC,
    EpisodesSortType.EPISODES_SORT_BY_DATE_DESC,
    EpisodesSortType.EPISODES_SORT_BY_DATE_ASC,
    EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC,
    EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC,
)

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvPodcastDetailsPreview() {
    TvPodcastDetailsContentPreview(
        podcast = previewPodcast(isSubscribed = false),
        episodes = List(4) { index ->
            PodcastEpisode(uuid = "episode-$index", title = "Episode $index", publishedDate = Date(0))
        },
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvPodcastDetailsFollowingPreview() {
    TvPodcastDetailsContentPreview(
        podcast = previewPodcast(isSubscribed = true),
        episodes = List(4) { index ->
            PodcastEpisode(uuid = "episode-$index", title = "Episode $index", publishedDate = Date(0))
        },
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvPodcastDetailsEmptyPreview() {
    TvPodcastDetailsContentPreview(
        podcast = previewPodcast(isSubscribed = false),
        episodes = emptyList(),
    )
}

@Composable
private fun TvPodcastDetailsContentPreview(
    podcast: Podcast,
    episodes: List<PodcastEpisode>,
) {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvPodcastDetailsContent(
                uiState = TvPodcastDetailsUiState.Loaded(
                    podcast = podcast,
                    episodes = episodes,
                    archivedEpisodeCount = 0,
                    isShowingArchived = false,
                    isLoggedIn = true,
                ),
                accountAuthState = MutableStateFlow(TvSignInUiState.Loading),
                onToggleSubscribe = {},
                onStartAccountAuth = {},
                onStopAccountAuth = {},
                onRetryAccountAuth = {},
                onChangeSortType = {},
                onToggleArchiveFilter = {},
            )
        }
    }
}

private fun previewPodcast(isSubscribed: Boolean) = Podcast(
    uuid = "podcast-uuid",
    title = "The Daily",
    author = "The New York Times",
    podcastDescription = "This is what the news should sound like. The biggest stories of our time, told by the best journalists in the world.",
    isSubscribed = isSubscribed,
)
