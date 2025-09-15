package au.com.shiftyjelly.pocketcasts.playlists.component

import android.content.res.ColorStateList
import android.transition.AutoTransition
import android.transition.TransitionManager
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.filters.databinding.AdapterEpisodeAvailableBinding
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadProgressUpdate
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.containsUuid
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowLayout
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class EpisodeAvailableViewHolder(
    private val binding: AdapterEpisodeAvailableBinding,
    private val playlistType: Playlist.Type,
    private val imageRequestFactory: PocketCastsImageRequestFactory,
    private val downloadProgressUpdates: Observable<DownloadProgressUpdate>,
    private val playbackStateUpdates: Observable<PlaybackState>,
    private val upNextChangesObservable: Observable<UpNextQueue.State>,
    private val bookmarksObservable: Observable<List<Bookmark>>,
    private val playButtonListener: PlayButton.OnClickListener,
    private val onRowClick: (PlaylistEpisode.Available) -> Unit,
    private val onRowLongClick: (PlaylistEpisode.Available) -> Unit,
    private val onSwipeAction: (PlaylistEpisode.Available, SwipeAction) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private inline val context get() = itemView.context

    @Suppress("UNCHECKED_CAST")
    private val swipeLayout = binding.root as SwipeRowLayout<SwipeAction>

    private val primaryText01Tint = context.getThemeColor(UR.attr.primary_text_01)
    private val primaryText01TintAlpha = ColorUtils.colorWithAlpha(primaryText01Tint, alpha = 128)
    private val primaryText02Tint = context.getThemeColor(UR.attr.primary_text_02)
    private val primaryText02TintAlpha = ColorUtils.colorWithAlpha(primaryText02Tint, alpha = 128)
    private val primaryIcon01Tint = context.getThemeColor(UR.attr.primary_icon_01)
    private val primaryIcon02Tint = context.getThemeColor(UR.attr.primary_icon_02)
    private val primaryUi02Tint = context.getThemeColor(UR.attr.primary_ui_02)
    private val primaryUi02SelectedTint = context.getThemeColor(UR.attr.primary_ui_02_selected)
    private val support02Tint = context.getThemeColor(UR.attr.support_02)

    private val dateFormatter = RelativeDateFormatter(context)

    private val disposable: CompositeDisposable = CompositeDisposable()

    private var episodeWrapper: PlaylistEpisode.Available? = null
    private val episode get() = requireNotNull(episodeWrapper).episode
    private var isMultiSelectEnabled = false
    private var streamByDefault = false
    private var upNextAction = Settings.UpNextAction.PLAY_NEXT

    init {
        binding.progressCircle.setColor(primaryText02Tint)
        binding.progressBar.indeterminateTintList = ColorStateList.valueOf(primaryText02Tint)
        binding.imgBookmark.imageTintList = ColorStateList.valueOf(primaryIcon01Tint)

        binding.episodeRow.setOnClickListener {
            onRowClick(requireNotNull(episodeWrapper))
            swipeLayout.settle()
        }
        binding.episodeRow.setOnLongClickListener {
            onRowLongClick(requireNotNull(episodeWrapper))
            swipeLayout.settle()
            true
        }
        binding.playButton.listener = playButtonListener
        binding.checkbox.setOnClickListener {
            binding.episodeRow.performClick()
        }
        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            bindBackgroundColor(isChecked)
        }
        swipeLayout.addOnSwipeActionListener { action -> onSwipeAction(requireNotNull(episodeWrapper), action) }
    }

    fun bind(
        episodeWrapper: PlaylistEpisode.Available,
        isMultiSelectEnabled: Boolean,
        isSelected: Boolean,
        useEpisodeArtwork: Boolean,
        streamByDefault: Boolean,
        upNextAction: Settings.UpNextAction,
    ) {
        if (episodeWrapper.uuid != this.episodeWrapper?.uuid) {
            swipeLayout.clearTranslation()
        }
        this.episodeWrapper = episodeWrapper
        this.isMultiSelectEnabled = isMultiSelectEnabled
        this.streamByDefault = streamByDefault
        this.upNextAction = upNextAction

        if (isMultiSelectEnabled) {
            swipeLayout.clearTranslation()
            swipeLayout.lock()
        } else {
            swipeLayout.unlock()
        }

        disposable.clear()
        disposable += createObservableData().subscribeBy(onNext = ::bindObservableData)

        bindArtwork(useEpisodeArtwork)
        bindTitle()
        bindDate()
        bindStatus(downloadProgress = 0)
        bindContentDescription(isInUpNext = false)
        bindSwipeActions(isInUpNext = false)
        bindGreyedOutColors()
        bindBackgroundColor(isSelected)
        bindAutoTransition(isSelected)
    }

    fun unbind() {
        disposable.clear()
    }

    private fun bindPlaybackButton(streamByDefault: Boolean) {
        val buttonType = PlayButton.calculateButtonType(episode, streamByDefault)
        binding.playButton.setButtonType(episode, buttonType, primaryIcon01Tint, fromListUuid = null)
    }

    private fun bindArtwork(useEpisodeArtwork: Boolean) {
        imageRequestFactory.create(episode, useEpisodeArtwork).loadInto(binding.imgArtwork)
    }

    private fun bindTitle() {
        binding.title.text = episode.title
    }

    private fun bindDate() {
        binding.date.text = episode.getSummaryText(dateFormatter, primaryIcon01Tint, showDuration = false, context)
    }

    private fun bindStatus(downloadProgress: Int) {
        binding.star.isVisible = episode.isStarred
        binding.video.isVisible = episode.isVideo
        if (episode.episodeStatus == EpisodeStatusEnum.DOWNLOADED) {
            bindTimeLeft(
                iconId = IR.drawable.ic_downloaded,
                iconTint = support02Tint,
            )
        } else if (episode.episodeStatus == EpisodeStatusEnum.DOWNLOADING) {
            bindStatus(
                text = context.getString(LR.string.episode_row_downloading, downloadProgress),
            )
            binding.progressCircle.isVisible = true
            binding.progressCircle.setPercent(downloadProgress / 100.0f)
        } else if (episode.episodeStatus == EpisodeStatusEnum.DOWNLOAD_FAILED) {
            bindStatus(
                text = context.getString(LR.string.episode_row_download_failed),
                iconId = IR.drawable.ic_download_failed_row,
                iconTint = primaryIcon02Tint,
            )
        } else if (episode.episodeStatus == EpisodeStatusEnum.WAITING_FOR_POWER) {
            bindStatus(
                text = context.getString(LR.string.episode_row_waiting_for_power),
                iconId = IR.drawable.ic_waitingforpower,
                iconTint = primaryIcon02Tint,
            )
        } else if (episode.episodeStatus == EpisodeStatusEnum.WAITING_FOR_WIFI) {
            bindStatus(
                text = context.getString(LR.string.episode_row_waiting_for_wifi),
                iconId = IR.drawable.ic_waitingforwifi,
                iconTint = primaryIcon02Tint,
            )
        } else if (episode.episodeStatus == EpisodeStatusEnum.QUEUED) {
            bindStatus(
                text = context.getString(LR.string.episode_row_queued),
                iconId = IR.drawable.ic_waitingforwifi,
                iconTint = primaryIcon02Tint,
            )
        } else if (episode.isArchived) {
            val archivedString = context.getString(LR.string.archived)
            val timeLeft = TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, context)
            bindStatus(
                text = "$archivedString. ${timeLeft.description}",
                description = "$archivedString. ${timeLeft.description}",
                iconId = IR.drawable.ic_archive,
                iconTint = primaryIcon02Tint,
            )
        } else if (episode.playErrorDetails != null) {
            bindStatus(
                text = episode.playErrorDetails.orEmpty(),
                iconId = IR.drawable.ic_alert_small,
                iconTint = primaryIcon02Tint,
            )
        } else {
            bindTimeLeft()
        }
    }

    private fun bindTimeLeft(@DrawableRes iconId: Int? = null, iconTint: Int? = null) {
        val timeLeft = TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, context)
        bindStatus(timeLeft.text, timeLeft.description, iconId, iconTint)
    }

    private fun bindStatus(text: String, description: String = "", @DrawableRes iconId: Int? = null, iconTint: Int? = null) {
        binding.lblStatus.text = text
        binding.lblStatus.contentDescription = description
        binding.imgIcon.isVisible = iconId != null
        if (iconId != null) {
            binding.imgIcon.setImageResource(iconId)
        }
        if (iconTint != null) {
            ImageViewCompat.setImageTintList(binding.imgIcon, ColorStateList.valueOf(iconTint))
        }
    }

    private fun bindContentDescription(isInUpNext: Boolean) {
        binding.episodeRow.contentDescription = buildList {
            add(binding.title.text)
            add(binding.date.text)
            if (binding.lblStatus.contentDescription.isNullOrEmpty()) {
                add(binding.lblStatus.text.toString())
            } else {
                add(binding.lblStatus.contentDescription)
            }
            if (episode.isInProgress) {
                add(context.getString(LR.string.in_progress))
            } else if (isInUpNext) {
                add(context.getString(LR.string.episode_in_up_next))
            }
            if (episode.isDownloaded) {
                add(context.getString(LR.string.downloaded))
            }
            if (episode.isDownloading) {
                add(context.getString(LR.string.episode_downloading))
            }
            if (episode.isStarred) {
                add(context.getString(LR.string.starred))
            }
        }.joinToString(separator = ". ", postfix = ".")
    }

    private fun bindBackgroundColor(isChecked: Boolean) {
        binding.episodeRow.setBackgroundColor(if (isMultiSelectEnabled && isChecked) primaryUi02SelectedTint else primaryUi02Tint)
    }

    private fun bindGreyedOutColors() {
        val isGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived

        binding.title.setTextColor(if (isGreyedOut) primaryText01TintAlpha else primaryText01Tint)
        binding.date.setTextColor(if (isGreyedOut) primaryText02TintAlpha else primaryText02Tint)
        binding.lblStatus.setTextColor(if (isGreyedOut) primaryText02TintAlpha else primaryText02Tint)
        binding.artworkBox.elevation = if (isGreyedOut) 0f else 2.dpToPx(context).toFloat()
        binding.imgArtwork.alpha = if (isGreyedOut) 0.5f else 1f
        binding.imgBookmark.alpha = if (isGreyedOut) 0.5f else 1f
        binding.imgIcon.alpha = if (isGreyedOut) 0.5f else 1f
    }

    private fun bindAutoTransition(isSelected: Boolean) {
        val transition = AutoTransition()
        transition.duration = 100
        TransitionManager.beginDelayedTransition(binding.episodeRow, transition)
        binding.episodeRow
        binding.checkbox.isVisible = isMultiSelectEnabled
        binding.checkbox.isChecked = isSelected

        binding.playButton.isInvisible = isMultiSelectEnabled
        binding.playButton.updateLayoutParams<ConstraintLayout.LayoutParams> {
            rightMargin = if (isMultiSelectEnabled) -binding.checkbox.marginLeft else 0.dpToPx(context)
            width = if (isMultiSelectEnabled) 16.dpToPx(context) else 52.dpToPx(context)
        }

        TransitionManager.endTransitions(binding.episodeRow)
    }

    private fun bindObservableData(data: ObservableData) {
        episode.playing = data.playbackState.isPlaying && data.playbackState.episodeUuid == episode.uuid
        bindPlaybackButton(streamByDefault)

        binding.imgUpNext.isVisible = data.isInUpNext
        binding.imgBookmark.isVisible = data.hasBookmarks
        binding.progressBar.isVisible = false
        binding.progressCircle.isVisible = false

        if (data.playbackState.episodeUuid == episode.uuid && data.playbackState.isBuffering) {
            bindStatus(text = context.getString(LR.string.episode_row_buffering))
            binding.progressBar.isVisible = true
        } else {
            bindStatus(downloadProgress = data.downloadProgress)
        }
        bindSwipeActions(isInUpNext = data.isInUpNext)
        bindContentDescription(isInUpNext = data.isInUpNext)
    }

    private fun bindSwipeActions(isInUpNext: Boolean) {
        if (playlistType == Playlist.Type.Manual) {
            swipeLayout.setRtl1State(SwipeAction.RemoveFromPlaylist)
            swipeLayout.setRtl2State(if (episode.isArchived) SwipeAction.Unarchive else SwipeAction.Archive)
            swipeLayout.setRtl3State(SwipeAction.Share)
        } else {
            swipeLayout.setRtl1State(if (episode.isArchived) SwipeAction.Unarchive else SwipeAction.Archive)
            swipeLayout.setRtl2State(SwipeAction.Share)
            swipeLayout.setRtl3State(null)
        }

        if (isInUpNext) {
            swipeLayout.setLtr1State(SwipeAction.RemoveFromUpNext)
            swipeLayout.setLtr2State(null)
        } else {
            val (upNext1, upNext2) = when (upNextAction) {
                Settings.UpNextAction.PLAY_NEXT -> SwipeAction.AddToUpNextTop to SwipeAction.AddToUpNextBottom
                Settings.UpNextAction.PLAY_LAST -> SwipeAction.AddToUpNextBottom to SwipeAction.AddToUpNextTop
            }
            swipeLayout.setLtr1State(upNext1)
            swipeLayout.setLtr2State(upNext2)
        }
    }

    private fun createObservableData(): Observable<ObservableData> {
        val observableData = Observables.combineLatest(
            createDownloadProgressObservable(),
            createPlaybackStateObservable(),
            createIsInUpNextObservable(),
            createBookmarksObservable(),
            ::ObservableData,
        )
        return observableData
            .debounce(50, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    private fun createDownloadProgressObservable(): Observable<Int> {
        return downloadProgressUpdates
            .filter { it.episodeUuid == episode.uuid }
            .map { (it.downloadProgress * 100f).roundToInt() }
            .sample(1, TimeUnit.SECONDS)
            .startWith(0)
            .distinctUntilChanged()
    }

    private fun createPlaybackStateObservable(): Observable<PlaybackState> {
        val emptyState = PlaybackState(episodeUuid = episode.uuid)
        return playbackStateUpdates
            .startWith(emptyState)
            .map { if (it.episodeUuid == episode.uuid) it else emptyState }
            .distinctUntilChanged()
    }

    private fun createIsInUpNextObservable(): Observable<Boolean> {
        return upNextChangesObservable
            .containsUuid(episode.uuid)
            .startWith(false)
            .distinctUntilChanged()
    }

    private fun createBookmarksObservable(): Observable<Boolean> {
        return bookmarksObservable
            .map { bookmarks -> bookmarks.any { it.episodeUuid == episode.uuid } }
            .startWith(false)
            .distinctUntilChanged()
    }

    data class ObservableData(
        val downloadProgress: Int,
        val playbackState: PlaybackState,
        val isInUpNext: Boolean,
        val hasBookmarks: Boolean,
    )
}
