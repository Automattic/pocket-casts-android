package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationIconButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.PagerDotIndicator
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.images.HorizontalLogo
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun AccountBenefitsPage(
    onGetStartedClick: () -> Unit,
    onLogIn: () -> Unit,
    onClose: () -> Unit,
    onShowBenefit: (AccountBenefit) -> Unit,
    modifier: Modifier = Modifier,
    mainCtaLabel: String = stringResource(LR.string.onboarding_get_started),
    mainCtaColor: Color = MaterialTheme.theme.colors.primaryText01,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(MaterialTheme.theme.colors.primaryUi01)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        LogoWithCloseButton(
            onClose = onClose,
        )

        BenefitsAdaptiveContent(
            noShownBenefit = onShowBenefit,
            modifier = Modifier.weight(1f),
        )

        ActionButtons(
            mainCtaColor = mainCtaColor,
            mainCtaLabel = mainCtaLabel,
            onGetStartedClick = onGetStartedClick,
            onLogIn = onLogIn,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                bottom = 12.dp,
                top = 20.dp,
            ),
        )
    }
}

@Composable
private fun LogoWithCloseButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier.weight(1f),
        ) {
            NavigationIconButton(
                tint = MaterialTheme.theme.colors.primaryText01,
                navigationButton = NavigationButton.Close,
                onClick = onClose,
            )
        }

        HorizontalLogo(
            modifier = Modifier.height(28.dp),
        )

        Spacer(
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HeaderText(
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        TextH20(
            text = stringResource(LR.string.account_encourage_title),
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        TextP40(
            text = stringResource(LR.string.account_encourage_description),
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ActionButtons(
    mainCtaColor: Color,
    mainCtaLabel: String,
    onGetStartedClick: () -> Unit,
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        RowTextButton(
            text = mainCtaLabel,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = mainCtaColor,
                contentColor = MaterialTheme.theme.colors.primaryUi01,
            ),
            includePadding = false,
            onClick = onGetStartedClick,
        )

        Spacer(
            modifier = Modifier.height(8.dp),
        )

        RowTextButton(
            text = stringResource(LR.string.onboarding_log_in),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.theme.colors.primaryText01,
            ),
            includePadding = false,
            onClick = onLogIn,
        )
    }
}

@Composable
private fun BenefitsAdaptiveContent(
    noShownBenefit: (AccountBenefit) -> Unit,
    modifier: Modifier = Modifier,
) {
    SubcomposeLayout(
        modifier = modifier,
    ) { constraints ->
        val verticalPagerEmptySpace = 152.dp.roundToPx()
        val horizontalPagerEmptySpace = 104.dp.roundToPx()

        val measurementMock = subcompose("measurementMock") {
            Column {
                HeaderText(
                    modifier = Modifier.padding(24.dp),
                )
                Box(
                    modifier = Modifier.padding(start = 48.dp, end = 56.dp),
                ) {
                    AccountBenefit.entries.forEach { benefit ->
                        BenefitCardText(benefit)
                    }
                }
            }
        }[0].measure(Constraints())

        val availableImageHeight = constraints.maxHeight - verticalPagerEmptySpace - measurementMock.height
        val availableImageWidth = constraints.maxWidth - horizontalPagerEmptySpace
        val availableImageAspectRatio = availableImageHeight.toFloat() / availableImageWidth

        val content = subcompose("content") {
            if (availableImageAspectRatio > 0.8f) {
                Column {
                    HeaderText(
                        modifier = Modifier.padding(24.dp),
                    )
                    BenefitPager(
                        onShowBenefit = noShownBenefit,
                        contentPadding = PaddingValues(horizontal = 24.dp),
                        contentSpacing = 16.dp,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            } else {
                FadedLazyColumn(
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    modifier = Modifier.padding(horizontal = 16.dp),
                ) {
                    item {
                        HeaderText(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, start = 20.dp, end = 20.dp),
                        )
                    }
                    item {
                        TextH30(
                            text = stringResource(LR.string.account_encourage_benefits_title),
                        )
                    }
                    items(AccountBenefit.entries) { benefit ->
                        BenefitEntry(benefit)
                    }
                }
            }
        }[0].measure(constraints)

        layout(constraints.maxWidth, constraints.maxHeight) {
            content.place(0, 0)
        }
    }
}

@Composable
private fun BenefitPager(
    contentPadding: PaddingValues,
    contentSpacing: Dp,
    onShowBenefit: (AccountBenefit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
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
            contentPadding = contentPadding,
            modifier = Modifier.weight(1f),
        ) { pageIndex ->
            val padding = when (pageIndex) {
                0 -> PaddingValues(end = contentSpacing / 2)
                benefits.lastIndex -> PaddingValues(start = contentSpacing / 2)
                else -> PaddingValues(horizontal = contentSpacing / 2)
            }
            BenefitCard(
                benefit = benefits[pageIndex],
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
            )
        }

        Spacer(
            modifier = Modifier.height(16.dp),
        )

        PagerDotIndicator(
            state = pagerState,
            activeDotColor = MaterialTheme.theme.colors.primaryText01,
        )
    }
}

@Composable
private fun BenefitCard(
    benefit: AccountBenefit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.theme.colors.primaryUi02Active, RoundedCornerShape(8.dp))
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Image(
            painter = painterResource(benefit.cardImage),
            contentDescription = null,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )

        Spacer(
            modifier = Modifier.height(24.dp),
        )

        BenefitCardText(
            benefit = benefit,
        )
    }
}

@Composable
private fun BenefitCardText(
    benefit: AccountBenefit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        TextH30(
            text = stringResource(benefit.title),
        )

        Spacer(
            modifier = Modifier.height(4.dp),
        )

        TextP50(
            text = stringResource(benefit.description),
            color = MaterialTheme.theme.colors.secondaryText02,
        )
    }
}

@Composable
private fun BenefitEntry(
    benefit: AccountBenefit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
    ) {
        Image(
            painter = painterResource(benefit.listIcon),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon03Active),
            modifier = Modifier.size(24.dp),
        )

        Spacer(
            modifier = Modifier.width(24.dp),
        )

        Column {
            TextH40(
                text = stringResource(benefit.title),
            )

            Spacer(
                modifier = Modifier.height(4.dp),
            )

            TextP50(
                text = stringResource(benefit.description),
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Preview(device = Devices.PORTRAIT_SMALL)
@Composable
private fun AccountBenefitsPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType = themeType) {
        AccountBenefitsPage(
            onGetStartedClick = {},
            onLogIn = {},
            onClose = {},
            onShowBenefit = {},
        )
    }
}
