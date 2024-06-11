package au.com.shiftyjelly.pocketcasts

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_CONTENT_STYLE_BROWSABLE
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_VALUE_CONTENT_STYLE_LIST_ITEM
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.playback.EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT
import au.com.shiftyjelly.pocketcasts.repositories.playback.FOLDER_ROOT_PREFIX
import au.com.shiftyjelly.pocketcasts.repositories.playback.MEDIA_ID_ROOT
import au.com.shiftyjelly.pocketcasts.repositories.playback.PODCASTS_ROOT
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackService
import au.com.shiftyjelly.pocketcasts.repositories.playback.RECENT_ROOT
import au.com.shiftyjelly.pocketcasts.repositories.playback.SUGGESTED_ROOT
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoConverter
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

const val FILTERS_ROOT = "__FILTERS__"
const val DISCOVER_ROOT = "__DISCOVER__"
const val PROFILE_ROOT = "__PROFILE__"
const val PROFILE_FILES = "__PROFILE_FILES__"
const val PROFILE_STARRED = "__PROFILE_STARRED__"
const val PROFILE_LISTENING_HISTORY = "__LISTENING_HISTORY__"

@SuppressLint("LogNotTimber")
@AndroidEntryPoint
class AutoPlaybackService : PlaybackService() {

    @Inject lateinit var listSource: ListRepository

    @Inject @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        settings.setAutomotiveConnectedToMediaSession(false)

        RefreshPodcastsTask.runNow(this, applicationScope)

        Log.d(Settings.LOG_TAG_AUTO, "Auto playback service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(Settings.LOG_TAG_AUTO, "Auto playback service destroyed")

        playbackManager.pause(transientLoss = false, sourceView = SourceView.AUTO_PAUSE)
    }

    override fun onLoadChildren(parentId: String, result: Result<List<MediaBrowserCompat.MediaItem>>) {
        result.detach()
        Log.d(Settings.LOG_TAG_AUTO, "onLoadChildren. Loading section $parentId")
        launch(Dispatchers.IO) {
            Log.d(Settings.LOG_TAG_AUTO, "onLoadChildren. Running in background $parentId")
            try {
                val items: List<MediaBrowserCompat.MediaItem> = when (parentId) {
                    MEDIA_ID_ROOT -> loadRootChildren()
                    PODCASTS_ROOT -> loadPodcastsChildren()
                    FILTERS_ROOT -> loadFiltersRoot()
                    DISCOVER_ROOT -> loadDiscoverRoot()
                    PROFILE_ROOT -> loadProfileRoot()
                    PROFILE_FILES -> loadFilesChildren()
                    PROFILE_LISTENING_HISTORY -> loadListeningHistoryChildren()
                    PROFILE_STARRED -> loadStarredChildren()
                    RECENT_ROOT -> loadRecentChildren()
                    SUGGESTED_ROOT -> loadSuggestedChildren()
                    else -> {
                        if (parentId.startsWith(FOLDER_ROOT_PREFIX)) {
                            loadFolderPodcastsChildren(folderUuid = parentId.substring(FOLDER_ROOT_PREFIX.length))
                        } else {
                            loadEpisodeChildren(parentId)
                        }
                    }
                }
                Log.d(Settings.LOG_TAG_AUTO, "onLoadChildren. Sending results $parentId")
                result.sendResult(items)
                Log.d(Settings.LOG_TAG_AUTO, "onLoadChildren. Results sent $parentId")
            } catch (e: Exception) {
                Log.e(Settings.LOG_TAG_AUTO, "onLoadChildren. Could not load $parentId", e)
                result.sendResult(emptyList())
            }
            podcastManager.refreshPodcastsIfRequired("Automotive")
        }
    }

