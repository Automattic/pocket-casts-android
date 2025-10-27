package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import kotlinx.coroutines.flow.Flow

interface SmartPlaylistManager {
    suspend fun findAll(): List<PlaylistEntity>
    fun findAllRxFlowable(): Flowable<List<PlaylistEntity>>

    fun findByIdBlocking(id: Long): PlaylistEntity?
    suspend fun findByUuid(playlistUuid: String): PlaylistEntity?
    fun findByUuidRxMaybe(playlistUuid: String): Maybe<PlaylistEntity>
    fun findByUuidRxFlowable(playlistUuid: String): Flowable<PlaylistEntity>
    fun findByUuidAsListRxFlowable(playlistUuid: String): Flowable<List<PlaylistEntity>>

    fun observeEpisodesBlocking(playlist: PlaylistEntity, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>>

    fun createPlaylistBlocking(name: String, iconId: Int, draft: Boolean): PlaylistEntity

    fun updateBlocking(playlist: PlaylistEntity, userPlaylistUpdate: UserPlaylistUpdate?, isCreatingFilter: Boolean = false)

    fun deleteBlocking(playlist: PlaylistEntity)
    fun deleteSyncedBlocking(playlist: PlaylistEntity)

    fun countEpisodesBlocking(id: Long?, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int
    fun countEpisodesRxFlowable(playlist: PlaylistEntity, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<Int>

    suspend fun removePodcastFromPlaylists(podcastUuid: String)

    fun updateAllBlocking(playlists: List<PlaylistEntity>)
    fun observeEpisodesPreviewBlocking(playlist: PlaylistEntity, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>>
}
