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
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeStatusRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun EpisodeStatusRulePage(
    rule: EpisodeStatusRule,
    onChangeUnplayedStatus: (Boolean) -> Unit,
    onChangeInProgressStatus: (Boolean) -> Unit,
    onChangeCompletedStatus: (Boolean) -> Unit,
    onSaveRule: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RulePage(
        title = stringResource(LR.string.filters_chip_episode_status),
        onSaveRule = onSaveRule,
        isSaveEnabled = rule.unplayed || rule.inProgress || rule.completed,
        onClickBack = onClickBack,
        modifier = modifier,
    ) { bottomPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = bottomPadding),
        ) {
            RuleCheckboxRow(
                title = stringResource(LR.string.unplayed),
                isChecked = rule.unplayed,
                onCheckedChange = onChangeUnplayedStatus,
            )
            RuleCheckboxRow(
                title = stringResource(LR.string.in_progress_uppercase),
                isChecked = rule.inProgress,
                onCheckedChange = onChangeInProgressStatus,
            )
            RuleCheckboxRow(
                title = stringResource(LR.string.played),
                isChecked = rule.completed,
                onCheckedChange = onChangeCompletedStatus,
            )
        }
    }
}

@Composable
@PreviewRegularDevice
private fun EpisodeStatusRulePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var rule by remember { mutableStateOf(SmartRules.Default.episodeStatus) }

    AppThemeWithBackground(themeType) {
        EpisodeStatusRulePage(
            rule = rule,
            onChangeUnplayedStatus = { rule.copy(unplayed = it) },
            onChangeInProgressStatus = { rule.copy(inProgress = it) },
            onChangeCompletedStatus = { rule.copy(completed = it) },
            onSaveRule = {},
            onClickBack = {},
        )
    }
}
