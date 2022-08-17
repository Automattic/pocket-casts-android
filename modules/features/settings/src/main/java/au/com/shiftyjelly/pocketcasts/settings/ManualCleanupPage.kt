package au.com.shiftyjelly.pocketcasts.settings

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralEpisodes
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.ManualCleanupViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ManualCleanupPage(
    viewModel: ManualCleanupViewModel,
    showToolbar: Boolean,
    onBackClick: () -> Unit,
) {
    val state: ManualCleanupViewModel.State by viewModel.state.collectAsState()
    var includeStarredSwitchState: Boolean by remember { mutableStateOf(false) }
    val context = LocalContext.current
    Column {
        if (showToolbar) {
            ThemedTopAppBar(
                title = stringResource(id = LR.string.settings_title_manage_downloads),
                navigationButton = NavigationButton.Back,
                onNavigationClick = onBackClick,
            )
        }
        ManageDownloadsView(
            state = state,
            includeStarredSwitchState = includeStarredSwitchState,
            onDiskSpaceCheckedChanged = { isChecked, diskSpaceView ->
                viewModel.onDiskSpaceCheckedChanged(isChecked, diskSpaceView)
            },
            onStarredSwitchClicked = {
                viewModel.onStarredSwitchClicked(it)
                includeStarredSwitchState = it
            },
            onDeleteButtonClicked = { viewModel.onDeleteButtonClicked() },
        )
        LaunchedEffect(Unit) {
            viewModel.snackbarMessage
                .collect { message ->
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
        }
    }
}

@Composable
private fun ManageDownloadsView(
    state: ManualCleanupViewModel.State,
    includeStarredSwitchState: Boolean,
    onDiskSpaceCheckedChanged: (Boolean, diskSpaceView: ManualCleanupViewModel.State.DiskSpaceView) -> Unit,
    onStarredSwitchClicked: (Boolean) -> Unit,
    onDeleteButtonClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val deleteButtonColor = MaterialTheme.theme.colors.support05
    Column(
        modifier = modifier
            .padding(top = 8.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        state.diskSpaceViews.forEach { DiskSpaceSizeRow(it, onDiskSpaceCheckedChanged) }
        IncludeStarredRow(includeStarredSwitchState, onStarredSwitchClicked)
        TotalSelectedDownloadSizeRow(state.totalSelectedDownloadSize)
        RowButton(
            text = stringResource(LR.string.settings_downloads_clean_up),
            enabled = state.deleteButton.isEnabled,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = deleteButtonColor,
                disabledBackgroundColor = deleteButtonColor.copy(alpha = ContentAlpha.disabled),
            ),
            onClick = onDeleteButtonClicked,
        )
    }
}

@Composable
private fun DiskSpaceSizeRow(
    diskSpaceSizeView: ManualCleanupViewModel.State.DiskSpaceView,
    onDiskSpaceCheckedChanged: (Boolean, diskSpaceView: ManualCleanupViewModel.State.DiskSpaceView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    SettingRow(
        primaryText = stringResource(diskSpaceSizeView.title),
        secondaryText = getFormattedSubtitle(diskSpaceSizeView, context),
        toggle = SettingRowToggle.Checkbox(checked = diskSpaceSizeView.isChecked),
        modifier = modifier.toggleable(
            value = diskSpaceSizeView.isChecked,
            role = Role.Checkbox
        ) { onDiskSpaceCheckedChanged(it, diskSpaceSizeView) }
    )
}

@Composable
private fun IncludeStarredRow(
    checkedState: Boolean,
    onStarredSwitchClicked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_manage_downloads_include_starred),
        toggle = SettingRowToggle.Switch(checked = checkedState),
        modifier = modifier.toggleable(
            value = checkedState,
            role = Role.Switch
        ) { onStarredSwitchClicked(it) },
    )
}

@Composable
private fun TotalSelectedDownloadSizeRow(
    totalSelectedDownloadSize: Long,
) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_manage_downloads_total),
        secondaryText = Util.formattedBytes(
            bytes = totalSelectedDownloadSize,
            context = LocalContext.current,
        ).replace("-", "0 bytes")
    )
}

private fun getFormattedSubtitle(
    diskSpaceView: ManualCleanupViewModel.State.DiskSpaceView,
    context: Context
): String {
    val byteString = Util.formattedBytes(bytes = diskSpaceView.episodesBytesSize, context = context)
    return if (diskSpaceView.episodes.isEmpty()) {
        context.resources.getStringPluralEpisodes(diskSpaceView.episodesSize)
    } else {
        "${context.resources.getStringPluralEpisodes(diskSpaceView.episodesSize)} · $byteString"
    }
}

@Preview(showBackground = true)
@Composable
private fun ManualCleanupPageNormalPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    ManualCleanupPagePreview(themeType)
}

@Preview(name = "Small", showBackground = true, heightDp = 150)
@Composable
private fun ManualCleanupPageSmallPreview() {
    ManualCleanupPagePreview()
}

@Composable
private fun ManualCleanupPagePreview(
    themeType: Theme.ThemeType = Theme.ThemeType.LIGHT
) {
    AppThemeWithBackground(themeType) {
        ManageDownloadsView(
            state = ManualCleanupViewModel.State(),
            includeStarredSwitchState = false,
            onDiskSpaceCheckedChanged = { _, _ -> },
            onStarredSwitchClicked = {},
            onDeleteButtonClicked = {},
        )
    }
}
