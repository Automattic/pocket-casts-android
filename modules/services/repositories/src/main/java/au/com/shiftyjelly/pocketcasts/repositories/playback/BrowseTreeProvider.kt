package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoConverter
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoConverter.convertFolderToMediaItem
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoConverter.convertPodcastToMediaItem
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.ServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.rx2.awaitSingleOrNull
import timber.log.Timber

private const val DOWNLOADS_ROOT = "__DOWNLOADS__"
private const val FILES_ROOT = "__FILES__"
private const val EPISODE_LIMIT = 100
private const val NUM_SUGGESTED_ITEMS = 8

@Singleton
class BrowseTreeProvider @Inject constructor(
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val folderManager: FolderManager,
    private val userEpisodeManager: UserEpisodeManager,
    private val playlistManager: PlaylistManager,
    private val upNextQueue: UpNextQueue,
    private val settings: Settings,
    private val serviceManager: ServiceManager,
    private val podcastCacheServiceManager: PodcastCacheServiceManager,
) {

    fun getRootId(isRecent: Boolean, isSuggested: Boolean, hasCurrentEpisode: Boolean): String? {
        return when {
            isRecent -> {
                Timber.d("Browser root hint for recent items")
                if (hasCurrentEpisode) RECENT_ROOT else null
            }

            isSuggested -> {
                Timber.d("Browser root hint for suggested items")
                SUGGESTED_ROOT
            }

            else -> MEDIA_ID_ROOT
        }
    }

    suspend fun loadChildren(parentId: String, context: Context): List<MediaBrowserCompat.MediaItem> {
        Timber.d("On load children: $parentId")
        return when (parentId) {
            RECENT_ROOT -> loadRecentChildren(context)

            SUGGESTED_ROOT -> loadSuggestedChildren(context)

            MEDIA_ID_ROOT -> loadRootChildren(context)

            PODCASTS_ROOT -> loadPodcastsChildren(context)

            FILES_ROOT -> loadFilesChildren(context)

            else -> {
                if (parentId.startsWith(FOLDER_ROOT_PREFIX)) {
                    loadFolderPodcastsChildren(folderUuid = parentId.substring(FOLDER_ROOT_PREFIX.length), context = context)
                } else {
                    loadEpisodeChildren(parentId, context)
                }
            }
        }
    }

    suspend fun loadRecentChildren(context: Context): List<MediaBrowserCompat.MediaItem> {
        Timber.d("Loading recent children")
        val episodes = listOfNotNull(upNextQueue.currentEpisode)
        return convertEpisodesToMediaItems(episodes, context)
    }

    suspend fun loadSuggestedChildren(context: Context): List<MediaBrowserCompat.MediaItem> {
        Timber.d("Loading suggested children")
        val episodes = mutableListOf<BaseEpisode>()
        val currentEpisode = upNextQueue.currentEpisode
        if (currentEpisode != null) {
            episodes.add(currentEpisode)
        }
        episodes.addAll(upNextQueue.queueEpisodes.take(NUM_SUGGESTED_ITEMS - 1))
        if (episodes.size < NUM_SUGGESTED_ITEMS) {
            val showPlayed = settings.autoShowPlayed.value
            val topPlaylist = getPlaylistPreviews().firstOrNull()
            if (topPlaylist != null) {
                val filterEpisodes = getPlaylistEpisodes(
                    uuid = topPlaylist.uuid,
                    filterEpisode = { playlistType, episode ->
                        when (playlistType) {
                            Playlist.Type.Manual -> showPlayed || !(episode.isFinished || episode.isArchived)
                            Playlist.Type.Smart -> true
                        }
                    },
                ).orEmpty()
                for (filterEpisode in filterEpisodes) {
                    if (episodes.size >= NUM_SUGGESTED_ITEMS) {
                        break
                    }
                    if (episodes.none { it.uuid == filterEpisode.uuid }) {
                        episodes.add(filterEpisode)
                    }
                }
            }
        }
        if (episodes.size < NUM_SUGGESTED_ITEMS) {
            val latestEpisode = episodeManager.findLatestEpisodeToPlayBlocking()
            if (latestEpisode != null && episodes.none { it.uuid == latestEpisode.uuid }) {
                episodes.add(latestEpisode)
            }
        }
        return convertEpisodesToMediaItems(episodes, context)
    }

    suspend fun loadRootChildren(context: Context): List<MediaBrowserCompat.MediaItem> {
        val rootItems = ArrayList<MediaBrowserCompat.MediaItem>()

        val podcastsDescription = MediaDescriptionCompat.Builder()
            .setTitle("Podcasts")
            .setMediaId(PODCASTS_ROOT)
            .setIconUri(AutoConverter.getPodcastsBitmapUri(context))
            .build()
        val podcastItem = MediaBrowserCompat.MediaItem(podcastsDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
        rootItems.add(podcastItem)

        for (playlist in getPlaylistPreviews()) {
            if (playlist.title.equals("video", ignoreCase = true)) continue

            val playlistItem = AutoConverter.convertPlaylistToMediaItem(context, playlist)
            rootItems.add(playlistItem)
        }

        val downloadsDescription = MediaDescriptionCompat.Builder()
            .setTitle("Downloads")
            .setMediaId(DOWNLOADS_ROOT)
            .setIconUri(AutoConverter.getDownloadsBitmapUri(context))
            .build()
        val downloadsItem = MediaBrowserCompat.MediaItem(downloadsDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
        rootItems.add(downloadsItem)

        val filesDescription = MediaDescriptionCompat.Builder()
            .setTitle("Files")
            .setMediaId(FILES_ROOT)
            .setIconUri(AutoConverter.getFilesBitmapUri(context))
            .build()
        val filesItem = MediaBrowserCompat.MediaItem(filesDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
        rootItems.add(filesItem)

        return rootItems
    }

    suspend fun loadPodcastsChildren(context: Context): List<MediaBrowserCompat.MediaItem> {
        return if (settings.cachedSubscription.value != null) {
            folderManager.getHomeFolder().mapNotNull { item ->
                when (item) {
                    is FolderItem.Folder -> convertFolderToMediaItem(context, item.folder)

                    is FolderItem.Podcast -> convertPodcastToMediaItem(
                        podcast = item.podcast,
                        context = context,
                        useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
                    )
                }
            }
        } else {
            podcastManager.findSubscribedSorted().mapNotNull { podcast ->
                convertPodcastToMediaItem(
                    podcast = podcast,
                    context = context,
                    useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
                )
            }
        }
    }

    suspend fun loadFolderPodcastsChildren(folderUuid: String, context: Context): List<MediaBrowserCompat.MediaItem> {
        return if (settings.cachedSubscription.value != null) {
            folderManager.findFolderPodcastsSorted(folderUuid).mapNotNull { podcast ->
                convertPodcastToMediaItem(
                    podcast = podcast,
                    context = context,
                    useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
                )
            }
        } else {
            emptyList()
        }
    }

    suspend fun loadEpisodeChildren(parentId: String, context: Context): List<MediaBrowserCompat.MediaItem> {
        val episodeItems = mutableListOf<MediaBrowserCompat.MediaItem>()
        val autoPlaySource: AutoPlaySource

        val showPlayed = settings.autoShowPlayed.value

        val episodesWithSource = if (DOWNLOADS_ROOT == parentId) {
            autoPlaySource = AutoPlaySource.Predefined.Downloads
            episodeManager.findDownloadedEpisodesRxFlowable().blockingFirst() to ""
        } else {
            autoPlaySource = AutoPlaySource.fromId(parentId)
            val episodes = getPlaylistEpisodes(
                uuid = parentId,
                filterEpisode = { playlistType, episode ->
                    when (playlistType) {
                        Playlist.Type.Manual -> showPlayed || !(episode.isFinished || episode.isArchived)
                        Playlist.Type.Smart -> true
                    }
                },
            )
            if (episodes != null) {
                episodes to parentId
            } else {
                null
            }
        }
        if (episodesWithSource != null) {
            val (episodeList, sourceId) = episodesWithSource
            val topEpisodes = episodeList.take(EPISODE_LIMIT)
            if (topEpisodes.isNotEmpty()) {
                for (episode in topEpisodes) {
                    podcastManager.findPodcastByUuid(episode.podcastUuid)?.let { parentPodcast ->
                        episodeItems.add(
                            AutoConverter.convertEpisodeToMediaItem(
                                context,
                                episode,
                                parentPodcast,
                                sourceId = sourceId,
                                useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
                            ),
                        )
                    }
                }
            }
        } else {
            val podcastFound = podcastManager.findPodcastByUuid(parentId)
                ?: podcastManager.findOrDownloadPodcastRxSingle(parentId).toMaybe().onErrorComplete().awaitSingleOrNull()
            podcastFound?.let { podcast ->
                val episodes = episodeManager
                    .findEpisodesByPodcastOrderedBlocking(podcast)
                    .filterNot { !showPlayed && (it.isFinished || it.isArchived) }
                    .take(EPISODE_LIMIT)
                    .toMutableList()
                if (!podcast.isSubscribed) {
                    episodes.sortBy { it.episodeType !is PodcastEpisode.EpisodeType.Trailer } // Bring trailers to the top
                }
                episodes.forEach { episode ->
                    episodeItems.add(
                        AutoConverter.convertEpisodeToMediaItem(
                            context,
                            episode,
                            podcast,
                            groupTrailers = !podcast.isSubscribed,
                            useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
                        ),
                    )
                }
            }
        }

        setAutoPlaySource(autoPlaySource)

        return episodeItems
    }

    suspend fun loadFilesChildren(context: Context): List<MediaBrowserCompat.MediaItem> {
        setAutoPlaySource(AutoPlaySource.Predefined.Files)
        return userEpisodeManager.findUserEpisodes().map {
            AutoConverter.convertEpisodeToMediaItem(
                context,
                it,
                Podcast.userPodcast,
                useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
            )
        }
    }

    suspend fun loadStarredChildren(context: Context): List<MediaBrowserCompat.MediaItem> {
        setAutoPlaySource(AutoPlaySource.Predefined.Starred)
        return episodeManager.findStarredEpisodes().take(EPISODE_LIMIT).mapNotNull { episode ->
            podcastManager.findPodcastByUuidBlocking(episode.podcastUuid)?.let { podcast ->
                AutoConverter.convertEpisodeToMediaItem(
                    context = context,
                    episode = episode,
                    parentPodcast = podcast,
                    useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
                )
            }
        }
    }

    suspend fun loadListeningHistoryChildren(context: Context): List<MediaBrowserCompat.MediaItem> {
        return episodeManager.findPlaybackHistoryEpisodes().take(EPISODE_LIMIT).mapNotNull { episode ->
            setAutoPlaySource(AutoPlaySource.fromId(episode.podcastUuid))
            podcastManager.findPodcastByUuidBlocking(episode.podcastUuid)?.let { podcast ->
                AutoConverter.convertEpisodeToMediaItem(
                    context = context,
                    episode = episode,
                    parentPodcast = podcast,
                    useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
                )
            }
        }
    }

    /**
     * Search for local and remote podcasts.
     * Returning an empty list displays "No media available for browsing here"
     * Returning null displays "Something went wrong". There is no way to display our own error message.
     */
    suspend fun search(query: String, context: Context): List<MediaBrowserCompat.MediaItem>? {
        val termCleaned = query.trim()
        val localPodcasts = podcastManager.findSubscribedNoOrder()
            .filter { it.title.contains(termCleaned, ignoreCase = true) || it.author.contains(termCleaned, ignoreCase = true) }
            .sortedBy { PodcastsSortType.cleanStringForSort(it.title) }
        val serverPodcasts = try {
            if (termCleaned.length <= 1) {
                emptyList()
            } else {
                serviceManager.searchForPodcasts(searchTerm = query).getOrThrow().searchResults
            }
        } catch (ex: Exception) {
            Timber.e(ex)
            if (localPodcasts.isEmpty()) {
                return null
            }
            emptyList()
        }
        val podcasts = (localPodcasts + serverPodcasts).distinctBy { it.uuid }
        return podcasts.mapNotNull { podcast ->
            convertPodcastToMediaItem(
                context = context,
                podcast = podcast,
                useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
            )
        }
    }

    suspend fun getPlaylistPreviews(): List<PlaylistPreview> {
        return playlistManager.playlistPreviewsFlow().first()
    }

    suspend fun getPlaylistEpisodes(
        uuid: String,
        filterEpisode: (Playlist.Type, PodcastEpisode) -> Boolean,
    ): List<PodcastEpisode>? {
        val playlist = playlistManager.smartPlaylistFlow(uuid).first() ?: playlistManager.manualPlaylistFlow(uuid).first()
        return playlist
            ?.episodes
            ?.mapNotNull(PlaylistEpisode::toPodcastEpisode)
            ?.filter { filterEpisode(playlist.type, it) }
    }

    private suspend fun convertEpisodesToMediaItems(
        episodes: List<BaseEpisode>,
        context: Context,
    ): List<MediaBrowserCompat.MediaItem> {
        return episodes.mapNotNull { episode ->
            val podcast = if (episode is PodcastEpisode) podcastManager.findPodcastByUuid(episode.podcastUuid) else Podcast.userPodcast
            if (podcast == null) {
                null
            } else {
                AutoConverter.convertEpisodeToMediaItem(
                    context = context,
                    episode = episode,
                    parentPodcast = podcast,
                    useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork,
                )
            }
        }
    }

    private fun setAutoPlaySource(autoPlaySource: AutoPlaySource) {
        settings.trackingAutoPlaySource.set(autoPlaySource, updateModifiedAt = false)
    }
}
