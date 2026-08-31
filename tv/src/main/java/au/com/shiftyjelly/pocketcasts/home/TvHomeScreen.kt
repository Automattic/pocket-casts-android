package au.com.shiftyjelly.pocketcasts.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.LocalOpenNowPlaying
import au.com.shiftyjelly.pocketcasts.component.LocalTvToastHostState
import au.com.shiftyjelly.pocketcasts.component.TvDetailOverlay
import au.com.shiftyjelly.pocketcasts.component.tvFocusInactiveWhen
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.discover.TvCategoryPodcastsScreen
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverBanner
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverEpisode
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverPodcast
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.discover.TvOpenedCategory
import au.com.shiftyjelly.pocketcasts.discover.TvOpenedCategorySaver
import au.com.shiftyjelly.pocketcasts.discover.tvDiscoverRow
import au.com.shiftyjelly.pocketcasts.podcasts.TvPodcastDetailsScreen
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.TvTopBarHeight
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvHomeScreen(
    onNavigateToSearch: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var openedCategory by rememberSaveable(stateSaver = TvOpenedCategorySaver) { mutableStateOf<TvOpenedCategory?>(null) }
    var openedPodcastUuid by rememberSaveable { mutableStateOf<String?>(null) }
    var restoreFocusTrigger by remember { mutableIntStateOf(0) }
    var categoryRestoreTrigger by remember { mutableIntStateOf(0) }

    CallOnce { viewModel.trackHomeShown() }

    val category = openedCategory
    val podcastUuid = openedPodcastUuid
    val openNowPlaying = LocalOpenNowPlaying.current
    val toastHostState = LocalTvToastHostState.current
    val playFailedMessage = stringResource(LR.string.error_generic_message)
    LaunchedEffect(Unit) {
        viewModel.playStarted.collect {
            openNowPlaying()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.playFailures.collect {
            toastHostState.show(playFailedMessage)
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        TvHomeContent(
            uiState = uiState,
            onRetry = viewModel::load,
            onTapBanner = { banner ->
                viewModel.trackBannerTapped(banner)
                when (banner) {
                    TvDiscoverBanner.DiscoverMore -> onNavigateToSearch()
                    TvDiscoverBanner.CreateAccount -> onCreateAccount()
                }
            },
            onPodcastClick = { row, podcast ->
                viewModel.trackDiscoverPodcastTapped(row, podcast)
                openedPodcastUuid = podcast.uuid
            },
            onEpisodePlay = { row, episode ->
                viewModel.trackDiscoverEpisodePlayed(row, episode)
                viewModel.playEpisode(episode)
            },
            onPlayLatestEpisode = viewModel::playLatestEpisode,
            onEpisodePodcastClick = { row, episode ->
                viewModel.trackDiscoverEpisodePodcastTapped(row, episode)
                openedPodcastUuid = episode.podcastUuid
            },
            onCategoryClick = { category, index ->
                viewModel.trackCategoryPillTapped(category, index)
                openedCategory = TvOpenedCategory(category.id, category.name, category.source)
            },
            onListImpression = viewModel::trackDiscoverListShown,
            loadCategoryCovers = viewModel::categoryCoverUrls,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = TvTopBarHeight)
                .tvFocusInactiveWhen(category != null || podcastUuid != null),
            restoreFocusTrigger = restoreFocusTrigger,
        )
        TvDetailOverlay(
            target = category,
            onBack = { openedCategory = null },
            modifier = Modifier.tvFocusInactiveWhen(podcastUuid != null),
            onHide = { restoreFocusTrigger++ },
        ) { openCategory ->
            TvCategoryPodcastsScreen(
                categoryName = openCategory.name,
                categorySource = openCategory.source,
                getCategoryPodcasts = { source -> viewModel.categoryPodcasts(openCategory.id, source) },
                onOpenPodcast = { openedPodcastUuid = it },
                onPodcastClick = { listId, podcast -> viewModel.trackCategoryPodcastTapped(openCategory, listId, podcast) },
                onClose = { openedCategory = null },
                restoreFocusTrigger = categoryRestoreTrigger,
            )
        }
        TvDetailOverlay(
            target = podcastUuid,
            onBack = { openedPodcastUuid = null },
            onHide = { if (openedCategory != null) categoryRestoreTrigger++ else restoreFocusTrigger++ },
        ) { uuid ->
            TvPodcastDetailsScreen(
                podcastUuid = uuid,
                onClose = { openedPodcastUuid = null },
            )
        }
    }
}

@Composable
private fun TvHomeContent(
    uiState: TvHomeUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onTapBanner: (TvDiscoverBanner) -> Unit = {},
    onPodcastClick: (TvDiscoverRow, TvDiscoverPodcast) -> Unit = { _, _ -> },
    onEpisodePlay: (TvDiscoverRow, TvDiscoverEpisode) -> Unit = { _, _ -> },
    onPlayLatestEpisode: (TvDiscoverRow, TvDiscoverPodcast) -> Unit = { _, _ -> },
    onEpisodePodcastClick: (TvDiscoverRow, TvDiscoverEpisode) -> Unit = { _, _ -> },
    onCategoryClick: (DiscoverCategory, Int) -> Unit = { _, _ -> },
    onListImpression: (TvDiscoverRow) -> Unit = {},
    loadCategoryCovers: (suspend (DiscoverCategory) -> List<String>)? = null,
    restoreFocusTrigger: Int = 0,
) {
    when (uiState) {
        is TvHomeUiState.Loading -> LoadingView(color = MaterialTheme.tvColors.textPrimary, modifier = modifier)

        is TvHomeUiState.Error -> TvHomeError(onRetry = onRetry, modifier = modifier)

        is TvHomeUiState.Ready -> if (uiState.rows.isEmpty()) {
            TvHomeError(onRetry = onRetry, modifier = modifier)
        } else {
            TvHomeRows(
                rows = uiState.rows,
                onTapBanner = onTapBanner,
                onPodcastClick = onPodcastClick,
                onEpisodePlay = onEpisodePlay,
                onPlayLatestEpisode = onPlayLatestEpisode,
                onEpisodePodcastClick = onEpisodePodcastClick,
                onCategoryClick = onCategoryClick,
                onListImpression = onListImpression,
                loadCategoryCovers = loadCategoryCovers,
                modifier = modifier,
                restoreFocusTrigger = restoreFocusTrigger,
            )
        }
    }
}

@Composable
private fun TvHomeError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(LR.string.error_generic_message),
                color = MaterialTheme.tvColors.textPrimary,
                style = MaterialTheme.tvTypography.caption1,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(LR.string.retry))
            }
        }
    }
}

