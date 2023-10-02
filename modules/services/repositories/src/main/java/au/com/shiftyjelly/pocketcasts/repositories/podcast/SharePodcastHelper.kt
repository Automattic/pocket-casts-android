package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.round
import au.com.shiftyjelly.pocketcasts.localization.R as LR

data class SharePodcastHelper(
    val podcast: Podcast, // Share just a podcast.
    val episode: PodcastEpisode? = null, // Share an episode of a podcast.
    val upToInSeconds: Double? = null, // Share a position in an episode of a podcast.
    val context: Context,
    private val shareType: ShareType,
    private val source: SourceView,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) {

    fun showShareDialogDirect() {
        val host = Settings.SERVER_SHORT_URL
        var url = ""
        episode?.let {
            var timeMarker = ""
            upToInSeconds?.let {
                timeMarker = "?t=${round(it).toInt()}"
            }
            url = "$host/episode/${it.uuid}$timeMarker"
        } ?: run {
            url = "$host/podcast/${podcast.uuid}"
        }
        
        // Android 10+ supports rich content previews: https://developer.android.com/training/sharing/send#adding-rich-content-previews
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            cachePodcastImage { sendText(url, it) }
        } else {
            sendText(url)
        }

        if (shouldTrackShareEvent()) {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SHARED, AnalyticsProp.shareMap(shareType, source))
        }
    }

    private fun sendText(shareLink: String?, thumbnailUri: Uri? = null) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareLink)
            putExtra(Intent.EXTRA_TITLE, episode?.cleanTitle ?: podcast.title)
            clipData = ClipData.newRawUri(null, thumbnailUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, context.getString(LR.string.podcasts_share_via)))
    }

    fun sendFile() {
        val episode = episode ?: return
        val path = episode.downloadedFilePath ?: return
        try {
            val file = File(path)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = episode.fileType
            val uri = FileUtil.createUriWithReadPermissions(file, intent, this.context)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            this.context.startActivity(Intent.createChooser(intent, context.getString(LR.string.podcasts_share_via)))
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun cachePodcastImage(onComplete: (Uri?) -> Unit) {
        val scope = context.getActivity()?.lifecycleScope ?: return onComplete(null)
        scope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                PodcastImageLoader(context, false, emptyList()).getBitmapSuspend(podcast, 128)
            } ?: return@launch onComplete(null)
            // overwrites with every share
            val imageFile = File(context.cacheDir, "share_podcast_thumbnail.jpg")
            try {
                val fileOutStream = FileOutputStream(imageFile)
                fileOutStream.use {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutStream)
                    val bitmapUri = FileUtil.getUriForFile(context, imageFile)
                    onComplete(bitmapUri)
                }
            } catch (e: IOException) {
                Timber.e(e)
                onComplete(null)
            }
        }
    }

    private fun shouldTrackShareEvent() =
        source != SourceView.PODCAST_SCREEN // Podcast screen has it's own share event

    private object AnalyticsProp {
        const val SOURCE = "source"
        const val TYPE = "type"
        fun shareMap(type: ShareType, source: SourceView) =
            mapOf(TYPE to type.value, SOURCE to source.analyticsValue)
    }

    enum class ShareType(val value: String) {
        PODCAST("podcast"),
        EPISODE("episode"),
        EPISODE_FILE("episode_file"),
        CURRENT_TIME("current_time")
    }
}
