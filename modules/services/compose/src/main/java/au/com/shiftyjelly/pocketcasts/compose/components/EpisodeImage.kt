package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode

@Composable
fun EpisodeImage(
    episode: BaseEpisode,
    modifier: Modifier = Modifier,
) {
    when (episode) {
        is PodcastEpisode -> {
            PodcastImage(
                uuid = episode.podcastUuid,
                dropShadow = false,
                modifier = modifier,
            )
        }
        is UserEpisode -> {
            UserEpisodeImage(
                episode = episode,
                contentDescription = null,
                modifier = modifier,
            )
        }
    }
}
