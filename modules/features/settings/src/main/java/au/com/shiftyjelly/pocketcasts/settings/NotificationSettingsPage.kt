package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.NotificationsSettingsViewModel.NotificationSettingsState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun NotificationsSettingsPage(
    state: NotificationSettingsState,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    onSelectPodcasts: () -> Unit,
) {
    var openActionsDialog by remember { mutableStateOf(false) }

    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_notifications),
            bottomShadow = true,
            onNavigationClick = { onBackPressed() },
        )

        LazyColumn(
            modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .fillMaxHeight(),
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item {
                SettingSection(
                    heading = stringResource(LR.string.settings_notifications_new_episodes),
                    indent = false,
                ) {
                    SettingRow(
                        primaryText = stringResource(LR.string.settings_notification_notify_me),
                        toggle = SettingRowToggle.Switch(state.newEpisodesState.isChecked),
                        indent = true,
                        modifier = modifier.toggleable(
                            value = state.newEpisodesState.isChecked,
                            role = Role.Switch,
                            onValueChange = { state.newEpisodesState.onCheckedChange(it) },
                        ),
                    )
                    AnimatedVisibility(state.newEpisodesState.isChecked) {
                        Column {
                            SettingRow(
                                primaryText = stringResource(LR.string.settings_notification_choose_podcasts),
                                secondaryText = stringResource(LR.string.settings_podcasts_selected_zero),
                                indent = true,
                                modifier = modifier.clickable { onSelectPodcasts() },
                            )

                            SettingRow(
                                primaryText = stringResource(LR.string.settings_notification_actions),
                                indent = true,
                            )

                            SettingRow(
                                primaryText = stringResource(LR.string.settings_notification_advanced_summary),
                                secondaryText = stringResource(LR.string.settings_notification_advanced),
                                indent = true,
                            )
                        }
                    }
                }
            }
        }

        if (openActionsDialog) {
//            RadioOptionsDialog(
//                title = stringResource(LR.string.settings_inactive_episodes),
//                selectedOption = state.archiveInactive,
//                allOptions = AutoArchiveInactive.All,
//                optionName = { option -> stringResource(option.stringRes) },
//                onSelectOption = { newValue: AutoArchiveInactive ->
//                    openActionsDialog = false
//                },
//                onDismiss = { openActionsDialog = false },
//                modifier = Modifier.verticalScroll(rememberScrollState()),
//            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun NotificationsSettingsPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppTheme(theme) {
        NotificationsSettingsPage(
            bottomInset = 0.dp,
            state = NotificationSettingsState(
                newEpisodesState = NotificationSettingsState.NewEpisodesState(
                    isChecked = true,
                    onCheckedChange = {},
                ),
            ),
            onBackPressed = {},
            onSelectPodcasts = {},
        )
    }
}
