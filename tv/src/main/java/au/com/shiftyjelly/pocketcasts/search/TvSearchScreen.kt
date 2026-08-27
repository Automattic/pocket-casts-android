package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import au.com.shiftyjelly.pocketcasts.component.TvFolderCard
import au.com.shiftyjelly.pocketcasts.component.TvPodcastGridScaffold
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTile
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTileDefaults
import au.com.shiftyjelly.pocketcasts.component.TvRow
import au.com.shiftyjelly.pocketcasts.component.TvTile
import au.com.shiftyjelly.pocketcasts.component.tvFocusInactiveWhen
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.discover.TvCategoryPodcastsScreen
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverEpisode
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverPodcast
import au.com.shiftyjelly.pocketcasts.discover.TvDiscoverRow
import au.com.shiftyjelly.pocketcasts.discover.TvOpenedCategory
import au.com.shiftyjelly.pocketcasts.discover.TvOpenedCategorySaver
import au.com.shiftyjelly.pocketcasts.discover.tvDiscoverRow
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.podcasts.TvFolderDetailScreen
import au.com.shiftyjelly.pocketcasts.podcasts.TvPodcastDetailsScreen
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.TvTopBarHeight
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import java.util.Date
import kotlin.time.Duration.Companion.seconds
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private val ContentHorizontalPadding = 48.dp
private val ContentPadding = PaddingValues(horizontal = ContentHorizontalPadding)
private const val SEARCH_ROW_LIMIT = 10
private const val FOLDER_COVER_COUNT = 4
private val SearchEpisodeCardWidth = 360.dp
private const val EPISODE_GRID_COLUMNS = 2

private data class SearchOpenedFolder(val uuid: String, val name: String)

private val SearchOpenedFolderSaver = listSaver<SearchOpenedFolder?, String>(
    save = { folder -> folder?.let { listOf(it.uuid, it.name) } ?: emptyList() },
    restore = { saved -> saved.takeIf { it.size == 2 }?.let { (uuid, name) -> SearchOpenedFolder(uuid, name) } },
)

