package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules
import kotlinx.coroutines.flow.Flow

interface PlaylistManager {
    fun observePlaylistsPreview(): Flow<List<PlaylistPreview>>

    fun observeSmartEpisodes(rules: SmartRules): Flow<List<PodcastEpisode>>

    suspend fun deletePlaylist(uuid: String)

    suspend fun upsertSmartPlaylist(draft: SmartPlaylistDraft)

    suspend fun updatePlaylistsOrder(sortedUuids: List<String>)
}
