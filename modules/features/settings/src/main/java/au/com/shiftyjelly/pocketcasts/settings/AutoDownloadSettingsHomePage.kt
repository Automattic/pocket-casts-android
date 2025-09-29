package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.models.type.AutoDownloadLimitSetting
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AutoDownloadSettingsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AutoDownloadSettingsHomePage(
    uiState: UiState,
    onChangeUpNextDownload: (Boolean) -> Unit,
    onChangeNewEpisodesDownload: (Boolean) -> Unit,
    onChangePodcastsSetting: () -> Unit,
    onChangeOnFollowDownload: (Boolean) -> Unit,
    onChangeAutoDownloadLimitSetting: () -> Unit,
    onChangePlaylistsSetting: () -> Unit,
    onChangeOnUnmeteredDownload: (Boolean) -> Unit,
    onChangeOnlyWhenChargingDownload: (Boolean) -> Unit,
    onStopAllDownloads: () -> Unit,
    onClearDownloadErrors: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabledPodcastsCount = remember(uiState.podcasts) {
        uiState.podcasts.count { it.isAutoDownloadNewEpisodes }
    }
    val enabledPlaylistsCount = remember(uiState.playlists) {
        uiState.playlists.count { it.settings.isAutoDownloadEnabled }
    }

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
    ) {
        SettingSection(
            heading = stringResource(LR.string.up_next),
        ) {
            SettingRow(
                primaryText = stringResource(LR.string.settings_auto_download_up_next),
                toggle = SettingRowToggle.Switch(
                    checked = uiState.isUpNextDownloadEnabled,
                ),
                modifier = Modifier.toggleable(
                    value = uiState.isUpNextDownloadEnabled,
                    role = Role.Switch,
                    onValueChange = onChangeUpNextDownload,
                ),
            )
        }
        SettingSection(
            heading = stringResource(LR.string.podcasts),
        ) {
            Column {
                SettingRow(
                    primaryText = stringResource(LR.string.settings_auto_download_new_episodes),
                    secondaryText = stringResource(LR.string.settings_auto_download_new_episodes_description),
                    toggle = SettingRowToggle.Switch(
                        checked = uiState.isNewEpisodesDownloadEnabled,
                    ),
                    modifier = Modifier.toggleable(
                        value = uiState.isNewEpisodesDownloadEnabled,
                        role = Role.Switch,
                        onValueChange = onChangeNewEpisodesDownload,
                    ),
                )
                AnimatedVisibility(
                    visible = uiState.isNewEpisodesDownloadEnabled,
                ) {
                    SettingRow(
                        primaryText = stringResource(LR.string.settings_choose_podcasts),
                        secondaryText = when (enabledPodcastsCount) {
                            uiState.podcasts.size -> stringResource(LR.string.podcasts_selected_all)
                            else -> pluralStringResource(LR.plurals.podcasts_selected_count, enabledPodcastsCount, enabledPodcastsCount)
                        },
                        modifier = Modifier.clickable(
                            role = Role.Button,
                            onClick = onChangePodcastsSetting,
                        ),
                    )
                }
                SettingRow(
                    primaryText = stringResource(LR.string.settings_auto_download_on_follow_podcast),
                    secondaryText = pluralStringResource(
                        LR.plurals.settings_auto_download_on_follow_podcast_description,
                        uiState.autoDownloadLimit.episodeCount,
                        stringResource(uiState.autoDownloadLimit.episodeCountRes),
                    ),
                    toggle = SettingRowToggle.Switch(
                        checked = uiState.isOnFollowDownloadEnabled,
                    ),
                    modifier = Modifier.toggleable(
                        value = uiState.isOnFollowDownloadEnabled,
                        role = Role.Switch,
                        onValueChange = onChangeOnFollowDownload,
                    ),
                )
                SettingRow(
                    primaryText = stringResource(LR.string.settings_auto_download_limit),
                    secondaryText = stringResource(uiState.autoDownloadLimit.titleRes),
                    modifier = Modifier.clickable(
                        role = Role.Button,
                        onClick = onChangeAutoDownloadLimitSetting,
                    ),
                )
            }
        }
        val usePlaylists = FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)
        SettingSection(
            heading = if (usePlaylists) {
                stringResource(LR.string.playlists)
            } else {
                stringResource(LR.string.filters)
            },
        ) {
            SettingRow(
                primaryText = if (usePlaylists) {
                    stringResource(LR.string.settings_choose_playlists)
                } else {
                    stringResource(LR.string.settings_auto_download_filters_episodes)
                },
                secondaryText = if (usePlaylists) {
                    when (enabledPlaylistsCount) {
                        uiState.playlists.size -> stringResource(LR.string.playlists_selected_all)
                        else -> pluralStringResource(LR.plurals.playlists_selected_count, enabledPlaylistsCount, enabledPlaylistsCount)
                    }
                } else {
                    when (enabledPlaylistsCount) {
                        1 -> stringResource(LR.string.filters_chosen_singular)
                        else -> stringResource(LR.string.filters_chosen_plural, enabledPlaylistsCount)
                    }
                },
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClick = onChangePlaylistsSetting,
                ),
            )
        }
        SettingSection(
            heading = stringResource(LR.string.settings),
        ) {
            SettingRow(
                primaryText = stringResource(LR.string.settings_auto_download_unmetered),
                secondaryText = stringResource(LR.string.settings_auto_download_unmetered_summary),
                toggle = SettingRowToggle.Switch(
                    checked = uiState.isOnUnmeteredDownloadEnabled,
                ),
                modifier = Modifier.toggleable(
                    value = uiState.isOnUnmeteredDownloadEnabled,
                    role = Role.Switch,
                    onValueChange = onChangeOnUnmeteredDownload,
                ),
            )
            SettingRow(
                primaryText = stringResource(LR.string.settings_auto_download_charging),
                toggle = SettingRowToggle.Switch(
                    checked = uiState.isOnlyWhenChargingDownloadEnabled,
                ),
                modifier = Modifier.toggleable(
                    value = uiState.isOnlyWhenChargingDownloadEnabled,
                    role = Role.Switch,
                    onValueChange = onChangeOnlyWhenChargingDownload,
                ),
            )
        }
        SettingSection(
            heading = stringResource(LR.string.downloads),
        ) {
            SettingRow(
                primaryText = stringResource(LR.string.settings_auto_download_stop_all),
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClick = onStopAllDownloads,
                ),
            )
            SettingRow(
                primaryText = stringResource(LR.string.settings_auto_download_clear_errors),
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClick = onClearDownloadErrors,
                ),
            )
        }
    }
}

@PreviewRegularDevice
@Composable
private fun AutoDownloadSettingsHomePagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AutoDownloadSettingsHomePage(
            uiState = UiState(
                isUpNextDownloadEnabled = true,
                isNewEpisodesDownloadEnabled = true,
                isOnFollowDownloadEnabled = true,
                autoDownloadLimit = AutoDownloadLimitSetting.TEN_LATEST_EPISODE,
                isOnUnmeteredDownloadEnabled = true,
                isOnlyWhenChargingDownloadEnabled = true,
                podcasts = emptyList(),
                playlists = emptyList(),
            ),
            onChangeUpNextDownload = {},
            onChangeNewEpisodesDownload = {},
            onChangePodcastsSetting = {},
            onChangeOnFollowDownload = {},
            onChangeAutoDownloadLimitSetting = {},
            onChangePlaylistsSetting = {},
            onChangeOnUnmeteredDownload = {},
            onChangeOnlyWhenChargingDownload = {},
            onStopAllDownloads = {},
            onClearDownloadErrors = {},
        )
    }
}
