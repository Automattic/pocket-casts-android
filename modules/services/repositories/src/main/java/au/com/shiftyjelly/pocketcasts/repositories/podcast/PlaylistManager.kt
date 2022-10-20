package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe

interface PlaylistManager {
    fun findAll(): List<Playlist>
    suspend fun findAllSuspend(): List<Playlist>
    fun observeAll(): Flowable<List<Playlist>>

    fun findById(id: Long): Playlist?
    fun findByUuid(playlistUuid: String): Playlist?
    fun findByUuidRx(playlistUuid: String): Maybe<Playlist>
    fun observeByUuid(playlistUuid: String): Flowable<Playlist>
    fun observeByUuidAsList(playlistUuid: String): Flowable<List<Playlist>>

    fun findFirstByTitle(title: String): Playlist?
    fun findPlaylistsToSync(): List<Playlist>
    fun findEpisodes(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): List<Episode>
    fun observeEpisodes(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<Episode>>

    fun count(): Int
    fun countEpisodesNotCompleted(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int
    fun countEpisodesDownloading(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int
    fun countEpisodesNotDownloaded(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int

    fun createPlaylist(name: String, iconId: Int, draft: Boolean): Playlist

    fun create(playlist: Playlist): Long
    fun update(playlist: Playlist, userPlaylistUpdate: UserPlaylistUpdate?, isCreatingFilter: Boolean = false)

    fun updateAutoDownloadStatus(playlist: Playlist, autoDownloadEnabled: Boolean, unmeteredOnly: Boolean, powerOnly: Boolean)
    fun rxUpdateAutoDownloadStatus(playlist: Playlist, autoDownloadEnabled: Boolean, unmeteredOnly: Boolean, powerOnly: Boolean): Completable

    fun delete(playlist: Playlist)
    fun deleteSynced()
    fun deleteSynced(playlist: Playlist)

    fun countEpisodes(id: Long?, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int
    fun countEpisodesRx(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<Int>

    fun savePlaylistsOrder(playlists: List<Playlist>)

    fun checkForEpisodesToDownload(episodeManager: EpisodeManager, playbackManager: PlaybackManager)

    fun removePodcastFromPlaylists(podcastUuid: String)

    fun getSystemDownloadsFilter(): Playlist

    fun markAllSynced()

    fun updateAll(playlists: List<Playlist>)
    fun observeEpisodesPreview(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<Episode>>
}
