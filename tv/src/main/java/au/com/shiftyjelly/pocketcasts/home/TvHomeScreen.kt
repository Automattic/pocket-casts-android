package au.com.shiftyjelly.pocketcasts.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvDetailOverlay
import au.com.shiftyjelly.pocketcasts.component.TvFeaturedTile
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTile
import au.com.shiftyjelly.pocketcasts.component.TvPodcastTileDefaults
import au.com.shiftyjelly.pocketcasts.component.TvRow
import au.com.shiftyjelly.pocketcasts.component.TvVideoTile
import au.com.shiftyjelly.pocketcasts.component.tvFocusInactiveWhen
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.podcasts.TvPodcastDetailsScreen
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.TvTopBarHeight
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvHomeScreen(
    modifier: Modifier = Modifier,
    viewModel: TvHomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var openedPodcastUuid by rememberSaveable { mutableStateOf<String?>(null) }
    var restoreFocusTrigger by remember { mutableIntStateOf(0) }

    val podcastUuid = openedPodcastUuid
    Box(modifier = modifier.fillMaxSize()) {
        TvHomeContent(
            uiState = uiState,
            onRetry = viewModel::load,
            onOpenPodcast = { openedPodcastUuid = it },
            modifier = Modifier
                .fillMaxSize()
                .padding(top = TvTopBarHeight)
                .tvFocusInactiveWhen(podcastUuid != null),
            restoreFocusTrigger = restoreFocusTrigger,
        )
        TvDetailOverlay(
            target = podcastUuid,
            onBack = { openedPodcastUuid = null },
            onHide = { restoreFocusTrigger++ },
        ) { uuid ->
            TvPodcastDetailsScreen(
                podcastUuid = uuid,
                onClose = { openedPodcastUuid = null },
            )
        }
    }
}

@Composable
private fun TvHomeContent(
    uiState: TvHomeUiState,
    onRetry: () -> Unit,
    onOpenPodcast: (String) -> Unit,
    modifier: Modifier = Modifier,
    restoreFocusTrigger: Int = 0,
) {
    when (uiState) {
        is TvHomeUiState.Loading -> LoadingView(color = MaterialTheme.tvColors.textPrimary, modifier = modifier)

        is TvHomeUiState.Error -> TvHomeError(onRetry = onRetry, modifier = modifier)

        is TvHomeUiState.Ready -> if (uiState.rows.isEmpty()) {
            TvHomeError(onRetry = onRetry, modifier = modifier)
        } else {
            TvHomeRows(
                rows = uiState.rows,
                onOpenPodcast = onOpenPodcast,
                modifier = modifier,
                restoreFocusTrigger = restoreFocusTrigger,
            )
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
                color = MaterialTheme.tvColors.textPrimary,
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
    onOpenPodcast: (String) -> Unit,
    modifier: Modifier = Modifier,
    restoreFocusTrigger: Int = 0,
) {
    var lastFocusedRowIndex by rememberSaveable(rows.size) { mutableIntStateOf(0) }
    val rowFocusRequesters = remember(rows.size) { List(rows.size) { FocusRequester() } }

    var isInitialComposition by remember { mutableStateOf(true) }
    LaunchedEffect(restoreFocusTrigger) {
        // Only restore on an actual trigger change, not on the initial composition.
        if (isInitialComposition) {
            isInitialComposition = false
        } else {
            runCatching { rowFocusRequesters.getOrNull(lastFocusedRowIndex)?.requestFocus() }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        rows.forEachIndexed { rowIndex, row ->
            val rowModifier = Modifier.onFocusChanged { focusState ->
                if (focusState.hasFocus) {
                    lastFocusedRowIndex = rowIndex
                }
            }
            val rowFocusRequester = rowFocusRequesters[rowIndex]
            when (row) {
                is TvHomeRow.FeaturedPodcasts -> item(key = row.id) {
                    TvRow(
                        title = row.title,
                        items = row.podcasts,
                        itemSpacing = 32.dp,
                        key = TvHomePodcast::uuid,
                        focusRequester = rowFocusRequester,
                        modifier = rowModifier,
                    ) { podcast ->
                        TvFeaturedTile(
                            artworkUrl = podcast.artworkUrl,
                            isSponsored = podcast.isSponsored,
                            title = podcast.title,
                            description = podcast.description,
                            onGoToPodcast = { onOpenPodcast(podcast.uuid) },
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
                        focusRequester = rowFocusRequester,
                        modifier = rowModifier,
                    ) { episode ->
                        TvVideoTile(
                            thumbnailUrl = episode.thumbnailUrl,
                            podcastArtworkUrl = episode.podcastArtworkUrl,
                            podcastTitle = episode.podcastTitle,
                            episodeTitle = episode.episodeTitle,
                            onPlayEpisode = {},
                            onGoToPodcast = { onOpenPodcast(episode.podcastUuid) },
                        )
                    }
                }

                is TvHomeRow.Podcasts -> item(key = row.id) {
                    TvRow(
                        title = row.title,
                        items = row.podcasts,
                        key = TvHomePodcast::uuid,
                        focusRequester = rowFocusRequester,
                        modifier = rowModifier,
                    ) { podcast ->
                        TvPodcastTile(
                            artworkUrl = podcast.artworkUrl,
                            podcastTitle = podcast.title,
                            onClick = { onOpenPodcast(podcast.uuid) },
                            imageModifier = Modifier.width(TvPodcastTileDefaults.RowImageWidth),
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
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
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
                onOpenPodcast = {},
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvHomeErrorPreview() {
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvHomeContent(
                uiState = TvHomeUiState.Error,
                onRetry = {},
                onOpenPodcast = {},
            )
        }
    }
}

private fun previewPodcast(index: Int) = TvHomePodcast(
    uuid = "podcast-$index",
    title = "Podcast $index",
    description = "Description of podcast $index",
)
