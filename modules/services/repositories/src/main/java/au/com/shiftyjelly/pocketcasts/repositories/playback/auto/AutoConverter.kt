package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaDescriptionCompat.EXTRA_DOWNLOAD_STATUS
import android.support.v4.media.MediaDescriptionCompat.STATUS_DOWNLOADED
import android.support.v4.media.MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_PERCENTAGE
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocaliseFilters
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.extensions.autoDrawableId
import au.com.shiftyjelly.pocketcasts.repositories.extensions.automotiveDrawableId
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getArtworkUrl
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT
import au.com.shiftyjelly.pocketcasts.repositories.playback.EpisodeFileMetadata
import au.com.shiftyjelly.pocketcasts.repositories.playback.FOLDER_ROOT_PREFIX
import au.com.shiftyjelly.pocketcasts.utils.Util
import coil.executeBlocking
import coil.imageLoader
import java.io.File
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

data class AutoMediaId(
    val episodeId: String,
    val sourceId: String?,
) {
    companion object {
        private const val DIVIDER = "#"
        fun fromMediaId(mediaId: String): AutoMediaId {
            val components = mediaId.split(DIVIDER)
            return if (components.size == 2) {
                AutoMediaId(components[1], components[0])
            } else {
                AutoMediaId(mediaId, null)
            }
        }
    }

    fun toMediaId(): String {
        return "$sourceId$DIVIDER$episodeId"
    }
}

