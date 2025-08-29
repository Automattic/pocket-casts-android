package au.com.shiftyjelly.pocketcasts.referrals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.LocalColors
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.components.TipPosition
import au.com.shiftyjelly.pocketcasts.compose.components.TooltipPopup
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ReferralsIconWithTooltip(
    state: UiState,
    onIconClick: () -> Unit,
    onTooltipClick: () -> Unit,
    onTooltipShow: () -> Unit,
) {
    CallOnce {
        onTooltipShow()
    }

    when (state) {
        is UiState.Loading, UiState.NoOffer -> Unit
        is UiState.Loaded -> {
            ReferralsIconWithTooltip(
                state = state,
                onIconClick = onIconClick,
                onTooltipClick = onTooltipClick,
            )
        }
    }
}

@Composable
private fun ReferralsIconWithTooltip(
    state: UiState.Loaded,
    onIconClick: () -> Unit,
    onTooltipClick: () -> Unit,
) {
    if (state.showIcon) {
        Box {
            Icon(
                onIconClick = onIconClick,
                colors = LocalColors.current.colors,
            )

            if (state.showTooltip) {
                TooltipPopup(
                    title = stringResource(LR.string.referrals_tooltip_message, state.referralPlan.offerDurationText),
                    tipPosition = TipPosition.TopStart,
                    anchorOffset = DpOffset(0.dp, (-4).dp),
                    onClick = onTooltipClick,
                )
            }
        }
    }
}

@Composable
private fun Icon(
    onIconClick: () -> Unit,
    colors: ThemeColors,
) {
    IconButton(onClick = onIconClick) {
        Icon(
            painter = painterResource(id = R.drawable.ic_gift),
            contentDescription = stringResource(LR.string.gift),
            tint = colors.secondaryIcon01,
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun IconWithBadgePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Box(
            modifier = Modifier.background(MaterialTheme.theme.colors.secondaryUi01),
        ) {
            Icon(
                onIconClick = {},
                colors = LocalColors.current.colors,
            )
        }
    }
}
