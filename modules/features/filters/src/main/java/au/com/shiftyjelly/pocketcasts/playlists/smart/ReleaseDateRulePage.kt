package au.com.shiftyjelly.pocketcasts.playlists.smart

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun ReleaseDateRulePage(
    selectedRule: ReleaseDateRule,
    onSelectReleaseDate: (ReleaseDateRule) -> Unit,
    onSaveRule: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RulePage(
        title = stringResource(LR.string.filters_release_date),
        onSaveRule = onSaveRule,
        onClickBack = onClickBack,
        modifier = modifier,
    ) { bottomPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = bottomPadding),
        ) {
            ReleaseDateRule.entries.forEach { rule ->
                RuleRadioRow(
                    title = stringResource(rule.displayLabelId),
                    isSelected = rule == selectedRule,
                    onSelect = { onSelectReleaseDate(rule) },
                )
            }
        }
    }
}

private val ReleaseDateRule.displayLabelId get() = when (this) {
    ReleaseDateRule.AnyTime -> LR.string.filters_time_anytime
    ReleaseDateRule.Last24Hours -> LR.string.filters_time_24_hours
    ReleaseDateRule.Last3Days -> LR.string.filters_time_3_days
    ReleaseDateRule.LastWeek -> LR.string.filters_time_week
    ReleaseDateRule.Last2Weeks -> LR.string.filters_time_2_weeks
    ReleaseDateRule.LastMonth -> LR.string.filters_time_month
}

@Composable
@PreviewRegularDevice
private fun ReleaseDateRulePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var rule by remember { mutableStateOf(SmartRules.Default.releaseDate) }

    AppThemeWithBackground(themeType) {
        ReleaseDateRulePage(
            selectedRule = rule,
            onSelectReleaseDate = { rule = it },
            onSaveRule = {},
            onClickBack = {},
        )
    }
}
