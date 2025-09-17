package au.com.shiftyjelly.pocketcasts.views.component

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowData
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.R
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class FileStatusIconsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : LinearLayout(context, attrs, defStyleAttr) {

    private val disposables = CompositeDisposable()
    var statusText: String? = null
    private val progressCircle: ProgressCircleView
    private val progressBar: ProgressBar
    private val imgUpNext: ImageView
    private val imgIcon: ImageView
    private val imgBookmark: ImageView
    private val lblStatus: TextView
    private val imgCloud: GradientIcon

    init {
        LayoutInflater.from(context).inflate(R.layout.view_file_status_icons, this, true)
        progressCircle = findViewById(R.id.progressCircle)
        progressBar = findViewById(R.id.progressBar)
        imgUpNext = findViewById(R.id.imgUpNext)
        imgIcon = findViewById(R.id.imgIcon)
        imgBookmark = findViewById(R.id.imgBookmark)
        lblStatus = findViewById(R.id.lblStatus)
        imgCloud = findViewById(R.id.imgCloud)
        setPadding(0, 0, 0, 12.dpToPx(context))
    }

    fun setup(
        episode: UserEpisode,
        tintColor: Int,
        rowDataObservable: Observable<EpisodeRowData>,
        hideErrorDetails: Boolean = false,
    ) {
        val captionColor = context.getThemeColor(UR.attr.primary_text_02)
        val captionWithAlpha = ColorUtils.colorWithAlpha(captionColor, 128)
        val iconColor = context.getThemeColor(UR.attr.primary_icon_02)
        progressCircle.setColor(captionColor)
        progressBar.indeterminateTintList = ColorStateList.valueOf(captionColor)
        imgBookmark.imageTintList = ColorStateList.valueOf(tintColor)

        disposables.clear()
        rowDataObservable
            .doOnNext { data ->
                episode.playing = data.playbackState.isPlaying && data.playbackState.episodeUuid == episode.uuid
                imgUpNext.isVisible = data.isInUpNext
                imgBookmark.isVisible = data.hasBookmarks

                imgIcon.isVisible = false
                if (data.playbackState.episodeUuid == episode.uuid && data.playbackState.isBuffering) {
                    imgIcon.isVisible = false
                    progressBar.isVisible = true
                    progressCircle.isVisible = false
                    lblStatus.text = context.getString(LR.string.episode_row_buffering)
                } else if (episode.playErrorDetails != null) {
                    imgIcon.isVisible = true
                    progressBar.isVisible = false
                    progressCircle.isVisible = false
                    imgIcon.setImageResource(IR.drawable.ic_failed)
                    lblStatus.text = if (!hideErrorDetails) episode.playErrorDetails else ""
                    ImageViewCompat.setImageTintList(imgIcon, null)
                } else if (episode.episodeStatus == EpisodeStatusEnum.DOWNLOADED) {
                    imgIcon.isVisible = true
                    progressBar.isVisible = false
                    progressCircle.isVisible = false
                    imgIcon.setImageResource(IR.drawable.ic_downloaded)
                    updateTimeLeft(textView = lblStatus, episode = episode)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(context.getThemeColor(UR.attr.support_02)))
                } else if (episode.episodeStatus == EpisodeStatusEnum.DOWNLOADING) {
                    imgIcon.isVisible = false
                    progressBar.isVisible = false
                    progressCircle.isVisible = true
                    lblStatus.text = context.getString(LR.string.episode_row_downloading, data.downloadProgress)
                    progressCircle.setPercent(data.downloadProgress / 100f)
                } else if (episode.episodeStatus == EpisodeStatusEnum.DOWNLOAD_FAILED) {
                    imgIcon.isVisible = true
                    progressBar.isVisible = false
                    progressCircle.isVisible = false
                    imgIcon.setImageResource(IR.drawable.ic_download_failed_row)
                    lblStatus.text = context.getString(LR.string.episode_row_download_failed)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(iconColor))
                } else if (episode.episodeStatus == EpisodeStatusEnum.WAITING_FOR_POWER) {
                    imgIcon.isVisible = true
                    progressBar.isVisible = false
                    progressCircle.isVisible = false
                    imgIcon.setImageResource(IR.drawable.ic_waitingforpower)
                    lblStatus.text = context.getString(LR.string.episode_row_waiting_for_power)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(iconColor))
                } else if (episode.episodeStatus == EpisodeStatusEnum.WAITING_FOR_WIFI) {
                    imgIcon.isVisible = true
                    progressBar.isVisible = false
                    progressCircle.isVisible = false
                    imgIcon.setImageResource(IR.drawable.ic_waitingforwifi)
                    lblStatus.text = context.getString(LR.string.episode_row_waiting_for_wifi)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(iconColor))
                } else if (episode.episodeStatus == EpisodeStatusEnum.QUEUED) {
                    imgIcon.isVisible = false
                    progressBar.isVisible = false
                    progressCircle.isVisible = false
                    lblStatus.text = context.getString(LR.string.episode_row_queued)
                    imgIcon.setImageResource(IR.drawable.ic_waitingforwifi)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(iconColor))
                } else {
                    imgIcon.isVisible = false
                    progressBar.isVisible = false
                    progressCircle.isVisible = false
                    updateTimeLeft(textView = lblStatus, episode = episode)
                }

                val drawable = AppCompatResources.getDrawable(context, IR.drawable.ic_cloud)
                imgCloud.setup(drawable)

                imgCloud.isVisible = episode.serverStatus == UserEpisodeServerStatus.UPLOADED
                if (episode.serverStatus == UserEpisodeServerStatus.UPLOADING) {
                    imgIcon.isVisible = false
                    imgCloud.isVisible = false
                    progressCircle.isVisible = true
                    lblStatus.text = context.getString(LR.string.episode_row_uploading, data.uploadProgress)
                    progressCircle.setPercent(data.uploadProgress / 100f)
                } else if (episode.serverStatus == UserEpisodeServerStatus.WAITING_FOR_WIFI) {
                    imgIcon.isVisible = true
                    progressBar.isVisible = false
                    progressCircle.isVisible = false
                    imgIcon.setImageResource(IR.drawable.ic_waitingforwifi)
                    lblStatus.text = context.getString(LR.string.episode_row_waiting_for_wifi)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(iconColor))
                } else if (episode.serverStatus == UserEpisodeServerStatus.QUEUED) {
                    imgIcon.isVisible = false
                    progressBar.isVisible = false
                    progressCircle.isVisible = false
                    lblStatus.text = context.getString(LR.string.episode_row_queued_upload)
                    imgIcon.setImageResource(IR.drawable.ic_waitingforwifi)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(iconColor))
                }

                val episodeGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived

                val statusColor = if (episodeGreyedOut) {
                    captionWithAlpha
                } else {
                    context.getThemeColor(
                        UR.attr.primary_text_02,
                    )
                }
                lblStatus.setTextColor(statusColor)
                lblStatus.contentDescription = lblStatus.text.toString()
                statusText = lblStatus.text.toString()

                val imageAlpha = if (episodeGreyedOut) 0.5f else 1f
                imgCloud.alpha = imageAlpha
                imgBookmark.alpha = imageAlpha
                imgIcon.alpha = imageAlpha
            }
            .subscribe()
            .addTo(disposables)

        val episodeGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived

        val statusColor = if (episodeGreyedOut) captionWithAlpha else context.getThemeColor(UR.attr.primary_text_02)
        lblStatus.setTextColor(statusColor)
    }

    private fun updateTimeLeft(textView: TextView, episode: UserEpisode) {
        val timeLeft = TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, context)
        textView.text = timeLeft.text
        textView.contentDescription = timeLeft.description
    }

    fun clearObservers() {
        disposables.clear()
    }
}
