package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.SettingInfoRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveLimit
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PodcastSettingsArchivePage(
    podcast: Podcast,
    onChangeAutoArchive: (Boolean) -> Unit,
    onChangeAutoArchiveAfterPlayingSetting: () -> Unit,
    onChangeAutoArchiveAfterInactiveSetting: () -> Unit,
    onChangeAutoArchiveLimitSetting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
    ) {
        SettingSection(
            showDivider = podcast.overrideGlobalArchive,
        ) {
            SettingRow(
                primaryText = stringResource(LR.string.podcast_settings_auto_archive_custom),
                secondaryText = stringResource(LR.string.podcast_settings_auto_archive_custom_summary),
                toggle = SettingRowToggle.Switch(checked = podcast.overrideGlobalArchive),
                modifier = Modifier.toggleable(
                    value = podcast.overrideGlobalArchive,
                    role = Role.Switch,
                    onValueChange = onChangeAutoArchive,
                ),
            )
        }
        AnimatedVisibility(
            visible = podcast.overrideGlobalArchive,
        ) {
            Column {
                SettingSection {
                    val afterPlaying = rememberLastNotNull(podcast.autoArchiveAfterPlaying, AutoArchiveAfterPlaying.Never)
                    SettingRow(
                        primaryText = stringResource(LR.string.settings_archive_played),
                        secondaryText = stringResource(afterPlaying.stringRes),
                        modifier = Modifier.clickable(
                            role = Role.Button,
                            onClick = onChangeAutoArchiveAfterPlayingSetting,
                        ),
                    )
                    val afterInactive = rememberLastNotNull(podcast.autoArchiveInactive, AutoArchiveInactive.Never)
                    SettingRow(
                        primaryText = stringResource(LR.string.settings_auto_archive_inactive),
                        secondaryText = stringResource(afterInactive.stringRes),
                        modifier = Modifier.clickable(
                            role = Role.Button,
                            onClick = onChangeAutoArchiveAfterInactiveSetting,
                        ),
                    )
                    SettingInfoRow(
                        text = stringResource(LR.string.settings_auto_archive_time_limits),
                    )
                }
                SettingSection(
                    showDivider = false,
                ) {
                    val episodeLimit = rememberLastNotNull(podcast.autoArchiveEpisodeLimit, AutoArchiveLimit.None)
                    SettingRow(
                        primaryText = stringResource(LR.string.settings_auto_archive_episode_limit),
                        secondaryText = stringResource(episodeLimit.stringRes),
                        modifier = Modifier.clickable(
                            role = Role.Button,
                            onClick = onChangeAutoArchiveLimitSetting,
                        ),
                    )
                    SettingInfoRow(
                        text = stringResource(LR.string.settings_auto_archive_episode_limit_summary),
                    )
                }
            }
        }
    }
}

@Composable
private fun <T : Any> rememberLastNotNull(value: T?, fallback: T): T {
    var lastNonNull by remember { mutableStateOf(value) }
    if (value != null) {
        lastNonNull = value
    }
    return lastNonNull ?: fallback
}

@Preview
@Composable
private fun PodcastSettingsArchivePagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppTheme(themeType) {
        PodcastSettingsArchivePage(
            podcast = Podcast(
                uuid = "uuid",
                overrideGlobalArchive = true,
            ),
            onChangeAutoArchive = {},
            onChangeAutoArchiveAfterPlayingSetting = {},
            onChangeAutoArchiveAfterInactiveSetting = {},
            onChangeAutoArchiveLimitSetting = {},
            modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi02),
        )
    }
}
