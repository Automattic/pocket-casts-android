package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import kotlinx.coroutines.flow.Flow

interface SmartPlaylistManager {
    fun findAllBlocking(): List<SmartPlaylist>
    suspend fun findAll(): List<SmartPlaylist>
    fun findAllFlow(): Flow<List<SmartPlaylist>>
    fun findAllRxFlowable(): Flowable<List<SmartPlaylist>>

    fun findByIdBlocking(id: Long): SmartPlaylist?
    suspend fun findByUuid(playlistUuid: String): SmartPlaylist?
    fun findByUuidBlocking(playlistUuid: String): SmartPlaylist?
    fun findByUuidRxMaybe(playlistUuid: String): Maybe<SmartPlaylist>
    fun findByUuidRxFlowable(playlistUuid: String): Flowable<SmartPlaylist>
    fun findByUuidAsListRxFlowable(playlistUuid: String): Flowable<List<SmartPlaylist>>

    fun findFirstByTitleBlocking(title: String): SmartPlaylist?
    fun findPlaylistsToSyncBlocking(): List<SmartPlaylist>
    fun findEpisodesBlocking(smartPlaylist: SmartPlaylist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): List<PodcastEpisode>
    fun observeEpisodesBlocking(smartPlaylist: SmartPlaylist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>>

    fun createPlaylistBlocking(name: String, iconId: Int, draft: Boolean): SmartPlaylist

    suspend fun create(smartPlaylist: SmartPlaylist): Long
    suspend fun update(smartPlaylist: SmartPlaylist, userPlaylistUpdate: UserPlaylistUpdate?, isCreatingFilter: Boolean = false)
    fun updateBlocking(smartPlaylist: SmartPlaylist, userPlaylistUpdate: UserPlaylistUpdate?, isCreatingFilter: Boolean = false)

    fun updateAutoDownloadStatus(smartPlaylist: SmartPlaylist, autoDownloadEnabled: Boolean, unmeteredOnly: Boolean, powerOnly: Boolean)
    fun updateAutoDownloadStatusRxCompletable(smartPlaylist: SmartPlaylist, autoDownloadEnabled: Boolean, unmeteredOnly: Boolean, powerOnly: Boolean): Completable

    fun deleteBlocking(smartPlaylist: SmartPlaylist)
    suspend fun resetDb()
    fun deleteSyncedBlocking()
    suspend fun deleteSynced(smartPlaylist: SmartPlaylist)
    fun deleteSyncedBlocking(smartPlaylist: SmartPlaylist)

    fun countEpisodesBlocking(id: Long?, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int
    fun countEpisodesRxFlowable(smartPlaylist: SmartPlaylist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<Int>

    fun checkForEpisodesToDownloadBlocking(episodeManager: EpisodeManager, playbackManager: PlaybackManager)

    fun removePodcastFromPlaylistsBlocking(podcastUuid: String)

    fun getSystemDownloadsFilter(): SmartPlaylist

    suspend fun markAllSynced()

    fun updateAllBlocking(smartPlaylists: List<SmartPlaylist>)
    fun observeEpisodesPreviewBlocking(smartPlaylist: SmartPlaylist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>>
}
