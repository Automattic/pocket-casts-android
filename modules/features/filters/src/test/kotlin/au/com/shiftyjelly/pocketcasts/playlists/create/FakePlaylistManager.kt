package au.com.shiftyjelly.pocketcasts.playlists.create

import app.cash.turbine.Turbine
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisodeMetadata
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
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
    override fun observeSmartPlaylist(uuid: String): Flow<SmartPlaylist?> = smartPlaylist

    val smartEpisodes = MutableStateFlow(emptyList<PodcastEpisode>())
    override fun observeSmartEpisodes(rules: SmartRules, sortType: PlaylistEpisodeSortType) = smartEpisodes.asStateFlow()

    val episodeMetadata = MutableStateFlow(PlaylistEpisodeMetadata.Empty)
    override fun observeEpisodeMetadata(rules: SmartRules) = episodeMetadata.asStateFlow()

    override suspend fun updateSmartRules(uuid: String, rules: SmartRules) = Unit

    override suspend fun updateSortType(uuid: String, sortType: PlaylistEpisodeSortType) = Unit

    override suspend fun updateAutoDownload(uuid: String, isEnabled: Boolean) = Unit

    override suspend fun updateAutoDownloadLimit(uuid: String, limit: Int) = Unit

    override suspend fun updateName(uuid: String, name: String) = Unit

    override suspend fun deletePlaylist(uuid: String) = Unit

    val upsertSmartPlaylistTurbine = Turbine<SmartPlaylistDraft>(name = "upsertSmartPlaylist")
    override suspend fun upsertSmartPlaylist(draft: SmartPlaylistDraft): String {
        upsertSmartPlaylistTurbine.add(draft)
        return UUID.randomUUID().toString()
    }

    override suspend fun updatePlaylistsOrder(sortedUuids: List<String>) = Unit
}
