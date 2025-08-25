package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.compose.bars.singleAuto
import au.com.shiftyjelly.pocketcasts.compose.bars.transparent
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
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
            viewModel.saveInterests()
            onShowRecommendations()
        },
        onNotNowPress = {
            viewModel.skipSelection()
            onShowRecommendations()
        },
        onShowMoreCategories = viewModel::showMore,
    )
}

@Composable
private fun Content(
    state: OnboardingInterestsViewModel.State,
    onCategorySelectionChange: (String, Boolean) -> Unit,
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
                .padding(top = 12.dp, end = 4.dp)
                .clickable(onClick = onNotNowPress),
            text = stringResource(LR.string.not_now),
            color = MaterialTheme.theme.colors.primaryInteractive01,
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
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3,
        ) {
            state.availableCategories.forEachIndexed { index, item ->
                CategoryPill(
                    modifier = Modifier.wrapContentWidth(),
                    category = item,
                    isSelected = state.selectedCategories.contains(item),
                    onSelectedChange = { isSelected -> onCategorySelectionChange(item, isSelected) },
                )
                if (index % 2 == 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }

        if (!state.isShowingAllCategories) {
            Spacer(modifier = Modifier.height(24.dp))
            TextP40(
                text = stringResource(LR.string.onboarding_interests_show_more),
                color = MaterialTheme.theme.colors.primaryInteractive01,
                modifier = Modifier.clickable(onClick = onShowMoreCategories),
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        RowButton(
            text = stringResource(state.ctaLabelResId),
            enabled = state.isCtaEnabled,
            onClick = onContinuePress,
        )
    }
}

@Composable
private fun CategoryPill(
    category: String,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SelectablePillContainer(
        isSelected = isSelected,
        onSelectedChange = onSelectedChange,
        modifier = modifier,
    ) {
        TextP30(
            text = category,
            color = if (isSelected) {
                MaterialTheme.theme.colors.primaryUi01Active
            } else {
                MaterialTheme.theme.colors.primaryText02
            },
        )
    }
}

@Composable
private fun SelectablePillContainer(
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(percent = 100))
            .then(
                if (isSelected) {
                    Modifier.background(color = Color.Green.copy(alpha = .3f))
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.theme.colors.primaryText02,
                        shape = RoundedCornerShape(percent = 100),
                    )
                }
                    .toggleable(value = isSelected, onValueChange = onSelectedChange)
                    .padding(horizontal = 16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Preview
@Composable
private fun PreviewCategoryPill(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) = AppThemeWithBackground(themeType) {
    Column(modifier = Modifier.padding(32.dp)) {
        CategoryPill(
            category = "Category",
            isSelected = false,
            onSelectedChange = {},
        )
        CategoryPill(
            category = "Selected Category",
            isSelected = true,
            onSelectedChange = {},
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
        state = OnboardingInterestsViewModel.State(availableCategories = List(4) { "Category $it" }, isShowingAllCategories = false),
        onContinuePress = {},
        onNotNowPress = {},
        onShowMoreCategories = {},
        onCategorySelectionChange = { _, _ -> },
    )
}
