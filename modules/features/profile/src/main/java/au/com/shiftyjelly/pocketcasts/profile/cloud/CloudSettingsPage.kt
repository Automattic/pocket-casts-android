package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.Banner
import au.com.shiftyjelly.pocketcasts.compose.components.GradientIcon
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSectionHeader
import au.com.shiftyjelly.pocketcasts.compose.components.SettingsSection
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.profile.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
fun CloudSettingsPage(
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CloudSettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

    CallOnce {
        viewModel.onShown()
    }

    CloudSettingsContent(
        uiState = uiState,
        bottomInset = bottomInset,
        onBackPress = onBackPress,
        onUpgradeClick = onUpgradeClick,
        onAddToUpNextChange = viewModel::setAddToUpNext,
        onDeleteLocalFileChange = viewModel::setDeleteLocalFileAfterPlaying,
        onDeleteCloudFileChange = viewModel::setDeleteCloudFileAfterPlaying,
        onCloudAutoUploadChange = viewModel::setCloudAutoUpload,
        onCloudAutoDownloadChange = viewModel::setCloudAutoDownload,
        onCloudOnlyOnWifiChange = viewModel::setCloudOnlyWifi,
        onUpgradeBannerDismiss = { viewModel.onUpgradeBannerDismissed(SourceView.FILES_SETTINGS) },
        modifier = modifier,
    )
}

@Composable
private fun CloudSettingsContent(
    uiState: CloudSettingsViewModel.UiState,
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onUpgradeClick: () -> Unit,
    onAddToUpNextChange: (Boolean) -> Unit,
    onDeleteLocalFileChange: (Boolean) -> Unit,
    onDeleteCloudFileChange: (Boolean) -> Unit,
    onCloudAutoUploadChange: (Boolean) -> Unit,
    onCloudAutoDownloadChange: (Boolean) -> Unit,
    onCloudOnlyOnWifiChange: (Boolean) -> Unit,
    onUpgradeBannerDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ThemedTopAppBar(
            title = stringResource(LR.string.profile_cloud_settings_title),
            bottomShadow = true,
            onNavigationClick = onBackPress,
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item {
                FileSettingsSection(
                    uiState = uiState,
                    onAddToUpNextChange = onAddToUpNextChange,
                    onDeleteLocalFileChange = onDeleteLocalFileChange,
                    onDeleteCloudFileChange = onDeleteCloudFileChange,
                )
            }
            item {
                PlusFeaturesSection(
                    uiState = uiState,
                    onCloudAutoUploadChange = onCloudAutoUploadChange,
                    onCloudAutoDownloadChange = onCloudAutoDownloadChange,
                    onCloudOnlyOnWifiChange = onCloudOnlyOnWifiChange,
                    onUpgradeClick = onUpgradeClick,
                )
            }
            if (uiState.isUpgradeBannerVisible) {
                item {
                    Banner(
                        title = stringResource(LR.string.pocket_casts_plus),
                        description = stringResource(LR.string.profile_get_plus),
                        actionLabel = stringResource(LR.string.plus_learn_more_button),
                        icon = painterResource(IR.drawable.ic_plus_feature_cloud_storage),
                        onActionClick = onUpgradeClick,
                        onDismiss = onUpgradeBannerDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SettingsSection.horizontalPadding, vertical = 16.dp)
                            .clickable(role = Role.Button, onClick = onUpgradeClick),
                    )
                }
            }
        }
    }
}

@Composable
private fun FileSettingsSection(
    uiState: CloudSettingsViewModel.UiState,
    onAddToUpNextChange: (Boolean) -> Unit,
    onDeleteLocalFileChange: (Boolean) -> Unit,
    onDeleteCloudFileChange: (Boolean) -> Unit,
) {
    SettingSection {
        SettingRow(
            primaryText = stringResource(LR.string.profile_cloud_auto_add_to_up_next),
            secondaryText = stringResource(LR.string.profile_cloud_all_files_added),
            toggle = SettingRowToggle.Switch(checked = uiState.cloudAddToUpNext),
            modifier = Modifier.toggleable(
                value = uiState.cloudAddToUpNext,
                role = Role.Switch,
            ) { onAddToUpNextChange(!uiState.cloudAddToUpNext) },
        )
        SettingSectionHeader(text = stringResource(LR.string.profile_cloud_after_playing))
        SettingRow(
            primaryText = stringResource(LR.string.profile_cloud_delete_local_file),
            toggle = SettingRowToggle.Switch(checked = uiState.deleteLocalFileAfterPlaying),
            modifier = Modifier.toggleable(
                value = uiState.deleteLocalFileAfterPlaying,
                role = Role.Switch,
            ) { onDeleteLocalFileChange(!uiState.deleteLocalFileAfterPlaying) },
        )
        if (uiState.isSignedInAsPlusOrPatron) {
            SettingRow(
                primaryText = stringResource(LR.string.profile_cloud_delete_cloud_file),
                toggle = SettingRowToggle.Switch(checked = uiState.deleteCloudFileAfterPlaying),
                modifier = Modifier.toggleable(
                    value = uiState.deleteCloudFileAfterPlaying,
                    role = Role.Switch,
                ) { onDeleteCloudFileChange(!uiState.deleteCloudFileAfterPlaying) },
            )
        }
    }
}

