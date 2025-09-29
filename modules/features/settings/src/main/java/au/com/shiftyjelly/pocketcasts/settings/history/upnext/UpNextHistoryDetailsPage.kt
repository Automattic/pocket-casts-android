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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP60
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.settings.history.upnext.UpNextHistoryDetailsViewModel.UiState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.text.DateFormat
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun UpNextHistoryDetailsPage(
    date: Long,
    bottomInset: Dp,
    onRestoreClick: (UiState, restoreUpNext: () -> Unit) -> Unit,
    onBackPress: () -> Unit,
    viewModel: UpNextHistoryDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    UpNextHistoryDetailsView(
        date = date,
        state = state,
        onRestoreClick = { onRestoreClick(state) { viewModel.restoreUpNext() } },
        onBackPress = onBackPress,
        bottomInset = bottomInset,
    )
}

@Composable
private fun UpNextHistoryDetailsView(
    state: UiState,
    date: Long,
    bottomInset: Dp,
    onRestoreClick: () -> Unit,
    onBackPress: () -> Unit,
) {
    Column {
        ThemedTopAppBar(
            title = formatDate(date),
            onNavigationClick = onBackPress,
            actions = {
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
            .fillMaxHeight(),
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
    val formattedDuration = remember(episode.durationMs, episode.playedUpToMs, episode.isInProgress) {
        TimeHelper
            .getTimeLeft(
                episode.playedUpToMs,
                episode.durationMs.toLong(),
                episode.isInProgress,
                context,
            )
            .text
    }

    val tintColor = MaterialTheme.theme.colors.primaryText02.toArgb()
    val summary = remember(episode.durationMs) {
        val dateFormatter = RelativeDateFormatter(context)
        episode.getSummaryText(
            dateFormatter = dateFormatter,
            tintColor = tintColor,
            showDuration = false,
            context = context,
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        EpisodeImage(
            episode = episode,
            useEpisodeArtwork = useEpisodeArtwork,
            modifier = Modifier.size(56.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
        ) {
            TextC70(
                text = summary.toString(),
                maxLines = 1,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
            )

            TextP40(
                text = episode.title,
                maxLines = 2,
            )

            TextP60(
                text = formattedDuration,
                maxLines = 1,
                color = MaterialTheme.theme.colors.primaryText02,
            )
        }
    }
    HorizontalDivider()
}

@Composable
private fun formatDate(date: Long) = remember(date) {
    val dateFormat = DateFormat.getDateTimeInstance(
        DateFormat.MEDIUM,
        DateFormat.SHORT,
    )
    dateFormat.format(date)
}

@Preview(device = Devices.PORTRAIT_REGULAR)
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
            onBackPress = {},
            bottomInset = 0.dp,
        )
    }
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun UpNextHistoryDetailsErrorViewPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        UpNextHistoryDetailsView(
            date = Date().time,
            state = UiState.Error,
            onRestoreClick = {},
            onBackPress = {},
            bottomInset = 0.dp,
        )
    }
}