@Composable
fun TvSearchScreen(
    modifier: Modifier = Modifier,
    viewModel: TvSearchViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val hasFolderResults by viewModel.hasFolderResults.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val discoverRows by viewModel.discoverRows.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val actionsEpisode by viewModel.actionsEpisode.collectAsStateWithLifecycle()

    var openedPodcastUuid by rememberSaveable { mutableStateOf<String?>(null) }
    var openedCategory by rememberSaveable(stateSaver = TvOpenedCategorySaver) { mutableStateOf<TvOpenedCategory?>(null) }
    var openedFolder by rememberSaveable(stateSaver = SearchOpenedFolderSaver) { mutableStateOf<SearchOpenedFolder?>(null) }
    var detailsEpisode by remember { mutableStateOf<PodcastEpisode?>(null) }
    var restoreFocusTrigger by remember { mutableIntStateOf(0) }
    var categoryRestoreTrigger by remember { mutableIntStateOf(0) }
    var folderRestoreTrigger by remember { mutableIntStateOf(0) }
    val podcastUuid = openedPodcastUuid
    val category = openedCategory
    val folder = openedFolder

    val openNowPlaying = LocalOpenNowPlaying.current
    val toastHostState = LocalTvToastHostState.current
    val playFailedMessage = stringResource(LR.string.error_generic_message)

    CallOnce { viewModel.trackSearchShown() }

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
            filter = filter,
            hasFolderResults = hasFolderResults,
            categories = categories,
            discoverRows = discoverRows,
            onQueryChange = viewModel::onQueryChange,
            onFilterSelect = viewModel::onFilterSelected,
            onOpenFolder = { openedFolder = SearchOpenedFolder(it.folder.uuid, it.folder.name) },
            onPodcastResultClick = { podcast ->
                viewModel.trackPodcastResultTapped(podcast)
                openedPodcastUuid = podcast.uuid
            },
            onDiscoverPodcastClick = { row, podcast ->
                viewModel.trackDiscoverPodcastTapped(row, podcast)
                openedPodcastUuid = podcast.uuid
            },
            onDiscoverEpisodePodcastClick = { row, episode ->
                viewModel.trackDiscoverEpisodePodcastTapped(row, episode)
                openedPodcastUuid = episode.podcastUuid
            },
            onDiscoverCategoryClick = { discoverCategory, index ->
                viewModel.trackCategoryPillTapped(discoverCategory, index)
                openedCategory = TvOpenedCategory(discoverCategory.id, discoverCategory.name, discoverCategory.source)
            },
            onDiscoverListImpression = viewModel::trackDiscoverListShown,
            onPlayEpisode = { episode ->
                viewModel.trackEpisodeResultTapped(episode.uuid)
                viewModel.playEpisode(episode)
            },
            onOpenEpisodeActions = viewModel::openEpisodeActions,
            history = history,
            onHistorySelect = viewModel::selectHistoryItem,
            suggestions = suggestions,
            onSuggestionSelect = viewModel::selectSuggestion,
            onSaveSearch = viewModel::saveSearchTerm,
            loadCategoryCovers = viewModel::categoryCoverUrls,
            restoreFocusTrigger = restoreFocusTrigger,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = TvTopBarHeight)
                .tvFocusInactiveWhen(podcastUuid != null || category != null || folder != null),
        )
        TvDetailOverlay(
            target = folder,
            onBack = { openedFolder = null },
            modifier = Modifier.tvFocusInactiveWhen(podcastUuid != null),
            onHide = { restoreFocusTrigger++ },
        ) { openFolder ->
            TvFolderDetailScreen(
                folderUuid = openFolder.uuid,
                folderName = openFolder.name,
                getFolderPodcasts = viewModel::folderPodcasts,
                onOpenPodcast = { openedPodcastUuid = it },
                onClose = { openedFolder = null },
                onFolderImpression = {},
                restoreFocusTrigger = folderRestoreTrigger,
            )
        }
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
            onHide = {
                when {
                    openedCategory != null -> categoryRestoreTrigger++
                    openedFolder != null -> folderRestoreTrigger++
                    else -> restoreFocusTrigger++
                }
            },
        ) { uuid ->
            TvPodcastDetailsScreen(
                podcastUuid = uuid,
                onClose = { openedPodcastUuid = null },
            )
        }
        actionsEpisode?.let { episode ->
            TvEpisodeActionsModal(
                episode = episode,
                actionContext = TvEpisodeActionContext.SearchResults,
                onDismissRequest = viewModel::dismissEpisodeActions,
                onShowEpisodeDetails = {
                    detailsEpisode = episode
                    viewModel.dismissEpisodeActions()
                },
                onGoToPodcast = {
                    viewModel.trackEpisodeResultTapped(episode.uuid)
                    viewModel.dismissEpisodeActions()
                    openedPodcastUuid = episode.podcastUuid
                },
            )
        }
        detailsEpisode?.let { episode ->
            TvEpisodeInfoModal(
                episode = episode,
                actionContext = TvEpisodeActionContext.SearchResults,
                onDismissRequest = { detailsEpisode = null },
            )
        }
    }
}

