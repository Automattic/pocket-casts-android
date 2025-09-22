package au.com.shiftyjelly.pocketcasts.playlists.create

import app.cash.turbine.Turbine
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisodeSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistPodcastSource
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisodeMetadata
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreviewForEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.SmartPlaylistDraft
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf

class FakePlaylistManager : PlaylistManager {
    val playlistPreviews = MutableStateFlow(emptyList<PlaylistPreview>())
    override fun playlistPreviewsFlow(): Flow<List<PlaylistPreview>> {
        return playlistPreviews.asStateFlow()
    }

    override suspend fun getAutoDownloadEpisodes(): List<PodcastEpisode> {
        return emptyList()
    }

    override suspend fun sortPlaylists(sortedUuids: List<String>) = Unit

    override suspend fun updateName(uuid: String, name: String) = Unit

    override suspend fun updateSortType(uuid: String, type: PlaylistEpisodeSortType) = Unit

    override suspend fun updateAutoDownload(uuid: String, isEnabled: Boolean) = Unit

    override suspend fun updateAutoDownloadLimit(uuid: String, limit: Int) = Unit

    override suspend fun toggleShowArchived(uuid: String) = Unit

    override suspend fun deletePlaylist(uuid: String) = Unit

    val createSmartPlaylistTurbine = Turbine<SmartPlaylistDraft>(name = "createSmartPlaylistTurbine")
    override suspend fun createSmartPlaylist(draft: SmartPlaylistDraft): String {
        createSmartPlaylistTurbine.add(draft)
        return UUID.randomUUID().toString()
    }

    val smartPlaylist = MutableStateFlow<SmartPlaylist?>(null)
    override fun smartPlaylistFlow(uuid: String, searchTerm: String?): Flow<SmartPlaylist?> {
        return smartPlaylist
    }

    val smartEpisodes = MutableStateFlow(emptyList<PlaylistEpisode.Available>())
    override fun smartEpisodesFlow(rules: SmartRules, sortType: PlaylistEpisodeSortType, searchTerm: String?): Flow<List<PlaylistEpisode.Available>> {
        return smartEpisodes
    }

    val smartEpisodesMetadata = MutableStateFlow(PlaylistEpisodeMetadata.Empty)
    override fun smartEpisodesMetadataFlow(rules: SmartRules): Flow<PlaylistEpisodeMetadata> {
        return smartEpisodesMetadata
    }

    override suspend fun updateSmartRules(uuid: String, rules: SmartRules) = Unit

    val createManualPlaylistTurbine = Turbine<String>(name = "createManualPlaylistTurbine")
    override suspend fun createManualPlaylist(name: String): String {
        createManualPlaylistTurbine.add(name)
        return UUID.randomUUID().toString()
    }

    val manualPlaylist = MutableStateFlow<ManualPlaylist?>(null)
    override fun manualPlaylistFlow(uuid: String, searchTerm: String?): Flow<ManualPlaylist?> {
        return manualPlaylist
    }

    override fun playlistPreviewsForEpisodeFlow(episodeUuid: String, searchTerm: String?): Flow<List<PlaylistPreviewForEpisode>> {
        return flowOf(emptyList())
    }

    override suspend fun getManualEpisodeSources(searchTerm: String?): List<ManualPlaylistEpisodeSource> {
        return emptyList()
    }

    override suspend fun getManualEpisodeSourcesForFolder(folderUuid: String, searchTerm: String?): List<ManualPlaylistPodcastSource> {
        return emptyList()
    }

    override fun notAddedManualEpisodesFlow(playlistUuid: String, podcastUuid: String, searchTerm: String?): Flow<List<PodcastEpisode>> {
        return flowOf(emptyList())
    }

    override suspend fun addManualEpisode(playlistUuid: String, episodeUuid: String): Boolean {
        return true
    }

    override suspend fun sortManualEpisodes(playlistUuid: String, episodeUuids: List<String>) = Unit

    override suspend fun deleteManualEpisodes(playlistUuid: String, episodeUuids: Collection<String>) = Unit
}
