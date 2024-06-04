package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.SettingInfoRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.dialogs.RadioOptionsDialog
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AutoArchiveFragmentViewModel
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AutoArchiveFragmentViewModel.State
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun AutoArchiveSettingsPage(
    modifier: Modifier = Modifier,
    bottomInset: Dp,
    onBackPressed: () -> Unit,
    viewModel: AutoArchiveFragmentViewModel,
) {
    val state: State by viewModel.state.collectAsState()
    var openArchiveAfterPlayingDialog by remember { mutableStateOf(false) }
    var openArchiveInactiveDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.theme.colors.primaryUi02),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_title_auto_archive),
            onNavigationClick = onBackPressed,
        )

        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = bottomInset),
        ) {
            item {
                SettingRow(
                    primaryText = stringResource(LR.string.settings_archive_played),
                    secondaryText = stringResource(state.archiveAfterPlaying.stringRes),
                    toggle = SettingRowToggle.None,
                    modifier = Modifier.clickable { openArchiveAfterPlayingDialog = true },
                )
            }

            item {
                SettingRow(
                    primaryText = stringResource(LR.string.settings_auto_archive_inactive),
                    secondaryText = stringResource(state.archiveInactive.stringRes),
                    toggle = SettingRowToggle.None,
                    modifier = Modifier.clickable { openArchiveInactiveDialog = true },
                )
            }

            item {
                SettingInfoRow(
                    text = stringResource(LR.string.settings_auto_archive_time_limits),
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            item {
                SettingRow(
                    primaryText = stringResource(LR.string.settings_auto_archive_starred),
                    secondaryText = stringResource(if (state.starredEpisodes) LR.string.settings_auto_archive_starred_summary else LR.string.settings_auto_archive_no_starred_summary),
                    toggle = SettingRowToggle.Switch(checked = state.starredEpisodes),
                    modifier = modifier
                        .toggleable(
                            value = state.starredEpisodes,
                            role = Role.Switch,
                            onValueChange = { viewModel.onStarredChanged(it) },
                        ),
                )
            }
        }

        if (openArchiveAfterPlayingDialog) {
            RadioOptionsDialog(
                title = stringResource(LR.string.podcast_settings_played_episodes),
                selectedOption = state.archiveAfterPlaying,
                allOptions = AutoArchiveAfterPlaying.All,
                optionName = { option -> stringResource(option.stringRes) },
                onSelectOption = { newValue: AutoArchiveAfterPlaying ->
                    viewModel.onPlayedEpisodesAfterChanged(newValue)
                    openArchiveAfterPlayingDialog = false
                },
                onDismiss = { openArchiveAfterPlayingDialog = false },
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }

        if (openArchiveInactiveDialog) {
            RadioOptionsDialog(
                title = stringResource(LR.string.settings_inactive_episodes),
                selectedOption = state.archiveInactive,
                allOptions = AutoArchiveInactive.All,
                optionName = { option -> stringResource(option.stringRes) },
                onSelectOption = { newValue: AutoArchiveInactive ->
                    viewModel.onInactiveChanged(newValue)
                    openArchiveInactiveDialog = false
                },
                onDismiss = { openArchiveInactiveDialog = false },
                modifier = Modifier.verticalScroll(rememberScrollState()),
            )
        }
    }
}
