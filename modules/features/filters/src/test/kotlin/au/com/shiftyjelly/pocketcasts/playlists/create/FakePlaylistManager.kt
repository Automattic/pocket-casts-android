package au.com.shiftyjelly.pocketcasts.playlists.create

import app.cash.turbine.Turbine
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistDraft
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakePlaylistManager : PlaylistManager {
    val playlistPreviews = MutableStateFlow(emptyList<PlaylistPreview>())
    override fun observePlaylistsPreview() = playlistPreviews.asStateFlow()

    val smartPlaylist = MutableStateFlow<SmartPlaylist?>(null)
    val smartPlaylistUuidTurbine = Turbine<String>(name = "observeSmartPlaylist:uuid")
    override fun observeSmartPlaylist(uuid: String): Flow<SmartPlaylist?> {
        smartPlaylistUuidTurbine.add(uuid)
        return smartPlaylist
    }

    val smartEpisodes = MutableStateFlow(emptyList<PodcastEpisode>())
    override fun observeSmartEpisodes(rules: SmartRules) = smartEpisodes.asStateFlow()

    val deletePlaylistTurbine = Turbine<String>(name = "deletePlaylist")
    override suspend fun deletePlaylist(uuid: String) {
        deletePlaylistTurbine.add(uuid)
    }

    val upsertSmartPlaylistTurbine = Turbine<SmartPlaylistDraft>(name = "upsertSmartPlaylist")
    override suspend fun upsertSmartPlaylist(draft: SmartPlaylistDraft): String {
        upsertSmartPlaylistTurbine.add(draft)
        return UUID.randomUUID().toString()
    }

    val updatePlaylistsOrderTurbine = Turbine<List<String>>(name = "updatePlaylistsOrder")
    override suspend fun updatePlaylistsOrder(sortedUuids: List<String>) {
        updatePlaylistsOrderTurbine.add(sortedUuids)
    }
}
