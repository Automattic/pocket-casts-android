package au.com.shiftyjelly.pocketcasts.repositories.playlist

import kotlinx.coroutines.flow.Flow

interface PlaylistManager {
    fun observePlaylistsPreview(): Flow<List<PlaylistPreview>>
}
