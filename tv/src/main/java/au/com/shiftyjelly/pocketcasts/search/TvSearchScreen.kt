package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.LocalOpenNowPlaying
import au.com.shiftyjelly.pocketcasts.component.LocalTvToastHostState
import au.com.shiftyjelly.pocketcasts.component.TvCategoryTile
import au.com.shiftyjelly.pocketcasts.component.TvDetailOverlay
import au.com.shiftyjelly.pocketcasts.component.TvEmptyState
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionContext
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeActionsModal
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeInfoModal
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTile
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTileDefaults
import au.com.shiftyjelly.pocketcasts.component.TvRow
import au.com.shiftyjelly.pocketcasts.component.tvFocusInactiveWhen
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.discover.tvDiscoverRow
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.podcasts.TvPodcastDetailsScreen
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val ContentPadding = PaddingValues(horizontal = 48.dp)

@Composable
fun TvSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: TvSearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val discoverRows by viewModel.discoverRows.collectAsStateWithLifecycle()
    val actionsEpisode by viewModel.actionsEpisode.collectAsStateWithLifecycle()

    var openedPodcastUuid by rememberSaveable { mutableStateOf<String?>(null) }
    var detailsEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }
    var restoreFocusTrigger by remember { mutableIntStateOf(0) }
    val podcastUuid = openedPodcastUuid

    val openNowPlaying = LocalOpenNowPlaying.current
    val toastHostState = LocalTvToastHostState.current
    val playFailedMessage = stringResource(LR.string.error_generic_message)
    LaunchedEffect(Unit) {
        viewModel.playStarted.collect { openNowPlaying() }
    }
    LaunchedEffect(Unit) {
        viewModel.playFailures.collect { toastHostState.show(playFailedMessage) }
    }

    Box(modifier = modifier.fillMaxSize()) {
        TvSearchContent(
            query = query,
            searchState = searchState,
            categories = categories,
            discoverRows = discoverRows,
            onQueryChange = viewModel::onQueryChange,
            onOpenPodcast = { openedPodcastUuid = it },
            onPlayEpisode = viewModel::playEpisode,
            onOpenEpisodeActions = viewModel::openEpisodeActions,
            restoreFocusTrigger = restoreFocusTrigger,
            modifier = Modifier
                .fillMaxSize()
                .tvFocusInactiveWhen(podcastUuid != null),
        )
        TvDetailOverlay(
            target = podcastUuid,
            onBack = { openedPodcastUuid = null },
            onHide = { restoreFocusTrigger++ },
        ) { uuid ->
            TvPodcastDetailsScreen(
                podcastUuid = uuid,
                onClose = { openedPodcastUuid = null },
            )
        }
        actionsEpisode?.let { episode ->
            TvEpisodeActionsModal(
                episode = episode,
                actionContext = TvEpisodeActionContext.PodcastDetails,
                onDismissRequest = viewModel::dismissEpisodeActions,
                onShowEpisodeDetails = {
                    detailsEpisode = episode
                    viewModel.dismissEpisodeActions()
                },
                onGoToPodcast = {
                    viewModel.dismissEpisodeActions()
                    openedPodcastUuid = episode.podcastUuid
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
}

@Composable
private fun TvSearchContent(
    query: String,
    searchState: TvSearchState,
    categories: List<DiscoverCategory>,
    discoverRows: List<TvDiscoverRow>,
    onQueryChange: (String) -> Unit,
    onOpenPodcast: (String) -> Unit,
    onPlayEpisode: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    onOpenEpisodeActions: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    modifier: Modifier = Modifier,
    restoreFocusTrigger: Int = 0,
) {
    val searchFieldFocusRequester = remember { FocusRequester() }
    Column(
        modifier = modifier
            .fillMaxSize()
            .focusGroup()
            .focusProperties {
                onEnter = { runCatching { searchFieldFocusRequester.requestFocus() } }
            },
    ) {
        Column(modifier = Modifier.padding(ContentPadding)) {
            Spacer(modifier = Modifier.height(40.dp))
            TvSearchField(
                query = query,
                onQueryChange = onQueryChange,
                modifier = Modifier.focusRequester(searchFieldFocusRequester),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when (searchState) {
                is TvSearchState.Idle -> TvSearchDiscover(
                    categories = categories,
                    discoverRows = discoverRows,
                )

                is TvSearchState.Searching -> LoadingView(
                    color = MaterialTheme.tvColors.textPrimary,
                    modifier = Modifier.fillMaxSize(),
                )

                is TvSearchState.Error -> TvSearchMessage(
                    title = stringResource(LR.string.error_generic_message),
                    subtitle = stringResource(LR.string.tv_search_no_results_subtitle),
                )

                is TvSearchState.NoResults -> TvSearchMessage(
                    title = stringResource(LR.string.tv_search_no_results_title),
                    subtitle = stringResource(LR.string.tv_search_no_results_subtitle),
                )

                is TvSearchState.Results -> TvSearchResults(
                    results = searchState,
                    onOpenPodcast = onOpenPodcast,
                    onPlayEpisode = onPlayEpisode,
                    onOpenEpisodeActions = onOpenEpisodeActions,
                    restoreFocusTrigger = restoreFocusTrigger,
                )
            }
        }
    }
}

@Composable
private fun TvSearchDiscover(
    categories: List<DiscoverCategory>,
    discoverRows: List<TvDiscoverRow>,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (categories.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(ContentPadding)
                        .height(1.dp)
                        .background(MaterialTheme.tvColors.overlayBorder),
                )
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
                TvRow(
                    title = stringResource(LR.string.tv_search_browse_categories),
                    items = categories,
                    contentPadding = ContentPadding,
                    key = { it.id },
                ) { category ->
                    TvCategoryTile(category = category, onClick = {})
                }
            }
        }

        discoverRows.forEach { row ->
            item { Spacer(modifier = Modifier.height(24.dp)) }
            tvDiscoverRow(
                row = row,
                onOpenPodcast = {},
                onPlayEpisode = {},
                contentPadding = ContentPadding,
            )
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
private fun TvSearchResults(
    results: TvSearchState.Results,
    onOpenPodcast: (String) -> Unit,
    onPlayEpisode: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    onOpenEpisodeActions: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    restoreFocusTrigger: Int,
) {
    val podcastsRowFocusRequester = remember { FocusRequester() }
    var isInitialComposition by remember { mutableStateOf(true) }
    LaunchedEffect(restoreFocusTrigger) {
        if (isInitialComposition) {
            isInitialComposition = false
        } else {
            runCatching { podcastsRowFocusRequester.requestFocus() }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (results.podcasts.isNotEmpty()) {
            tvSearchPodcastsRow(
                podcasts = results.podcasts,
                onOpenPodcast = onOpenPodcast,
                focusRequester = podcastsRowFocusRequester,
            )
        }
        if (results.episodes.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                TvSearchSectionTitle(stringResource(LR.string.episodes))
            }
            items(
                items = results.episodes,
                key = ImprovedSearchResultItem.EpisodeItem::uuid,
            ) { episode ->
                TvSearchEpisodeRow(
                    episode = episode,
                    onClick = { onPlayEpisode(episode) },
                    onOpenActions = { onOpenEpisodeActions(episode) },
                    modifier = Modifier.padding(ContentPadding),
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

private fun LazyListScope.tvSearchPodcastsRow(
    podcasts: List<ImprovedSearchResultItem.PodcastItem>,
    onOpenPodcast: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    item {
        TvRow(
            title = stringResource(LR.string.podcasts),
            items = podcasts,
            contentPadding = ContentPadding,
            key = ImprovedSearchResultItem.PodcastItem::uuid,
            focusRequester = focusRequester,
        ) { podcast ->
            TvPodcastTile(
                artworkUrl = PodcastImage.getMediumArtworkUrl(podcast.uuid),
                podcastTitle = podcast.title,
                onClick = { onOpenPodcast(podcast.uuid) },
                imageModifier = Modifier.width(TvPodcastTileDefaults.RowImageWidth),
            )
        }
    }
}

@Composable
private fun TvSearchSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.tvTypography.title3,
        color = MaterialTheme.tvColors.textPrimary,
        modifier = Modifier.padding(start = 48.dp, bottom = 10.dp),
    )
}

@Composable
private fun TvSearchMessage(
    title: String,
    subtitle: String,
) {
    TvEmptyState(
        title = title,
        subtitle = subtitle,
        modifier = Modifier.fillMaxSize(),
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchScreenPreview() {
    TvTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.tvColors.backgroundSunken)) {
            TvSearchContent(
                query = "",
                searchState = TvSearchState.Idle,
                categories = listOf(
                    DiscoverCategory(id = 1, name = "Comedy", icon = "", source = ""),
                    DiscoverCategory(id = 2, name = "True Crime", icon = "", source = ""),
                    DiscoverCategory(id = 3, name = "Fiction", icon = "", source = ""),
                ),
                discoverRows = emptyList(),
                onQueryChange = {},
                onOpenPodcast = {},
                onPlayEpisode = {},
                onOpenEpisodeActions = {},
            )
        }
    }
}
