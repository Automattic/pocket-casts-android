package au.com.shiftyjelly.pocketcasts.player.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.components.TipPosition
import au.com.shiftyjelly.pocketcasts.compose.components.TooltipPopup
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun UpNextSortButton(
    showTooltip: Boolean,
    onClick: () -> Unit,
    onTooltipClick: () -> Unit,
    onTooltipShow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        IconButton(onClick = onClick) {
            Icon(
                painter = painterResource(IR.drawable.ic_sort),
                contentDescription = stringResource(LR.string.player_up_next_sort),
                tint = MaterialTheme.theme.colors.primaryIcon02,
            )
        }
        var tooltipComposed by remember { mutableStateOf(showTooltip) }
        val tooltipAlpha = remember { Animatable(if (showTooltip) 1f else 0f) }
        LaunchedEffect(showTooltip) {
            if (showTooltip) {
                tooltipComposed = true
                tooltipAlpha.snapTo(1f)
            } else {
                tooltipAlpha.animateTo(0f, tween())
                tooltipComposed = false
            }
        }

        if (tooltipComposed) {
            CallOnce {
                onTooltipShow()
            }
            TooltipPopup(
                title = stringResource(LR.string.up_next_sort_duration_tooltip_title),
                body = stringResource(LR.string.up_next_sort_duration_tooltip_body),
                tipPosition = TipPosition.TopEnd,
                anchorOffset = DpOffset(0.dp, 4.dp),
                maxWidth = 300.dp,
                onClick = onTooltipClick,
                modifier = Modifier.alpha(tooltipAlpha.value),
            )
        }
    }
}

@Preview
@Composable
private fun UpNextSortButtonPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        UpNextSortButton(
            showTooltip = false,
            onClick = {},
            onTooltipClick = {},
            onTooltipShow = {},
        )
    }
}
