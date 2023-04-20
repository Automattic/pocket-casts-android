package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.content.Context
import android.content.res.ColorStateList
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadProgressUpdate
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.images.into
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.containsUuid
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.RowSwipeable
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class EpisodeViewHolder(
    val binding: AdapterEpisodeBinding,
    val viewMode: ViewMode,
    val downloadProgressUpdates: Observable<DownloadProgressUpdate>,
    val playbackStateUpdates: Observable<PlaybackState>,
    val upNextChangesObservable: Observable<UpNextQueue.State>,
    val imageLoader: PodcastImageLoader? = null
) : RecyclerView.ViewHolder(binding.root), RowSwipeable {
    override val episodeRow: ViewGroup
        get() = binding.episodeRow
    override val swipeLeftIcon: ImageView
        get() = binding.archiveIcon
    override val leftRightIcon1: ImageView
        get() = binding.leftRightIcon1
    override val leftRightIcon2: ImageView
        get() = binding.leftRightIcon2
    override val episode: Playable?
        get() = binding.episode
    override val positionAdapter: Int
        get() = bindingAdapterPosition
    override val rightToLeftSwipeLayout: ViewGroup
        get() = binding.rightToLeftSwipeLayout
    override val leftToRightSwipeLayout: ViewGroup
        get() = binding.leftToRightSwipeLayout

    sealed class ViewMode {
        object NoArtwork : ViewMode()
        object Artwork : ViewMode()
    }

    val dateFormatter = RelativeDateFormatter(context)
    val context: Context
        get() = binding.root.context
    private var disposable: Disposable? = null
    override var upNextAction = Settings.UpNextAction.PLAY_NEXT
    override var isMultiSelecting: Boolean = false
    override val leftIconDrawablesRes: List<EpisodeItemTouchHelper.IconWithBackground>
        get() {
            return if (binding.inUpNext == true) {
                listOf(EpisodeItemTouchHelper.IconWithBackground(IR.drawable.ic_upnext_remove, binding.episodeRow.context.getThemeColor(UR.attr.support_05)))
            } else {
                val addToUpNextIcon = when (upNextAction) {
                    Settings.UpNextAction.PLAY_NEXT -> IR.drawable.ic_upnext_playnext
                    Settings.UpNextAction.PLAY_LAST -> IR.drawable.ic_upnext_playlast
                }
                val secondaryUpNextIcon = when (upNextAction) {
                    Settings.UpNextAction.PLAY_NEXT -> IR.drawable.ic_upnext_playlast
                    Settings.UpNextAction.PLAY_LAST -> IR.drawable.ic_upnext_playnext
                }

                listOf(
                    EpisodeItemTouchHelper.IconWithBackground(addToUpNextIcon, binding.episodeRow.context.getThemeColor(UR.attr.support_04)),
                    EpisodeItemTouchHelper.IconWithBackground(secondaryUpNextIcon, binding.episodeRow.context.getThemeColor(UR.attr.support_03))
                )
            }
        }
    override val rightIconDrawableRes: List<EpisodeItemTouchHelper.IconWithBackground>
        get() {
            return if (episode?.isArchived == true)
                listOf(EpisodeItemTouchHelper.IconWithBackground(IR.drawable.ic_unarchive, binding.episodeRow.context.getThemeColor(UR.attr.support_06)))
            else
                listOf(EpisodeItemTouchHelper.IconWithBackground(IR.drawable.ic_archive, binding.episodeRow.context.getThemeColor(UR.attr.support_06)))
        }

    fun setup(episode: PodcastEpisode, fromListUuid: String?, tintColor: Int, playButtonListener: PlayButton.OnClickListener, streamByDefault: Boolean, upNextAction: Settings.UpNextAction, multiSelectEnabled: Boolean = false, isSelected: Boolean = false, disposables: CompositeDisposable) {
        this.upNextAction = upNextAction
        this.isMultiSelecting = multiSelectEnabled

        val sameEpisode = episode.uuid == binding.episode?.uuid

        // don't set initial values if it's already been done, a side effect is when an episode is playing that it will quickly toggle between the play and pause icon
        if (!sameEpisode) {
            val buttonType = PlayButton.calculateButtonType(episode, streamByDefault)
            binding.playButton.setButtonType(episode, buttonType, tintColor, fromListUuid)
            updateTimeLeft(textView = binding.lblStatus, episode = episode)
        }
        binding.episode = episode
        binding.playButton.listener = playButtonListener

        val captionColor = context.getThemeColor(UR.attr.primary_text_02)
        val iconColor = context.getThemeColor(UR.attr.primary_icon_02)
        binding.progressCircle.setColor(captionColor)
        binding.progressBar.indeterminateTintList = ColorStateList.valueOf(captionColor)

        val downloadUpdates = downloadProgressUpdates
            .filter { it.episodeUuid == episode.uuid }
            .map { (it.downloadProgress * 100f).roundToInt() }
            .sample(1, TimeUnit.SECONDS)
            .startWith(0)

        val isInUpNextObservable = upNextChangesObservable.containsUuid(episode.uuid)
        val emptyState = PlaybackState(episodeUuid = episode.uuid)
        val playbackStateForThisEpisode = playbackStateUpdates
            .startWith(emptyState) // Pre load with a blank state so it doesn't wait for the first update
            .map { if (it.episodeUuid == episode.uuid) it else emptyState } // When another episode is playing return an empty state to clear fields like the buffering status

        val imgIcon = binding.imgIcon
        val progressBar = binding.progressBar
        val progressCircle = binding.progressCircle
        val lblStatus = binding.lblStatus
        val imgArtwork = binding.imgArtwork
        val checkbox = binding.checkbox
        val title = binding.title
        val date = binding.date

        disposable?.dispose()
        disposable = Observables.combineLatest(downloadUpdates, playbackStateForThisEpisode, isInUpNextObservable)
            .distinctUntilChanged()
            .toFlowable(BackpressureStrategy.LATEST)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { (downloadProgress, playbackState, isInUpNext) ->
                episode.playing = playbackState.isPlaying && playbackState.episodeUuid == episode.uuid
                val playButtonType = PlayButton.calculateButtonType(episode, streamByDefault)
                binding.playButton.setButtonType(episode, playButtonType, tintColor, fromListUuid)
                binding.inUpNext = isInUpNext

                imgIcon.isVisible = false
                progressCircle.isVisible = false
                progressBar.isVisible = false
                imgIcon.alpha = 1.0f
                if (playbackState.episodeUuid == episode.uuid && playbackState.isBuffering) {
                    progressBar.isVisible = true
                    lblStatus.text = context.getString(LR.string.episode_row_buffering)
                } else if (episode.episodeStatus == EpisodeStatusEnum.DOWNLOADED) {
                    imgIcon.isVisible = true
                    imgIcon.setImageResource(IR.drawable.ic_downloaded)
                    updateTimeLeft(textView = lblStatus, episode = episode)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(context.getThemeColor(UR.attr.support_02)))
                } else if (episode.episodeStatus == EpisodeStatusEnum.DOWNLOADING) {
                    progressCircle.isVisible = true
                    lblStatus.text = context.getString(LR.string.episode_row_downloading, downloadProgress)
                    progressCircle.setPercent(downloadProgress / 100.0f)
                } else if (episode.episodeStatus == EpisodeStatusEnum.DOWNLOAD_FAILED) {
                    imgIcon.isVisible = true
                    imgIcon.setImageResource(IR.drawable.ic_download_failed_row)
                    lblStatus.text = context.getString(LR.string.episode_row_download_failed)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(iconColor))
                } else if (episode.episodeStatus == EpisodeStatusEnum.WAITING_FOR_POWER) {
                    imgIcon.isVisible = true
                    imgIcon.setImageResource(IR.drawable.ic_waitingforpower)
                    lblStatus.text = context.getString(LR.string.episode_row_waiting_for_power)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(iconColor))
                } else if (episode.episodeStatus == EpisodeStatusEnum.WAITING_FOR_WIFI) {
                    imgIcon.isVisible = true
                    imgIcon.setImageResource(IR.drawable.ic_waitingforwifi)
                    lblStatus.text = context.getString(LR.string.episode_row_waiting_for_wifi)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(iconColor))
                } else if (episode.episodeStatus == EpisodeStatusEnum.QUEUED) {
                    lblStatus.text = context.getString(LR.string.episode_row_queued)
                    imgIcon.setImageResource(IR.drawable.ic_waitingforwifi)
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(iconColor))
                } else if (episode.isArchived) {
                    imgIcon.isVisible = true
                    imgIcon.setImageResource(IR.drawable.ic_archive)
                    imgIcon.alpha = 0.5f
                    val archivedString = context.getString(LR.string.archived)
                    val timeLeft = TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, context)
                    lblStatus.text = "$archivedString • ${timeLeft.text}"
                    lblStatus.contentDescription = "$archivedString. ${timeLeft.description}"
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(iconColor))
                } else if (episode.playErrorDetails != null) {
                    imgIcon.isVisible = true
                    imgIcon.setImageResource(IR.drawable.ic_alert_small)
                    lblStatus.text = episode.playErrorDetails
                    ImageViewCompat.setImageTintList(imgIcon, ColorStateList.valueOf(iconColor))
                } else {
                    updateTimeLeft(textView = lblStatus, episode = episode)
                }
                updateRowText(episode, captionColor, tintColor, date, title, lblStatus, isInUpNext)

                val episodeGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived
                imgArtwork.alpha = if (episodeGreyedOut) 0.5f else 1f
                binding.executePendingBindings()
            }
            .subscribe()
            .addTo(disposables)

        title.text = episode.title
        updateRowText(episode, captionColor, tintColor, date, title, lblStatus)

        val artworkVisible = viewMode is ViewMode.Artwork
        imgArtwork.isVisible = artworkVisible
        if (!sameEpisode && artworkVisible && imageLoader != null) {
            imageLoader.load(episode).into(imgArtwork)
        }

        val transition = AutoTransition()
        transition.duration = 100
        TransitionManager.beginDelayedTransition(episodeRow, transition)
        checkbox.isVisible = multiSelectEnabled
        checkbox.isChecked = isSelected
        checkbox.setOnClickListener {
            episodeRow.performClick()
        }

        val selectedColor = context.getThemeColor(UR.attr.primary_ui_02_selected)
        val unselectedColor = context.getThemeColor(UR.attr.primary_ui_02)
        checkbox.setOnCheckedChangeListener { _, isChecked ->
            binding.episodeRow.setBackgroundColor(if (isMultiSelecting && isChecked) selectedColor else unselectedColor)
        }
        binding.episodeRow.setBackgroundColor(if (isMultiSelecting && isSelected) selectedColor else unselectedColor)

        binding.playButton.visibility = if (multiSelectEnabled) View.INVISIBLE else View.VISIBLE
        binding.playButton.updateLayoutParams<ConstraintLayout.LayoutParams> { // Adjust the spacing of the play button to avoid line wrapping when turning on multiselect
            rightMargin = if (multiSelectEnabled) -checkbox.marginLeft else 0.dpToPx(context)
            width = if (multiSelectEnabled) 16.dpToPx(context) else 52.dpToPx(context)
        }

        TransitionManager.endTransitions(episodeRow)
    }

    private fun updateTimeLeft(textView: TextView, episode: PodcastEpisode) {
        val timeLeft = TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, context)
        textView.text = timeLeft.text
        textView.contentDescription = timeLeft.description
    }

    private fun updateRowText(episode: PodcastEpisode, captionColor: Int, tintColor: Int, date: TextView, title: TextView, lblStatus: TextView, isInUpNext: Boolean = false) {
        val episodeGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived
        val alphaCaptionColor = ColorUtils.colorWithAlpha(captionColor, 128)
        val dateTintColor = if (episodeGreyedOut) alphaCaptionColor else tintColor
        val dateTextColor = if (episodeGreyedOut) alphaCaptionColor else context.getThemeColor(UR.attr.primary_text_02)
        date.text = episode.getSummaryText(dateFormatter = dateFormatter, tintColor = dateTintColor, showDuration = false, context = date.context)
        date.setTextColor(dateTextColor)

        val textColor = if (episodeGreyedOut) ColorUtils.colorWithAlpha(context.getThemeColor(UR.attr.primary_text_01), 128) else context.getThemeColor(
            UR.attr.primary_text_01
        )
        title.setTextColor(textColor)

        val statusColor = if (episodeGreyedOut) alphaCaptionColor else context.getThemeColor(UR.attr.primary_text_02)
        lblStatus.setTextColor(statusColor)

        val attributes = mutableListOf(
            title.text.toString(),
            date.text.toString(),
        )
        attributes.add(
            if (!lblStatus.contentDescription.isNullOrEmpty()) {
                lblStatus.contentDescription.toString()
            } else {
                lblStatus.text.toString()
            }
        )
        if (episode.isInProgress) {
            attributes.add(context.getString(LR.string.in_progress))
        } else if (isInUpNext) {
            attributes.add(context.getString(LR.string.episode_in_up_next))
        }
        if (episode.isDownloaded) {
            attributes.add(context.getString(LR.string.downloaded))
        }
        if (episode.isDownloading) {
            attributes.add(context.getString(LR.string.episode_downloading))
        }
        if (episode.isStarred) {
            attributes.add(context.getString(LR.string.starred))
        }
        episodeRow.contentDescription = attributes.joinToString(separator = ". ") + ". "
    }

    fun clearObservers() {
        disposable?.dispose()
        binding.playButton.listener = null
    }
}
