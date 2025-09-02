package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisodeSource
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisodeMetadata
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import kotlinx.coroutines.flow.Flow

interface PlaylistManager {
    fun observePlaylistsPreview(): Flow<List<PlaylistPreview>>

    fun observeSmartPlaylist(
        uuid: String,
        episodeSearchTerm: String? = null,
    ): Flow<SmartPlaylist?>

    fun observeManualPlaylist(
        uuid: String,
    ): Flow<ManualPlaylist?>

    fun observeSmartEpisodes(
        rules: SmartRules,
        sortType: PlaylistEpisodeSortType = PlaylistEpisodeSortType.NewestToOldest,
        searchTerm: String? = null,
    ): Flow<List<PodcastEpisode>>

    fun observeEpisodeMetadata(rules: SmartRules): Flow<PlaylistEpisodeMetadata>

    suspend fun updateSmartRules(uuid: String, rules: SmartRules)

    suspend fun updateSortType(uuid: String, sortType: PlaylistEpisodeSortType)

    suspend fun updateAutoDownload(uuid: String, isEnabled: Boolean)

    suspend fun updateAutoDownloadLimit(uuid: String, limit: Int)

    suspend fun updateName(uuid: String, name: String)

    suspend fun deletePlaylist(uuid: String)

    suspend fun createSmartPlaylist(draft: SmartPlaylistDraft): String

    suspend fun createManualPlaylist(name: String): String

    suspend fun updatePlaylistsOrder(sortedUuids: List<String>)

    suspend fun getManualPlaylistEpisodeSources(searchTerm: String? = null): List<ManualPlaylistEpisodeSource>

    fun observeManualPlaylistAvailableEpisodes(playlistUuid: String, podcastUuid: String): Flow<List<PodcastEpisode>>

    suspend fun addManualPlaylistEpisode(playlistUuid: String, episodeUuid: String): Boolean

    companion object {
        const val PLAYLIST_ARTWORK_EPISODE_LIMIT = 4
        const val SMART_PLAYLIST_EPISODE_LIMIT = 1000
        const val MANUAL_PLAYLIST_EPISODE_LIMIT = 1000
    }
}
