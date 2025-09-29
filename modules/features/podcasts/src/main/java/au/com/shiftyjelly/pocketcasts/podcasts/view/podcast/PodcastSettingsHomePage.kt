package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.NumberStepper
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.toFriendlyString
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastSettingsViewModel
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistPreview
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import kotlin.time.Duration.Companion.seconds
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PodcastSettingsHomePage(
    uiState: PodcastSettingsViewModel.UiState,
    toolbarColors: ToolbarColors,
    onChangeNotifications: (Boolean) -> Unit,
    onChangeAutoDownload: (Boolean) -> Unit,
    onChangeAddToUpNext: (Boolean) -> Unit,
    onChangeUpNextPosition: () -> Unit,
    onChangeUpNextGlobalSettings: () -> Unit,
    onChangeAutoArchiveSettings: () -> Unit,
    onChangePlaybackEffectsSettings: () -> Unit,
    onDecrementSkipFirst: () -> Unit,
    onIncrementSkipFirst: () -> Unit,
    onDecrementSkipLast: () -> Unit,
    onIncrementSkipLast: () -> Unit,
    onChangePlaylistSettings: () -> Unit,
    onUnfollow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val podcast = uiState.podcast
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
    ) {
        SettingRow(
            primaryText = stringResource(LR.string.podcast_notifications),
            icon = if (podcast.isShowNotifications) {
                painterResource(IR.drawable.ic_notifications_on)
            } else {
                painterResource(IR.drawable.ic_notifications)
            },
            iconTint = toolbarColors.iconComposeColor,
            toggle = SettingRowToggle.Switch(
                checked = podcast.isShowNotifications,
                enabled = true,
            ),
            modifier = Modifier.toggleable(
                value = podcast.isShowNotifications,
                role = Role.Switch,
                onValueChange = onChangeNotifications,
            ),
        )
        SettingRow(
            primaryText = stringResource(LR.string.podcast_settings_auto_download),
            secondaryText = stringResource(LR.string.podcast_settings_auto_download_summary),
            icon = painterResource(IR.drawable.ic_download),
            iconTint = toolbarColors.iconComposeColor,
            toggle = SettingRowToggle.Switch(
                checked = podcast.isAutoDownloadNewEpisodes,
                enabled = true,
            ),
            modifier = Modifier.toggleable(
                value = podcast.isAutoDownloadNewEpisodes,
                role = Role.Switch,
                onValueChange = onChangeAutoDownload,
            ),
        )
        SettingRow(
            primaryText = stringResource(LR.string.podcast_settings_add_to_up_next),
            secondaryText = stringResource(LR.string.podcast_settings_add_to_up_next_summary),
            icon = painterResource(IR.drawable.ic_upnext),
            iconTint = toolbarColors.iconComposeColor,
            toggle = SettingRowToggle.Switch(
                checked = podcast.autoAddToUpNext != Podcast.AutoAddUpNext.OFF,
                enabled = true,
            ),
            modifier = Modifier.toggleable(
                value = podcast.autoAddToUpNext != Podcast.AutoAddUpNext.OFF,
                role = Role.Switch,
                onValueChange = onChangeAddToUpNext,
            ),
        )
        AnimatedVisibility(
            visible = podcast.autoAddToUpNext != Podcast.AutoAddUpNext.OFF,
        ) {
            Column {
                SettingRow(
                    primaryText = stringResource(LR.string.podcast_settings_position),
                    secondaryText = when (podcast.autoAddToUpNext) {
                        Podcast.AutoAddUpNext.OFF -> stringResource(LR.string.play_last)
                        Podcast.AutoAddUpNext.PLAY_LAST -> stringResource(LR.string.play_last)
                        Podcast.AutoAddUpNext.PLAY_NEXT -> stringResource(LR.string.play_next)
                    },
                    modifier = Modifier.clickable(
                        role = Role.Button,
                        onClick = onChangeUpNextPosition,
                    ),
                )
                SettingRow(
                    primaryText = stringResource(LR.string.podcast_settings_up_next_global),
                    secondaryText = stringResource(LR.string.podcast_settings_up_next_episode_limit, uiState.globalUpNextLimit),
                    additionalContent = {
                        TextP50(
                            text = stringResource(LR.string.settings_auto_up_next_limit_reached_stop_summary, uiState.globalUpNextLimit),
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.theme.colors.primaryText02,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    },
                    modifier = Modifier.clickable(
                        role = Role.Button,
                        onClick = onChangeUpNextGlobalSettings,
                    ),
                )
            }
        }
        SettingRow(
            primaryText = stringResource(LR.string.podcast_settings_auto_archive),
            icon = painterResource(IR.drawable.ic_archive),
            iconTint = toolbarColors.iconComposeColor,
            modifier = Modifier.clickable(
                role = Role.Button,
                onClick = onChangeAutoArchiveSettings,
            ),
        )
        SettingRow(
            primaryText = stringResource(LR.string.podcast_settings_playback_effects),
            secondaryText = if (podcast.overrideGlobalEffects) {
                buildString {
                    append(stringResource(LR.string.podcast_effects_summary_speed, podcast.playbackSpeed.toString()))
                    append(", ")
                    append(
                        if (podcast.isSilenceRemoved) {
                            stringResource(LR.string.podcast_effects_summary_trim_silence_on)
                        } else {
                            stringResource(LR.string.podcast_effects_summary_trim_silence_off)
                        },
                    )
                    append(", ")
                    append(
                        if (podcast.isSilenceRemoved) {
                            stringResource(LR.string.podcast_effects_summary_volume_boost_on)
                        } else {
                            stringResource(LR.string.podcast_effects_summary_volume_boost_off)
                        },
                    )
                }
            } else {
                stringResource(LR.string.podcast_effects_summary_default)
            },
            icon = if (podcast.overrideGlobalEffects) {
                painterResource(R.drawable.ic_effects_on)
            } else {
                painterResource(R.drawable.ic_effects_off)
            },
            iconTint = toolbarColors.iconComposeColor,
            modifier = Modifier.clickable(
                role = Role.Button,
                onClick = onChangePlaybackEffectsSettings,
            ),
        )
        Row(
            verticalAlignment = Alignment.Companion.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
        ) {
            val resources = LocalResources.current
            val duration = remember(podcast.startFromSecs, resources) {
                podcast.startFromSecs.seconds.toFriendlyString(
                    resources = resources,
                    maxPartCount = 2,
                    pluralResourceId = { it.shortResourceId },
                )
            }
            SettingRow(
                primaryText = stringResource(LR.string.podcast_settings_skip_first),
                secondaryText = duration,
                icon = painterResource(R.drawable.ic_skipintros),
                iconTint = toolbarColors.iconComposeColor,
                modifier = Modifier.weight(1f),
            )
            NumberStepper(
                onMinusClick = onDecrementSkipFirst,
                onPlusClick = onIncrementSkipFirst,
                tint = toolbarColors.iconComposeColor,
            )
        }
        Row(
            verticalAlignment = Alignment.Companion.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
        ) {
            val resources = LocalResources.current
            val duration = remember(podcast.skipLastSecs, resources) {
                podcast.skipLastSecs.seconds.toFriendlyString(
                    resources = resources,
                    maxPartCount = 2,
                    pluralResourceId = { it.shortResourceId },
                )
            }
            SettingRow(
                primaryText = stringResource(LR.string.podcast_settings_skip_last),
                secondaryText = duration,
                icon = painterResource(R.drawable.ic_skip_outro),
                iconTint = toolbarColors.iconComposeColor,
                modifier = Modifier.weight(1f),
            )
            NumberStepper(
                onMinusClick = onDecrementSkipLast,
                onPlusClick = onIncrementSkipLast,
                tint = toolbarColors.iconComposeColor,
            )
        }
        if (uiState.playlists.isNotEmpty()) {
            val usePlaylists = FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)

            SettingRow(
                primaryText = if (usePlaylists) {
                    stringResource(LR.string.smart_playlists)
                } else {
                    stringResource(LR.string.filters)
                },
                secondaryText = when (uiState.selectedPlaylists.size) {
                    0 -> if (usePlaylists) {
                        stringResource(LR.string.podcast_not_in_playlists)
                    } else {
                        stringResource(LR.string.podcast_not_in_filters)
                    }

                    else -> {
                        val titles = remember(uiState.selectedPlaylists) {
                            uiState.selectedPlaylists.joinToString(separator = ", ") { it.title }
                        }
                        stringResource(LR.string.podcast_included_in_filters, titles)
                    }
                },
                icon = if (usePlaylists) {
                    painterResource(IR.drawable.ic_playlists)
                } else {
                    painterResource(IR.drawable.ic_filters)
                },
                iconTint = toolbarColors.iconComposeColor,
                modifier = Modifier.clickable(
                    role = Role.Button,
                    onClick = onChangePlaylistSettings,
                ),
            )
        }
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        Box(
            contentAlignment = Alignment.Companion.Center,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(
                    indication = ripple(color = MaterialTheme.theme.colors.support05),
                    interactionSource = null,
                    role = Role.Button,
                    onClick = onUnfollow,
                ),
        ) {
            TextH30(
                text = stringResource(LR.string.unsubscribe),
                color = MaterialTheme.theme.colors.support05,
            )
        }
    }
}

