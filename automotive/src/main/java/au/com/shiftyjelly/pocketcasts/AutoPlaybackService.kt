package au.com.shiftyjelly.pocketcasts

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaSession
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.playback.EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT
import au.com.shiftyjelly.pocketcasts.repositories.playback.FOLDER_ROOT_PREFIX
import au.com.shiftyjelly.pocketcasts.repositories.playback.MEDIA_ID_ROOT
import au.com.shiftyjelly.pocketcasts.repositories.playback.PODCASTS_ROOT
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackService
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoConverter
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.guava.future
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

const val FILTERS_ROOT = "__FILTERS__"
const val DISCOVER_ROOT = "__DISCOVER__"
const val PROFILE_ROOT = "__PROFILE__"
const val PROFILE_FILES = "__PROFILE_FILES__"
const val PROFILE_STARRED = "__PROFILE_STARRED__"
const val PROFILE_LISTENING_HISTORY = "__LISTENING_HISTORY__"

@UnstableApi
@SuppressLint("LogNotTimber")
@AndroidEntryPoint
open class AutoPlaybackService : PlaybackService() {
    override fun onCreate() {
        super.onCreate()
        RefreshPodcastsTask.runNow(this)

        Log.d(Settings.LOG_TAG_AUTO, "Auto playback service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(Settings.LOG_TAG_AUTO, "Auto playback service destroyed")

        playbackManager.pause(transientLoss = false, playbackSource = AnalyticsSource.AUTO_PAUSE)
    }

    // TODO: Set on media session
    class AutoMediaLibrarySessionCallback constructor(
        context: Context,
        episodeManager: EpisodeManager,
        folderManager: FolderManager,
        private val listRepository: ListRepository,
        playbackManager: PlaybackManager,
        playlistManager: PlaylistManager,
        podcastManager: PodcastManager,
        serverManager: ServerManager,
        serviceScope: CoroutineScope,
        settings: Settings,
        subscriptionManager: SubscriptionManager,
        userEpisodeManager: UserEpisodeManager,
    ) : CustomMediaLibrarySessionCallback(
        context = context,
        episodeManager = episodeManager,
        folderManager = folderManager,
        playbackManager = playbackManager,
        playlistManager = playlistManager,
        podcastManager = podcastManager,
        serverManager = serverManager,
        serviceScope = serviceScope,
        settings = settings,
        subscriptionManager = subscriptionManager,
        userEpisodeManager = userEpisodeManager,
    ) {
        override fun onSubscribe(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<Void>> {
            serviceScope.launch {
                val items = mediaItems(parentId)
                session.notifyChildrenChanged(browser, parentId, items.size, params)
            }

            return Futures.immediateFuture(LibraryResult.ofVoid())
        }

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
            var items: List<MediaItem>
            return serviceScope.future {
                try {
                    items = mediaItems(parentId)
                    Log.d(Settings.LOG_TAG_AUTO, "onLoadChildren. Sending results $parentId")
                    LibraryResult.ofItemList(items, params)
                } catch (e: Exception) {
                    Log.e(Settings.LOG_TAG_AUTO, "onLoadChildren. Could not load $parentId", e)
                    LibraryResult.ofItemList(emptyList(), params)
                } finally {
                    podcastManager.refreshPodcastsIfRequired("Automotive")
                }
            }
        }

        private suspend fun mediaItems(parentId: String) = when (parentId) {
            "androidx.media3.session.MediaLibraryService", // FIXME
            MEDIA_ID_ROOT -> loadRootChildren()
            PODCASTS_ROOT -> loadPodcastsChildren()
            FILTERS_ROOT -> loadFiltersRoot()
            DISCOVER_ROOT -> loadDiscoverRoot()
            PROFILE_ROOT -> loadProfileRoot()
            PROFILE_FILES -> loadFilesChildren()
            PROFILE_LISTENING_HISTORY -> loadListeningHistoryChildren()
            PROFILE_STARRED -> loadStarredChildren()
            else -> {
                if (parentId.startsWith(FOLDER_ROOT_PREFIX)) {
                    loadFolderPodcastsChildren(
                        folderUuid = parentId.substring(FOLDER_ROOT_PREFIX.length)
                    )
                } else {
                    loadEpisodeChildren(parentId)
                }
            }
        }

        override suspend fun loadRootChildren(): List<MediaItem> {
            val extrasContentAsList = bundleOf(DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE to DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM)

            val podcastsItem = buildListMediaItem(id = PODCASTS_ROOT, title = LR.string.podcasts, drawable = IR.drawable.auto_tab_podcasts)
            val filtersItem = buildListMediaItem(id = FILTERS_ROOT, title = LR.string.filters, drawable = IR.drawable.auto_tab_filter, extras = extrasContentAsList)
            val discoverItem = buildListMediaItem(id = DISCOVER_ROOT, title = LR.string.discover, drawable = IR.drawable.auto_tab_discover)
            val profileItem = buildListMediaItem(id = PROFILE_ROOT, title = LR.string.profile, drawable = IR.drawable.auto_tab_profile, extras = extrasContentAsList)

            // show the user's podcast collection first if they are subscribed any
            return if (podcastManager.countSubscribed() > 0) {
                listOf(podcastsItem, filtersItem, discoverItem, profileItem)
            } else {
                listOf(discoverItem, podcastsItem, filtersItem, profileItem)
            }
        }

        suspend fun loadFiltersRoot(): List<MediaItem> {
            return playlistManager.findAllSuspend().mapNotNull {
                Log.d(Settings.LOG_TAG_AUTO, "Filters ${it.title}")

                try {
                    AutoConverter.convertPlaylistToMediaItem(context, it)
                } catch (e: Exception) {
                    Log.e(Settings.LOG_TAG_AUTO, "Filter ${it.title} load failed", e)
                    null
                }
            }
        }

        suspend fun loadDiscoverRoot(): List<MediaItem> {
            Log.d(Settings.LOG_TAG_AUTO, "Loading discover root")
            val discoverFeed: Discover
            try {
                discoverFeed = listRepository.getDiscoverFeedSuspend()
            } catch (e: Exception) {
                Log.e(Settings.LOG_TAG_AUTO, "Error loading discover", e)
                return emptyList()
            }

            val region = discoverFeed.regions[discoverFeed.defaultRegionCode] ?: return emptyList()
            val replacements = mapOf(
                discoverFeed.regionCodeToken to region.code,
                discoverFeed.regionNameToken to region.name
            )

            val updatedList = discoverFeed.layout.transformWithRegion(region, replacements, context.resources)
                .filter { it.type is ListType.PodcastList && it.displayStyle !is DisplayStyle.CollectionList && !it.sponsored && it.displayStyle !is DisplayStyle.SinglePodcast }
                .map { discoverItem ->
                    Log.d(Settings.LOG_TAG_AUTO, "Loading discover feed ${discoverItem.source}")
                    val listFeed = listRepository.getListFeedSuspend(discoverItem.source)
                    Pair(discoverItem.title, listFeed.podcasts?.take(6) ?: emptyList())
                }
                .flatMap { (title, podcasts) ->
                    Log.d(Settings.LOG_TAG_AUTO, "Mapping $title to media item")
                    val groupTitle = title.tryToLocalise(context.resources)
                    podcasts.map {
                        val extras = Bundle()
                        extras.putString(EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT, groupTitle)

                        val artworkUri = PodcastImage.getArtworkUrl(size = 480, uuid = it.uuid)
                        val localUri = AutoConverter.getArtworkUriForContentProvider(Uri.parse(artworkUri), context)

                        val discoverDescription = MediaMetadata.Builder()
                            .setTitle(it.title)
                            .setArtworkUri(localUri)
                            .setExtras(extras)
                            .setIsBrowsable(true)
                            .setIsPlayable(false)
                            .build()

                        return@map MediaItem.Builder()
                            .setMediaId(it.uuid)
                            .setMediaMetadata(discoverDescription)
                            .build()
                    }
                }

            return updatedList
        }

        private fun loadProfileRoot(): List<MediaItem> {
            return buildList {
                // Add the user uploaded Files if they are a Plus subscriber
                val isPlusUser = subscriptionManager.getCachedStatus() is SubscriptionStatus.Plus
                if (isPlusUser) {
                    add(buildListMediaItem(id = PROFILE_FILES, title = LR.string.profile_navigation_files, drawable = IR.drawable.automotive_files))
                }
                add(buildListMediaItem(id = PROFILE_STARRED, title = LR.string.profile_navigation_starred, drawable = IR.drawable.automotive_filter_star))
                add(buildListMediaItem(id = PROFILE_LISTENING_HISTORY, title = LR.string.profile_navigation_listening_history, drawable = IR.drawable.automotive_listening_history))
            }
        }

        private fun buildListMediaItem(id: String, @StringRes title: Int, @DrawableRes drawable: Int, extras: Bundle? = null): MediaItem {
            val description = MediaMetadata.Builder()
                .setTitle(context.getString(title))
                .setExtras(extras)
                .setArtworkUri(AutoConverter.getBitmapUri(drawable = drawable, context))
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
            return MediaItem.Builder()
                .setMediaId(id)
                .setMediaMetadata(description)
                .build()
        }
    }
}
