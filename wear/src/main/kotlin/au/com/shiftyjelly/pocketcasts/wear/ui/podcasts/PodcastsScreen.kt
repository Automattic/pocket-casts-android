package au.com.shiftyjelly.pocketcasts.wear.ui.podcasts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.extensions.darker
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.wear.theme.WearColors
import au.com.shiftyjelly.pocketcasts.wear.ui.component.LoadingSpinner
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object PodcastsScreen {
    const val argumentFolderUuid = "folderUuid"
    const val routeFolder = "podcasts/{$argumentFolderUuid}"
    const val routeHomeFolder = "podcasts"

    fun navigateRoute(folderUuid: String) = "podcasts/$folderUuid"
}

@Composable
fun PodcastsScreen(
    modifier: Modifier = Modifier,
    viewModel: PodcastsViewModel = hiltViewModel(),
    columnState: ScalingLazyColumnState,
    navigateToPodcast: (String) -> Unit,
    navigateToFolder: (String) -> Unit,
) {
    when (val uiState = viewModel.uiState.collectAsState().value) {
        is PodcastsViewModel.UiState.Empty -> {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                Text(
                    text = stringResource(LR.string.podcasts_no_subscriptions),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onPrimary,
                    style = MaterialTheme.typography.title2,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(LR.string.podcasts_subscribe_on_phone),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onPrimary,
                    style = MaterialTheme.typography.body1,
                )
            }
        }

        is PodcastsViewModel.UiState.Loaded -> {
            ScalingLazyColumn(
                modifier = modifier.fillMaxWidth(),
                columnState = columnState,
            ) {
                item {
                    ScreenHeaderChip(if (uiState.folder == null) stringResource(LR.string.podcasts) else uiState.folder.name)
                }
                items(items = uiState.items, key = { item -> item.uuid }) { item ->
                    when (item) {
                        is FolderItem.Podcast -> {
                            PodcastChip(podcast = item, onClick = navigateToPodcast)
                        }

                        is FolderItem.Folder -> {
                            FolderChip(folderItem = item, onClick = navigateToFolder)
                        }
                    }
                }
            }
        }

        is PodcastsViewModel.UiState.Loading -> {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize(),
            ) {
                LoadingSpinner()
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(LR.string.podcast_loading),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.onPrimary,
                    style = MaterialTheme.typography.body1,
                )
            }
        }
    }
}

@Composable
private fun FolderChip(
    folderItem: FolderItem.Folder,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val folder = folderItem.folder
    Chip(
        onClick = { onClick(folder.uuid) },
        label = {
            ChipText(folder.name)
        },
        icon = {
            Icon(
                painter = painterResource(IR.drawable.ic_folder),
                contentDescription = null,
                tint = WearColors.getFolderColor(folder.color),
                modifier = Modifier.padding(horizontal = 8.dp).size(24.dp),
            )
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun PodcastChip(
    podcast: FolderItem.Podcast,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Chip(
        onClick = { onClick(podcast.uuid) },
        colors = ChipDefaults.gradientBackgroundChipColors(
            startBackgroundColor = Color(podcast.podcast.tintColorForDarkBg).darker(0.5f),
            endBackgroundColor = MaterialTheme.colors.surface,
        ),
        label = {
            ChipText(podcast.title)
        },
        icon = {
            PodcastImage(
                uuid = podcast.uuid,
                dropShadow = false,
                modifier = Modifier.size(32.dp),
            )
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ChipText(title: String) {
    Text(
        text = title,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.button,
        color = MaterialTheme.colors.onPrimary,
    )
}
