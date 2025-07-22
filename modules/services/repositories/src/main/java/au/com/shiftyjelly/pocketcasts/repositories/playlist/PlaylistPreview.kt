package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

data class PlaylistPreview(
    val uuid: String,
    val title: String,
    val episodeCount: Int,
    val podcasts: List<Podcast>,
)
