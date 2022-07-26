package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun StorageSettingsPage() {
    StorageSettingsView()
}

@Composable
fun StorageSettingsView() {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_storage),
            bottomShadow = true,
            onNavigationClick = {},
        )

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.theme.colors.primaryUi02)
                .padding(vertical = 8.dp)
        ) {
            DownloadedFilesRow()
            ClearDownloadCacheRow()
            StorageChoiceRow()
            StorageFolderRow()
            BackgroundRefreshRow()
            StorageDataWarningRow()
        }
    }
}

@Composable
private fun DownloadedFilesRow() {
    SettingRow(
        primaryText = stringResource(LR.string.settings_storage_manage_downloads),
    )
}

@Composable
private fun ClearDownloadCacheRow() {
    SettingRow(
        primaryText = stringResource(LR.string.settings_storage_clear_download_cache),
    )
}

@Composable
private fun StorageChoiceRow() {
    SettingRow(
        primaryText = stringResource(LR.string.settings_storage_store_on),
    )
}

@Composable
private fun StorageFolderRow() {
    SettingRow(
        primaryText = stringResource(LR.string.settings_storage_custom_folder_location),
    )
}

@Composable
private fun BackgroundRefreshRow() {
    SettingRow(
        primaryText = stringResource(LR.string.settings_storage_background_refresh),
        secondaryText = stringResource(LR.string.settings_storage_background_refresh_on),
        toggle = SettingRowToggle.Switch(checked = true),
    )
}

@Composable
private fun StorageDataWarningRow(
    modifier: Modifier = Modifier
) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_storage_data_warning),
        secondaryText = stringResource(LR.string.settings_storage_data_warning_summary),
        toggle = SettingRowToggle.Switch(checked = false),
    ) {
        TextP50(
            text = stringResource(LR.string.settings_storage_data_warning_car),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.theme.colors.primaryText02,
            modifier = modifier
                .padding(top = 16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StorageSettingsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        StorageSettingsView()
    }
}
