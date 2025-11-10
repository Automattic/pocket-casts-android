package au.com.shiftyjelly.pocketcasts.settings

import android.content.Context
import android.os.StatFs
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.DialogButtonProperties
import au.com.shiftyjelly.pocketcasts.compose.components.FormFieldDialog
import au.com.shiftyjelly.pocketcasts.compose.components.ProgressDialog
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRadioDialogRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.components.SimpleDialog
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.StorageSettingsViewModel
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.StorageSettingsViewModel.State.DatabaseEpisodeNormalizationState.NormalizationState
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.material.snackbar.Snackbar
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun StorageSettingsPage(
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onManageDownloadedFilesClick: () -> Unit,
    viewModel: StorageSettingsViewModel,
    modifier: Modifier = Modifier,
) {
    val state: StorageSettingsViewModel.State by viewModel.state.collectAsState()
    val context = LocalContext.current
    StorageSettingsView(
        state = state,
        onBackPress = onBackPress,
        onClearDownloadCacheClick = { viewModel.onClearDownloadCacheClick() },
        onManageDownloadedFilesClick = onManageDownloadedFilesClick,
        onFixDownloadsClick = { viewModel.fixDownloadedFiles() },
        bottomInset = bottomInset,
        modifier = modifier,
    )
    var showProgressDialog by remember { mutableStateOf(false) }
    if (showProgressDialog) {
        ProgressDialog(
            text = stringResource(LR.string.settings_storage_move_podcasts),
        )
    }

    CallOnce {
        viewModel.onShown()
    }

    LaunchedEffect(Unit) {
        viewModel.progressDialog
            .collect { showDialog ->
                showProgressDialog = showDialog
            }
    }
    var alertDialogState by remember {
        mutableStateOf(StorageSettingsViewModel.AlertDialogState(title = "", buttons = emptyList()))
    }
    var showAlertDialog by remember { mutableStateOf(false) }
    if (showAlertDialog) {
        AlertDialogView(
            alertDialogState = alertDialogState,
            onDismiss = { showAlertDialog = false },
        )
    }
    LaunchedEffect(Unit) {
        viewModel.alertDialog
            .collect { state ->
                alertDialogState = state
                showAlertDialog = true
            }
    }
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage
            .collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
fun StorageSettingsView(
    bottomInset: Dp,
    state: StorageSettingsViewModel.State,
    onBackPress: () -> Unit,
    onClearDownloadCacheClick: () -> Unit,
    onManageDownloadedFilesClick: () -> Unit,
    onFixDownloadsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_storage),
            bottomShadow = true,
            onNavigationClick = { onBackPress() },
        )

        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomInset),
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .fillMaxHeight(),
        ) {
            item {
                SettingSection(heading = stringResource(LR.string.settings_storage_section_heading_usage)) {
                    DownloadedFilesRow(
                        state = state.downloadedFilesState,
                        onClick = onManageDownloadedFilesClick,
                    )
                    FixDownloads(onFixDownloadsClick)
                    ClearDownloadCacheRow(onClearDownloadCacheClick)
                    StorageChoiceRow(state.storageChoiceState)
                    StorageFolderRow(state.storageFolderState)
                }
            }
            item {
                SettingSection(heading = stringResource(LR.string.settings_storage_section_heading_mobile_data)) {
                    BackgroundRefreshRow(state.backgroundRefreshState)
                    StorageDataWarningRow(state.storageDataWarningState)
                }
            }
            item {
                SettingSection(heading = stringResource(LR.string.database)) {
                    NormalizeEpisodeTitles(state.episodeTitlesState)
                }
            }
        }
    }
}

@Composable
private fun DownloadedFilesRow(
    state: StorageSettingsViewModel.State.DownloadedFilesState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_storage_downloaded_files),
        secondaryText = Util.formattedBytes(
            bytes = state.size,
            context = LocalContext.current,
        ).replace("-", stringResource(LR.string.settings_storage_downloaded_bytes, 0)),
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 6.dp),
    )
}

@Composable
private fun ClearDownloadCacheRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_storage_clear_download_cache),
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 6.dp),
    )
}

@Composable
private fun FixDownloads(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = stringResource(R.string.settings_storage_fix_downloads),
        secondaryText = stringResource(R.string.settings_storage_fix_downloads_description),
        modifier = modifier
            .clickable { onClick() }
            .padding(vertical = 6.dp),
    )
}

@Composable
private fun NormalizeEpisodeTitles(
    state: StorageSettingsViewModel.State.DatabaseEpisodeNormalizationState,
    modifier: Modifier = Modifier,
) {
    val activity = LocalActivity.current
    SettingRow(
        primaryText = stringResource(LR.string.settings_storage_normalize_episode_titles),
        secondaryText = stringResource(
            LR.string.settings_storage_normalize_episode_titles_description,
            stringResource(state.normalizationState.labelId),
        ),
        modifier = modifier.clickable(
            role = Role.Button,
            onClick = {
                state.onNormalize()
                (activity as? FragmentHostListener)?.snackBarView()?.let { view ->
                    Snackbar.make(view, activity.getString(LR.string.normalization_started), Snackbar.LENGTH_LONG).show()
                }
            },
            enabled = state.normalizationState == NormalizationState.NotNormalized,
        ),
    )
}

