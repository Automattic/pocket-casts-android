package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.settings.components.DiskSpaceSizeView
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ManualCleanupPage(
    totalSize: String,
) {
    Column {
        DiskSpaceSizeView(stringResource(LR.string.unplayed), "")
        DiskSpaceSizeView(stringResource(LR.string.in_progress), "")
        DiskSpaceSizeView(stringResource(LR.string.played), "")
        TotalDownloadSizeRow(totalSize)
        IncludeStarredRow()
        RowButton(
            text = stringResource(LR.string.settings_select_episodes_to_delete),
            onClick = {}
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
private fun ManualCleanupPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        ManualCleanupPage(totalSize = "100mb")
    }
}
