package au.com.shiftyjelly.pocketcasts.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.settings.components.DiskSpaceSizeView
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.ManualCleanupViewModel
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ManualCleanupPage(
    viewModel: ManualCleanupViewModel,
    showToolbar: Boolean,
    onBackClick: () -> Unit,
) {
    val state: ManualCleanupViewModel.State by viewModel.state.collectAsState()
    val context = LocalContext.current
    Column {
        if (showToolbar) {
            ThemedTopAppBar(
                title = stringResource(id = LR.string.settings_title_manage_downloads),
                navigationButton = NavigationButton.Back,
                onNavigationClick = { onBackClick() },
            )
        }
        ManageDownloadsView(
            state = state,
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
    onDeleteButtonClicked: () -> Unit,
) {
    val deleteButtonContentColor =
        Color(LocalContext.current.getThemeColor(state.deleteButton.contentColor))
    Column {
        DiskSpaceSizeView(stringResource(LR.string.unplayed), "")
        DiskSpaceSizeView(stringResource(LR.string.in_progress), "")
        DiskSpaceSizeView(stringResource(LR.string.played), "")
        TotalDownloadSizeRow("100mb")
        IncludeStarredRow()
        RowButton(
            text = stringResource(id = state.deleteButton.title),
            enabled = state.deleteButton.isEnabled,
            colors = ButtonDefaults.buttonColors(
                contentColor = deleteButtonContentColor,
                disabledContentColor = deleteButtonContentColor.copy(alpha = ContentAlpha.disabled),
            ),
            onClick = { onDeleteButtonClicked() },
        )
    }
}

@Composable
private fun TotalDownloadSizeRow(
    totalSize: String,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        TextH30(
            text = stringResource(LR.string.settings_manage_downloads_total),
            modifier = modifier.weight(1f)
        )
        TextC70(text = totalSize)
    }
}

@Composable
private fun IncludeStarredRow(
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(horizontal = 16.dp)
    ) {
        TextH30(
            text = stringResource(LR.string.settings_manage_downloads_include_starred),
            modifier = modifier.weight(1f)
        )
        Switch(checked = false, onCheckedChange = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun ManualCleanupPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        ManageDownloadsView(
            state = ManualCleanupViewModel.State(),
            onDeleteButtonClicked = {},
        )
    }
}
