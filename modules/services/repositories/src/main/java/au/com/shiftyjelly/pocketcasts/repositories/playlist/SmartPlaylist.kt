package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import kotlin.time.Duration

data class SmartPlaylist(
    val uuid: String,
    val title: String,
    val smartRules: SmartRules,
    val episodes: List<PodcastEpisode>,
    val totalEpisodeCount: Int,
    val playbackDurationLeft: Duration,
    val artworkPodcasts: List<Podcast>,
)
