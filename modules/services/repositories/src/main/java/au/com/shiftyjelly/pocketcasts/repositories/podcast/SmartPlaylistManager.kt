package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import kotlinx.coroutines.flow.Flow

interface SmartPlaylistManager {
    fun findAllBlocking(): List<PlaylistEntity>
    suspend fun findAll(): List<PlaylistEntity>
    fun findAllFlow(): Flow<List<PlaylistEntity>>
    fun findAllRxFlowable(): Flowable<List<PlaylistEntity>>

    fun findByIdBlocking(id: Long): PlaylistEntity?
    suspend fun findByUuid(playlistUuid: String): PlaylistEntity?
    fun findByUuidBlocking(playlistUuid: String): PlaylistEntity?
    fun findByUuidRxMaybe(playlistUuid: String): Maybe<PlaylistEntity>
    fun findByUuidRxFlowable(playlistUuid: String): Flowable<PlaylistEntity>
    fun findByUuidAsListRxFlowable(playlistUuid: String): Flowable<List<PlaylistEntity>>

    fun findFirstByTitleBlocking(title: String): PlaylistEntity?
    fun findPlaylistsToSyncBlocking(): List<PlaylistEntity>
    fun findEpisodesBlocking(playlist: PlaylistEntity, episodeManager: EpisodeManager, playbackManager: PlaybackManager): List<PodcastEpisode>
    fun observeEpisodesBlocking(playlist: PlaylistEntity, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>>

    fun createPlaylistBlocking(name: String, iconId: Int, draft: Boolean): PlaylistEntity

    suspend fun create(playlist: PlaylistEntity): Long
    suspend fun update(playlist: PlaylistEntity, userPlaylistUpdate: UserPlaylistUpdate?, isCreatingFilter: Boolean = false)
    fun updateBlocking(playlist: PlaylistEntity, userPlaylistUpdate: UserPlaylistUpdate?, isCreatingFilter: Boolean = false)

    fun updateAutoDownloadStatus(playlist: PlaylistEntity, autoDownloadEnabled: Boolean)

    fun deleteBlocking(playlist: PlaylistEntity)
    suspend fun resetDb()
    fun deleteSyncedBlocking()
    suspend fun deleteSynced(playlist: PlaylistEntity)
    fun deleteSyncedBlocking(playlist: PlaylistEntity)

    fun countEpisodesBlocking(id: Long?, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int
    fun countEpisodesRxFlowable(playlist: PlaylistEntity, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<Int>

    suspend fun removePodcastFromPlaylists(podcastUuid: String)

    suspend fun markAllSynced()

    fun updateAllBlocking(playlists: List<PlaylistEntity>)
    fun observeEpisodesPreviewBlocking(playlist: PlaylistEntity, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>>
}
