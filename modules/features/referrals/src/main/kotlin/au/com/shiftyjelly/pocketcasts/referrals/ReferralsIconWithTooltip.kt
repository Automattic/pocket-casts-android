package au.com.shiftyjelly.pocketcasts.referrals

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import au.com.shiftyjelly.pocketcasts.compose.components.Tooltip
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ReferralsIconWithTooltip(
    viewModel: ReferralsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val activity = LocalContext.current.getActivity()
    ReferralsIconWithTooltip(
        state = state,
        onIconClick = {
            viewModel.onIconClick()
            val fragment = ReferralsSendGuestPassFragment.newInstance()
            (activity as FragmentHostListener).showBottomSheet(fragment)
        },
    )
}

@Composable
private fun ReferralsIconWithTooltip(
    state: ReferralsViewModel.UiState,
    onIconClick: () -> Unit,
) {
    if (state.showIcon) {
        Icon(
            onIconClick = onIconClick,
            colors = LocalColors.current.colors,
        )

        Tooltip(
            show = state.showTooltip,
        ) {
            TooltipContent()
        }
    }
}

@Composable
private fun TooltipContent() {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 24.dp, bottom = 16.dp),
    ) {
        TextC70(
            text = stringResource(LR.string.referrals_tooltip_message),
            isUpperCase = false,
        )
    }
}

@Composable
private fun Icon(
    onIconClick: () -> Unit,
    colors: ThemeColors,
) {
    Box {
        IconButton(onClick = onIconClick) {
            Icon(
                painter = painterResource(id = R.drawable.ic_gift),
                contentDescription = stringResource(LR.string.gift),
                tint = colors.primaryIcon01,
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
        Icon(
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
        TooltipContent()
    }
}
