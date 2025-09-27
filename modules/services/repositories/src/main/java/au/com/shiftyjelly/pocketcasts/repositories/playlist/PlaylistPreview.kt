package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.type.SmartRules

sealed interface PlaylistPreview {
    val uuid: String
    val title: String
    val episodeCount: Int
    val artworkPodcastUuids: List<String>
    val settings: Playlist.Settings
    val type: Playlist.Type
}

data class SmartPlaylistPreview(
    override val uuid: String,
    override val title: String,
    override val episodeCount: Int,
    override val artworkPodcastUuids: List<String>,
    override val settings: Playlist.Settings,
    val smartRules: SmartRules,
    val iconId: Int,
) : PlaylistPreview {
    override val type get() = Playlist.Type.Smart
}

data class ManualPlaylistPreview(
    override val uuid: String,
    override val title: String,
    override val episodeCount: Int,
    override val artworkPodcastUuids: List<String>,
    override val settings: Playlist.Settings,
) : PlaylistPreview {
    override val type get() = Playlist.Type.Manual
}

data class PlaylistPreviewForEpisode(
    val uuid: String,
    val title: String,
    val episodeCount: Int,
    val artworkPodcastUuids: List<String>,
    val hasEpisode: Boolean,
    val episodeLimit: Int,
) {
    val canAddOrRemoveEpisode get() = hasEpisode || episodeCount < episodeLimit
}
