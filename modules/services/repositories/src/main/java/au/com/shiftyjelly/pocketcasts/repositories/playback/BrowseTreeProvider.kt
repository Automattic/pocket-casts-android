package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.os.Bundle
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
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
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import au.com.shiftyjelly.pocketcasts.utils.Util
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.rx2.awaitSingleOrNull
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val DOWNLOADS_ROOT = "__DOWNLOADS__"
private const val FILES_ROOT = "__FILES__"
private const val EPISODE_LIMIT = 100
private const val NUM_SUGGESTED_ITEMS = 8

internal const val FILTERS_ROOT = "__FILTERS__"
internal const val DISCOVER_ROOT = "__DISCOVER__"
internal const val PROFILE_ROOT = "__PROFILE__"
internal const val PROFILE_FILES = "__PROFILE_FILES__"
internal const val PROFILE_STARRED = "__PROFILE_STARRED__"
internal const val PROFILE_LISTENING_HISTORY = "__LISTENING_HISTORY__"

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
    private val listRepository: ListRepository,
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

    suspend fun loadChildren(parentId: String, context: Context): List<MediaItem> {
        Timber.d("On load children: $parentId")
        return when (parentId) {
            RECENT_ROOT -> loadRecentChildren(context)

            SUGGESTED_ROOT -> loadSuggestedChildren(context)

            MEDIA_ID_ROOT -> if (Util.isAutomotive(context)) {
                loadAutomotiveRootChildren(context)
            } else {
                loadRootChildren(context)
            }

            UP_NEXT_ROOT -> loadUpNextChildren(context)

            PODCASTS_ROOT -> loadPodcastsChildren(context)

            FILES_ROOT -> loadFilesChildren(context)

            FILTERS_ROOT -> loadFiltersRoot(context)

            DISCOVER_ROOT -> loadDiscoverRoot(context)

            PROFILE_ROOT -> loadProfileRoot(context)

            PROFILE_FILES -> loadFilesChildren(context)

            PROFILE_STARRED -> loadStarredChildren(context)

            PROFILE_LISTENING_HISTORY -> loadListeningHistoryChildren(context)

            else -> {
                if (parentId.startsWith(FOLDER_ROOT_PREFIX)) {
                    loadFolderPodcastsChildren(folderUuid = parentId.substring(FOLDER_ROOT_PREFIX.length), context = context)
                } else {
                    loadEpisodeChildren(parentId, context)
                }
            }
        }
    }

    internal suspend fun loadRecentChildren(context: Context): List<MediaItem> {
        Timber.d("Loading recent children")
        val episodes = listOfNotNull(upNextQueue.currentEpisode)
        return convertEpisodesToMediaItems(episodes, context)
    }

    internal suspend fun loadUpNextChildren(context: Context): List<MediaItem> {
        Timber.d("Loading Up Next children")
        val episodes = mutableListOf<BaseEpisode>()
        upNextQueue.currentEpisode?.let { episodes.add(it) }
        episodes.addAll(upNextQueue.queueEpisodes)
        return convertEpisodesToMediaItems(episodes, context)
    }

    internal suspend fun loadSuggestedChildren(context: Context): List<MediaItem> {
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

    internal suspend fun loadRootChildren(context: Context): List<MediaItem> {
        val rootItems = ArrayList<MediaItem>()

        val podcastsMetadata = MediaMetadata.Builder()
            .setTitle(context.getString(LR.string.podcasts))
            .setArtworkUri(AutoConverter.getPodcastsBitmapUri(context))
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()
        val podcastItem = MediaItem.Builder()
            .setMediaId(PODCASTS_ROOT)
            .setMediaMetadata(podcastsMetadata)
            .build()
        rootItems.add(podcastItem)

        for (playlist in getPlaylistPreviews()) {
            if (playlist.title.equals("video", ignoreCase = true)) continue

            val playlistItem = AutoConverter.convertPlaylistToMediaItem(context, playlist)
            rootItems.add(playlistItem)
        }

        val downloadsMetadata = MediaMetadata.Builder()
            .setTitle(context.getString(LR.string.downloads))
            .setArtworkUri(AutoConverter.getDownloadsBitmapUri(context))
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()
        val downloadsItem = MediaItem.Builder()
            .setMediaId(DOWNLOADS_ROOT)
            .setMediaMetadata(downloadsMetadata)
            .build()
        rootItems.add(downloadsItem)

        val filesMetadata = MediaMetadata.Builder()
            .setTitle(context.getString(LR.string.profile_navigation_files))
            .setArtworkUri(AutoConverter.getFilesBitmapUri(context))
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()
        val filesItem = MediaItem.Builder()
            .setMediaId(FILES_ROOT)
            .setMediaMetadata(filesMetadata)
            .build()
        rootItems.add(filesItem)

        return rootItems
    }

    internal suspend fun loadPodcastsChildren(context: Context): List<MediaItem> {
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

    internal suspend fun loadFolderPodcastsChildren(folderUuid: String, context: Context): List<MediaItem> {
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

    internal suspend fun loadEpisodeChildren(parentId: String, context: Context): List<MediaItem> {
        val episodeItems = mutableListOf<MediaItem>()
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

    internal suspend fun loadFilesChildren(context: Context): List<MediaItem> {
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

    internal suspend fun loadStarredChildren(context: Context): List<MediaItem> {
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

    internal suspend fun loadListeningHistoryChildren(context: Context): List<MediaItem> {
        val episodes = episodeManager.findPlaybackHistoryEpisodes().take(EPISODE_LIMIT)
        episodes.firstOrNull()?.let { setAutoPlaySource(AutoPlaySource.fromId(it.podcastUuid)) }
        return episodes.mapNotNull { episode ->
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

    private suspend fun loadAutomotiveRootChildren(context: Context): List<MediaItem> {
        val extrasContentAsList = Bundle().apply {
            putInt(DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE, DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM)
        }

        val podcastsItem = buildListMediaItem(context, id = PODCASTS_ROOT, title = LR.string.podcasts, drawable = IR.drawable.auto_tab_podcasts)
        val filtersItem = buildListMediaItem(
            context,
            id = FILTERS_ROOT,
            title = LR.string.playlists,
            drawable = IR.drawable.auto_tab_playlists,
            extras = extrasContentAsList,
        )
        val discoverItem = buildListMediaItem(context, id = DISCOVER_ROOT, title = LR.string.discover, drawable = IR.drawable.auto_tab_discover)
        val profileItem = buildListMediaItem(context, id = PROFILE_ROOT, title = LR.string.profile, drawable = IR.drawable.auto_tab_profile, extras = extrasContentAsList)

        return if (podcastManager.countSubscribed() > 0) {
            listOf(podcastsItem, filtersItem, discoverItem, profileItem)
        } else {
            listOf(discoverItem, podcastsItem, filtersItem, profileItem)
        }
    }

    private suspend fun loadFiltersRoot(context: Context): List<MediaItem> {
        return getPlaylistPreviews().mapNotNull {
            Timber.d("Filters ${it.title}")
            try {
                AutoConverter.convertPlaylistToMediaItem(context, it)
            } catch (e: Exception) {
                Timber.e(e, "Filter ${it.title} load failed")
                null
            }
        }
    }

    private suspend fun loadDiscoverRoot(context: Context): List<MediaItem> {
        Timber.d("Loading discover root")
        val discoverFeed = try {
            listRepository.getDiscoverFeed()
        } catch (e: Exception) {
            Timber.e(e, "Error loading discover")
            return emptyList()
        }

        val region = discoverFeed.regions[discoverFeed.defaultRegionCode] ?: return emptyList()
        val replacements = mapOf(
            discoverFeed.regionCodeToken to region.code,
            discoverFeed.regionNameToken to region.name,
        )

        return discoverFeed.layout.transformWithRegion(region, replacements, context.resources)
            .filter {
                it.type is ListType.PodcastList &&
                    it.displayStyle !is DisplayStyle.CollectionList &&
                    !it.sponsored &&
                    it.authenticated == false &&
                    it.displayStyle !is DisplayStyle.SinglePodcast
            }
            .mapNotNull { discoverItem ->
                Timber.d("Loading discover feed ${discoverItem.source}")
                listRepository.getListFeed(
                    url = discoverItem.source,
                    authenticated = discoverItem.authenticated,
                )?.run {
                    Pair(
                        title.orEmpty().ifEmpty { discoverItem.title },
                        podcasts?.take(6).orEmpty(),
                    )
                }
            }
            .flatMap { (title, podcasts) ->
                Timber.d("Mapping $title to media item")
                val groupTitle = title.tryToLocalise(context.resources)
                podcasts.map {
                    val extras = Bundle()
                    extras.putString(EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT, groupTitle)

                    val artworkUri = PodcastImage.getMediumArtworkUrl(uuid = it.uuid)
                    val localUri = AutoConverter.getArtworkUriForContentProvider(artworkUri.toUri(), context)

                    val metadata = MediaMetadata.Builder()
                        .setTitle(it.title)
                        .setArtworkUri(localUri)
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .setExtras(extras)
                        .build()

                    return@map MediaItem.Builder()
                        .setMediaId(it.uuid)
                        .setMediaMetadata(metadata)
                        .build()
                }
            }
    }

    private fun loadProfileRoot(context: Context): List<MediaItem> {
        return buildList {
            val isPaidUser = settings.cachedSubscription.value != null
            if (isPaidUser) {
                add(buildListMediaItem(context, id = PROFILE_FILES, title = LR.string.profile_navigation_files, drawable = IR.drawable.automotive_files))
            }
            add(buildListMediaItem(context, id = PROFILE_STARRED, title = LR.string.profile_navigation_starred, drawable = IR.drawable.automotive_filter_star))
            add(buildListMediaItem(context, id = PROFILE_LISTENING_HISTORY, title = LR.string.profile_navigation_listening_history, drawable = IR.drawable.automotive_listening_history))
        }
    }

    private fun buildListMediaItem(context: Context, id: String, @StringRes title: Int, @DrawableRes drawable: Int, extras: Bundle? = null): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(context.getString(title))
            .setArtworkUri(AutoConverter.getBitmapUri(drawable = drawable, context))
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .apply { if (extras != null) setExtras(extras) }
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(metadata)
            .build()
    }

    /**
     * Search for local and remote podcasts.
     * Returning an empty list displays "No media available for browsing here"
     * Returning null displays "Something went wrong". There is no way to display our own error message.
     */
    suspend fun search(query: String, context: Context): List<MediaItem>? {
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

    @VisibleForTesting
    internal suspend fun getPlaylistPreviews(): List<PlaylistPreview> {
        return playlistManager.playlistPreviewsFlow().first()
    }

    @VisibleForTesting
    internal suspend fun getPlaylistEpisodes(
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
    ): List<MediaItem> {
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