object AutoConverter {
    fun convertEpisodeToMediaItem(context: Context, episode: BaseEpisode, parentPodcast: Podcast, useEpisodeArtwork: Boolean, groupTrailers: Boolean = false, sourceId: String = parentPodcast.uuid): MediaBrowserCompat.MediaItem {
        val localUri = getPodcastArtworkUri(parentPodcast, episode, context, useEpisodeArtwork)

        val extrasForEpisode = extrasForEpisode(episode)
        if (groupTrailers) {
            val groupTitle = if (episode is PodcastEpisode && episode.episodeType is PodcastEpisode.EpisodeType.Trailer) LR.string.episode_trailer else LR.string.episodes
            extrasForEpisode.putString(EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT, context.resources.getString(groupTitle))
        }
        val mediaId = AutoMediaId(episode.uuid, sourceId).toMediaId()
        val episodeDesc = MediaDescriptionCompat.Builder()
            .setDescription(episode.episodeDescription)
            .setTitle(episode.title)
            .setSubtitle(episode.getSummaryText(dateFormatter = RelativeDateFormatter(context), tintColor = Color.WHITE, showDuration = true, context = context).toString())
            .setMediaId(mediaId)
            .setExtras(extrasForEpisode)
            .setIconUri(localUri)
            .build()

        return MediaBrowserCompat.MediaItem(episodeDesc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    fun convertPodcastToMediaItem(podcast: Podcast, context: Context, useEpisodeArtwork: Boolean): MediaBrowserCompat.MediaItem? {
        return try {
            val localUri = getPodcastArtworkUri(podcast = podcast, episode = null, context = context, useEpisodeArtwork = useEpisodeArtwork)

            val podcastDesc = MediaDescriptionCompat.Builder()
                .setTitle(podcast.title)
                .setMediaId(podcast.uuid)
                .setIconUri(localUri)
                .build()

            MediaBrowserCompat.MediaItem(podcastDesc, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
        } catch (e: Exception) {
            null
        }
    }

    fun convertFolderToMediaItem(context: Context, folder: Folder): MediaBrowserCompat.MediaItem? {
        return try {
            val localUri = getBitmapUriForFolder(context, folder)

            val podcastDesc = MediaDescriptionCompat.Builder()
                .setTitle(folder.name)
                .setMediaId(FOLDER_ROOT_PREFIX + folder.uuid)
                .setIconUri(localUri)
                .build()

            MediaBrowserCompat.MediaItem(podcastDesc, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
        } catch (e: Exception) {
            null
        }
    }

    fun convertPlaylistToMediaItem(context: Context, smartPlaylist: SmartPlaylist): MediaBrowserCompat.MediaItem {
        val mediaDescription = MediaDescriptionCompat.Builder()
            .setTitle(smartPlaylist.title.tryToLocaliseFilters(context.resources))
            .setMediaId(smartPlaylist.uuid)
            .setIconUri(getPlaylistBitmapUri(smartPlaylist, context))
            .build()

        return MediaBrowserCompat.MediaItem(mediaDescription, MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    fun getPodcastArtworkUri(podcast: Podcast?, episode: BaseEpisode?, context: Context, useEpisodeArtwork: Boolean): Uri? {
        val artworkUri = when (episode) {
            is PodcastEpisode -> if (useEpisodeArtwork) {
                episode.imageUrl ?: EpisodeFileMetadata.artworkCacheFile(context, episode.uuid).takeIf(File::exists)?.path ?: podcast?.getArtworkUrl(480)
            } else {
                podcast?.getArtworkUrl(480)
            }

            is UserEpisode -> if (useEpisodeArtwork) {
                EpisodeFileMetadata.artworkCacheFile(context, episode.uuid).takeIf(File::exists)?.path ?: episode.artworkUrl
            } else {
                episode.artworkUrl
            }

            null -> {
                podcast?.getArtworkUrl(480)
            }
        }?.toUri()

        if (artworkUri == null) {
            return null
        }

        return getArtworkUriForContentProvider(artworkUri, context)
    }

    fun getPodcastArtworkBitmap(episode: BaseEpisode, context: Context, useEpisodeArtwork: Boolean): Bitmap? {
        val imageRequestFactory = PocketCastsImageRequestFactory(
            context,
            isDarkTheme = true,
            size = 480,
            placeholderType = PocketCastsImageRequestFactory.PlaceholderType.Small,
        )

        val request = imageRequestFactory.create(episode, useEpisodeArtwork)
        return context.imageLoader.executeBlocking(request).drawable?.toBitmap() ?: loadPlaceholderBitmap(imageRequestFactory, context)
    }

    private fun loadPlaceholderBitmap(imageRequestFactory: PocketCastsImageRequestFactory, context: Context): Bitmap? {
        val request = imageRequestFactory.createForPodcast(podcastUuid = null)
        return context.imageLoader.executeBlocking(request).drawable?.toBitmap()
    }

    private fun getBitmapUriForFolder(context: Context, folder: Folder?): Uri? {
        if (folder == null) return null

        return getBitmapUri(drawable = folder.automotiveDrawableId, context = context)
    }

    /**
     * This creates a Uri that will call the AlbumArtContentProvider to download and cache the artwork
     */
    fun getArtworkUriForContentProvider(podcastArtUri: Uri, context: Context): Uri {
        return podcastArtUri.asAlbumArtContentUri(context)
    }

    fun getPodcastsBitmapUri(context: Context): Uri {
        return getBitmapUri(drawable = IR.drawable.auto_tab_podcasts, context = context)
    }

    fun getPlaylistBitmapUri(smartPlaylist: SmartPlaylist?, context: Context): Uri {
        val drawableId = if (Util.isAutomotive(context)) {
            // the Automotive UI displays the icon in a list that requires more padding around the icon
            smartPlaylist?.automotiveDrawableId ?: IR.drawable.automotive_filter_play
        } else {
            smartPlaylist?.autoDrawableId ?: IR.drawable.auto_filter_play
        }
        return getBitmapUri(drawableId, context)
    }

    fun getDownloadsBitmapUri(context: Context): Uri {
        return getBitmapUri(drawable = IR.drawable.auto_filter_downloaded, context = context)
    }

    fun getFilesBitmapUri(context: Context): Uri {
        return getBitmapUri(drawable = IR.drawable.auto_files, context = context)
    }

    /**
     * Convert a drawable into a Uri
     * Use the drawable id so Proguard doesn't remove the asset in the production build.
     */
    fun getBitmapUri(@DrawableRes drawable: Int, context: Context): Uri {
        val resources = context.resources
        // This is an example of the URI android.resource://au.com.shiftyjelly.pocketcasts/drawable/auto_folder_01
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE) // android.resource
            .authority(resources.getResourcePackageName(drawable)) // au.com.shiftyjelly.pocketcasts
            .appendPath(resources.getResourceTypeName(drawable)) // drawable
            .appendPath(resources.getResourceEntryName(drawable)) // auto_folder_01
            .build()
    }

    private fun extrasForEpisode(episode: BaseEpisode): Bundle {
        val downloadStatus = if (episode.isDownloaded) STATUS_DOWNLOADED else STATUS_NOT_DOWNLOADED

        val completionStatus: Int
        val completionPercentage: Double
        when (episode.playingStatus) {
            EpisodePlayingStatus.NOT_PLAYED -> {
                completionStatus = DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED
                completionPercentage = 0.0
            }

            EpisodePlayingStatus.IN_PROGRESS -> {
                completionStatus = DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED
                completionPercentage = if (episode.duration == 0.0) 0.0 else (episode.playedUpTo / episode.duration).coerceIn(0.0, 1.0)
            }

            EpisodePlayingStatus.COMPLETED -> {
                completionStatus = DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED
                completionPercentage = 1.0
            }
        }

        return bundleOf(
            EXTRA_DOWNLOAD_STATUS to downloadStatus,
            DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS to completionStatus,
            DESCRIPTION_EXTRAS_KEY_COMPLETION_PERCENTAGE to completionPercentage,
        )
    }
}