@Composable
private fun StorageChoiceRow(
    storageChoiceState: StorageSettingsViewModel.State.StorageChoiceState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val defaultStorageFolderLabel = stringResource(LR.string.settings_storage_phone)
    val choices = storageChoiceState.choices
    SettingRadioDialogRow(
        primaryText = stringResource(LR.string.settings_storage_store_on),
        modifier = modifier,
        secondaryText = storageChoiceState.summary,
        options = choices.map { it.label },
        savedOption = storageChoiceState.summary,
        optionToLocalisedString = {
            val label = it ?: defaultStorageFolderLabel
            val folderLocation = choices
                .find { folderLocation ->
                    it == folderLocation.label
                }
            val path = folderLocation?.filePath
            if (path == Settings.STORAGE_ON_CUSTOM_FOLDER) {
                "$label…"
            } else {
                mapToStringWithStorageSpace(label, path, context)
            }
        },
        onSave = { label ->
            val folderLocation = choices
                .find { it.label == label }
            if (folderLocation == null) {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Could not find folder location for label $label")
            } else {
                storageChoiceState.onStateChange(folderLocation)
            }
        },
    )
}

@Composable
private fun StorageFolderRow(
    storageFolderState: StorageSettingsViewModel.State.StorageFolderState,
    modifier: Modifier = Modifier,
) {
    if (storageFolderState.isVisible) {
        var showDialog by remember { mutableStateOf(false) }
        SettingRow(
            primaryText = stringResource(LR.string.settings_storage_custom_folder_location),
            secondaryText = storageFolderState.summary,
            modifier = modifier
                .clickable { showDialog = true },
        ) {
            if (showDialog) {
                FormFieldDialog(
                    title = stringResource(LR.string.settings_storage_custom_folder_location),
                    placeholder = stringResource(LR.string.settings_storage_custom_folder_location),
                    initialValue = storageFolderState.summary.orEmpty(),
                    keyboardType = KeyboardType.Text,
                    onConfirm = { value -> storageFolderState.onStateChange(value) },
                    onDismissRequest = { showDialog = false },
                )
            }
        }
    }
}

@Composable
private fun BackgroundRefreshRow(
    state: StorageSettingsViewModel.State.BackgroundRefreshState,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_storage_background_refresh),
        secondaryText = stringResource(state.summary),
        toggle = SettingRowToggle.Switch(state.isChecked),
        modifier = modifier.toggleable(
            value = state.isChecked,
            role = Role.Switch,
        ) { state.onCheckedChange(it) },
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
        modifier = modifier.toggleable(
            value = state.isChecked,
            role = Role.Switch,
        ) { state.onCheckedChange(it) },
    ) {
        TextP50(
            text = stringResource(LR.string.settings_storage_data_warning_car),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.theme.colors.primaryText02,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun AlertDialogView(
    alertDialogState: StorageSettingsViewModel.AlertDialogState,
    onDismiss: () -> Unit,
) {
    SimpleDialog(
        title = alertDialogState.title,
        body = alertDialogState.message.orEmpty(),
        onDismissRequest = onDismiss,
        buttonProperties = alertDialogState.buttons.map {
            DialogButtonProperties(
                text = it.text,
                onClick = {
                    it.onClick()
                    onDismiss()
                },
            )
        },
    )
}

private fun mapToStringWithStorageSpace(
    option: String,
    path: String?,
    context: Context,
) = path?.let { option + ", " + getStorageSpaceString(it, context) } ?: option

private fun getStorageSpaceString(
    path: String,
    context: Context,
) = try {
    val stat = StatFs(path)
    val free = stat.availableBlocksLong * stat.blockSizeLong
    context.getString(
        LR.string.settings_storage_size_free,
        Util.formattedBytes(free, context = context),
    )
} catch (e: Exception) {
    ""
}

@Preview(showBackground = true)
@Composable
private fun StorageSettingsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        StorageSettingsView(
            state = StorageSettingsViewModel.State(
                downloadedFilesState = StorageSettingsViewModel.State.DownloadedFilesState(),
                storageChoiceState = StorageSettingsViewModel.State.StorageChoiceState(
                    onStateChange = {},
                ),
                storageFolderState = StorageSettingsViewModel.State.StorageFolderState(
                    summary = "Custom Folder",
                    onStateChange = {},
                ),
                backgroundRefreshState = StorageSettingsViewModel.State.BackgroundRefreshState(
                    isChecked = true,
                    summary = LR.string.settings_storage_background_refresh_on,
                    onCheckedChange = {},
                ),
                storageDataWarningState = StorageSettingsViewModel.State.StorageDataWarningState(
                    onCheckedChange = {},
                ),
                episodeTitlesState = StorageSettingsViewModel.State.DatabaseEpisodeNormalizationState(
                    normalizationState = NormalizationState.Normalized,
                    onNormalize = {},
                ),
            ),
            onBackPress = {},
            onClearDownloadCacheClick = {},
            onManageDownloadedFilesClick = {},
            onFixDownloadsClick = {},
            bottomInset = 0.dp,
        )
    }
}
