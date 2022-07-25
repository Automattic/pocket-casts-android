package au.com.shiftyjelly.pocketcasts.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.StorageSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun StorageSettingsPage(
    viewModel: StorageSettingsViewModel
) {
    val state: StorageSettingsViewModel.State by viewModel.state.collectAsState()
    val context = LocalContext.current
    StorageSettingsView(
        state = state,
        onClearDownloadCacheClick = { viewModel.onClearDownloadCacheClick() }
    )
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage
            .collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun StorageSettingsView(
    state: StorageSettingsViewModel.State,
    onClearDownloadCacheClick: () -> Unit,
) {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_storage),
            bottomShadow = true,
            onNavigationClick = {}
        )

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.theme.colors.primaryUi02)
                .padding(vertical = 8.dp)
        ) {
            DownloadedFilesRow()
            ClearDownloadCacheRow(onClearDownloadCacheClick)
            StorageChoiceRow()
            StorageFolderRow()
            BackgroundRefreshRow()
            StorageDataWarningRow(state.storageDataWarningState)
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
private fun ClearDownloadCacheRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_storage_clear_download_cache),
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 6.dp)
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
    state: StorageSettingsViewModel.State.StorageDataWarningState,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_storage_data_warning),
        secondaryText = stringResource(LR.string.settings_storage_data_warning_summary),
        toggle = SettingRowToggle.Switch(checked = state.isChecked),
        modifier = modifier.toggleable(value = state.isChecked, role = Role.Switch) { state.onCheckedChange(it) }
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
        StorageSettingsView(
            state = StorageSettingsViewModel.State(
                storageDataWarningState = StorageSettingsViewModel.State.StorageDataWarningState(
                    onCheckedChange = {}
                )
            ),
            onClearDownloadCacheClick = {},
        )
    }
}
