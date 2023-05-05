package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import okhttp3.Call
import timber.log.Timber
import java.io.File
import kotlin.math.round
import au.com.shiftyjelly.pocketcasts.localization.R as LR

data class SharePodcastHelper(
    val podcast: Podcast, // Share just a podcast.
    val episode: PodcastEpisode? = null, // Share an episode of a podcast.
    val upToInSeconds: Double? = null, // Share a position in an episode of a podcast.
    val context: Context,
    private val shareType: ShareType,
    private val source: AnalyticsSource,
    private val analyticsTracker: AnalyticsTrackerWrapper,
) {

    private var shareEpisodeTask: Call? = null

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
        sendText(url)

        if (shouldTrackShareEvent()) {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SHARED, AnalyticsProp.shareMap(shareType, source))
        }
    }

    private fun sendText(shareLink: String?) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, shareLink)
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

    private fun shouldTrackShareEvent() =
        source != AnalyticsSource.PODCAST_SCREEN // Podcast screen has it's own share event

    private object AnalyticsProp {
        const val SOURCE = "source"
        const val TYPE = "type"
        fun shareMap(type: ShareType, source: AnalyticsSource) =
            mapOf(TYPE to type.value, SOURCE to source.analyticsValue)
    }

    enum class ShareType(val value: String) {
        PODCAST("podcast"),
        EPISODE("episode"),
        EPISODE_FILE("episode_file"),
        CURRENT_TIME("current_time")
    }
}
