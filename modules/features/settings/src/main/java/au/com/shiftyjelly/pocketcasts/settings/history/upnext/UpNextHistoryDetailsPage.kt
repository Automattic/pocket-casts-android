package au.com.shiftyjelly.pocketcasts.settings.history.upnext

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.settings.history.upnext.UpNextHistoryDetailsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.text.DateFormat
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun UpNextHistoryDetailsPage(
    viewModel: UpNextHistoryDetailsViewModel = hiltViewModel(),
    date: Long,
    onRestoreClick: (restoreUpNext: () -> Unit) -> Unit,
    onBackClick: () -> Unit,
    bottomInset: Dp,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UpNextHistoryDetailsView(
        date = date,
        state = state,
        onRestoreClick = { onRestoreClick { viewModel.restoreUpNext() } },
        onBackClick = onBackClick,
        bottomInset = bottomInset,
    )
}

@Composable
private fun UpNextHistoryDetailsView(
    date: Long,
    state: UiState,
    onRestoreClick: () -> Unit,
    onBackClick: () -> Unit,
    bottomInset: Dp,
) {
    Column {
        ThemedTopAppBar(
            title = formatDate(date),
            onNavigationClick = onBackClick,
            actions = { iconColor ->
                TextButton(
                    onClick = onRestoreClick,
                    enabled = state is UiState.Loaded,
                ) {
                    TextH50(text = stringResource(LR.string.restore))
                }
            },
        )
        when (state) {
            is UiState.Loading -> LoadingView()
            is UiState.Loaded -> {
                UpNextHistoryEpisodes(
                    state = state,
                    bottomInset = bottomInset,
                )
            }

            is UiState.Error -> UpNextHistoryErrorView()
        }
    }
}

@Composable
private fun UpNextHistoryEpisodes(
    state: UiState.Loaded,
    bottomInset: Dp,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier
            .fillMaxHeight()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = bottomInset),
    ) {
        items(state.episodes) { episode ->
            EpisodeRow(
                episode = episode,
                useEpisodeArtwork = state.useEpisodeArtwork,
            )
        }
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
private fun EpisodeRow(
    episode: BaseEpisode,
    useEpisodeArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val formattedDuration = remember(episode.durationMs) {
        TimeHelper.getTimeDurationMediumString(
            episode.durationMs,
            context,
        )
    }
    val published = remember(episode.durationMs) {
        val dateFormatter = RelativeDateFormatter(context)
        dateFormatter.format(episode.publishedDate)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        EpisodeImage(
            episode = episode,
            useEpisodeArtwork = useEpisodeArtwork,
            modifier = modifier.size(56.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            TextH70(
                text = published,
                maxLines = 1,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            TextH40(
                text = episode.title,
                maxLines = 1,
            )

            TextH70(
                text = formattedDuration,
                maxLines = 1,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun formatDate(date: Long) = remember(date) {
    val dateFormat = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT,
    )
    dateFormat.format(date)
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun UpNextHistoryDetailsViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        UpNextHistoryDetailsView(
            date = Date().time,
            state = UiState.Loaded(
                episodes = listOf(
                    PodcastEpisode(
                        uuid = "1",
                        title = "Episode 1",
                        publishedDate = Date(),
                        duration = 1000.0,
                    ),
                    PodcastEpisode(
                        uuid = "2",
                        title = "Episode 2",
                        publishedDate = Date(),
                        duration = 2000.0,
                    ),
                ),
                useEpisodeArtwork = true,
            ),
            onRestoreClick = {},
            onBackClick = {},
            bottomInset = 0.dp,
        )
    }
}

@Preview(device = Devices.PortraitRegular)
@Composable
private fun UpNextHistoryDetailsErrorViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        UpNextHistoryDetailsView(
            date = Date().time,
            state = UiState.Error,
            onRestoreClick = {},
            onBackClick = {},
            bottomInset = 0.dp,
        )
    }
}
