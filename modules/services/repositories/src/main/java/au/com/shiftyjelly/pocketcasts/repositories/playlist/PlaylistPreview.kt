package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.to.PlaylistIcon
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules

sealed interface PlaylistPreview {
    val uuid: String
    val title: String
    val settings: Playlist.Settings
    val icon: PlaylistIcon
    val type: Playlist.Type
}

data class SmartPlaylistPreview(
    override val uuid: String,
    override val title: String,
    override val settings: Playlist.Settings,
    override val icon: PlaylistIcon,
    val smartRules: SmartRules,
) : PlaylistPreview {
    override val type get() = Playlist.Type.Smart
}

data class ManualPlaylistPreview(
    override val uuid: String,
    override val title: String,
    override val settings: Playlist.Settings,
    override val icon: PlaylistIcon,
) : PlaylistPreview {
    override val type get() = Playlist.Type.Manual
}
