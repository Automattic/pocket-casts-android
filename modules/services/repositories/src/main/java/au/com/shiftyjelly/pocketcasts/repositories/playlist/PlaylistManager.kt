package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisodeSource
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistPodcastSource
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisodeMetadata
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import kotlinx.coroutines.flow.Flow
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType as SortType

interface PlaylistManager {
    // <editor-fold desc="Generic playlists">
    fun playlistPreviewsFlow(): Flow<List<PlaylistPreview>>

    suspend fun getAutoDownloadEpisodes(): List<PodcastEpisode>

    suspend fun sortPlaylists(sortedUuids: List<String>)

    suspend fun updateName(uuid: String, name: String)

    suspend fun updateSortType(uuid: String, type: SortType)

    suspend fun updateAutoDownload(uuids: Collection<String>, isEnabled: Boolean)

    suspend fun updateAutoDownload(uuid: String, isEnabled: Boolean) = updateAutoDownload(listOf(uuid), isEnabled)

    suspend fun updateAutoDownloadLimit(uuid: String, limit: Int)

    suspend fun toggleShowArchived(uuid: String)

    suspend fun deletePlaylist(uuid: String)
    // </editor-fold>

    // <editor-fold desc="Smart playlists">
    suspend fun createSmartPlaylist(draft: SmartPlaylistDraft): String

    fun smartPlaylistFlow(uuid: String, searchTerm: String? = null): Flow<SmartPlaylist?>

    fun smartEpisodesFlow(rules: SmartRules, sortType: SortType = SortType.NewestToOldest, searchTerm: String? = null): Flow<List<PlaylistEpisode.Available>>

    fun smartEpisodesMetadataFlow(rules: SmartRules): Flow<PlaylistEpisodeMetadata>

    suspend fun updateSmartRules(uuidToRulesMap: Map<String, SmartRules>)

    suspend fun updateSmartRules(uuid: String, rules: SmartRules) = updateSmartRules(mapOf(uuid to rules))
    // </editor-fold>

    // <editor-fold desc="Manual playlists">
    suspend fun createManualPlaylist(name: String): String

    fun manualPlaylistFlow(
        uuid: String,
        searchTerm: String? = null,
    ): Flow<ManualPlaylist?>

    fun playlistPreviewsForEpisodeFlow(episodeUuid: String, searchTerm: String? = null): Flow<List<PlaylistPreviewForEpisode>>

    suspend fun getManualEpisodeSources(searchTerm: String? = null): List<ManualPlaylistEpisodeSource>

    suspend fun getManualEpisodeSourcesForFolder(folderUuid: String, searchTerm: String? = null): List<ManualPlaylistPodcastSource>

    fun notAddedManualEpisodesFlow(playlistUuid: String, podcastUuid: String, searchTerm: String? = null): Flow<List<PodcastEpisode>>

    suspend fun addManualEpisode(playlistUuid: String, episodeUuid: String): Boolean

    suspend fun sortManualEpisodes(playlistUuid: String, episodeUuids: List<String>)

    suspend fun deleteManualEpisodes(playlistUuid: String, episodeUuids: Collection<String>)

    suspend fun deleteManualEpisode(playlistUuid: String, episodeUuid: String) = deleteManualEpisodes(playlistUuid, listOf(episodeUuid))
    // </editor-fold>

    companion object {
        const val PLAYLIST_ARTWORK_EPISODE_LIMIT = 4
        const val SMART_PLAYLIST_EPISODE_LIMIT = 1000
        const val MANUAL_PLAYLIST_EPISODE_LIMIT = 1000
    }
}
