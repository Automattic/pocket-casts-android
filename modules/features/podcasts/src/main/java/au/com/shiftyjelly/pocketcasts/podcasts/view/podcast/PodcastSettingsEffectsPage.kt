package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.NumberStepper
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PodcastSettingsEffectsPage(
    podcast: Podcast,
    toolbarColors: ToolbarColors,
    onChangePlaybackEffects: (Boolean) -> Unit,
    onDecrementSpeed: () -> Unit,
    onIncrementSpeed: () -> Unit,
    onChangeTrimMode: (Boolean) -> Unit,
    onChangeTrimModeSetting: () -> Unit,
    onChangeVolumeBoost: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize(),
    ) {
        SettingRow(
            primaryText = stringResource(LR.string.settings_podcast_custom),
            toggle = SettingRowToggle.Switch(
                checked = podcast.overrideGlobalEffects,
                enabled = true,
            ),
            modifier = Modifier.toggleable(
                value = podcast.overrideGlobalEffects,
                role = Role.Switch,
                onValueChange = onChangePlaybackEffects,
            ),
        )
        AnimatedVisibility(
            visible = podcast.overrideGlobalEffects,
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.Companion.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 8.dp),
                ) {
                    SettingRow(
                        primaryText = stringResource(LR.string.player_effects_speed),
                        icon = painterResource(IR.drawable.ic_speed),
                        iconTint = toolbarColors.iconComposeColor,
                        modifier = Modifier.weight(1f),
                    )
                    NumberStepper(
                        onMinusClick = onDecrementSpeed,
                        onPlusClick = onIncrementSpeed,
                        minusContentDescription = LR.string.player_effects_speed_down,
                        plusContentDescription = LR.string.player_effects_speed_up,
                        tint = toolbarColors.iconComposeColor,
                    ) {
                        val locale = LocalResources.current.configuration.locales[0]
                        TextP40(
                            text = String.format(locale, "%.1fx", podcast.playbackSpeed),
                            color = MaterialTheme.theme.colors.primaryText01,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(min = 48.dp),
                        )
                    }
                }
                SettingRow(
                    primaryText = stringResource(LR.string.player_effects_trim_silence),
                    icon = painterResource(IR.drawable.ic_trim),
                    iconTint = toolbarColors.iconComposeColor,
                    toggle = SettingRowToggle.Switch(
                        checked = podcast.trimMode != TrimMode.OFF,
                        enabled = true,
                    ),
                    modifier = Modifier.toggleable(
                        value = podcast.trimMode != TrimMode.OFF,
                        role = Role.Switch,
                        onValueChange = onChangeTrimMode,
                    ),
                )
                AnimatedVisibility(
                    visible = podcast.trimMode != TrimMode.OFF,
                ) {
                    SettingRow(
                        primaryText = stringResource(LR.string.player_effects_trim_level),
                        secondaryText = when (podcast.trimMode) {
                            TrimMode.OFF -> stringResource(LR.string.off)
                            TrimMode.LOW -> stringResource(LR.string.player_effects_trim_mild)
                            TrimMode.MEDIUM -> stringResource(LR.string.player_effects_trim_medium)
                            TrimMode.HIGH -> stringResource(LR.string.player_effects_trim_mad_max)
                        },
                        modifier = Modifier.clickable(
                            role = Role.Button,
                            onClick = onChangeTrimModeSetting,
                        ),
                    )
                }
                SettingRow(
                    primaryText = stringResource(LR.string.player_effects_volume_boost),
                    icon = painterResource(R.drawable.ic_volumeboost),
                    iconTint = toolbarColors.iconComposeColor,
                    toggle = SettingRowToggle.Switch(
                        checked = podcast.isVolumeBoosted,
                        enabled = true,
                    ),
                    modifier = Modifier.toggleable(
                        value = podcast.isVolumeBoosted,
                        role = Role.Switch,
                        onValueChange = onChangeVolumeBoost,
                    ),
                )
            }
        }
    }
}

@Preview
@Composable
private fun PodcastSettingsEffectsPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    AppTheme(themeType) {
        PodcastSettingsEffectsPage(
            podcast = Podcast(
                uuid = "uuid",
                overrideGlobalEffects = true,
                playbackSpeed = 2.8,
                trimMode = TrimMode.HIGH,
                isVolumeBoosted = true,
            ),
            toolbarColors = ToolbarColors.podcast(
                lightColor = "#EC0404".toColorInt(),
                darkColor = "#F47C84".toColorInt(),
                theme = themeType,
            ),
            onChangePlaybackEffects = {},
            onDecrementSpeed = {},
            onIncrementSpeed = {},
            onChangeTrimMode = {},
            onChangeTrimModeSetting = {},
            onChangeVolumeBoost = {},
            modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi02),
        )
    }
}
