package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeImageData

data class PlaylistPreview(
    val uuid: String,
    val title: String,
    val episodeImages: List<EpisodeImageData>,
)
