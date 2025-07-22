package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode

data class PlaylistPreview(
    val uuid: String,
    val title: String,
    val artworkEpisodes: List<PodcastEpisode>,
    val episodeCount: Int,
)
