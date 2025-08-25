package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.onboarding.components.InterestCategoryPill
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.compose.bars.singleAuto
import au.com.shiftyjelly.pocketcasts.compose.bars.transparent
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingInterestsPage(
    theme: Theme.ThemeType,
    onBackPress: () -> Unit,
    onShowRecommendations: () -> Unit,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingInterestsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    val pocketCastsTheme = MaterialTheme.theme

    LaunchedEffect(onUpdateSystemBars) {
        val statusBar = SystemBarStyle.singleAuto(pocketCastsTheme.colors.primaryUi01.copy(alpha = 0.9f)) { theme.darkTheme }
        val navigationBar = SystemBarStyle.transparent { theme.darkTheme }
        onUpdateSystemBars(SystemBarsStyles(statusBar, navigationBar))
    }

    BackHandler {
        onBackPress()
    }

    Content(
        modifier = modifier,
        state = state,
        onCategorySelectionChange = viewModel::updateSelectedCategory,
        onContinuePress = {
            viewModel.saveInterests {
                onShowRecommendations()
            }
        },
        onNotNowPress = {
            viewModel.skipSelection {
                onShowRecommendations()
            }
        },
        onShowMoreCategories = viewModel::showMore,
    )
}

@Composable
private fun Content(
    state: OnboardingInterestsViewModel.State,
    onCategorySelectionChange: (DiscoverCategory, Boolean) -> Unit,
    onNotNowPress: () -> Unit,
    onContinuePress: () -> Unit,
    onShowMoreCategories: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TextP40(
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 10.dp)
                .clickable(onClick = onNotNowPress)
                .padding(horizontal = 4.dp, vertical = 2.dp),
            text = stringResource(LR.string.not_now),
            color = MaterialTheme.theme.colors.primaryInteractive01,
            fontWeight = FontWeight.W500,
        )
        Spacer(modifier = Modifier.height(24.dp))
        TextH10(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            text = stringResource(LR.string.onboarding_interests_title),
            textAlign = TextAlign.Center,
            color = MaterialTheme.theme.colors.primaryText01,
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextP40(
            text = stringResource(LR.string.onboarding_interests_description),
            color = MaterialTheme.theme.colors.primaryText02,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 38.dp),
            fontWeight = FontWeight.W500,
        )
        Spacer(modifier = Modifier.height(24.dp))

        FlowRow(
            modifier = Modifier
                .then(
                    if (state.isShowingAllCategories) {
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    } else {
                        Modifier
                    },
                )
                .animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, alignment = Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 2,
        ) {
            state.displayedCategories.forEachIndexed { index, item ->
                InterestCategoryPill(
                    modifier = Modifier.wrapContentWidth(),
                    category = item,
                    isSelected = state.selectedCategories.contains(item),
                    onSelectedChange = { isSelected -> onCategorySelectionChange(item, isSelected) },
                    index = index,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!state.isShowingAllCategories) {
            TextP40(
                text = stringResource(LR.string.onboarding_interests_show_more),
                color = MaterialTheme.theme.colors.primaryInteractive01,
                modifier = Modifier
                    .clickable(onClick = onShowMoreCategories)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                fontWeight = FontWeight.W500,
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        RowButton(
            text = stringResource(state.ctaLabelResId),
            enabled = state.isCtaEnabled,
            onClick = onContinuePress,
        )
    }
}

@Preview
@Composable
private fun PreviewInterestsScreen(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) = AppThemeWithBackground(themeType) {
    Content(
        modifier = Modifier.padding(32.dp),
        state = OnboardingInterestsViewModel.State(allCategories = emptyList(), displayedCategories = emptyList()),
        onContinuePress = {},
        onNotNowPress = {},
        onShowMoreCategories = {},
        onCategorySelectionChange = { _, _ -> },
    )
}
