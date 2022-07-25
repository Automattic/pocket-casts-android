package au.com.shiftyjelly.pocketcasts.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.MutableState
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
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.DialogButtonState
import au.com.shiftyjelly.pocketcasts.compose.components.DialogFrame
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRadioDialogRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.StorageSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.delay
import java.util.*
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun StorageSettingsPage(
    viewModel: StorageSettingsViewModel,
    onBackPressed: () -> Unit
) {
    val state: StorageSettingsViewModel.State by viewModel.state.collectAsState()
    val context = LocalContext.current
    StorageSettingsView(
        state = state,
        onBackPressed = onBackPressed,
        onClearDownloadCacheClick = { viewModel.onClearDownloadCacheClick() }
    )
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage
            .collect { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
    }
    var showAlertDialog by remember<MutableState<StorageSettingsViewModel.AlertDialogState?>> {
        mutableStateOf(null)
    }
    showAlertDialog?.let { dialogState ->
        DialogFrame(
            title = dialogState.title,
            buttons = dialogState.buttons.map { button ->
                DialogButtonState(
                    text = button.text.uppercase(
                        Locale.getDefault()
                    ),
                    onClick = {
                        button.onClick()
                        showAlertDialog = null
                    }
                )
            },
            onDismissRequest = { showAlertDialog = null },
            content = {
                dialogState.message?.let {
                    TextP40(
                        text = dialogState.message,
                        modifier = Modifier
                            .padding(bottom = 12.dp)
                            .padding(horizontal = 24.dp)
                    )
                }
            }
        )
    }
    LaunchedEffect(Unit) {
        viewModel.alertDialog
            .collect { dialog ->
                showAlertDialog = dialog
            }
    }
}

@Composable
fun StorageSettingsView(
    state: StorageSettingsViewModel.State,
    onBackPressed: () -> Unit,
    onClearDownloadCacheClick: () -> Unit,
) {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_storage),
            bottomShadow = true,
            onNavigationClick = { onBackPressed() }
        )

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.theme.colors.primaryUi02)
                .padding(vertical = 8.dp)
        ) {
            DownloadedFilesRow()
            ClearDownloadCacheRow(onClearDownloadCacheClick)
            StorageChoiceRow(state.storageChoiceState)
            StorageFolderRow(state.storageFolderState)
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
) {
    val (folderLabels, folderPaths) = storageChoiceState.choices
    SettingRadioDialogRow(
        primaryText = stringResource(LR.string.settings_storage_store_on),
        secondaryText = storageChoiceState.summary,
        options = folderLabels.asList(),
        savedOption = storageChoiceState.summary,
        optionToStringRes = ::mapToStringRes,
        onSave = { storageChoiceState.onStateChange(folderPaths[folderLabels.indexOf(it)]) }
    )
}

fun mapToStringRes(storageLocation: String?): Int {
    return when (storageLocation?.lowercase()) {
        "custom folder" -> LR.string.settings_storage_custom_folder
        "sd card" -> LR.string.settings_storage_sd_card
        "phone" -> LR.string.settings_storage_phone
        else -> LR.string.settings_storage_phone
    }
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
                ),
                storageChoiceState = StorageSettingsViewModel.State.StorageChoiceState(
                    onStateChange = {}
                ),
                storageFolderState = StorageSettingsViewModel.State.StorageFolderState(
                    summary = "Custom Folder",
                    onStateChange = {}
                ),
            ),
            onBackPressed = {},
            onClearDownloadCacheClick = {},
        )
    }
}
