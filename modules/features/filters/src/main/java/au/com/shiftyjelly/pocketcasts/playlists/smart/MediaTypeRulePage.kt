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
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun MediaTypeRulePage(
    selectedRule: MediaTypeRule,
    onSelectMediaType: (MediaTypeRule) -> Unit,
    onSaveRule: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RulePage(
        title = stringResource(LR.string.filters_chip_media_type),
        onSaveRule = onSaveRule,
        onClickBack = onClickBack,
        modifier = modifier,
    ) { bottomPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = bottomPadding),
        ) {
            MediaTypeRule.entries.forEach { rule ->
                RuleRadioRow(
                    title = stringResource(rule.displayLabelId),
                    isSelected = rule == selectedRule,
                    onSelect = { onSelectMediaType(rule) },
                )
            }
        }
    }
}

private val MediaTypeRule.displayLabelId get() = when (this) {
    MediaTypeRule.Any -> LR.string.all
    MediaTypeRule.Audio -> LR.string.audio
    MediaTypeRule.Video -> LR.string.video
}

@Composable
@PreviewRegularDevice
private fun MediaTypeRulePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var rule by remember { mutableStateOf(SmartRules.Default.mediaType) }

    AppThemeWithBackground(themeType) {
        MediaTypeRulePage(
            selectedRule = rule,
            onSelectMediaType = { rule = it },
            onSaveRule = {},
            onClickBack = {},
        )
    }
}