    override suspend fun loadRootChildren(): List<MediaBrowserCompat.MediaItem> {
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

    suspend fun loadFiltersRoot(): List<MediaBrowserCompat.MediaItem> {
        return playlistManager.findAllSuspend().mapNotNull {
            Log.d(Settings.LOG_TAG_AUTO, "Filters ${it.title}")

            try {
                AutoConverter.convertPlaylistToMediaItem(this, it)
            } catch (e: Exception) {
                Log.e(Settings.LOG_TAG_AUTO, "Filter ${it.title} load failed", e)
                null
            }
        }
    }

    private fun loadProfileRoot(): List<MediaBrowserCompat.MediaItem> {
        return buildList {
            // Add the user uploaded Files if they are a paying subscriber
            val isPaidUser = subscriptionManager.getCachedStatus() is SubscriptionStatus.Paid
            if (isPaidUser) {
                add(buildListMediaItem(id = PROFILE_FILES, title = LR.string.profile_navigation_files, drawable = IR.drawable.automotive_files))
            }
            add(buildListMediaItem(id = PROFILE_STARRED, title = LR.string.profile_navigation_starred, drawable = IR.drawable.automotive_filter_star))
            add(buildListMediaItem(id = PROFILE_LISTENING_HISTORY, title = LR.string.profile_navigation_listening_history, drawable = IR.drawable.automotive_listening_history))
        }
    }

    private fun buildListMediaItem(id: String, @StringRes title: Int, @DrawableRes drawable: Int, extras: Bundle? = null): MediaBrowserCompat.MediaItem {
        val description = MediaDescriptionCompat.Builder()
            .setTitle(getString(title))
            .setMediaId(id)
            .setExtras(extras)
            .setIconUri(AutoConverter.getBitmapUri(drawable = drawable, this))
            .build()
        return MediaBrowserCompat.MediaItem(description, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    suspend fun loadDiscoverRoot(): List<MediaBrowserCompat.MediaItem> {
        Log.d(Settings.LOG_TAG_AUTO, "Loading discover root")
        val discoverFeed: Discover
        try {
            discoverFeed = listSource.getDiscoverFeedSuspend()
        } catch (e: Exception) {
            Log.e(Settings.LOG_TAG_AUTO, "Error loading discover", e)
            return emptyList()
        }

        val region = discoverFeed.regions[discoverFeed.defaultRegionCode] ?: return emptyList()
        val replacements = mapOf(
            discoverFeed.regionCodeToken to region.code,
            discoverFeed.regionNameToken to region.name,
        )

        val updatedList = discoverFeed.layout.transformWithRegion(region, replacements, resources)
            .filter { it.type is ListType.PodcastList && it.displayStyle !is DisplayStyle.CollectionList && !it.sponsored && it.displayStyle !is DisplayStyle.SinglePodcast }
            .map { discoverItem ->
                Log.d(Settings.LOG_TAG_AUTO, "Loading discover feed ${discoverItem.source}")
                val listFeed = listSource.getListFeedSuspend(discoverItem.source)
                Pair(discoverItem.title, listFeed.podcasts?.take(6) ?: emptyList())
            }
            .flatMap { (title, podcasts) ->
                Log.d(Settings.LOG_TAG_AUTO, "Mapping $title to media item")
                val groupTitle = title.tryToLocalise(resources)
                podcasts.map {
                    val extras = Bundle()
                    extras.putString(EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT, groupTitle)

                    val artworkUri = PodcastImage.getArtworkUrl(size = 480, uuid = it.uuid)
                    val localUri = AutoConverter.getArtworkUriForContentProvider(Uri.parse(artworkUri), this)

                    val discoverDescription = MediaDescriptionCompat.Builder()
                        .setTitle(it.title)
                        .setMediaId(it.uuid)
                        .setIconUri(localUri)
                        .setExtras(extras)
                        .build()

                    return@map MediaBrowserCompat.MediaItem(discoverDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
                }
            }

        return updatedList
    }
}

private const val ERROR_RESOLUTION_ACTION_LABEL =
    "android.media.extras.ERROR_RESOLUTION_ACTION_LABEL"
private const val ERROR_RESOLUTION_ACTION_INTENT =
    "android.media.extras.ERROR_RESOLUTION_ACTION_INTENT"
