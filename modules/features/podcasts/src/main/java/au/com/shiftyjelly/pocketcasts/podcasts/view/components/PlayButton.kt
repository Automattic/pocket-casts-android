package au.com.shiftyjelly.pocketcasts.podcasts.view.components

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.annotation.ColorInt
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.views.component.ProgressCircleView
import au.com.shiftyjelly.pocketcasts.views.extensions.inflate
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class PlayButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    var listener: OnClickListener? = null

    private var buttonType: PlayButtonType = PlayButtonType.PLAY
    private var episodeUuid: String? = null
    private var podcastUuid: String? = null
    private var fromListUuid: String? = null
    private var episodeStatus: EpisodeStatusEnum = EpisodeStatusEnum.NOT_DOWNLOADED
    private val progressCircle: ProgressCircleView
    private val buttonImage: ImageView

    init {
        this.inflate(R.layout.play_button, attachToThis = true)
        progressCircle = findViewById(R.id.progressCircle)
        buttonImage = findViewById(R.id.buttonImage)
        setOnClickListener { onClick() }
        setOnLongClickListener {
            onLongClick()
            true
        }
    }

    companion object {
        private const val LIST_ID_KEY = "list_id"
        private const val PODCAST_UUID_KEY = "podcast_uuid"
        fun calculateButtonType(episode: BaseEpisode, streamByDefault: Boolean): PlayButtonType {
            return when {
                episode.lastPlaybackFailed() -> PlayButtonType.PLAYBACK_FAILED
                episode.playing -> PlayButtonType.PAUSE
                episode.isFinished -> PlayButtonType.PLAYED
                episode.isDownloaded -> PlayButtonType.PLAY
                episode.isDownloading || episode.isQueued -> if (streamByDefault) PlayButtonType.PLAY else PlayButtonType.STOP_DOWNLOAD
                !streamByDefault -> PlayButtonType.DOWNLOAD
                else -> PlayButtonType.PLAY
            }
        }
    }

    interface OnClickListener {
        var source: SourceView
        fun onPlayClicked(episodeUuid: String)
        fun onPauseClicked()
        fun onPlayNext(episodeUuid: String)
        fun onPlayLast(episodeUuid: String)
        fun onDownload(episodeUuid: String)
        fun onStopDownloading(episodeUuid: String)
        fun onPlayedClicked(episodeUuid: String)
    }

    private fun onClick() {
        val episodeUuid = episodeUuid ?: return
        when (buttonType) {
            PlayButtonType.PLAY -> {
                val currentFromListUuid = fromListUuid
                val currentPodcastUuid = podcastUuid
                if (currentFromListUuid != null && currentPodcastUuid != null) {
                    FirebaseAnalyticsTracker.podcastEpisodePlayedFromList(currentFromListUuid, currentPodcastUuid)
                    analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_EPISODE_PLAY, mapOf(LIST_ID_KEY to currentFromListUuid, PODCAST_UUID_KEY to currentPodcastUuid))
                }
                listener?. onPlayClicked(episodeUuid)
                UiUtil.hideKeyboard(this)
            }
            PlayButtonType.PAUSE -> listener?.onPauseClicked()
            PlayButtonType.DOWNLOAD -> listener?.onDownload(episodeUuid)
            PlayButtonType.STOP_DOWNLOAD -> listener?.onStopDownloading(episodeUuid)
            PlayButtonType.PLAYBACK_FAILED -> listener?.onPlayClicked(episodeUuid)
            PlayButtonType.PLAYED -> listener?.onPlayedClicked(episodeUuid)
        }
    }

    private fun onLongClick() {
        FirebaseAnalyticsTracker.longPressedEpisodeButton()
        val popup = PopupMenu(context, this)
        this.setOnTouchListener(popup.dragToOpenListener)
        popup.inflate(R.menu.play_button)
        val menu = popup.menu
        menu.findItem(R.id.download).isVisible = (episodeStatus == EpisodeStatusEnum.NOT_DOWNLOADED && buttonType != PlayButtonType.DOWNLOAD)
        menu.findItem(R.id.stream).isVisible = (episodeStatus == EpisodeStatusEnum.NOT_DOWNLOADED && buttonType == PlayButtonType.DOWNLOAD)
        menu.findItem(R.id.stop_downloading).isVisible = episodeStatus == EpisodeStatusEnum.DOWNLOADING || episodeStatus == EpisodeStatusEnum.QUEUED || episodeStatus == EpisodeStatusEnum.WAITING_FOR_POWER || episodeStatus == EpisodeStatusEnum.WAITING_FOR_WIFI
        popup.setOnMenuItemClickListener { item ->
            val episodeUuid = episodeUuid
            if (episodeUuid != null) {
                when (item.itemId) {
                    R.id.play_next -> listener?.onPlayNext(episodeUuid)
                    R.id.play_last -> listener?.onPlayLast(episodeUuid)
                    R.id.stream -> listener?.onPlayClicked(episodeUuid)
                    R.id.download -> listener?.onDownload(episodeUuid)
                    R.id.stop_downloading -> listener?.onStopDownloading(episodeUuid)
                }
            }
            true
        }

        popup.show()
    }

    fun setButtonType(episode: BaseEpisode, buttonType: PlayButtonType, @ColorInt color: Int, fromListUuid: String?) {
        if (buttonType == this.buttonType && episode.uuid == this.episodeUuid) {
            return
        }

        this.buttonType = buttonType
        this.episodeUuid = episode.uuid
        if (episode is PodcastEpisode) {
            this.podcastUuid = episode.podcastUuid
            this.fromListUuid = fromListUuid
        }
        this.episodeStatus = episode.episodeStatus
        val buttonColor = when (buttonType) {
            PlayButtonType.PLAYED -> context.getThemeColor(UR.attr.primary_icon_02)
            else -> color
        }
        setIconDrawable(buttonType.drawableId, buttonColor)
        progressCircle.setColor(buttonColor)
        progressCircle.setEpisode(episode, buttonType == PlayButtonType.PLAYED)
        contentDescription = buttonType.label
    }

    private fun setIconDrawable(drawableId: Int, @ColorInt tintColor: Int) {
        buttonImage.setImageResource(drawableId)
        buttonImage.imageTintList = ColorStateList.valueOf(tintColor)
    }
}
