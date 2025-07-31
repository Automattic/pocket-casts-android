package au.com.shiftyjelly.pocketcasts.playlists.create

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EpisodeStatusRulePage(
    rule: SmartRules.EpisodeStatusRule,
    onToggleUnplayedStatus: (Boolean) -> Unit,
    onToggleInProgressStatus: (Boolean) -> Unit,
    onToggleCompletedStatus: (Boolean) -> Unit,
    onSaveRule: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        IconButton(
            onClick = onClickBack,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(LR.string.back),
                tint = MaterialTheme.theme.colors.primaryIcon03,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
        ) {
            TextH20(
                text = stringResource(LR.string.filters_chip_episode_status),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(
                modifier = Modifier.height(12.dp),
            )
            EpisodeStatusRow(
                text = stringResource(LR.string.unplayed),
                isSelected = rule.unplayed,
                onToggle = onToggleUnplayedStatus,
            )
            EpisodeStatusRow(
                text = stringResource(LR.string.in_progress_uppercase),
                isSelected = rule.inProgress,
                onToggle = onToggleInProgressStatus,
            )
            EpisodeStatusRow(
                text = stringResource(LR.string.played),
                isSelected = rule.completed,
                onToggle = onToggleCompletedStatus,
            )
            Spacer(
                modifier = Modifier.weight(1f),
            )
            RowButton(
                text = stringResource(LR.string.save_smart_rule),
                enabled = rule.unplayed || rule.inProgress || rule.completed,
                onClick = onSaveRule,
                includePadding = false,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding(),
            )
        }
    }
}

@Composable
private fun EpisodeStatusRow(
    text: String,
    isSelected: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .toggleable(
                value = isSelected,
                role = Role.Checkbox,
                onValueChange = onToggle,
            )
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        TextH30(
            text = text,
            modifier = Modifier.weight(1f),
        )
        Checkbox(
            checked = isSelected,
            onCheckedChange = null,
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun EpisodeStatusRulePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var rule by remember { mutableStateOf(SmartRules.Default.episodeStatus) }

    AppThemeWithBackground(themeType) {
        EpisodeStatusRulePage(
            rule = rule,
            onToggleUnplayedStatus = { rule.copy(unplayed = it) },
            onToggleInProgressStatus = { rule.copy(inProgress = it) },
            onToggleCompletedStatus = { rule.copy(completed = it) },
            onSaveRule = {},
            onClickBack = {},
        )
    }
}
