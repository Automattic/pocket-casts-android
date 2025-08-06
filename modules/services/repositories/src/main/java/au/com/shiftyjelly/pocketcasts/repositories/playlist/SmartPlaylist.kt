package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import kotlin.time.Duration

data class SmartPlaylist(
    val uuid: String,
    val title: String,
    val totalEpisodeCount: Int,
    val playbackDurationLeft: Duration,
    val episodes: List<PodcastEpisode>,
    val artworkPodcasts: List<Podcast>,
)
