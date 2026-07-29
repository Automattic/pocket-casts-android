package au.com.shiftyjelly.pocketcasts.podcasts

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvEmptyState
import au.com.shiftyjelly.pocketcasts.component.TvFolderCard
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTile
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvYourPodcastsScreen(
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvYourPodcastsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var openedFolder by rememberSaveable(stateSaver = OpenedFolderSaver) { mutableStateOf<OpenedFolder?>(null) }

    val folder = openedFolder
    if (folder != null) {
        BackHandler {
            openedFolder = null
        }
        TvFolderDetailScreen(
            folderUuid = folder.uuid,
            folderName = folder.name,
            getFolderPodcasts = viewModel::folderPodcasts,
            onClose = { openedFolder = null },
            modifier = modifier,
        )
    } else {
        TvYourPodcastsContent(
            uiState = uiState,
            getFolderCoverUuids = viewModel::folderCoverUuids,
            onNavigateToHome = onNavigateToHome,
            onOpenFolder = { openedFolder = OpenedFolder(it.uuid, it.name) },
            modifier = modifier,
        )
    }
}

private data class OpenedFolder(val uuid: String, val name: String)

private val OpenedFolderSaver = listSaver<OpenedFolder?, String>(
    save = { folder -> folder?.let { listOf(it.uuid, it.name) }.orEmpty() },
    restore = { saved -> saved.takeIf { it.size == 2 }?.let { (uuid, name) -> OpenedFolder(uuid, name) } },
)

@Composable
private fun TvYourPodcastsContent(
    uiState: TvYourPodcastsUiState,
    getFolderCoverUuids: suspend (String) -> List<String>,
    onNavigateToHome: () -> Unit,
    onOpenFolder: (Folder) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = uiState,
        transitionSpec = { fadeIn(tween(durationMillis = 300)) togetherWith fadeOut(tween(durationMillis = 300)) },
        contentKey = { state ->
            when (state) {
                is TvYourPodcastsUiState.Loading -> "loading"
                is TvYourPodcastsUiState.Empty -> "empty"
                is TvYourPodcastsUiState.Loaded -> "content"
            }
        },
        label = "TvYourPodcastsContent",
        modifier = modifier,
    ) { state ->
        when (state) {
            is TvYourPodcastsUiState.Loading -> LoadingView(color = Color.White, modifier = Modifier.fillMaxSize())

            is TvYourPodcastsUiState.Empty -> TvEmptyState(
                title = stringResource(LR.string.tv_your_podcasts_empty_title),
                subtitle = stringResource(LR.string.tv_your_podcasts_empty_subtitle),
                actionLabel = stringResource(LR.string.tv_your_podcasts_empty_action_title),
                onAction = onNavigateToHome,
                modifier = Modifier.fillMaxSize(),
            )

            is TvYourPodcastsUiState.Loaded -> TvYourPodcastsGrid(
                items = state.items,
                getFolderCoverUuids = getFolderCoverUuids,
                onOpenFolder = onOpenFolder,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TvYourPodcastsGrid(
    items: List<FolderItem>,
    getFolderCoverUuids: suspend (String) -> List<String>,
    onOpenFolder: (Folder) -> Unit,
    modifier: Modifier = Modifier,
) {
    TvPodcastGridScaffold(
        title = stringResource(LR.string.tv_tab_your_podcasts),
        itemKeys = items.map(FolderItem::uuid),
        modifier = modifier,
    ) { index, itemModifier ->
        when (val item = items[index]) {
            is FolderItem.Podcast -> TvPodcastTile(
                artworkUrl = PodcastImage.getMediumArtworkUrl(item.podcast.uuid),
                podcastTitle = item.podcast.title,
                onClick = {},
                imageModifier = Modifier.fillMaxWidth(),
                modifier = itemModifier,
            )

            is FolderItem.Folder -> FolderGridItem(
                folder = item.folder,
                getFolderCoverUuids = getFolderCoverUuids,
                onOpenFolder = onOpenFolder,
                modifier = itemModifier,
            )
        }
    }
}

@Composable
private fun FolderGridItem(
    folder: Folder,
    getFolderCoverUuids: suspend (String) -> List<String>,
    onOpenFolder: (Folder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var coverUrls by remember(folder.uuid) { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(folder.uuid, getFolderCoverUuids) {
        coverUrls = getFolderCoverUuids(folder.uuid).map(PodcastImage::getMediumArtworkUrl)
    }
    TvFolderCard(
        folder = folder,
        coverUrls = coverUrls,
        onClick = { onOpenFolder(folder) },
        modifier = modifier,
    )
}

@Composable
internal fun TvPodcastGridScaffold(
    title: String,
    itemKeys: List<Any>,
    modifier: Modifier = Modifier,
    autoFocusFirstItem: Boolean = false,
    itemContent: @Composable (index: Int, itemModifier: Modifier) -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = TvTextStyles.ScreenTitle,
            color = Color.White,
            modifier = Modifier.padding(start = 32.dp, top = 8.dp, bottom = 10.dp),
        )
        val gridState = rememberLazyGridState()
        var lastFocusedKey by rememberSaveable { mutableStateOf<String?>(null) }
        val focusRequesters = remember(itemKeys.size) { List(itemKeys.size) { FocusRequester() } }

        if (autoFocusFirstItem) {
            LaunchedEffect(focusRequesters) {
                focusRequesters.firstOrNull()?.requestFocus()
            }
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(GRID_COLUMNS),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(start = 32.dp, top = 16.dp, end = 32.dp, bottom = 32.dp),
            modifier = Modifier
                .focusGroup()
                .focusProperties {
                    onEnter = {
                        val visible = gridState.layoutInfo.visibleItemsInfo
                        val target = itemKeys.indexOfFirst { it.toString() == lastFocusedKey }
                            .takeIf { index -> index >= 0 && visible.any { it.index == index } }
                            ?: visible.firstOrNull()?.index
                        target?.let { runCatching { focusRequesters.getOrNull(it)?.requestFocus() } }
                    }
                },
        ) {
            items(
                count = itemKeys.size,
                key = { index -> itemKeys[index] },
            ) { index ->
                itemContent(
                    index,
                    Modifier
                        .focusRequester(focusRequesters[index])
                        .onFocusChanged { focusState ->
                            if (focusState.hasFocus) {
                                lastFocusedKey = itemKeys[index].toString()
                            }
                        },
                )
            }
        }
    }
}

private const val GRID_COLUMNS = 6

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvYourPodcastsGridPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvYourPodcastsContent(
                    uiState = TvYourPodcastsUiState.Loaded(
                        items = buildList {
                            add(
                                FolderItem.Folder(
                                    folder = Folder(
                                        uuid = "folder",
                                        name = "Tech & Science",
                                        color = 3,
                                        addedDate = Date(0),
                                        sortPosition = 0,
                                        podcastsSortType = PodcastsSortType.NAME_A_TO_Z,
                                        deleted = false,
                                        syncModified = 0,
                                    ),
                                    podcasts = emptyList(),
                                ),
                            )
                            repeat(11) { index ->
                                add(FolderItem.Podcast(Podcast(uuid = "podcast-$index", title = "Podcast $index")))
                            }
                        },
                    ),
                    getFolderCoverUuids = { emptyList() },
                    onNavigateToHome = {},
                    onOpenFolder = {},
                )
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvYourPodcastsEmptyPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvYourPodcastsContent(
                    uiState = TvYourPodcastsUiState.Empty,
                    getFolderCoverUuids = { emptyList() },
                    onNavigateToHome = {},
                    onOpenFolder = {},
                )
            }
        }
    }
}
