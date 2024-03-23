package au.com.shiftyjelly.pocketcasts.wear.ui.podcast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.wear.ui.component.EpisodeChip
import au.com.shiftyjelly.pocketcasts.wear.ui.podcast.PodcastViewModel.UiState
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState

object PodcastScreen {
    const val argument = "podcastUuid"
    const val route = "podcast/{$argument}"
    val podcastImageSize = 72.dp

    fun navigateRoute(podcastUuid: String) = "podcast/$podcastUuid"
}

@Composable
fun PodcastScreen(
    onEpisodeTap: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PodcastViewModel = hiltViewModel(),
    columnState: ScalingLazyColumnState,
) {
    val useRssArtwork by viewModel.useRssArtwork.collectAsState()

    when (val state = viewModel.uiState) {
        is UiState.Loaded -> Content(
            state = state,
            useRssArtwork = useRssArtwork,
            onEpisodeTap = onEpisodeTap,
            modifier = modifier,
            columnState = columnState,
        )

        UiState.Empty -> Unit // Do Nothing
    }
}

@Composable
private fun Content(
    state: UiState.Loaded,
    useRssArtwork: Boolean,
    onEpisodeTap: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
    columnState: ScalingLazyColumnState,
) {
    val podcast = state.podcast ?: return
    Box(modifier = modifier.fillMaxWidth()) {
        PodcastColorBackground(
            podcast = podcast,
            theme = state.theme,
            modifier = modifier,
        )

        ScalingLazyColumn(
            modifier = modifier.fillMaxWidth(),
            columnState = columnState,
        ) {
            item { Spacer(Modifier.height(4.dp)) }
            item {
                PodcastImage(
                    uuid = podcast.uuid,
                    modifier = Modifier.size(PodcastScreen.podcastImageSize),
                )
                Spacer(Modifier.height(4.dp))
            }
            item {
                Column {
                    Text(
                        modifier = modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onPrimary,
                        text = podcast.title,
                        style = MaterialTheme.typography.button,
                    )
                    Text(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onSecondary,
                        text = podcast.author,
                        style = MaterialTheme.typography.body2.merge(
                            @Suppress("DEPRECATION")
                            (
                                TextStyle(
                                    platformStyle = PlatformTextStyle(
                                        includeFontPadding = false,
                                    ),
                                )
                                ),
                        ),
                    )
                }
            }
            items(state.episodes) { episode ->
                EpisodeChip(
                    episode = episode,
                    useRssArtwork = useRssArtwork,
                    onClick = {
                        onEpisodeTap(episode)
                    },
                    showImage = false,
                )
            }
        }
    }
}

@Composable
private fun PodcastColorBackground(
    podcast: Podcast,
    theme: Theme,
    modifier: Modifier = Modifier,
) {
    val localConfig = LocalConfiguration.current
    val tintColor = podcast.tintColorForDarkBg
    val color = Color(ThemeColor.podcastIcon02(theme.activeTheme, tintColor))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height((localConfig.screenHeightDp / 2).dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        color.copy(alpha = 0.3f),
                        Color.Transparent,
                    ),
                ),
            ),
    )
}