@Composable
private fun TvSearchContent(
    query: String,
    searchState: TvSearchState,
    filter: TvSearchFilter,
    hasFolderResults: Boolean,
    categories: List<DiscoverCategory>,
    discoverRows: List<TvDiscoverRow>,
    onQueryChange: (String) -> Unit,
    onFilterSelect: (TvSearchFilter) -> Unit,
    onOpenFolder: (FolderItem.Folder) -> Unit,
    onPodcastResultClick: (ImprovedSearchResultItem.PodcastItem) -> Unit,
    onDiscoverPodcastClick: (TvDiscoverRow, TvDiscoverPodcast) -> Unit,
    onDiscoverEpisodePodcastClick: (TvDiscoverRow, TvDiscoverEpisode) -> Unit,
    onDiscoverCategoryClick: (DiscoverCategory, Int) -> Unit,
    onPlayEpisode: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    onOpenEpisodeActions: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    modifier: Modifier = Modifier,
    onDiscoverListImpression: (TvDiscoverRow) -> Unit = {},
    history: List<String> = emptyList(),
    onHistorySelect: (String) -> Unit = {},
    suggestions: List<String> = emptyList(),
    onSuggestionSelect: (String) -> Unit = {},
    onSaveSearch: (String) -> Unit = {},
    loadCategoryCovers: (suspend (DiscoverCategory) -> List<String>)? = null,
    restoreFocusTrigger: Int = 0,
) {
    val searchFieldFocusRequester = remember { FocusRequester() }
    var isEditing by remember { mutableStateOf(false) }
    var suggestionsFocused by remember { mutableStateOf(false) }
    val showSuggestions = (isEditing || suggestionsFocused) && suggestions.isNotEmpty()
    LaunchedEffect(Unit) {
        withFrameNanos {}
        runCatching { searchFieldFocusRequester.requestFocus() }
    }
    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(ContentPadding)) {
            Spacer(modifier = Modifier.height(40.dp))
            TvSearchField(
                query = query,
                onQueryChange = onQueryChange,
                onEditingChange = { editing ->
                    if (!editing && isEditing && query.isNotBlank()) {
                        onSaveSearch(query)
                    }
                    isEditing = editing
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(searchFieldFocusRequester),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (showSuggestions) {
            TvSearchSuggestions(
                suggestions = suggestions,
                onSuggestionSelect = onSuggestionSelect,
                modifier = Modifier.onFocusChanged { suggestionsFocused = it.hasFocus },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        val filters = remember(hasFolderResults) { TvSearchFilter.entries.filter { it != TvSearchFilter.Folders || hasFolderResults } }
        val effectiveFilter = if (filter in filters) filter else TvSearchFilter.TopResults

        if (searchState !is TvSearchState.Idle) {
            TvSearchFilters(
                selected = effectiveFilter,
                onFilterSelect = onFilterSelect,
                filters = filters,
                modifier = Modifier.padding(ContentPadding),
                upFocusRequester = searchFieldFocusRequester,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when (searchState) {
                is TvSearchState.Idle -> TvSearchIdle(
                    history = history,
                    categories = categories,
                    discoverRows = discoverRows,
                    onHistorySelect = onHistorySelect,
                    onDiscoverPodcastClick = onDiscoverPodcastClick,
                    onDiscoverEpisodePodcastClick = onDiscoverEpisodePodcastClick,
                    onDiscoverCategoryClick = onDiscoverCategoryClick,
                    onDiscoverListImpression = onDiscoverListImpression,
                    loadCategoryCovers = loadCategoryCovers,
                    restoreFocusTrigger = restoreFocusTrigger,
                )

                is TvSearchState.Searching -> TvSearchLoading()

                is TvSearchState.Error -> TvSearchMessage(
                    title = stringResource(LR.string.error_generic_message),
                    subtitle = stringResource(LR.string.tv_search_error_subtitle),
                    actionLabel = stringResource(LR.string.retry),
                    onAction = { onQueryChange(query) },
                )

                is TvSearchState.NoResults -> TvSearchMessage(
                    title = stringResource(LR.string.tv_search_no_results_for_title, query.trim()),
                    subtitle = stringResource(LR.string.tv_search_no_results_subtitle),
                )

                is TvSearchState.Results -> TvSearchResults(
                    results = searchState,
                    filter = effectiveFilter,
                    searchTerm = query.trim(),
                    onPodcastResultClick = onPodcastResultClick,
                    onOpenFolder = onOpenFolder,
                    onPlayEpisode = onPlayEpisode,
                    onOpenEpisodeActions = onOpenEpisodeActions,
                    restoreFocusTrigger = restoreFocusTrigger,
                )
            }
        }
    }
}

@Composable
private fun TvSearchIdle(
    history: List<String>,
    categories: List<DiscoverCategory>,
    discoverRows: List<TvDiscoverRow>,
    onHistorySelect: (String) -> Unit,
    onDiscoverPodcastClick: (TvDiscoverRow, TvDiscoverPodcast) -> Unit,
    onDiscoverEpisodePodcastClick: (TvDiscoverRow, TvDiscoverEpisode) -> Unit,
    onDiscoverCategoryClick: (DiscoverCategory, Int) -> Unit,
    onDiscoverListImpression: (TvDiscoverRow) -> Unit,
    restoreFocusTrigger: Int,
    loadCategoryCovers: (suspend (DiscoverCategory) -> List<String>)? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (history.isNotEmpty()) {
            TvRow(
                title = stringResource(LR.string.tv_search_recent),
                items = history,
                contentPadding = ContentPadding,
                key = { it },
            ) { term ->
                TvTile(onClick = { onHistorySelect(term) }) {
                    Text(
                        text = term,
                        style = MaterialTheme.tvTypography.body,
                        color = MaterialTheme.tvColors.textPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        TvSearchDiscover(
            categories = categories,
            discoverRows = discoverRows,
            onDiscoverPodcastClick = onDiscoverPodcastClick,
            onDiscoverEpisodePodcastClick = onDiscoverEpisodePodcastClick,
            onDiscoverCategoryClick = onDiscoverCategoryClick,
            onDiscoverListImpression = onDiscoverListImpression,
            loadCategoryCovers = loadCategoryCovers,
            restoreFocusTrigger = restoreFocusTrigger,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TvSearchDiscover(
    categories: List<DiscoverCategory>,
    discoverRows: List<TvDiscoverRow>,
    onDiscoverPodcastClick: (TvDiscoverRow, TvDiscoverPodcast) -> Unit,
    onDiscoverEpisodePodcastClick: (TvDiscoverRow, TvDiscoverEpisode) -> Unit,
    onDiscoverCategoryClick: (DiscoverCategory, Int) -> Unit,
    onDiscoverListImpression: (TvDiscoverRow) -> Unit,
    restoreFocusTrigger: Int,
    modifier: Modifier = Modifier,
    loadCategoryCovers: (suspend (DiscoverCategory) -> List<String>)? = null,
) {
    val categoryOffset = if (categories.isNotEmpty()) 1 else 0
    val rowCount = categoryOffset + discoverRows.size
    val rowFocusRequesters = remember(rowCount) { List(rowCount) { FocusRequester() } }
    var lastFocusedRowIndex by rememberSaveable(rowCount) { mutableIntStateOf(0) }

    var isInitialComposition by remember { mutableStateOf(true) }
    LaunchedEffect(restoreFocusTrigger) {
        if (isInitialComposition) {
            isInitialComposition = false
        } else {
            runCatching { rowFocusRequesters.getOrNull(lastFocusedRowIndex)?.requestFocus() }
        }
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
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
                    focusRequester = rowFocusRequesters.getOrNull(0),
                    modifier = Modifier.onFocusChanged { if (it.hasFocus) lastFocusedRowIndex = 0 },
                ) { category ->
                    val categoryIndex = remember(category.id, categories) {
                        categories.indexOfFirst { it.id == category.id }
                    }
                    TvCategoryTile(
                        category = category,
                        onClick = { onDiscoverCategoryClick(category, categoryIndex) },
                        colorIndex = categoryIndex,
                        loadCoverUrls = loadCategoryCovers?.let { load -> { load(category) } },
                    )
                }
            }
        }

        discoverRows.forEachIndexed { index, row ->
            val rowIndex = categoryOffset + index
            item { Spacer(modifier = Modifier.height(24.dp)) }
            tvDiscoverRow(
                row = row,
                onPodcastClick = onDiscoverPodcastClick,
                onEpisodePlay = { _, _ -> }, // TODO: wire discover-feed episode playback in search
                onEpisodePodcastClick = onDiscoverEpisodePodcastClick,
                onCategoryClick = onDiscoverCategoryClick,
                onListImpression = onDiscoverListImpression,
                contentPadding = ContentPadding,
                focusRequester = rowFocusRequesters.getOrNull(rowIndex),
                loadCategoryCovers = loadCategoryCovers,
                modifier = Modifier.onFocusChanged { if (it.hasFocus) lastFocusedRowIndex = rowIndex },
            )
        }

        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
private fun TvSearchResults(
    results: TvSearchState.Results,
    filter: TvSearchFilter,
    searchTerm: String,
    onPodcastResultClick: (ImprovedSearchResultItem.PodcastItem) -> Unit,
    onOpenFolder: (FolderItem.Folder) -> Unit,
    onPlayEpisode: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    onOpenEpisodeActions: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    restoreFocusTrigger: Int,
) {
    when (filter) {
        TvSearchFilter.TopResults -> TvSearchTopResults(
            podcasts = results.podcasts,
            episodes = results.episodes,
            folders = results.folders,
            onPodcastResultClick = onPodcastResultClick,
            onOpenFolder = onOpenFolder,
            onPlayEpisode = onPlayEpisode,
            onOpenEpisodeActions = onOpenEpisodeActions,
            restoreFocusTrigger = restoreFocusTrigger,
        )

        TvSearchFilter.Podcasts -> if (results.podcasts.isEmpty()) {
            if (results.isPartial) {
                TvSearchLoading()
            } else {
                TvSearchMessage(
                    title = stringResource(LR.string.tv_search_no_results_for_title, searchTerm),
                    subtitle = stringResource(LR.string.tv_search_no_results_subtitle),
                )
            }
        } else {
            TvPodcastGridScaffold(
                itemKeys = results.podcasts.map(ImprovedSearchResultItem.PodcastItem::uuid),
                modifier = Modifier.fillMaxSize(),
                horizontalContentPadding = ContentHorizontalPadding,
                restoreFocusTrigger = restoreFocusTrigger,
            ) { index, itemModifier ->
                val podcast = results.podcasts[index]
                TvPodcastTile(
                    artworkUrl = PodcastImage.getMediumArtworkUrl(podcast.uuid),
                    podcastTitle = podcast.title,
                    onClick = { onPodcastResultClick(podcast) },
                    imageModifier = Modifier.fillMaxWidth(),
                    modifier = itemModifier,
                )
            }
        }

        TvSearchFilter.Episodes -> if (results.episodes.isEmpty()) {
            if (results.isPartial) {
                TvSearchLoading()
            } else {
                TvSearchMessage(
                    title = stringResource(LR.string.tv_search_no_results_for_title, searchTerm),
                    subtitle = stringResource(LR.string.tv_search_no_results_subtitle),
                )
            }
        } else {
            TvSearchEpisodeGrid(
                episodes = results.episodes,
                onPlayEpisode = onPlayEpisode,
                onOpenEpisodeActions = onOpenEpisodeActions,
                restoreFocusTrigger = restoreFocusTrigger,
            )
        }

        TvSearchFilter.Folders -> if (results.folders.isEmpty()) {
            if (results.isPartial) {
                TvSearchLoading()
            } else {
                TvSearchMessage(
                    title = stringResource(LR.string.tv_search_no_results_for_title, searchTerm),
                    subtitle = stringResource(LR.string.tv_search_no_results_subtitle),
                )
            }
        } else {
            TvPodcastGridScaffold(
                itemKeys = results.folders.map { it.folder.uuid },
                modifier = Modifier.fillMaxSize(),
                horizontalContentPadding = ContentHorizontalPadding,
                restoreFocusTrigger = restoreFocusTrigger,
            ) { index, itemModifier ->
                val folderItem = results.folders[index]
                TvFolderCard(
                    folder = folderItem.folder,
                    coverUrls = folderItem.podcasts.take(FOLDER_COVER_COUNT).map { PodcastImage.getMediumArtworkUrl(it.uuid) },
                    onClick = { onOpenFolder(folderItem) },
                    modifier = itemModifier,
                )
            }
        }
    }
}

@Composable
private fun TvSearchTopResults(
    podcasts: List<ImprovedSearchResultItem.PodcastItem>,
    episodes: List<ImprovedSearchResultItem.EpisodeItem>,
    folders: List<FolderItem.Folder>,
    onPodcastResultClick: (ImprovedSearchResultItem.PodcastItem) -> Unit,
    onOpenFolder: (FolderItem.Folder) -> Unit,
    onPlayEpisode: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    onOpenEpisodeActions: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    restoreFocusTrigger: Int,
) {
    val restoreFocusRequester = remember { FocusRequester() }
    var isInitialComposition by remember { mutableStateOf(true) }
    LaunchedEffect(restoreFocusTrigger) {
        if (isInitialComposition) {
            isInitialComposition = false
        } else {
            runCatching { restoreFocusRequester.requestFocus() }
        }
    }

    val featured = episodes.filter { it.hasVideo }.take(SEARCH_ROW_LIMIT)
    val otherEpisodes = episodes.filterNot { it.hasVideo }.take(SEARCH_ROW_LIMIT)
    val topFolders = folders.take(SEARCH_ROW_LIMIT)
    val topPodcasts = podcasts.take(SEARCH_ROW_LIMIT)
    val featuredFirst = featured.isNotEmpty()
    val episodesFirst = !featuredFirst && otherEpisodes.isNotEmpty()
    val foldersFirst = !featuredFirst && !episodesFirst && topFolders.isNotEmpty()
    val podcastsFirst = !featuredFirst && !episodesFirst && !foldersFirst

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        if (featured.isNotEmpty()) {
            item {
                TvSearchEpisodeCarousel(
                    title = stringResource(LR.string.tv_search_featured),
                    episodes = featured,
                    onPlayEpisode = onPlayEpisode,
                    onOpenEpisodeActions = onOpenEpisodeActions,
                    focusRequester = restoreFocusRequester.takeIf { featuredFirst },
                )
            }
        }
        if (otherEpisodes.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(24.dp)) }
            item {
                TvSearchEpisodeCarousel(
                    title = stringResource(LR.string.episodes),
                    episodes = otherEpisodes,
                    onPlayEpisode = onPlayEpisode,
                    onOpenEpisodeActions = onOpenEpisodeActions,
                    focusRequester = restoreFocusRequester.takeIf { episodesFirst },
                )
            }
        }
        if (topFolders.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(24.dp)) }
            tvSearchFoldersRow(
                folders = topFolders,
                onOpenFolder = onOpenFolder,
                focusRequester = restoreFocusRequester.takeIf { foldersFirst },
            )
        }
        if (topPodcasts.isNotEmpty()) {
            item { Spacer(modifier = Modifier.height(24.dp)) }
            tvSearchPodcastsRow(
                podcasts = topPodcasts,
                onPodcastResultClick = onPodcastResultClick,
                focusRequester = restoreFocusRequester.takeIf { podcastsFirst },
            )
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

private fun LazyListScope.tvSearchFoldersRow(
    folders: List<FolderItem.Folder>,
    onOpenFolder: (FolderItem.Folder) -> Unit,
    focusRequester: FocusRequester?,
) {
    item {
        TvRow(
            title = stringResource(LR.string.folders),
            items = folders,
            contentPadding = ContentPadding,
            key = { it.folder.uuid },
            focusRequester = focusRequester,
        ) { folderItem ->
            TvFolderCard(
                folder = folderItem.folder,
                coverUrls = folderItem.podcasts.take(FOLDER_COVER_COUNT).map { PodcastImage.getMediumArtworkUrl(it.uuid) },
                onClick = { onOpenFolder(folderItem) },
                modifier = Modifier.width(TvPodcastTileDefaults.RowImageWidth),
            )
        }
    }
}

@Composable
private fun TvSearchEpisodeCarousel(
    title: String,
    episodes: List<ImprovedSearchResultItem.EpisodeItem>,
    onPlayEpisode: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    onOpenEpisodeActions: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    focusRequester: FocusRequester?,
) {
    TvRow(
        title = title,
        items = episodes,
        contentPadding = ContentPadding,
        key = ImprovedSearchResultItem.EpisodeItem::uuid,
        focusRequester = focusRequester,
    ) { episode ->
        TvSearchEpisodeCard(
            episode = episode,
            onClick = { onPlayEpisode(episode) },
            onLongClick = { onOpenEpisodeActions(episode) },
            modifier = Modifier.width(SearchEpisodeCardWidth),
        )
    }
}

@Composable
private fun TvSearchEpisodeGrid(
    episodes: List<ImprovedSearchResultItem.EpisodeItem>,
    onPlayEpisode: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    onOpenEpisodeActions: (ImprovedSearchResultItem.EpisodeItem) -> Unit,
    restoreFocusTrigger: Int,
) {
    val gridState = rememberLazyGridState()
    val focusRequesters = remember(episodes.size) { List(episodes.size) { FocusRequester() } }
    val gridFocusRequester = remember { FocusRequester() }
    var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }

    var isInitialComposition by remember { mutableStateOf(true) }
    LaunchedEffect(restoreFocusTrigger) {
        if (isInitialComposition) {
            isInitialComposition = false
        } else {
            runCatching { gridFocusRequester.requestFocus() }
        }
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(EPISODE_GRID_COLUMNS),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 48.dp, end = 48.dp, bottom = 40.dp),
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(gridFocusRequester)
            .focusGroup()
            .focusProperties {
                onEnter = {
                    val visible = gridState.layoutInfo.visibleItemsInfo
                    val target = episodes.indexOfFirst { it.uuid == lastFocusedKey }
                        .takeIf { index -> index >= 0 && visible.any { it.index == index } }
                        ?: visible.firstOrNull()?.index
                    target?.let { runCatching { focusRequesters.getOrNull(it)?.requestFocus() } }
                }
            },
    ) {
        gridItemsIndexed(episodes, key = { _, episode -> episode.uuid }) { index, episode ->
            TvSearchEpisodeCard(
                episode = episode,
                onClick = { onPlayEpisode(episode) },
                onLongClick = { onOpenEpisodeActions(episode) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequesters[index])
                    .onFocusChanged { focusState ->
                        if (focusState.hasFocus) {
                            lastFocusedKey = episode.uuid
                        }
                    },
            )
        }
    }
}

private fun LazyListScope.tvSearchPodcastsRow(
    podcasts: List<ImprovedSearchResultItem.PodcastItem>,
    onPodcastResultClick: (ImprovedSearchResultItem.PodcastItem) -> Unit,
    focusRequester: FocusRequester?,
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
                onClick = { onPodcastResultClick(podcast) },
                imageModifier = Modifier.width(TvPodcastTileDefaults.RowImageWidth),
            )
        }
    }
}

@Composable
private fun TvSearchMessage(
    title: String,
    subtitle: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    TvEmptyState(
        title = title,
        subtitle = subtitle,
        actionLabel = actionLabel,
        onAction = onAction,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun TvSearchSuggestions(
    suggestions: List<String>,
    onSuggestionSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = ContentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(suggestions) { term ->
            TvTile(onClick = { onSuggestionSelect(term) }) {
                Text(
                    text = term,
                    style = MaterialTheme.tvTypography.body,
                    color = MaterialTheme.tvColors.textPrimary,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun TvSearchLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LoadingView(
            color = MaterialTheme.tvColors.textPrimary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(LR.string.tv_search_searching),
            style = MaterialTheme.tvTypography.body,
            color = MaterialTheme.tvColors.textSecondary,
        )
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchScreenPreview() {
    TvTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.tvColors.backgroundSunken)) {
            TvSearchContent(
                query = "",
                searchState = TvSearchState.Idle,
                filter = TvSearchFilter.TopResults,
                hasFolderResults = false,
                categories = listOf(
                    DiscoverCategory(id = 1, name = "Comedy", icon = "", source = ""),
                    DiscoverCategory(id = 2, name = "True Crime", icon = "", source = ""),
                    DiscoverCategory(id = 3, name = "Fiction", icon = "", source = ""),
                ),
                discoverRows = emptyList(),
                onQueryChange = {},
                onFilterSelect = {},
                onOpenFolder = {},
                onPodcastResultClick = {},
                onDiscoverPodcastClick = { _, _ -> },
                onDiscoverEpisodePodcastClick = { _, _ -> },
                onDiscoverCategoryClick = { _, _ -> },
                onPlayEpisode = {},
                onOpenEpisodeActions = {},
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchIdleWithHistoryPreview() {
    TvTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.tvColors.backgroundSunken)) {
            TvSearchContent(
                query = "",
                searchState = TvSearchState.Idle,
                filter = TvSearchFilter.TopResults,
                hasFolderResults = false,
                categories = emptyList(),
                discoverRows = emptyList(),
                onQueryChange = {},
                onFilterSelect = {},
                onPodcastResultClick = {},
                onDiscoverPodcastClick = { _, _ -> },
                onDiscoverEpisodePodcastClick = { _, _ -> },
                onDiscoverCategoryClick = { _, _ -> },
                onPlayEpisode = {},
                onOpenEpisodeActions = {},
                onOpenFolder = {},
                history = listOf("Freakonomics", "Business Daily", "Science Weekly"),
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchResultsPreview() {
    TvTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.tvColors.backgroundSunken)) {
            TvSearchContent(
                query = "business",
                searchState = TvSearchState.Results(
                    podcasts = List(4) {
                        ImprovedSearchResultItem.PodcastItem(
                            uuid = "podcast-$it",
                            title = "Business Daily $it",
                            author = "BBC",
                            isFollowed = it == 0,
                        )
                    },
                    episodes = List(3) {
                        ImprovedSearchResultItem.EpisodeItem(
                            uuid = "episode-$it",
                            title = "The real cost of sugar and how it shapes the food we eat",
                            podcastUuid = "podcast-$it",
                            podcastTitle = "Business Daily",
                            publishedDate = Date(0),
                            duration = 1440.seconds,
                            hasVideo = it == 0,
                        )
                    },
                    folders = listOf(
                        FolderItem.Folder(
                            folder = Folder(
                                uuid = "folder-1",
                                name = "Business & Finance",
                                color = 3,
                                addedDate = Date(0),
                                sortPosition = 0,
                                podcastsSortType = PodcastsSortType.NAME_A_TO_Z,
                                deleted = false,
                                syncModified = 0,
                            ),
                            podcasts = List(4) { Podcast(uuid = "folder-podcast-$it") },
                        ),
                    ),
                ),
                filter = TvSearchFilter.TopResults,
                hasFolderResults = true,
                categories = emptyList(),
                discoverRows = emptyList(),
                onQueryChange = {},
                onFilterSelect = {},
                onPodcastResultClick = {},
                onDiscoverPodcastClick = { _, _ -> },
                onDiscoverEpisodePodcastClick = { _, _ -> },
                onDiscoverCategoryClick = { _, _ -> },
                onPlayEpisode = {},
                onOpenEpisodeActions = {},
                onOpenFolder = {},
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchSuggestionsPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken).padding(48.dp)) {
            TvSearchSuggestions(
                suggestions = listOf("business daily", "business wars", "business insider"),
                onSuggestionSelect = {},
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchLoadingPreview() {
    TvTheme {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.tvColors.backgroundSunken)) {
            TvSearchLoading()
        }
    }
}
