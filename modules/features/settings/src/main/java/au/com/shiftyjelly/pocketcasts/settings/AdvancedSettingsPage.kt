package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AdvancedSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.*
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun AdvancedSettingsPage(
    viewModel: AdvancedSettingsViewModel,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state: AdvancedSettingsViewModel.State by viewModel.state.collectAsState()
    AdvancedSettingsView(
        state = state,
        onBackPressed = onBackPressed,
        modifier = modifier
    )

    CallOnce {
        viewModel.onShown()
    }
}

@Composable
fun AdvancedSettingsView(
    state: AdvancedSettingsViewModel.State,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_advanced),
            bottomShadow = true,
            onNavigationClick = { onBackPressed() }
        )

        Column(
            modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            SettingSection(heading = stringResource(LR.string.settings_storage_section_heading_mobile_data)) {
                SyncOnMeteredRow(state.backgroundSyncOnMeteredState)
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
        toggle = SettingRowToggle.Switch(state.isChecked, state.isEnabled),
        modifier = modifier.toggleable(
            value = state.isChecked,
            role = Role.Switch
        ) { state.onCheckedChange(it) }
    )
    SettingRow(primaryText = "", secondaryText = stringResource(LR.string.settings_advanced_sync_on_metered_summary))
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
                    onCheckedChange = {}
                ),
            ),
            onBackPressed = {}
        )
    }
}
