package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeUuids

data class PlaylistPreview(
    val uuid: String,
    val title: String,
    val episodeUuids: List<EpisodeUuids>,
)
