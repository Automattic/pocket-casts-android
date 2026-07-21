package au.com.shiftyjelly.pocketcasts.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvFeaturedTile
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTile
import au.com.shiftyjelly.pocketcasts.component.TvRow
import au.com.shiftyjelly.pocketcasts.component.TvVideoTile
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvHomeScreen(
    modifier: Modifier = Modifier,
    viewModel: TvHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TvHomeContent(
        uiState = uiState,
        onRetry = viewModel::load,
        modifier = modifier,
    )
}

@Composable
private fun TvHomeContent(
    uiState: TvHomeUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is TvHomeUiState.Loading -> LoadingView(color = Color.White, modifier = modifier)

        is TvHomeUiState.Error -> TvHomeError(onRetry = onRetry, modifier = modifier)

        is TvHomeUiState.Ready -> if (uiState.rows.isEmpty()) {
            TvHomeError(onRetry = onRetry, modifier = modifier)
        } else {
            TvHomeRows(rows = uiState.rows, modifier = modifier)
        }
    }
}

@Composable
private fun TvHomeError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(LR.string.error_generic_message),
                color = Color.White,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onRetry) {
                Text(stringResource(LR.string.retry))
            }
        }
    }
}

@Composable
private fun TvHomeRows(
    rows: List<TvHomeRow>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        rows.forEach { row ->
            when (row) {
                is TvHomeRow.FeaturedPodcasts -> item(key = row.id) {
                    TvRow(
                        title = row.title,
                        items = row.podcasts,
                        itemSpacing = 32.dp,
                        key = TvHomePodcast::uuid,
                    ) { podcast ->
                        TvFeaturedTile(
                            artworkUrl = podcast.artworkUrl,
                            isSponsored = podcast.isSponsored,
                            title = podcast.title,
                            description = podcast.description,
                            onGoToPodcast = {},
                            onPlayLastEpisode = {},
                        )
                    }
                }

                is TvHomeRow.Episodes -> item(key = row.id) {
                    TvRow(
                        title = row.title,
                        items = row.episodes,
                        itemSpacing = 32.dp,
                        key = TvHomeEpisode::episodeUuid,
                    ) { episode ->
                        TvVideoTile(
                            thumbnailUrl = episode.thumbnailUrl,
                            podcastArtworkUrl = episode.podcastArtworkUrl,
                            podcastTitle = episode.podcastTitle,
                            episodeTitle = episode.episodeTitle,
                            onPlayEpisode = {},
                            onGoToPodcast = {},
                        )
                    }
                }

                is TvHomeRow.Podcasts -> item(key = row.id) {
                    TvRow(
                        title = row.title,
                        items = row.podcasts,
                        key = TvHomePodcast::uuid,
                    ) { podcast ->
                        TvPodcastTile(
                            artworkUrl = podcast.artworkUrl,
                            podcastTitle = podcast.title,
                            onClick = {},
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvHomeContentPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvHomeContent(
                    uiState = TvHomeUiState.Ready(
                        rows = listOf(
                            TvHomeRow.FeaturedPodcasts(
                                id = "featured",
                                title = "Featured",
                                podcasts = (1..3).map { previewPodcast(it) },
                            ),
                            TvHomeRow.Episodes(
                                id = "tv_featured_videos",
                                title = "Made for TV",
                                episodes = (1..6).map {
                                    TvHomeEpisode(
                                        episodeUuid = "episode-$it",
                                        episodeTitle = "Episode $it",
                                        podcastUuid = "podcast-$it",
                                        podcastTitle = "Podcast $it",
                                    )
                                },
                            ),
                            TvHomeRow.Podcasts(
                                id = "trending",
                                title = "Trending",
                                podcasts = (1..8).map { previewPodcast(it) },
                            ),
                        ),
                    ),
                    onRetry = {},
                )
            }
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvHomeErrorPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvHomeContent(
                    uiState = TvHomeUiState.Error,
                    onRetry = {},
                )
            }
        }
    }
}

private fun previewPodcast(index: Int) = TvHomePodcast(
    uuid = "podcast-$index",
    title = "Podcast $index",
    description = "Description of podcast $index",
)
