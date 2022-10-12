package au.com.shiftyjelly.pocketcasts

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.playback.CONTENT_STYLE_BROWSABLE_HINT
import au.com.shiftyjelly.pocketcasts.repositories.playback.CONTENT_STYLE_LIST_ITEM_HINT_VALUE
import au.com.shiftyjelly.pocketcasts.repositories.playback.EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT
import au.com.shiftyjelly.pocketcasts.repositories.playback.FOLDER_ROOT_PREFIX
import au.com.shiftyjelly.pocketcasts.repositories.playback.MEDIA_ID_ROOT
import au.com.shiftyjelly.pocketcasts.repositories.playback.PODCASTS_ROOT
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager.PlaybackSource
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackService
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoConverter
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

const val FILTERS_ROOT = "__FILTERS__"
const val DISCOVER_ROOT = "__DISCOVER__"

@SuppressLint("LogNotTimber")
@AndroidEntryPoint
class AutoPlaybackService : PlaybackService() {
    @Inject
    lateinit var listSource: ListRepository

    private fun requireLogin() {
        val loginIntent = Intent(this, AccountActivity::class.java)
        val loginActivityPendingIntent = PendingIntent.getActivity(this, 0, loginIntent, PendingIntent.FLAG_IMMUTABLE)
        val extras = Bundle().apply {
            putString(ERROR_RESOLUTION_ACTION_LABEL, "Login now")
            putParcelable(ERROR_RESOLUTION_ACTION_INTENT, loginActivityPendingIntent)
        }

        val playbackState = PlaybackStateCompat.Builder()
            .setState(PlaybackStateCompat.STATE_ERROR, 0, 0f)
            .setErrorMessage(PlaybackStateCompat.ERROR_CODE_AUTHENTICATION_EXPIRED, "Please log in to your Pocket Casts account")
            .setExtras(extras)
            .build()
        playbackManager.mediaSession.setPlaybackState(playbackState)
        playbackManager.mediaSession.setMetadata(MediaMetadataCompat.Builder().build()) // Set no metadata
    }

    override fun onCreate() {
        super.onCreate()
        RefreshPodcastsTask.runNow(this)

        Log.d(Settings.LOG_TAG_AUTO, "Auto playback service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(Settings.LOG_TAG_AUTO, "Auto playback service destroyed")

        playbackManager.pause(transientLoss = false, playbackSource = PlaybackSource.AUTO_PAUSE)
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
        // podcasts
        val podcastsDescription = MediaDescriptionCompat.Builder()
            .setTitle(getString(LR.string.podcasts))
            .setMediaId(PODCASTS_ROOT)
            .setIconUri(AutoConverter.getBitmapUri(IR.drawable.auto_tab_podcasts, this))
            .build()
        val podcastItem = MediaBrowserCompat.MediaItem(podcastsDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)

        // filters
        val extras = Bundle().apply {
            putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST_ITEM_HINT_VALUE)
        }
        val filtersDescription = MediaDescriptionCompat.Builder()
            .setTitle(getString(LR.string.episode_filters))
            .setMediaId(FILTERS_ROOT)
            .setIconUri(AutoConverter.getBitmapUri(IR.drawable.auto_tab_filter, this))
            .setExtras(extras)
            .build()
        val filtersItem = MediaBrowserCompat.MediaItem(filtersDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)

        // discover
        val discoverDescription = MediaDescriptionCompat.Builder()
            .setTitle(getString(LR.string.discover))
            .setMediaId(DISCOVER_ROOT)
            .setIconUri(AutoConverter.getBitmapUri(IR.drawable.auto_tab_discover, this))
            .build()
        val discoverItem = MediaBrowserCompat.MediaItem(discoverDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)

        // show the user's podcast collection first if they are subscribed any
        return if (podcastManager.countSubscribed() > 0) {
            listOf(podcastItem, filtersItem, discoverItem)
        } else {
            listOf(discoverItem, podcastItem, filtersItem)
        }
    }

    fun loadFiltersRoot(): List<MediaBrowserCompat.MediaItem> {
        return playlistManager.findAll().mapNotNull {
            Log.d(Settings.LOG_TAG_AUTO, "Filters ${it.title}")

            try {
                AutoConverter.convertPlaylistToMediaItem(this, it)
            } catch (e: Exception) {
                Log.e(Settings.LOG_TAG_AUTO, "Filter ${it.title} load failed", e)
                null
            }
        }
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
            discoverFeed.regionNameToken to region.name
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