@Composable
private fun TvHomeRows(
    rows: List<TvDiscoverRow>,
    modifier: Modifier = Modifier,
    onTapBanner: (TvDiscoverBanner) -> Unit = {},
    onPodcastClick: (TvDiscoverRow, TvDiscoverPodcast) -> Unit = { _, _ -> },
    onEpisodePlay: (TvDiscoverRow, TvDiscoverEpisode) -> Unit = { _, _ -> },
    onPlayLatestEpisode: (TvDiscoverRow, TvDiscoverPodcast) -> Unit = { _, _ -> },
    onEpisodePodcastClick: (TvDiscoverRow, TvDiscoverEpisode) -> Unit = { _, _ -> },
    onCategoryClick: (DiscoverCategory, Int) -> Unit = { _, _ -> },
    onListImpression: (TvDiscoverRow) -> Unit = {},
    loadCategoryCovers: (suspend (DiscoverCategory) -> List<String>)? = null,
    restoreFocusTrigger: Int = 0,
) {
    var lastFocusedRowIndex by rememberSaveable(rows.size) { mutableIntStateOf(0) }
    val rowFocusRequesters = remember(rows.size) { List(rows.size) { FocusRequester() } }

    var isInitialComposition by remember { mutableStateOf(true) }
    LaunchedEffect(restoreFocusTrigger) {
        // Only restore on an actual trigger change, not on the initial composition.
        if (isInitialComposition) {
            isInitialComposition = false
        } else {
            runCatching { rowFocusRequesters.getOrNull(lastFocusedRowIndex)?.requestFocus() }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        rows.forEachIndexed { rowIndex, row ->
            val rowModifier = Modifier.onFocusChanged { focusState ->
                if (focusState.hasFocus) {
                    lastFocusedRowIndex = rowIndex
                }
            }
            val rowFocusRequester = rowFocusRequesters[rowIndex]
            tvDiscoverRow(
                row = row,
                onPodcastClick = onPodcastClick,
                onEpisodePlay = onEpisodePlay,
                onEpisodePodcastClick = onEpisodePodcastClick,
                onCategoryClick = onCategoryClick,
                onPlayLatestEpisode = onPlayLatestEpisode,
                modifier = rowModifier,
                focusRequester = rowFocusRequester,
                onTapBanner = onTapBanner,
                onListImpression = onListImpression,
                loadCategoryCovers = loadCategoryCovers,
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvHomeContentPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvHomeContent(
                uiState = TvHomeUiState.Ready(
                    rows = listOf(
                        TvDiscoverRow.Banner(
                            id = "discover_more",
                            title = "",
                            banner = TvDiscoverBanner.DiscoverMore,
                        ),
                        TvDiscoverRow.FeaturedPodcasts(
                            id = "featured",
                            title = "Featured",
                            podcasts = (1..3).map { previewPodcast(it) },
                        ),
                        TvDiscoverRow.Categories(
                            id = "categories",
                            title = "Browse categories",
                            categories = (1..4).map { index ->
                                DiscoverCategory(id = index, name = "Category $index", icon = "", source = "")
                            },
                        ),
                        TvDiscoverRow.Episodes(
                            id = "tv_featured_videos",
                            title = "Made for TV",
                            episodes = (1..6).map {
                                TvDiscoverEpisode(
                                    episodeUuid = "episode-$it",
                                    episodeTitle = "Episode $it",
                                    podcastUuid = "podcast-$it",
                                    podcastTitle = "Podcast $it",
                                )
                            },
                        ),
                        TvDiscoverRow.Podcasts(
                            id = "trending",
                            title = "Trending",
                            podcasts = (1..8).map { previewPodcast(it) },
                        ),
                    ),
                ),
                onRetry = {},
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvHomeErrorPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvHomeContent(
                uiState = TvHomeUiState.Error,
                onRetry = {},
            )
        }
    }
}

private fun previewPodcast(index: Int) = TvDiscoverPodcast(
    uuid = "podcast-$index",
    title = "Podcast $index",
    author = "Author $index",
    description = "Description of podcast $index",
)
