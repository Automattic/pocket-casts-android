package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.components.SettingsSection
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AdvancedSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * The advanced settings page is for settings that we are not sure many users will need, or that we are
 * worried might create a worse user experience for most users. Basically, this is a place where we can
 * add settings without complicating things for the typical user.
 */
@Composable
fun AdvancedSettingsPage(
    viewModel: AdvancedSettingsViewModel,
    onBackPressed: () -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    val state: AdvancedSettingsViewModel.State by viewModel.state.collectAsState()
    AdvancedSettingsView(
        state = state,
        onBackPressed = onBackPressed,
        bottomInset = bottomInset,
        modifier = modifier,
    )

    CallOnce {
        viewModel.onShown()
    }
}

@Composable
fun AdvancedSettingsView(
    state: AdvancedSettingsViewModel.State,
    onBackPressed: () -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_advanced),
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
                TextP50(
                    text = stringResource(LR.string.settings_description_advanced),
                    color = MaterialTheme.theme.colors.primaryText02,
                    modifier = Modifier.padding(SettingsSection.horizontalPadding),
                )
            }
            item {
                SettingSection(
                    heading = stringResource(LR.string.settings_storage_section_heading_mobile_data),
                    indent = false,
                ) {
                    SyncOnMeteredRow(state.backgroundSyncOnMeteredState)
                }
            }
            item {
                SettingSection(
                    heading = stringResource(LR.string.settings_storage_section_heading_seek_accuracy),
                    indent = false,
                ) {
                    PrioritizeSeekAccuracydRow(state.prioritizeSeekAccuracyState)
                }
            }
        }
    }
}

@Composable
private fun SyncOnMeteredRow(
    state: AdvancedSettingsViewModel.State.BackgroundSyncOnMeteredState,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_advanced_sync_on_metered),
        secondaryText = stringResource(LR.string.settings_advanced_sync_on_metered_summary),
        toggle = SettingRowToggle.Switch(state.isChecked, state.isEnabled),
        indent = false,
        modifier = modifier.toggleable(
            value = state.isChecked,
            role = Role.Switch,
            onValueChange = { state.onCheckedChange(it) },
        ),
    )
}

@Composable
private fun PrioritizeSeekAccuracydRow(
    state: AdvancedSettingsViewModel.State.PrioritizeSeekAccuracyState,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = stringResource(LR.string.settings_advanced_prioritize_seek_accuracy),
        secondaryText = stringResource(LR.string.settings_advanced_prioritize_seek_accuracy_summary),
        toggle = SettingRowToggle.Switch(state.isChecked),
        indent = false,
        modifier = modifier.toggleable(
            value = state.isChecked,
            role = Role.Switch,
            onValueChange = { state.onCheckedChange(it) },
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun AdvancedSettingsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        AdvancedSettingsView(
            state = AdvancedSettingsViewModel.State(
                backgroundSyncOnMeteredState = AdvancedSettingsViewModel.State.BackgroundSyncOnMeteredState(
                    isChecked = true,
                    isEnabled = true,
                    onCheckedChange = {},
                ),
                prioritizeSeekAccuracyState = AdvancedSettingsViewModel.State.PrioritizeSeekAccuracyState(
                    isChecked = true,
                    onCheckedChange = {},
                ),
            ),
            onBackPressed = {},
            bottomInset = 0.dp,
        )
    }
}
