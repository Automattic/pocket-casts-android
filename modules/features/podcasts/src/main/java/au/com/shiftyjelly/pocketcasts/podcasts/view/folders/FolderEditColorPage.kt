package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.BottomSheetAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.FolderColorPicker
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.folder.FolderImage
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts.FolderListRow
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun FolderEditColorPage(
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    viewModel: FolderEditViewModel
) {
    val state: FolderEditViewModel.State by viewModel.state.collectAsState()
    val colorId: Int by viewModel.colorId.collectAsState()
    val folderName: String by viewModel.folderName.collectAsState()
    val context = LocalContext.current
    val gridImageWidthDp = remember(state.layout) {
        viewModel.getGridImageWidthDp(layout = state.layout, context = context)
    }

    FolderEditColorForm(
        state = state,
        colorId = colorId,
        folderName = folderName,
        gridImageWidthDp = gridImageWidthDp,
        onBackClick = onBackClick,
        onSaveClick = onSaveClick,
        onColorChange = { viewModel.changeColor(it) }
    )
}

@Composable
private fun FolderEditColorForm(
    state: FolderEditViewModel.State,
    colorId: Int,
    folderName: String,
    gridImageWidthDp: Int,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onColorChange: (Int) -> Unit
) {
    BoxWithConstraints {
        val maxHeight = this.maxHeight
        Column {
            BottomSheetAppBar(
                title = stringResource(LR.string.folder_choose_a_color),
                navigationButton = NavigationButton.Back,
                onNavigationClick = { onBackClick() }
            )
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .weight(1f)
            ) {
                TextC70(
                    text = stringResource(LR.string.color),
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, end = 16.dp, start = 16.dp)
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                FolderColorPicker(
                    selectedId = colorId,
                    onClick = { colorId -> onColorChange(colorId) }
                )
                TextP60(
                    text = stringResource(LR.string.folder_background_color_summary),
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
                TextC70(
                    text = stringResource(LR.string.preview),
                    modifier = Modifier.padding(top = 16.dp, end = 16.dp, start = 16.dp, bottom = 8.dp)
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                FolderPreview(
                    layout = state.layout,
                    name = folderName.ifBlank { stringResource(LR.string.new_folder_title) },
                    colorId = colorId,
                    podcastUuids = state.selectedUuids.toList(),
                    gridImageWidthDp = gridImageWidthDp
                )
            }
            // only elevate the bottom button if the content will go under it
            Card(elevation = if (maxHeight > 600.dp) 0.dp else 6.dp) {
                RowButton(
                    text = stringResource(LR.string.save_folder),
                    onClick = { onSaveClick() }
                )
            }
        }
    }
}

@Composable
private fun FolderPreview(layout: Int, name: String, colorId: Int, gridImageWidthDp: Int, podcastUuids: List<String>, modifier: Modifier = Modifier) {
    val backgroundColor = MaterialTheme.theme.colors.getFolderColor(colorId)
    when (layout) {
        Settings.PODCAST_GRID_LAYOUT_LIST_VIEW -> {
            FolderListRow(
                color = backgroundColor,
                name = name,
                podcastUuids = podcastUuids,
                onClick = null,
                modifier = modifier.padding(vertical = 8.dp)
            )
        }
        else -> {
            Card(
                modifier = modifier.padding(all = 16.dp),
                elevation = 4.dp
            ) {
                FolderImage(
                    name = name,
                    color = backgroundColor,
                    podcastUuids = podcastUuids,
                    modifier = Modifier.size(gridImageWidthDp.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FolderEditColorFormNormalPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    FolderEditColorFormPreview(themeType)
}

@Preview(name = "Small", showBackground = true, widthDp = 300, heightDp = 500)
@Composable
private fun FolderEditColorFormSmallPreview() {
    FolderEditColorFormPreview()
}

@Preview(name = "Landscape", showBackground = true, widthDp = 500, heightDp = 300)
@Composable
private fun FolderEditColorFormLandscapePreview() {
    FolderEditColorFormPreview()
}

@Composable
private fun FolderEditColorFormPreview(themeType: Theme.ThemeType = Theme.ThemeType.LIGHT) {
    AppThemeWithBackground(themeType) {
        FolderEditColorForm(
            state = FolderEditViewModel.State(),
            colorId = 0,
            folderName = "New folder",
            gridImageWidthDp = 100,
            onBackClick = {},
            onSaveClick = {},
            onColorChange = {}
        )
    }
}
