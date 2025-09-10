package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PreviewRegularDevice
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar.Style
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistNameInputField
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun SettingsPage(
    state: TextFieldState,
    isAutoDownloadEnabled: Boolean,
    autoDownloadEpisodeLimit: Int,
    onChangeAutoDownloadValue: (Boolean) -> Unit,
    onClickEpisodeLimit: () -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.playlist_options),
            onNavigationClick = onClickBack,
            style = Style.Immersive,
            windowInsets = WindowInsets.statusBars,
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        PlaylistNameInputField(
            state = state,
            onClickImeAction = {
                focusManager.clearFocus()
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        Spacer(
            modifier = Modifier.height(20.dp),
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )
        AutoDownloadSwitchRow(
            isAutoDownloadEnabled = isAutoDownloadEnabled,
            onChangeAutoDownloadValue = onChangeAutoDownloadValue,
        )
        AnimatedVisibility(
            visible = isAutoDownloadEnabled,
        ) {
            AutoDownloadEpisodeCountRow(
                episodeCount = autoDownloadEpisodeLimit,
                onClick = onClickEpisodeLimit,
            )
        }
    }
}

@Composable
private fun AutoDownloadSwitchRow(
    isAutoDownloadEnabled: Boolean,
    onChangeAutoDownloadValue: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = stringResource(LR.string.auto_download),
        secondaryText = stringResource(LR.string.playlist_setting_auto_download_description),
        toggle = SettingRowToggle.Switch(checked = isAutoDownloadEnabled),
        horizontalPadding = 16.dp,
        indent = false,
        modifier = modifier.toggleable(
            value = isAutoDownloadEnabled,
            role = Role.Switch,
            onValueChange = onChangeAutoDownloadValue,
        ),
    )
}

@Composable
private fun AutoDownloadEpisodeCountRow(
    episodeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(
        primaryText = stringResource(LR.string.filters_download_first),
        secondaryText = pluralStringResource(LR.plurals.episode_count, episodeCount, episodeCount),
        horizontalPadding = 16.dp,
        indent = false,
        modifier = modifier.clickable(
            role = Role.Button,
            onClick = onClick,
        ),
    )
}

@PreviewRegularDevice
@Composable
private fun SettingsPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    val state = rememberTextFieldState(initialText = "My Playlist")
    var isAutoDownloadEnabled by remember { mutableStateOf(true) }

    AppThemeWithBackground(themeType) {
        SettingsPage(
            state = state,
            isAutoDownloadEnabled = isAutoDownloadEnabled,
            autoDownloadEpisodeLimit = 10,
            onChangeAutoDownloadValue = { isAutoDownloadEnabled = it },
            onClickEpisodeLimit = {},
            onClickBack = {},
        )
    }
}
