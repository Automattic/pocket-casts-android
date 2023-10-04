package au.com.shiftyjelly.pocketcasts.views.component

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
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
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadProgressUpdate
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.containsUuid
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UploadProgressManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.R
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class FileStatusIconsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val disposables = CompositeDisposable()
    var uploadConsumer = BehaviorRelay.create<Float>()
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
        ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {}
        setPadding(0, 0, 0, 12.dpToPx(context))
    }

    fun setup(
        episode: UserEpisode,
        downloadProgressUpdates: Observable<DownloadProgressUpdate>,
        playbackStateUpdates: Observable<PlaybackState>,
        upNextChangesObservable: Observable<UpNextQueue.State>,
        userBookmarksObservable: Observable<List<Bookmark>>,
        hideErrorDetails: Boolean = false,
        bookmarksAvailable: Boolean = false,
        tintColor: Int,
    ) {
        val captionColor = context.getThemeColor(UR.attr.primary_text_02)
        val captionWithAlpha = ColorUtils.colorWithAlpha(captionColor, 128)
        val iconColor = context.getThemeColor(UR.attr.primary_icon_02)
        progressCircle.setColor(captionColor)
        progressBar.indeterminateTintList = ColorStateList.valueOf(captionColor)
        imgBookmark.imageTintList = ColorStateList.valueOf(tintColor)

        val downloadUpdates = downloadProgressUpdates
            .filter { it.episodeUuid == episode.uuid }
            .map { it.downloadProgress }
            .startWith(0f)

        UploadProgressManager.observeUploadProgress(episode.uuid, uploadConsumer)
        val uploadUpdates = uploadConsumer.sample(1, TimeUnit.SECONDS).startWith(0f).doOnDispose {
            UploadProgressManager.stopObservingUpload(episode.uuid, uploadConsumer)
        }
        val combinedProgress = Observables.combineLatest(downloadUpdates, uploadUpdates) { first, second ->
            EpisodeStreamProgress(first, second)
        }

        val isInUpNextObservable = upNextChangesObservable.containsUuid(episode.uuid)

        data class CombinedData(
            val streamingProgress: EpisodeStreamProgress,
            val playbackState: PlaybackState,
            val isInUpNext: Boolean,
            val userBookmarks: List<Bookmark>,
        )

        val playbackStateForThisEpisode = playbackStateUpdates
            .startWith(PlaybackState(episodeUuid = episode.uuid)) // Pre load with a blank state so it doesn't wait for the first update
            .filter { it.episodeUuid == episode.uuid } // We only care about playback for this row

        disposables.clear()
        Observables.combineLatest(
            combinedProgress,
            playbackStateForThisEpisode,
            isInUpNextObservable,
            userBookmarksObservable,
        ) { streamProgress, playbackState, isInUpNext, userBookmarks ->
            CombinedData(streamProgress, playbackState, isInUpNext, userBookmarks)
        }
            .distinctUntilChanged()
            .toFlowable(BackpressureStrategy.LATEST)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnTerminate { UploadProgressManager.stopObservingUpload(episode.uuid, uploadConsumer) }
            .doOnNext { combinedData ->
                episode.playing = combinedData.playbackState.isPlaying && combinedData.playbackState.episodeUuid == episode.uuid
                imgUpNext.visibility = if (combinedData.isInUpNext) View.VISIBLE else View.GONE
                imgBookmark.visibility = if (episode.hasBookmark && bookmarksAvailable) View.VISIBLE else View.GONE

                imgIcon.isVisible = false
                if (combinedData.playbackState.episodeUuid == episode.uuid && combinedData.playbackState.isBuffering) {
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
                    lblStatus.text = context.getString(LR.string.episode_row_downloading, (combinedData.streamingProgress.downloadProgress * 100f).roundToInt())
                    progressCircle.setPercent(combinedData.streamingProgress.downloadProgress)
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
                    lblStatus.text = context.getString(LR.string.episode_row_uploading, (combinedData.streamingProgress.uploadProgress * 100f).roundToInt())
                    progressCircle.setPercent(combinedData.streamingProgress.uploadProgress)
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

                val statusColor = if (episodeGreyedOut) captionWithAlpha else context.getThemeColor(
                    UR.attr.primary_text_02
                )
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
        uploadConsumer.accept(0.0f)
    }
}

private data class EpisodeStreamProgress(
    val downloadProgress: Float,
    val uploadProgress: Float
)
