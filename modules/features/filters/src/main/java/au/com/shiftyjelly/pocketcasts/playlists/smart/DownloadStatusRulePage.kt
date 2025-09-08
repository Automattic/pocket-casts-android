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
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun DownloadStatusRulePage(
    selectedRule: DownloadStatusRule,
    onSelectDownloadStatus: (DownloadStatusRule) -> Unit,
    onSaveRule: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RulePage(
        title = stringResource(LR.string.filters_chip_download_status),
        onSaveRule = onSaveRule,
        onClickBack = onClickBack,
        modifier = modifier,
    ) { bottomPadding ->
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = bottomPadding),
        ) {
            DownloadStatusRule.entries.forEach { rule ->
                RuleRadioRow(
                    title = stringResource(rule.displayLabelId),
                    isSelected = rule == selectedRule,
                    onSelect = { onSelectDownloadStatus(rule) },
                )
            }
        }
    }
}

private val DownloadStatusRule.displayLabelId get() = when (this) {
    DownloadStatusRule.Any -> LR.string.all
    DownloadStatusRule.Downloaded -> LR.string.downloaded
    DownloadStatusRule.NotDownloaded -> LR.string.not_downloaded
}

@Composable
@PreviewRegularDevice
private fun DownloadStatusRulePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var rule by remember { mutableStateOf(SmartRules.Default.downloadStatus) }

    AppThemeWithBackground(themeType) {
        DownloadStatusRulePage(
            selectedRule = rule,
            onSelectDownloadStatus = { rule = it },
            onSaveRule = {},
            onClickBack = {},
        )
    }
}
