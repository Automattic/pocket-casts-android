package au.com.shiftyjelly.pocketcasts.podcasts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvArtworkImage
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeListItem
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvPodcastDetailsScreen(
    podcastUuid: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvPodcastDetailsViewModel = hiltViewModel<TvPodcastDetailsViewModel, TvPodcastDetailsViewModel.Factory>(
        key = podcastUuid,
        creationCallback = { factory -> factory.create(podcastUuid) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState, onClose) {
        if (uiState is TvPodcastDetailsUiState.NotFound) {
            onClose()
        }
    }

    TvPodcastDetailsContent(
        uiState = uiState,
        modifier = modifier,
    )
}

@Composable
private fun TvPodcastDetailsContent(
    uiState: TvPodcastDetailsUiState,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (uiState) {
            is TvPodcastDetailsUiState.Loading, TvPodcastDetailsUiState.NotFound -> {
                LoadingView(color = Color.White, modifier = Modifier.fillMaxSize())
            }

            is TvPodcastDetailsUiState.Loaded -> {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(80.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 32.dp, top = 16.dp, end = 32.dp),
                ) {
                    PodcastInfo(
                        podcast = uiState.podcast,
                        modifier = Modifier.width(ArtworkSize),
                    )
                    if (uiState.episodes.isEmpty()) {
                        NoEpisodes(modifier = Modifier.weight(1f).fillMaxHeight())
                    } else {
                        EpisodeList(
                            episodes = uiState.episodes,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PodcastInfo(
    podcast: Podcast,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = modifier,
    ) {
        TvArtworkImage(
            model = PodcastImage.getArtworkUrl(size = null, uuid = podcast.uuid, isWearOS = false),
            modifier = Modifier
                .size(ArtworkSize)
                .clip(RoundedCornerShape(8.dp)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (podcast.author.isNotBlank()) {
                Text(
                    text = podcast.author,
                    style = TvTextStyles.PlaylistCardCaption,
                    color = TvColors.TextSecondary,
                )
            }
            Text(
                text = podcast.title,
                style = TvTextStyles.ScreenTitle,
                color = Color.White,
            )
            if (podcast.podcastDescription.isNotBlank()) {
                Text(
                    text = podcast.podcastDescription,
                    style = TvTextStyles.PlaylistCardCaption,
                    color = TvColors.TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EpisodeList(
    episodes: List<PodcastEpisode>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormatter = remember(context) { RelativeDateFormatter(context) }
    val firstEpisodeFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        firstEpisodeFocusRequester.requestFocus()
    }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier,
    ) {
        itemsIndexed(
            items = episodes,
            key = { _, episode -> episode.uuid },
        ) { index, episode ->
            TvEpisodeListItem(
                episode = episode,
                dateFormatter = dateFormatter,
                onClick = {},
                episodeFocusRequester = firstEpisodeFocusRequester.takeIf { index == 0 },
            )
        }
    }
}

@Composable
private fun NoEpisodes(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(LR.string.podcast_no_episodes_found),
                style = TvTextStyles.ScreenTitle,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(LR.string.podcast_no_episodes),
                style = MaterialTheme.typography.bodyLarge,
                color = TvColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 400.dp),
            )
        }
    }
}

private val ArtworkSize = 280.dp

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvPodcastDetailsPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvPodcastDetailsContent(
                uiState = TvPodcastDetailsUiState.Loaded(
                    podcast = Podcast(
                        uuid = "podcast-uuid",
                        title = "The Daily",
                        author = "The New York Times",
                        podcastDescription = "This is what the news should sound like. The biggest stories of our time, told by the best journalists in the world.",
                    ),
                    episodes = List(4) { index ->
                        PodcastEpisode(uuid = "episode-$index", title = "Episode $index", publishedDate = Date(0))
                    },
                ),
            )
        }
    }
}
