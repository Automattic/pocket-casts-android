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
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.LocalColors
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.referrals.ReferralsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ReferralsIcon(
    state: UiState,
    onIconClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is UiState.Loaded && state.showIcon) {
        Icon(
            onIconClick = onIconClick,
            colors = LocalColors.current.colors,
            modifier = modifier,
        )
    }
}

@Composable
private fun Icon(
    onIconClick: () -> Unit,
    colors: ThemeColors,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onIconClick,
        modifier = modifier,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_gift),
            contentDescription = stringResource(LR.string.gift),
            tint = colors.secondaryIcon01,
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun IconPreview(
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
