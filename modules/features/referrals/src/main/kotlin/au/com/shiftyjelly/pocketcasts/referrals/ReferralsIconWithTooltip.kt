package au.com.shiftyjelly.pocketcasts.referrals

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.LocalColors
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.Tooltip
import au.com.shiftyjelly.pocketcasts.compose.images.CountBadge
import au.com.shiftyjelly.pocketcasts.compose.images.CountBadgeStyle
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ReferralsIconWithTooltip(
    viewModel: ReferralsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    if (state.showIcon) {
        IconWithBadge(
            onIconClick = {
                viewModel.onIconClick()
            },
            colors = LocalColors.current.colors,
            state = state,
        )

        Tooltip(
            show = state.showTooltip,
        ) {
            TooltipContent(state)
        }
    }
}

@Composable
private fun TooltipContent(
    state: ReferralsViewModel.UiState,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 16.dp),
    ) {
        TextH40(
            text = pluralStringResource(
                LR.plurals.referrals_tooltip_heading,
                state.badgeCount,
                state.badgeCount,
            ),
            fontWeight = FontWeight.W700,
        )
        Spacer(modifier = Modifier.height(4.dp))
        TextC70(
            text = stringResource(LR.string.referrals_tooltip_message, state.badgeCount),
            isUpperCase = false,
        )
    }
}

@Composable
private fun IconWithBadge(
    onIconClick: () -> Unit,
    colors: ThemeColors,
    state: ReferralsViewModel.UiState,
) {
    Box {
        IconButton(onClick = onIconClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gift),
                contentDescription = stringResource(LR.string.gift),
                tint = colors.primaryIcon01,
            )
        }
        if (state.showBadge) {
            CountBadge(
                count = state.badgeCount,
                style = CountBadgeStyle.Custom(
                    backgroundColor = colors.primaryIcon01,
                    textColor = colors.primaryUi01,
                    borderColor = colors.primaryUi02,
                    size = 14.dp,
                    borderWidth = 1.dp,
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(-(6).dp, 8.dp),
            )
        }
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun IconWithBadgePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        IconWithBadge(
            state = ReferralsViewModel.UiState(
                showIcon = true,
                badgeCount = 3,
            ),
            onIconClick = {},
            colors = LocalColors.current.colors,
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
fun TooltipContentPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        TooltipContent(
            state = ReferralsViewModel.UiState(
                showIcon = true,
                badgeCount = 3,
            ),
        )
    }
}
