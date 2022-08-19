package au.com.shiftyjelly.pocketcasts.settings

import android.content.Context
import android.os.StatFs
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.DialogButtonState
import au.com.shiftyjelly.pocketcasts.compose.components.DialogFrame
import au.com.shiftyjelly.pocketcasts.compose.components.ProgressDialog
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRadioDialogRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.StorageSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.Util
import kotlinx.coroutines.delay
import java.util.*
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun StorageSettingsPage(
    viewModel: StorageSettingsViewModel,
    onBackPressed: () -> Unit,
    onManageDownloadedFilesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state: StorageSettingsViewModel.State by viewModel.state.collectAsState()
    val context = LocalContext.current
    StorageSettingsView(
        state = state,
        onBackPressed = onBackPressed,
        onClearDownloadCacheClick = { viewModel.onClearDownloadCacheClick() },
        onManageDownloadedFilesClick = onManageDownloadedFilesClick,
        modifier = modifier
    )
    var showProgressDialog by remember { mutableStateOf(false) }
    if (showProgressDialog) {
        ProgressDialog(
            text = stringResource(LR.string.settings_storage_move_podcasts),
            onDismiss = { showProgressDialog = false }
        )
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
            onDismiss = { showAlertDialog = false }
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
    state: StorageSettingsViewModel.State,
    onBackPressed: () -> Unit,
    onClearDownloadCacheClick: () -> Unit,
    onManageDownloadedFilesClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_storage),
            bottomShadow = true,
            onNavigationClick = { onBackPressed() }
        )

        Column(
            modifier
                .padding(vertical = 8.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            SettingSection(heading = stringResource(LR.string.settings_storage_section_heading_usage)) {
                DownloadedFilesRow(
                    state = state.downloadedFilesState,
                    onClick = onManageDownloadedFilesClick
                )
                ClearDownloadCacheRow(onClearDownloadCacheClick)
                StorageChoiceRow(state.storageChoiceState)
                StorageFolderRow(state.storageFolderState)
            }
            SettingSection(heading = stringResource(LR.string.settings_storage_section_heading_mobile_data)) {
                BackgroundRefreshRow(state.backgroundRefreshState)
                StorageDataWarningRow(state.storageDataWarningState)
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
            .padding(vertical = 6.dp)
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
            .padding(vertical = 6.dp)
    )
}

@Composable
private fun StorageChoiceRow(
    storageChoiceState: StorageSettingsViewModel.State.StorageChoiceState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val (storageLabels, storagePaths) = storageChoiceState.choices
    val defaultStorageFolderLabel = stringResource(LR.string.settings_storage_phone)
    SettingRadioDialogRow(
        primaryText = stringResource(LR.string.settings_storage_store_on),
        modifier = modifier,
        secondaryText = storageChoiceState.summary,
        options = storageLabels.asList(),
        savedOption = storageChoiceState.summary,
        optionToLocalisedString = {
            val label = it ?: defaultStorageFolderLabel
            val path = storagePaths[storageLabels.indexOf(it)]
            if (path == Settings.STORAGE_ON_CUSTOM_FOLDER) {
                "$label…"
            } else {
                mapToStringWithStorageSpace(label, path, context)
            }
        },
        onSave = {
            storageChoiceState.onStateChange(
                storagePaths[storageLabels.indexOf(it)]
            )
        }
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
                .clickable { showDialog = true }
        ) {
            if (showDialog) {
                val focusRequester = remember { FocusRequester() }
                LaunchedEffect(Unit) {
                    // delay apparently needed to ensure the soft keyboard opens
                    delay(100)
                    focusRequester.requestFocus()
                }

                var value by remember {
                    mutableStateOf(
                        TextFieldValue(
                            text = storageFolderState.summary ?: ""
                        )
                    )
                }

                val onFinish = {
                    storageFolderState.onStateChange(value.text)
                    showDialog = false
                }

                DialogFrame(
                    title = stringResource(LR.string.settings_storage_custom_folder_location),
                    buttons = listOf(
                        DialogButtonState(
                            text = stringResource(LR.string.cancel).uppercase(
                                Locale.getDefault()
                            ),
                            onClick = { showDialog = false }
                        ),
                        DialogButtonState(
                            text = stringResource(LR.string.ok),
                            onClick = onFinish
                        )
                    ),
                    onDismissRequest = { showDialog = false }
                ) {
                    OutlinedTextField(
                        value = value,
                        onValueChange = {
                            value = it
                        },
                        colors = TextFieldDefaults.textFieldColors(
                            textColor = MaterialTheme.theme.colors.primaryText01,
                            placeholderColor = MaterialTheme.theme.colors.primaryText02,
                            backgroundColor = MaterialTheme.theme.colors.primaryUi01
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        keyboardActions = KeyboardActions { onFinish() },
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .focusRequester(focusRequester)
                    )
                }
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
            role = Role.Switch
        ) { state.onCheckedChange(it) }
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
            role = Role.Switch
        ) { state.onCheckedChange(it) }
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

@Composable
private fun AlertDialogView(
    alertDialogState: StorageSettingsViewModel.AlertDialogState,
    onDismiss: () -> Unit,
) {
    DialogFrame(
        title = alertDialogState.title,
        buttons = alertDialogState.buttons.map {
            DialogButtonState(
                text = it.text,
                onClick = {
                    it.onClick()
                    onDismiss()
                }
            )
        },
        onDismissRequest = { onDismiss() },
        content = {
            alertDialogState.message?.let {
                TextP40(
                    text = it,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .padding(horizontal = 24.dp)
                )
            }
        }
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
        Util.formattedBytes(free, context = context)
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
                    onStateChange = {}
                ),
                storageFolderState = StorageSettingsViewModel.State.StorageFolderState(
                    summary = "Custom Folder",
                    onStateChange = {}
                ),
                backgroundRefreshState = StorageSettingsViewModel.State.BackgroundRefreshState(
                    isChecked = true,
                    summary = LR.string.settings_storage_background_refresh_on,
                    onCheckedChange = {}
                ),
                storageDataWarningState = StorageSettingsViewModel.State.StorageDataWarningState(
                    onCheckedChange = {}
                ),
            ),
            onBackPressed = {},
            onClearDownloadCacheClick = {},
            onManageDownloadedFilesClick = {}
        )
    }
}