@Preview
@Composable
private fun PodcastSettingsHomePagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppTheme(themeType) {
        PodcastSettingsHomePage(
            toolbarColors = ToolbarColors.podcast(
                lightColor = "#EC0404".toColorInt(),
                darkColor = "#F47C84".toColorInt(),
                theme = themeType,
            ),
            uiState = PodcastSettingsViewModel.UiState(
                podcast = Podcast(
                    uuid = "podcast-uuid",
                    isShowNotifications = true,
                    autoDownloadStatus = Podcast.AUTO_DOWNLOAD_NEW_EPISODES,
                    autoAddToUpNext = Podcast.AutoAddUpNext.PLAY_NEXT,
                    overrideGlobalEffects = true,
                    playbackSpeed = 2.3,
                    startFromSecs = 30,
                    skipLastSecs = 60,
                ),
                playlists = List(3) { index ->
                    SmartPlaylistPreview(
                        uuid = "playlist-uuid-$index",
                        title = "Playlist $index",
                        episodeCount = 0,
                        artworkPodcastUuids = emptyList(),
                        settings = Playlist.Settings.ForPreview,
                        smartRules = SmartRules.Default.copy(
                            podcasts = PodcastsRule.Selected(uuids = setOf("podcast-uuid")),
                        ),
                        icon = PlaylistIcon(0),
                    )
                },
                globalUpNextLimit = 100,
            ),
            onChangeNotifications = {},
            onChangeAutoDownload = {},
            onChangeAddToUpNext = {},
            onChangeUpNextPosition = {},
            onChangeUpNextGlobalSettings = {},
            onChangeAutoArchiveSettings = {},
            onChangePlaybackEffectsSettings = {},
            onDecrementSkipFirst = {},
            onIncrementSkipFirst = {},
            onDecrementSkipLast = {},
            onIncrementSkipLast = {},
            onChangePlaylistSettings = {},
            onUnfollow = {},
            modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi02),
        )
    }
}
