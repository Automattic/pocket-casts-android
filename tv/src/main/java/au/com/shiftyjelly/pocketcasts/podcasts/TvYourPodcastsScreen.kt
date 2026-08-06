package au.com.shiftyjelly.pocketcasts.podcasts

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.component.TvDetailOverlay
import au.com.shiftyjelly.pocketcasts.component.TvEmptyState
import au.com.shiftyjelly.pocketcasts.component.TvFolderCard
import au.com.shiftyjelly.pocketcasts.component.TvPodcastGridScaffold
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTile
import au.com.shiftyjelly.pocketcasts.component.tvFocusInactiveWhen
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTopBarHeight
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
    var openedPodcastUuid by rememberSaveable { mutableStateOf<String?>(null) }

    val podcastUuid = openedPodcastUuid
    val folder = openedFolder
    Box(modifier = modifier.fillMaxSize()) {
        TvYourPodcastsContent(
            uiState = uiState,
            onNavigateToHome = onNavigateToHome,
            onOpenFolder = { openedFolder = OpenedFolder(it.uuid, it.name) },
            onOpenPodcast = { openedPodcastUuid = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = TvTopBarHeight)
                .tvFocusInactiveWhen(folder != null || podcastUuid != null),
        )
        TvDetailOverlay(
            target = folder,
            onBack = { openedFolder = null },
            modifier = Modifier.tvFocusInactiveWhen(podcastUuid != null),
        ) { openFolder ->
            TvFolderDetailScreen(
                folderUuid = openFolder.uuid,
                folderName = openFolder.name,
                getFolderPodcasts = viewModel::folderPodcasts,
                onOpenPodcast = { openedPodcastUuid = it },
                onClose = { openedFolder = null },
            )
        }
        TvDetailOverlay(
            target = podcastUuid,
            onBack = { openedPodcastUuid = null },
        ) { uuid ->
            TvPodcastDetailsScreen(
                podcastUuid = uuid,
                onClose = { openedPodcastUuid = null },
            )
        }
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
    onNavigateToHome: () -> Unit,
    onOpenFolder: (Folder) -> Unit,
    onOpenPodcast: (String) -> Unit,
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
                onOpenFolder = onOpenFolder,
                onOpenPodcast = onOpenPodcast,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TvYourPodcastsGrid(
    items: List<FolderItem>,
    onOpenFolder: (Folder) -> Unit,
    onOpenPodcast: (String) -> Unit,
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
                onClick = { onOpenPodcast(item.podcast.uuid) },
                imageModifier = Modifier.fillMaxWidth(),
                modifier = itemModifier,
            )

            is FolderItem.Folder -> TvFolderCard(
                folder = item.folder,
                coverUrls = item.podcasts.take(FOLDER_COVER_COUNT).map { PodcastImage.getMediumArtworkUrl(it.uuid) },
                onClick = { onOpenFolder(item.folder) },
                modifier = itemModifier,
            )
        }
    }
}

private const val FOLDER_COVER_COUNT = 4

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
                                    podcasts = List(4) { index -> Podcast(uuid = "cover-$index") },
                                ),
                            )
                            repeat(11) { index ->
                                add(FolderItem.Podcast(Podcast(uuid = "podcast-$index", title = "Podcast $index")))
                            }
                        },
                    ),
                    onNavigateToHome = {},
                    onOpenFolder = {},
                    onOpenPodcast = {},
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
                    onNavigateToHome = {},
                    onOpenFolder = {},
                    onOpenPodcast = {},
                )
            }
        }
    }
}
