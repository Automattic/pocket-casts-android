package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisodeMetadata
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import kotlinx.coroutines.flow.Flow

interface PlaylistManager {
    fun observePlaylistsPreview(): Flow<List<PlaylistPreview>>

    fun observeSmartPlaylist(uuid: String): Flow<SmartPlaylist?>

    fun observeSmartEpisodes(rules: SmartRules, sortType: PlaylistEpisodeSortType = PlaylistEpisodeSortType.NewestToOldest): Flow<List<PodcastEpisode>>

    fun observeEpisodeMetadata(rules: SmartRules): Flow<PlaylistEpisodeMetadata>

    suspend fun updateSmartRules(uuid: String, rules: SmartRules)

    suspend fun updateSortType(uuid: String, sortType: PlaylistEpisodeSortType)

    suspend fun updateAutoDownload(uuid: String, isEnabled: Boolean)

    suspend fun updateAutoDownloadLimit(uuid: String, limit: Int)

    suspend fun updateName(uuid: String, name: String)

    suspend fun deletePlaylist(uuid: String)

    suspend fun insertSmartPlaylist(draft: SmartPlaylistDraft): String

    suspend fun updatePlaylistsOrder(sortedUuids: List<String>)
}
