package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode

data class SmartPlaylist(
    val uuid: String,
    val title: String,
    val episodes: List<PodcastEpisode>,
)