@Composable
private fun PlusFeaturesSection(
    uiState: CloudSettingsViewModel.UiState,
    onCloudAutoUploadChange: (Boolean) -> Unit,
    onCloudAutoDownloadChange: (Boolean) -> Unit,
    onCloudOnlyOnWifiChange: (Boolean) -> Unit,
    onUpgradeClick: () -> Unit,
) {
    val isPlus = uiState.isSignedInAsPlusOrPatron
    SettingSection(showDivider = false) {
        PlusFeaturesSectionHeader(isLocked = !isPlus)
        SettingRow(
            primaryText = stringResource(LR.string.profile_cloud_auto_upload_to_cloud),
            secondaryText = stringResource(LR.string.profile_cloud_files_added_uploaded),
            enabled = isPlus,
            toggle = SettingRowToggle.Switch(checked = uiState.cloudAutoUpload, enabled = isPlus),
            modifier = Modifier.toggleable(
                value = uiState.cloudAutoUpload,
                role = Role.Switch,
            ) {
                if (isPlus) onCloudAutoUploadChange(!uiState.cloudAutoUpload) else onUpgradeClick()
            },
        )
        SettingRow(
            primaryText = stringResource(LR.string.profile_cloud_auto_download_from_cloud),
            secondaryText = stringResource(LR.string.profile_cloud_files_added_downloaded),
            enabled = isPlus,
            toggle = SettingRowToggle.Switch(checked = uiState.cloudAutoDownload, enabled = isPlus),
            modifier = Modifier.toggleable(
                value = uiState.cloudAutoDownload,
                role = Role.Switch,
            ) {
                if (isPlus) onCloudAutoDownloadChange(!uiState.cloudAutoDownload) else onUpgradeClick()
            },
        )
        SettingRow(
            primaryText = stringResource(LR.string.profile_cloud_only_on_wifi),
            enabled = isPlus,
            toggle = SettingRowToggle.Switch(checked = uiState.cloudDownloadOnlyOnWifi, enabled = isPlus),
            modifier = Modifier.toggleable(
                value = uiState.cloudDownloadOnlyOnWifi,
                role = Role.Switch,
            ) {
                if (isPlus) onCloudOnlyOnWifiChange(!uiState.cloudDownloadOnlyOnWifi) else onUpgradeClick()
            },
        )
    }
}

@Composable
private fun PlusFeaturesSectionHeader(isLocked: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
            start = SettingsSection.indentedStartPadding,
            end = SettingsSection.horizontalPadding,
            top = SettingsSection.verticalPadding,
            bottom = SettingsSection.verticalPadding,
        ),
    ) {
        TextH40(
            text = stringResource(LR.string.profile_cloud_plus_features),
            color = MaterialTheme.theme.colors.primaryInteractive01,
        )
        if (isLocked) {
            Spacer(Modifier.width(8.dp))
            GradientIcon(
                painter = painterResource(R.drawable.ic_lock),
                colors = listOf(
                    colorResource(UR.color.plus_gold_dark),
                    colorResource(UR.color.plus_gold_light),
                ),
                modifier = Modifier.size(width = 12.dp, height = 16.dp),
            )
        }
    }
}

@PreviewRegularDevice
@Composable
private fun CloudSettingsPagePlusPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        CloudSettingsContent(
            uiState = CloudSettingsViewModel.UiState(
                cloudAddToUpNext = true,
                deleteLocalFileAfterPlaying = true,
                deleteCloudFileAfterPlaying = false,
                cloudAutoUpload = true,
                cloudAutoDownload = false,
                cloudDownloadOnlyOnWifi = true,
                isSignedInAsPlusOrPatron = true,
                isUpgradeBannerVisible = false,
            ),
            bottomInset = 0.dp,
            onBackPress = {},
            onUpgradeClick = {},
            onAddToUpNextChange = {},
            onDeleteLocalFileChange = {},
            onDeleteCloudFileChange = {},
            onCloudAutoUploadChange = {},
            onCloudAutoDownloadChange = {},
            onCloudOnlyOnWifiChange = {},
            onUpgradeBannerDismiss = {},
        )
    }
}

@PreviewRegularDevice
@Composable
private fun CloudSettingsPageFreePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        CloudSettingsContent(
            uiState = CloudSettingsViewModel.UiState(
                cloudAddToUpNext = true,
                deleteLocalFileAfterPlaying = false,
                deleteCloudFileAfterPlaying = false,
                cloudAutoUpload = false,
                cloudAutoDownload = false,
                cloudDownloadOnlyOnWifi = false,
                isSignedInAsPlusOrPatron = false,
                isUpgradeBannerVisible = true,
            ),
            bottomInset = 0.dp,
            onBackPress = {},
            onUpgradeClick = {},
            onAddToUpNextChange = {},
            onDeleteLocalFileChange = {},
            onDeleteCloudFileChange = {},
            onCloudAutoUploadChange = {},
            onCloudAutoDownloadChange = {},
            onCloudOnlyOnWifiChange = {},
            onUpgradeBannerDismiss = {},
        )
    }
}
