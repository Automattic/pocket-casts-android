package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.images.HorizontalLogo
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
internal fun AccountBenefitsDialog(
    onGetStartedClick: () -> Unit,
    onLogIn: () -> Unit,
    onDismiss: () -> Unit,
    onShowBenefit: (AccountBenefit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .background(MaterialTheme.theme.colors.primaryUi01, RoundedCornerShape(16.dp)),
        ) {
            LogoWithCloseButton(
                onClose = onDismiss,
            )

            Spacer(
                modifier = Modifier.height(24.dp),
            )

            HeaderTexts()

            Spacer(
                modifier = Modifier.height(20.dp),
            )

            BenefitsPager(
                onShowBenefit = onShowBenefit,
            )

            Spacer(
                modifier = Modifier.height(32.dp),
            )

            ActionButtons(
                onGetStartedClick = onGetStartedClick,
                onLogIn = onLogIn,
                modifier = Modifier.padding(horizontal = 70.dp),
            )

            Spacer(
                modifier = Modifier.height(16.dp),
            )
        }
    }
}

@Composable
private fun LogoWithCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_close),
                contentDescription = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.close),
                tint = MaterialTheme.theme.colors.primaryText01,
            )
        }

        HorizontalLogo(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
                .height(28.dp),
        )
    }
}

@Composable
private fun HeaderTexts(
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 108.dp),
    ) {
        TextH20(
            text = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.account_encourage_title),
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        TextP40(
            text = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.account_encourage_description),
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun BenefitsPager(
    onShowBenefit: (AccountBenefit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val benefits = AccountBenefit.entries
    val pagerState = PagerState { benefits.size }

    LaunchedEffect(pagerState, onShowBenefit) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            onShowBenefit(benefits[page])
        }
    }

    HorizontalPager(
        state = pagerState,
        beyondViewportPageCount = benefits.size,
        contentPadding = PaddingValues(horizontal = 62.dp),
        modifier = modifier.fillMaxWidth(),
    ) { pageIndex ->
        val benefit = benefits[pageIndex]
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .background(MaterialTheme.theme.colors.primaryUi02Active, RoundedCornerShape(8.dp))
                .fillMaxWidth(),
        ) {
            Spacer(
                modifier = Modifier.height(20.dp),
            )

            Image(
                painter = painterResource(benefit.cardImage),
                contentDescription = null,
                modifier = Modifier.height(152.dp),
            )

            Spacer(
                modifier = Modifier.height(16.dp),
            )

            TextH30(
                text = stringResource(benefit.title),
                letterSpacing = 0.5.sp,
            )

            Spacer(
                modifier = Modifier.height(4.dp),
            )

            TextP50(
                text = stringResource(benefit.description),
                color = MaterialTheme.theme.colors.secondaryText02,
                textAlign = TextAlign.Center,
                letterSpacing = 0.5.sp,
                modifier = Modifier.width(220.dp),
            )

            Spacer(
                modifier = Modifier.height(20.dp),
            )
        }
    }
}

@Composable
private fun ActionButtons(
    onGetStartedClick: () -> Unit,
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        RowButton(
            text = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.onboarding_get_started),
            includePadding = false,
            onClick = onGetStartedClick,
        )

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        RowTextButton(
            text = stringResource(au.com.shiftyjelly.pocketcasts.localization.R.string.onboarding_log_in),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.theme.colors.primaryText01,
            ),
            includePadding = false,
            onClick = onLogIn,
        )
    }
}

@Preview(device = Devices.LANDSCAPE_TABLET)
@Composable
private fun AccountBenefitsDialogPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType = themeType) {
        AccountBenefitsDialog(
            onGetStartedClick = {},
            onLogIn = {},
            onDismiss = {},
            onShowBenefit = {},
        )
    }
}
