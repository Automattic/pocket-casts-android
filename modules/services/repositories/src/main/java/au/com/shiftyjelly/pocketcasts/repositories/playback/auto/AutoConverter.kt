package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat.EXTRA_DOWNLOAD_STATUS
import android.support.v4.media.MediaDescriptionCompat.STATUS_DOWNLOADED
import android.support.v4.media.MediaDescriptionCompat.STATUS_NOT_DOWNLOADED
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.os.bundleOf
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED
import androidx.media.utils.MediaConstants.DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocaliseFilters
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.extensions.autoDrawableId
import au.com.shiftyjelly.pocketcasts.repositories.extensions.automotiveDrawableId
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getArtworkUrl
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.playback.EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT
import au.com.shiftyjelly.pocketcasts.repositories.playback.FOLDER_ROOT_PREFIX
import au.com.shiftyjelly.pocketcasts.utils.Util
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import java.io.File
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

data class AutoMediaId(
    val playableId: String,
    val sourceId: String?
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
        return "$sourceId$DIVIDER$playableId"
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
object AutoConverter {

    private const val THUMBNAIL_IMAGE_SIZE = 200
    private const val FULL_IMAGE_SIZE = 800

    fun convertEpisodeToMediaItem(context: Context, episode: Playable, parentPodcast: Podcast, groupTrailers: Boolean = false, sourceId: String = parentPodcast.uuid): MediaItem {
        val localUri = getBitmapUriForPodcast(parentPodcast, episode, context)

        val extrasForEpisode = extrasForEpisode(episode)
        if (groupTrailers) {
            val groupTitle = if (episode is Episode && episode.episodeType is Episode.EpisodeType.Trailer) LR.string.episode_trailer else LR.string.episodes
            extrasForEpisode.putString(EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT, context.resources.getString(groupTitle))
        }
        val mediaId = AutoMediaId(episode.uuid, sourceId).toMediaId()
        val episodeMetadata = MediaMetadata.Builder()
            .setDescription(episode.episodeDescription)
            .setTitle(episode.title)
            .setSubtitle(episode.getSummaryText(dateFormatter = RelativeDateFormatter(context), tintColor = Color.WHITE, showDuration = true, context = context).toString())
            .setExtras(extrasForEpisode)
            .setArtworkUri(localUri)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .build()

        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(episodeMetadata)
            .setUri(episode.downloadUrl)
            .setMimeType(episode.fileType)
            .build()
    }

    fun convertPodcastToMediaItem(podcast: Podcast, context: Context): MediaItem? {
        return try {
            val localUri = getBitmapUriForPodcast(podcast = podcast, episode = null, context = context)

            val podcastMetadata = MediaMetadata.Builder()
                .setTitle(podcast.title)
                .setArtworkUri(localUri)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()

            MediaItem.Builder()
                .setMediaId(podcast.uuid)
                .setMediaMetadata(podcastMetadata)
                .build()
        } catch (e: Exception) {
            null
        }
    }

    fun convertFolderToMediaItem(context: Context, folder: Folder): MediaItem? {
        return try {
            val localUri = getBitmapUriForFolder(context, folder)

            val folderMetadata = MediaMetadata.Builder()
                .setTitle(folder.name)
                .setArtworkUri(localUri)
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .build()

            MediaItem.Builder()
                .setMediaId(FOLDER_ROOT_PREFIX + folder.uuid)
                .setMediaMetadata(folderMetadata)
                .build()
        } catch (e: Exception) {
            null
        }
    }

    fun convertPlaylistToMediaItem(context: Context, playlist: Playlist): MediaItem {
        val playlistMetadata = MediaMetadata.Builder()
            .setTitle(playlist.title.tryToLocaliseFilters(context.resources))
            .setArtworkUri(getPlaylistBitmapUri(playlist, context))
            .setIsBrowsable(true)
            .setIsPlayable(false)
            .build()

        return MediaItem.Builder()
            .setMediaId(playlist.uuid)
            .setMediaMetadata(playlistMetadata)
            .build()
    }

    fun getBitmapUriForPodcast(podcast: Podcast?, episode: Playable?, context: Context): Uri? {
        val url = if (episode is UserEpisode) {
            // the artwork for user uploaded episodes are stored on each episode
            episode.artworkUrl
        } else {
            podcast?.getArtworkUrl(480)
        }

        if (url.isNullOrBlank()) {
            return null
        }

        val podcastArtUri = Uri.parse(url)
        return getArtworkUriForContentProvider(podcastArtUri, context)
    }

    private fun getBitmapUriForFolder(context: Context, folder: Folder?): Uri? {
        if (folder == null) return null

        return getBitmapUri(drawable = folder.automotiveDrawableId, context = context)
    }

    val autoImageLoaderListener = object : RequestListener<File> {
        override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<File>?, isFirstResource: Boolean): Boolean {
            Log.e("AutoConverter", "Could not load image in automotive $e")
            return false
        }

        override fun onResourceReady(resource: File?, model: Any?, target: Target<File>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
            return true
        }
    }

    /**
     * This creates a Uri that will call the AlbumArtContentProvider to download and cache the artwork
     */
    fun getArtworkUriForContentProvider(podcastArtUri: Uri?, context: Context): Uri? {
        return podcastArtUri?.asAlbumArtContentUri(context)
    }

    fun getBitmapForPodcast(podcast: Podcast?, useThumbnail: Boolean, context: Context): Bitmap? {
        if (podcast == null) {
            return null
        }

        val size = if (useThumbnail) THUMBNAIL_IMAGE_SIZE else FULL_IMAGE_SIZE
        val imageLoader = PodcastImageLoader(context = context, isDarkTheme = true, transformations = emptyList()).smallPlaceholder()
        return imageLoader.getBitmap(podcast, size)
    }

    fun getPodcastsBitmapUri(context: Context): Uri {
        return getBitmapUri(drawable = IR.drawable.auto_tab_podcasts, context = context)
    }

    fun getPlaylistBitmapUri(playlist: Playlist?, context: Context): Uri {
        val drawableId = if (Util.isAutomotive(context)) {
            // the Automotive UI displays the icon in a list that requires more padding around the icon
            playlist?.automotiveDrawableId ?: IR.drawable.automotive_filter_play
        } else {
            playlist?.autoDrawableId ?: IR.drawable.auto_filter_play
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
        val drawableName = context.resources.getResourceEntryName(drawable)
        return Uri.parse("android.resource://" + context.packageName + "/drawable/" + drawableName)
    }

    private fun extrasForEpisode(episode: Playable): Bundle {
        val downloadStatus = if (episode.isDownloaded) STATUS_DOWNLOADED else STATUS_NOT_DOWNLOADED

        val completionStatus = when (episode.playingStatus) {
            EpisodePlayingStatus.NOT_PLAYED -> DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_NOT_PLAYED
            EpisodePlayingStatus.IN_PROGRESS -> DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_PARTIALLY_PLAYED
            EpisodePlayingStatus.COMPLETED -> DESCRIPTION_EXTRAS_VALUE_COMPLETION_STATUS_FULLY_PLAYED
        }

        return bundleOf(
            EXTRA_DOWNLOAD_STATUS to downloadStatus,
            DESCRIPTION_EXTRAS_KEY_COMPLETION_STATUS to completionStatus
        )
    }
}
