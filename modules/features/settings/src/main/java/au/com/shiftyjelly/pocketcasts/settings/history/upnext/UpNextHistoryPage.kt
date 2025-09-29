package au.com.shiftyjelly.pocketcasts.settings.history.upnext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingsSection
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextHistoryEntry
import au.com.shiftyjelly.pocketcasts.settings.history.upnext.UpNextHistoryViewModel.NavigationState
import au.com.shiftyjelly.pocketcasts.settings.history.upnext.UpNextHistoryViewModel.UiState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.text.DateFormat
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun UpNextHistoryPage(
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onHistoryEntryClick: (Date) -> Unit,
    viewModel: UpNextHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UpNextHistoryPageView(
        state = state,
        onBackPress = onBackPress,
        onHistoryEntryClick = { viewModel.onHistoryEntryClick(it) },
        bottomInset = bottomInset,
    )

    LaunchedEffect(onHistoryEntryClick) {
        viewModel.navigationState.collect { navigationState ->
            when (navigationState) {
                is NavigationState.ShowHistoryDetails -> onHistoryEntryClick(navigationState.date)
            }
        }
    }
}

@Composable
private fun UpNextHistoryPageView(
    state: UiState,
    bottomInset: Dp,
    onBackPress: () -> Unit,
    onHistoryEntryClick: (UpNextHistoryEntry) -> Unit,
) {
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.up_next_history),
            onNavigationClick = onBackPress,
        )

        when (state) {
            is UiState.Loading -> LoadingView()
            is UiState.Loaded -> {
                UpNextHistoryEntries(
                    historyEntries = state.entries,
                    onHistoryEntryClick = onHistoryEntryClick,
                    bottomInset = bottomInset,
                )
            }

            is UiState.Error -> UpNextHistoryErrorView()
        }
    }
}

@Composable
private fun UpNextHistoryEntries(
    historyEntries: List<UpNextHistoryEntry>,
    onHistoryEntryClick: (UpNextHistoryEntry) -> Unit,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier.fillMaxHeight(),
        contentPadding = PaddingValues(bottom = bottomInset),
    ) {
        item {
            TextP50(
                text = stringResource(LR.string.up_next_history_explanation),
                modifier = Modifier.padding(SettingsSection.horizontalPadding),
            )
        }
        items(historyEntries) { entry ->
            HistoryEntryRow(
                entry = entry,
                onClick = { onHistoryEntryClick(entry) },
            )
        }
    }
}

@Composable
private fun HistoryEntryRow(
    entry: UpNextHistoryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val episodesCount = pluralStringResource(
        id = LR.plurals.episode_count,
        count = entry.episodeCount,
        entry.episodeCount,
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = SettingsSection.horizontalPadding, vertical = SettingsSection.verticalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextH40(
            text = "${formatDate(entry.date)} $episodesCount",
        )
    }
}

@Composable
private fun UpNextHistoryErrorView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        TextH50(
            text = stringResource(LR.string.error_generic_message),
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun formatDate(date: Date) = remember(date) {
    val dateFormat = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT,
    )
    dateFormat.format(date)
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun UpNextHistoryPageViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        UpNextHistoryPageView(
            state = UiState.Loaded(
                entries = listOf(
                    UpNextHistoryEntry(
                        date = Date(),
                        episodeCount = 5,
                    ),
                    UpNextHistoryEntry(
                        date = Date(),
                        episodeCount = 3,
                    ),
                ),
            ),
            onBackPress = {},
            onHistoryEntryClick = {},
            bottomInset = 0.dp,
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun UpNextHistoryErrorViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        UpNextHistoryPageView(
            state = UiState.Error,
            onHistoryEntryClick = {},
            onBackPress = {},
            bottomInset = 0.dp,
        )
    }
}
