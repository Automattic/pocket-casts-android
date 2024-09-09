package au.com.shiftyjelly.pocketcasts.settings.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.rearrange.MenuAction
import au.com.shiftyjelly.pocketcasts.compose.rearrange.MenuActionRearrange
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.settings.notification.MediaActionsViewModel.State
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun MediaActionsPage(
    bottomInset: Dp,
    state: State,
    onBackClick: () -> Unit,
    onShowCustomActionsChanged: (Boolean) -> Unit,
    onActionsOrderChanged: (List<MenuAction>) -> Unit,
    modifier: Modifier = Modifier,
    onActionMoved: (fromIndex: Int, toIndex: Int, action: MenuAction) -> Unit = { _, _, _ -> },
) {
    Column(modifier = modifier.background(MaterialTheme.theme.colors.primaryUi02)) {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_media_actions_customise),
            onNavigationClick = onBackClick,
        )

        MenuActionRearrange(
            header = {
                PageHeader(
                    customActionsVisibility = state.customActionsVisibility,
                    onShowCustomActionsChanged = onShowCustomActionsChanged,
                )
            },
            menuActions = state.actions,
            onActionsOrderChanged = onActionsOrderChanged,
            onActionMoved = onActionMoved,
            chooseCount = 3,
            enabled = state.customActionsVisibility,
            otherActionsTitle = stringResource(LR.string.settings_other_media_actions),
            contentPadding = PaddingValues(top = 8.dp, bottom = bottomInset + 8.dp),
        )
    }
}

@Composable
private fun PageHeader(
    customActionsVisibility: Boolean,
    onShowCustomActionsChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        ShowCustomActionsSettings(
            customActionsVisibility = customActionsVisibility,
            onShowCustomActionsChanged = onShowCustomActionsChanged,
        )
        Spacer(Modifier.height(16.dp))
        SettingRow(
            primaryText = stringResource(LR.string.settings_media_actions_prioritize_title),
            secondaryText = stringResource(LR.string.settings_media_actions_prioritize_subtitle),
            indent = false,
            modifier = Modifier
                .then(if (customActionsVisibility) Modifier else Modifier.alpha(0.4f))
                .semantics(mergeDescendants = true) {},
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ShowCustomActionsSettings(
    customActionsVisibility: Boolean,
    onShowCustomActionsChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_media_actions_show_title),
        secondaryText = stringResource(LR.string.settings_media_actions_show_subtitle),
        toggle = SettingRowToggle.Switch(checked = customActionsVisibility),
        indent = false,
        modifier = modifier
            .padding(top = 8.dp)
            .toggleable(
                value = customActionsVisibility,
                role = Role.Switch,
            ) {
                onShowCustomActionsChanged(it)
            },
    )
}

@Preview
@Composable
fun MediaActionsPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppTheme(themeType) {
        MediaActionsPage(
            bottomInset = 0.dp,
            state = State(
                actions = listOf(
                    MenuAction("1", LR.string.archive, R.drawable.ic_archive),
                    MenuAction("2", LR.string.mark_as_played, R.drawable.ic_markasplayed),
                    MenuAction("3", LR.string.play_next, R.drawable.ic_skip_next),
                    MenuAction("4", LR.string.playback_speed, R.drawable.ic_speed_number),
                    MenuAction("5", LR.string.star, R.drawable.ic_star),
                ),
                customActionsVisibility = true,
            ),
            onBackClick = {},
            onShowCustomActionsChanged = {},
            onActionsOrderChanged = {},
        )
    }
}
