package au.com.shiftyjelly.pocketcasts.sharing

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.EXTRA_TITLE
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.drawable.toBitmap
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.sharing.BuildConfig.SERVER_SHORT_URL
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import coil.executeBlocking
import coil.imageLoader
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class ShareActions(
    private val context: Context,
    private val tracker: AnalyticsTracker,
    private val source: SourceView,
    private val displayPodcastCover: Boolean,
    private val hostUrl: String,
    private val shareStarter: ShareStarter,
) {
    @AssistedInject constructor(
        @Assisted sourceView: SourceView,
        @ApplicationContext context: Context,
        analyticsTracker: AnalyticsTracker,
    ) : this(
        context = context,
        tracker = analyticsTracker,
        source = sourceView,
        displayPodcastCover = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
        hostUrl = SERVER_SHORT_URL,
        shareStarter = ShareStarter { ctx, intent -> ctx.startActivity(intent) },
    )

    private val imageRequestFactory = PocketCastsImageRequestFactory(context, isDarkTheme = false).smallSize()

    suspend fun sharePodcast(podcast: Podcast) {
        trackSharing(Type.Podcast)
        shareUrl(
            url = "$hostUrl/podcast/${podcast.uuid}",
            title = podcast.title,
            podcast = podcast,
        )
    }

    suspend fun shareEpisode(podcast: Podcast, episode: PodcastEpisode) {
        trackSharing(Type.Episode)
        shareUrl(
            url = episodeUrl(episode, start = null, end = null),
            title = episode.title,
            podcast = podcast,
        )
    }

    suspend fun shareEpisodePosition(podcast: Podcast, episode: PodcastEpisode, start: Duration) {
        trackSharing(Type.EpisodeTimestamp)
        shareUrl(
            url = episodeUrl(episode, start, end = null),
            title = episode.title,
            podcast = podcast,
        )
    }

    suspend fun shareBookmark(podcast: Podcast, episode: PodcastEpisode, start: Duration) {
        trackSharing(Type.BookmarkTimestamp)
        shareUrl(
            url = episodeUrl(episode, start, end = null),
            title = episode.title,
            podcast = podcast,
        )
    }

    suspend fun shareClipLink(podcast: Podcast, episode: PodcastEpisode, start: Duration, end: Duration) {
        trackSharing(Type.ClipLink)
        shareUrl(
            url = episodeUrl(episode, start, end),
            title = episode.title,
            podcast = podcast,
        )
    }

    suspend fun shareEpisodeFile(episode: PodcastEpisode) {
        trackSharing(Type.EpisodeFile)
        runCatching {
            episode.downloadedFilePath?.let(::File)?.let { file ->
                val intent = Intent(ACTION_SEND)
                    .setType(episode.fileType)
                    .setExtraStream(file)
                shareStarter.start(context, intent.toChooserIntent())
            }
        }
    }

    private fun episodeUrl(episode: PodcastEpisode, start: Duration?, end: Duration?): String {
        val timeMarker = listOfNotNull(start, end).takeIf { it.isNotEmpty() }?.joinToString(prefix = "?t=", separator = ",") { it.inWholeSeconds.toString() }.orEmpty()
        return "$hostUrl/episode/${episode.uuid}$timeMarker"
    }

    private suspend fun shareUrl(
        url: String,
        title: String,
        podcast: Podcast,
    ) = runCatching {
        val intent = createUrlShareIntent(url, title, podcast)
        shareStarter.start(context, intent.toChooserIntent())
    }

    private fun Intent.toChooserIntent() = Intent
        .createChooser(this, context.getString(LR.string.podcasts_share_via))
        .addFlags(FLAG_ACTIVITY_NEW_TASK)

    private suspend fun createUrlShareIntent(
        url: String,
        title: String,
        podcast: Podcast,
    ) = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(EXTRA_TEXT, url)
        .putExtra(EXTRA_TITLE, title)
        .addFlags(FLAG_GRANT_READ_URI_PERMISSION)
        .setPodcastCover(podcast)

    private suspend fun Intent.setPodcastCover(podcast: Podcast): Intent {
        if (displayPodcastCover) {
            val coverUri = getPodcastCoverUri(podcast)
            if (coverUri != null) {
                clipData = ClipData.newRawUri(null, coverUri)
            }
        }
        return this
    }

    private suspend fun Intent.setExtraStream(file: File) = putExtra(EXTRA_STREAM, FileUtil.createUriWithReadPermissions(context, file, this))

    private suspend fun getPodcastCoverUri(podcast: Podcast) = runCatching {
        withContext(Dispatchers.IO) {
            val request = imageRequestFactory.create(podcast)
            context.imageLoader.executeBlocking(request).drawable?.toBitmap()?.let { bitmap ->
                val imageFile = File(context.cacheDir, "share_podcast_thumbnail.jpg")
                FileOutputStream(imageFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }
                FileUtil.getUriForFile(context, imageFile)
            }
        }
    }.getOrNull()

    private fun trackSharing(type: Type) {
        tracker.track(
            AnalyticsEvent.PODCAST_SHARED,
            mapOf(
                "source" to source.analyticsValue,
                "type" to type.analyticsValue,
            ),
        )
        if (source == SourceView.PODCAST_SCREEN && type == Type.Podcast) {
            tracker.track(AnalyticsEvent.PODCAST_SCREEN_SHARE_TAPPED)
        }
    }

    private enum class Type(
        val analyticsValue: String,
    ) {
        Podcast("podcast"),
        Episode("episode"),
        EpisodeTimestamp("current_time"),
        EpisodeFile("episode_file"),
        BookmarkTimestamp("bookmark_time"),
        ClipLink("clip_link"),
    }

    @AssistedFactory
    interface Factory {
        fun create(source: SourceView): ShareActions
    }
}
