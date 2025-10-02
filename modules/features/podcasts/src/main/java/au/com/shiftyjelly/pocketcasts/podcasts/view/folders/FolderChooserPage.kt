package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.BottomSheetAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.components.rememberViewInteropNestedScrollConnection
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralPodcasts
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.LargePageTitle
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun FolderChooserPage(
    podcastUuid: String?,
    onCloseClick: () -> Unit,
    onNewFolderClick: () -> Unit,
    viewModel: FolderEditViewModel,
    modifier: Modifier = Modifier,
) {
    val state: FolderEditViewModel.State by viewModel.state.collectAsState()
    Surface(
        modifier = modifier
            .nestedScroll(rememberViewInteropNestedScrollConnection()),
    ) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                ),
        ) {
            BottomSheetAppBar(
                title = null,
                navigationButton = NavigationButton.Close,
                onNavigationClick = onCloseClick,
            )
            FolderList(
                currentFolder = state.folder,
                folders = state.folders,
                folderUuidToPodcastCount = state.folderUuidToPodcastCount,
                onFolderClick = { folder ->
                    if (podcastUuid != null) {
                        viewModel.movePodcastToFolder(podcastUuid, folder)
                    }
                },
                onNewFolderClick = onNewFolderClick,
                modifier = Modifier.weight(1f),
            )
            RowButton(
                modifier = Modifier
                    .navigationBarsPadding(),
                text = stringResource(LR.string.done),
                onClick = { onCloseClick() },
            )
        }
    }
}

@Composable
private fun FolderList(
    currentFolder: Folder?,
    folders: List<Folder>,
    folderUuidToPodcastCount: Map<String?, Int>,
    onFolderClick: (Folder) -> Unit,
    onNewFolderClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier) {
        item {
            Column {
                LargePageTitle(text = stringResource(LR.string.choose_folder))
                HorizontalDivider()
            }
        }
        item {
            Column {
                FolderMoveRow(
                    onClick = onNewFolderClick,
                )
                HorizontalDivider()
            }
        }
        items(folders) { folder ->
            Column {
                val selected = currentFolder != null && currentFolder.uuid == folder.uuid
                FolderSelectRow(
                    folder = folder,
                    podcastCount = folderUuidToPodcastCount[folder.uuid] ?: 0,
                    selected = selected,
                    onClick = { onFolderClick(folder) },
                )
                HorizontalDivider()
            }
        }
        item {
            Spacer(modifier = Modifier.height(7.dp))
        }
    }
}

@Composable
private fun FolderSelectRow(
    folder: Folder,
    podcastCount: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(56.dp),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_folder),
                contentDescription = null,
                tint = Color(folder.getColor(context)),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            TextH30(
                text = folder.name,
                maxLines = 1,
            )
            TextH70(
                text = context.resources.getStringPluralPodcasts(podcastCount),
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
        if (selected) {
            Icon(
                painter = painterResource(id = IR.drawable.ic_tick),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.primaryIcon01,
                modifier = Modifier.padding(end = 21.dp),
            )
        }
    }
}

@Composable
fun FolderMoveRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(64.dp)
            .clickable { onClick() },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.width(56.dp),
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_folder_plus),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.primaryInteractive01,
            )
        }
        TextH30(
            text = stringResource(LR.string.new_folder),
            color = MaterialTheme.theme.colors.primaryInteractive01,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
    }
}
