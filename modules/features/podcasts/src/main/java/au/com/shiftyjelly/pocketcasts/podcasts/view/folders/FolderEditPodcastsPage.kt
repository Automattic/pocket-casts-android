package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.compose.bars.BottomSheetAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastSelectedText
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.components.rememberViewInteropNestedScrollConnection
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.view.compose.components.LargePageTitle
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun FolderEditPodcastsPage(
    onCloseClick: () -> Unit,
    onNextClick: () -> Unit,
    viewModel: FolderEditViewModel,
    navigationButton: NavigationButton = NavigationButton.Close,
    settings: Settings,
    fragmentManager: FragmentManager
) {
    val state: FolderEditViewModel.State by viewModel.state.collectAsState()
    val context = LocalContext.current
    Surface(modifier = Modifier.nestedScroll(rememberViewInteropNestedScrollConnection())) {
        Column {
            BottomSheetAppBar(
                navigationButton = navigationButton,
                onNavigationClick = onCloseClick
            )
            PageList(
                onNextClick = onNextClick,
                onSortClick = {
                    SelectSortByDialog(settings, viewModel::changeSortOrder).show(context = context, fragmentManager = fragmentManager)
                },
                state = state,
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PageList(
    onNextClick: () -> Unit,
    onSortClick: () -> Unit,
    state: FolderEditViewModel.State,
    viewModel: FolderEditViewModel,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        item {
            Column {
                LargePageTitle(text = stringResource(if (state.isCreateFolder) LR.string.create_folder else LR.string.filters_choose_podcasts))
                SearchSortBar(
                    searchText = state.searchText,
                    onSearchTextChanged = { text -> viewModel.searchPodcasts(text) },
                    onSortClick = onSortClick
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PodcastSelectedText(count = state.selectedCount)
                }
                HorizontalDivider(modifier = Modifier.padding(bottom = 10.dp))
            }
        }
        items(state.filteredPodcasts) { podcastWithFolder ->
            PodcastSelectRow(
                podcast = podcastWithFolder.podcast,
                folder = if (state.folder?.uuid == podcastWithFolder.folder?.uuid) null else podcastWithFolder.folder,
                selected = state.isSelected(podcastWithFolder.podcast),
                addPodcast = { uuid -> viewModel.addPodcast(uuid) },
                removePodcast = { uuid -> viewModel.removePodcast(uuid) }
            )
        }
        item {
            Spacer(modifier = Modifier.height(7.dp))
        }
    }
    val buttonText = when {
        state.isEditFolder -> stringResource(LR.string.update)
        state.selectedCount == 1 -> stringResource(LR.string.add_podcasts_singular)
        else -> stringResource(LR.string.add_podcasts_plural, state.selectedCount)
    }
    Card(elevation = 8.dp) {
        RowButton(text = buttonText, onClick = { onNextClick() })
    }
}

@Composable
private fun SearchSortBar(searchText: String, onSearchTextChanged: (String) -> Unit, onSortClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(bottom = 16.dp)
            .fillMaxWidth()
    ) {
        SearchBar(
            text = searchText,
            placeholder = stringResource(LR.string.search_podcasts),
            onTextChanged = onSearchTextChanged,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        )
        IconButton(onClick = { onSortClick() }) {
            Icon(
                painter = painterResource(IR.drawable.ic_sort),
                contentDescription = stringResource(LR.string.podcasts_sort_order),
                tint = MaterialTheme.theme.colors.primaryIcon01,
                modifier = Modifier.padding(end = 16.dp, start = 16.dp)
            )
        }
    }
}

@Composable
private fun PodcastSelectRow(
    podcast: Podcast,
    folder: Folder?,
    selected: Boolean,
    addPodcast: (uuid: String) -> Unit,
    removePodcast: (uuid: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val onSelectionChanged: (Boolean) -> Unit = {
        if (it) {
            addPodcast(podcast.uuid)
        } else {
            removePodcast(podcast.uuid)
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { onSelectionChanged(!selected) }
    ) {
        Box(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            PodcastImage(
                uuid = podcast.uuid,
                modifier = Modifier.size(56.dp)
            )
        }
        Column(
            modifier = Modifier
                .padding(end = 8.dp)
                .weight(1f)
        ) {
            if (folder != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val folderColor = MaterialTheme.theme.colors.getFolderColor(folder.color)
                    Icon(
                        painter = painterResource(R.drawable.ic_folder_small),
                        contentDescription = null,
                        tint = folderColor,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    TextP50(
                        text = folder.name,
                        maxLines = 1,
                        color = folderColor,
                        style = if (selected) TextStyle(textDecoration = TextDecoration.LineThrough) else LocalTextStyle.current
                    )
                }
            }
            TextP40(
                text = podcast.title,
                maxLines = 1
            )
            TextP50(
                text = podcast.author,
                maxLines = 1,
                color = MaterialTheme.theme.colors.primaryText02
            )
        }
        Checkbox(
            checked = selected,
            onCheckedChange = { selected -> onSelectionChanged(selected) },
            modifier = Modifier.padding(end = 10.dp)
        )
    }
}
