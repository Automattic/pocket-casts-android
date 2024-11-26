package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import kotlinx.coroutines.flow.Flow

interface PlaylistManager {
    fun findAllBlocking(): List<Playlist>
    suspend fun findAll(): List<Playlist>
    fun findAllFlow(): Flow<List<Playlist>>
    fun findAllRxFlowable(): Flowable<List<Playlist>>

    fun findByIdBlocking(id: Long): Playlist?
    suspend fun findByUuid(playlistUuid: String): Playlist?
    fun findByUuidBlocking(playlistUuid: String): Playlist?
    fun findByUuidRxMaybe(playlistUuid: String): Maybe<Playlist>
    fun findByUuidRxFlowable(playlistUuid: String): Flowable<Playlist>
    fun findByUuidAsListRxFlowable(playlistUuid: String): Flowable<List<Playlist>>

    fun findFirstByTitleBlocking(title: String): Playlist?
    fun findPlaylistsToSyncBlocking(): List<Playlist>
    fun findEpisodesBlocking(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): List<PodcastEpisode>
    fun observeEpisodesBlocking(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>>

    fun createPlaylistBlocking(name: String, iconId: Int, draft: Boolean): Playlist

    fun createBlocking(playlist: Playlist): Long
    fun updateBlocking(playlist: Playlist, userPlaylistUpdate: UserPlaylistUpdate?, isCreatingFilter: Boolean = false)

    fun updateAutoDownloadStatus(playlist: Playlist, autoDownloadEnabled: Boolean, unmeteredOnly: Boolean, powerOnly: Boolean)
    fun updateAutoDownloadStatusRxCompletable(playlist: Playlist, autoDownloadEnabled: Boolean, unmeteredOnly: Boolean, powerOnly: Boolean): Completable

    fun deleteBlocking(playlist: Playlist)
    suspend fun resetDb()
    fun deleteSyncedBlocking()
    fun deleteSyncedBlocking(playlist: Playlist)

    fun countEpisodesBlocking(id: Long?, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int
    fun countEpisodesRxFlowable(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<Int>

    fun checkForEpisodesToDownloadBlocking(episodeManager: EpisodeManager, playbackManager: PlaybackManager)

    fun removePodcastFromPlaylistsBlocking(podcastUuid: String)

    fun getSystemDownloadsFilter(): Playlist

    fun markAllSyncedBlocking()

    fun updateAllBlocking(playlists: List<Playlist>)
    fun observeEpisodesPreviewBlocking(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>>
}
