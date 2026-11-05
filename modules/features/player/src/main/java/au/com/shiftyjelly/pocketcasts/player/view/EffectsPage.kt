package au.com.shiftyjelly.pocketcasts.player.view

import androidx.annotation.DrawableRes
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Switch
import androidx.compose.material.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.Pill
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.SegmentedTabBar
import au.com.shiftyjelly.pocketcasts.compose.components.SegmentedTabBarDefaults
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class EffectsColors(
    val background: Color,
    val contrast01: Color,
    val contrast02: Color,
    val contrast03: Color,
    val contrast05: Color,
    val contrast06: Color,
    val switchTint: Color,
    val switchOff: Color,
)

@Composable
internal fun EffectsPage(
    speedText: String,
    trimMode: TrimMode,
    trimDetail: String,
    isVolumeBoosted: Boolean,
    colors: EffectsColors,
    onSpeedDownClick: () -> Unit,
    onSpeedUpClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onTrimToggle: (Boolean) -> Unit,
    onTrimModeSelect: (TrimMode) -> Unit,
    onVolumeBoostToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    settingsTabBar: @Composable () -> Unit = {},
) {
    val isTrimOn = trimMode != TrimMode.OFF
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Pill(
                backgroundColor = colors.contrast01,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.TopCenter),
            )
            TextH30(
                text = stringResource(LR.string.podcast_playback_effects),
                color = colors.contrast01,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp),
            )
        }
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            settingsTabBar()
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(start = 16.dp, top = 30.dp, end = 16.dp)
                .height(29.dp),
        ) {
            EffectIcon(R.drawable.ic_speed, colors.contrast02)
            TextP40(
                text = stringResource(LR.string.player_effects_speed),
                color = colors.contrast01,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f),
            )
            SpeedButton(IR.drawable.ic_minus, stringResource(LR.string.player_effects_speed_down), colors.contrast02, onSpeedDownClick)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .wrapContentHeight(unbounded = true)
                    .border(1.dp, colors.contrast05, RoundedCornerShape(4.dp))
                    .clickable(interactionSource = null, indication = null, onClick = onSpeedClick)
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                TextP40(text = speedText, color = colors.contrast01)
            }
            SpeedButton(IR.drawable.ic_effects_plus, stringResource(LR.string.player_effects_speed_up), colors.contrast02, onSpeedUpClick)
        }
        EffectsDivider(colors.contrast05)
        EffectSection(
            iconId = IR.drawable.ic_trim,
            title = stringResource(LR.string.player_effects_trim_silence),
            detail = trimDetail,
            isChecked = isTrimOn,
            colors = colors,
            onCheckedChange = onTrimToggle,
        ) {
            if (isTrimOn) {
                TrimModeTabBar(
                    trimMode = trimMode,
                    colors = colors,
                    onTrimModeSelect = onTrimModeSelect,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                )
            }
        }
        EffectsDivider(colors.contrast05)
        EffectSection(
            iconId = R.drawable.ic_volumeboost,
            title = stringResource(LR.string.player_effects_volume_boost),
            detail = stringResource(LR.string.player_effects_voices_sound_louder),
            isChecked = isVolumeBoosted,
            colors = colors,
            onCheckedChange = onVolumeBoostToggle,
            isDetailFullWidth = false,
        )
        val bottomPadding = with(LocalDensity.current) { ((if (isTrimOn) 16 else 68) * density).toInt().toDp() }
        Spacer(Modifier.height(bottomPadding))
    }
}

@Composable
private fun EffectSection(
    @DrawableRes iconId: Int,
    title: String,
    detail: String,
    isChecked: Boolean,
    colors: EffectsColors,
    onCheckedChange: (Boolean) -> Unit,
    isDetailFullWidth: Boolean = true,
    belowTitle: @Composable () -> Unit = {},
) {
    var switchWidth by remember { mutableIntStateOf(0) }
    Row(modifier = Modifier.padding(horizontal = 16.dp)) {
        EffectIcon(iconId, colors.contrast02)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextP40(
                    text = title,
                    color = colors.contrast01,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = colors.switchTint,
                        checkedTrackColor = colors.switchTint,
                        uncheckedThumbColor = colors.switchOff,
                        uncheckedTrackColor = colors.switchTint,
                    ),
                    modifier = Modifier
                        .semantics { contentDescription = title }
                        .onSizeChanged { switchWidth = it.width }
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
                            layout(placeable.width, 0) { placeable.place(0, -placeable.height / 2) }
                        },
                )
            }
            belowTitle()
            val detailEnd = if (isDetailFullWidth) with(LocalDensity.current) { switchWidth.toDp() } else 0.dp
            Box(modifier = Modifier.padding(top = 4.dp, end = detailEnd)) {
                TextP50(
                    text = detail,
                    color = colors.contrast02,
                )
            }
        }
    }
}

@Composable
private fun EffectIcon(@DrawableRes iconId: Int, tint: Color) {
    Icon(
        painter = painterResource(iconId),
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .size(29.dp)
            .padding(2.dp),
    )
}

@Composable
private fun SpeedButton(@DrawableRes iconId: Int, contentDescription: String, tint: Color, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.wrapContentHeight(unbounded = true),
    ) {
        Icon(painter = painterResource(iconId), contentDescription = contentDescription, tint = tint)
    }
}

@Composable
private fun EffectsDivider(color: Color) {
    HorizontalDivider(
        color = color,
        modifier = Modifier.padding(vertical = 15.5.dp),
    )
}

@Composable
private fun TrimModeTabBar(
    trimMode: TrimMode,
    colors: EffectsColors,
    onTrimModeSelect: (TrimMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SegmentedTabBar(
        items = listOf(
            stringResource(LR.string.player_effects_trim_mild),
            stringResource(LR.string.player_effects_trim_medium),
            stringResource(LR.string.player_effects_trim_mad_max),
        ),
        selectedIndex = trimMode.ordinal - 1,
        colors = SegmentedTabBarDefaults.colors.copy(
            selectedTabBackgroundColor = colors.contrast01,
            selectedTabTextColor = colors.background,
            unSelectedTabBackgroundColor = colors.contrast06,
            unSelectedTabTextColor = colors.contrast01,
            borderColor = colors.contrast03,
        ),
        onSelectItem = { index -> onTrimModeSelect(TrimMode.entries[index + 1]) },
        modifier = modifier,
    )
}
