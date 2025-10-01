package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.content.res.ColorStateList
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterEpisodeBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowLayout
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

abstract class BaseEpisodeViewHolder<T : Any>(
    private val binding: AdapterEpisodeBinding,
    private val fromListUuid: String?,
    private val showArtwork: Boolean,
    private val imageRequestFactory: PocketCastsImageRequestFactory,
    private val swipeRowActionsFactory: SwipeRowActions.Factory,
    private val rowDataProvider: EpisodeRowDataProvider,
    private val playButtonListener: PlayButton.OnClickListener,
    private val onRowClick: (T) -> Unit,
    private val onRowLongClick: (T) -> Unit,
    private val onSwipeAction: (T, SwipeAction) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    protected abstract fun toPodcastEpisode(item: T): PodcastEpisode

    protected abstract fun getSwipeActions(item: T, factory: SwipeRowActions.Factory): SwipeRowActions?

    private var boundItem: T? = null

    private val episode get() = toPodcastEpisode(requireNotNull(boundItem))

    private inline val context get() = itemView.context

    private val dateFormatter = RelativeDateFormatter(context)

    private val disposable = CompositeDisposable()

    private var isMultiSelectEnabled = false

    private var streamByDefault = false

    @Suppress("UNCHECKED_CAST")
    private val swipeLayout = binding.root as SwipeRowLayout<SwipeAction>

    private val primaryText01Tint = context.getThemeColor(UR.attr.primary_text_01)
    private val primaryText01TintAlpha = ColorUtils.colorWithAlpha(primaryText01Tint, alpha = 128)
    private val primaryText02Tint = context.getThemeColor(UR.attr.primary_text_02)
    private val primaryText02TintAlpha = ColorUtils.colorWithAlpha(primaryText02Tint, alpha = 128)
    private val primaryIcon02Tint = context.getThemeColor(UR.attr.primary_icon_02)
    private val primaryUi02Tint = context.getThemeColor(UR.attr.primary_ui_02)
    private val primaryUi02SelectedTint = context.getThemeColor(UR.attr.primary_ui_02_selected)
    private val support02Tint = context.getThemeColor(UR.attr.support_02)
    private var tint = context.getThemeColor(UR.attr.primary_icon_01)

    init {
        binding.progressCircle.setColor(primaryText02Tint)
        binding.progressBar.indeterminateTintList = ColorStateList.valueOf(primaryText02Tint)

        binding.episodeRow.setOnClickListener {
            onRowClick(requireNotNull(boundItem))
            swipeLayout.settle()
        }
        binding.episodeRow.setOnLongClickListener {
            onRowLongClick(requireNotNull(boundItem))
            swipeLayout.settle()
            true
        }
        binding.playButton.listener = playButtonListener
        binding.checkbox.setOnClickListener {
            binding.episodeRow.performClick()
        }
        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            bindSelectedRow(isChecked)
        }
        swipeLayout.addOnSwipeActionListener { action -> onSwipeAction(requireNotNull(boundItem), action) }
    }

    fun bind(
        item: T,
        isMultiSelectEnabled: Boolean,
        isSelected: Boolean,
        useEpisodeArtwork: Boolean,
        streamByDefault: Boolean,
        tint: Int? = null,
        animateMultiSelection: Boolean = false,
    ) {
        val wasMultiSelecting = this.isMultiSelectEnabled
        val previousUuid = boundItem?.let(::toPodcastEpisode)?.uuid
        setupInitialState(item, tint, isMultiSelectEnabled, streamByDefault)

        if (previousUuid != episode.uuid) {
            observeRowData()
        }
        bindArtwork(useEpisodeArtwork)
        bindPlaybackButton()
        bindTitle()
        bindDate()
        bindStatus(downloadProgress = 0)
        bindContentDescription(isInUpNext = false)
        bindColors()
        bindSwipeActions()
        bindSelectedRow(isSelected)
        if (wasMultiSelecting != isMultiSelectEnabled) {
            bindMultiSelection(animateMultiSelection)
        }
    }

    private fun setupInitialState(item: T, tint: Int?, isMultiSelectEnabled: Boolean, streamByDefault: Boolean) {
        if (toPodcastEpisode(item).uuid != boundItem?.let(::toPodcastEpisode)?.uuid) {
            swipeLayout.clearTranslation()
        }
        boundItem = item
        this.streamByDefault = streamByDefault
        if (tint != null) {
            this.tint = tint
        }
        if (this.isMultiSelectEnabled != isMultiSelectEnabled) {
            this.isMultiSelectEnabled = isMultiSelectEnabled
            if (isMultiSelectEnabled) {
                swipeLayout.clearTranslation()
                swipeLayout.lock()
            } else {
                swipeLayout.unlock()
            }
        }
    }

    fun unbind() {
        disposable.clear()
        binding.episodeRow.handler?.removeCallbacksAndMessages(null)
    }

    private fun observeRowData() {
        disposable.clear()
        disposable += rowDataProvider.episodeRowDataObservable(episode.uuid).subscribeBy(onNext = { data ->
            episode.playing = data.playbackState.isPlaying && data.playbackState.episodeUuid == episode.uuid
            bindPlaybackButton()

            binding.imgUpNext.isVisible = data.isInUpNext
            binding.imgBookmark.isVisible = data.hasBookmarks

            if (data.playbackState.episodeUuid == episode.uuid && data.playbackState.isBuffering) {
                bindStatus(text = context.getString(LR.string.episode_row_buffering))
                binding.progressBar.isVisible = true
            } else {
                bindStatus(downloadProgress = data.downloadProgress)
            }
            bindSwipeActions()
            bindContentDescription(isInUpNext = data.isInUpNext)
        })
    }

    private fun bindArtwork(useEpisodeArtwork: Boolean) {
        binding.artworkBox.isVisible = showArtwork
        if (showArtwork) {
            imageRequestFactory.create(episode, useEpisodeArtwork).loadInto(binding.imgArtwork)
        } else {
            binding.imgArtwork.setImageDrawable(null)
        }
    }

    private fun bindTitle() {
        binding.title.text = episode.title
    }

    private fun bindDate() {
        binding.date.text = episode.getSummaryText(dateFormatter, tint, showDuration = false, context)
    }

    private fun bindStatus(downloadProgress: Int) {
        binding.star.isVisible = episode.isStarred
        binding.video.isVisible = episode.isVideo
        binding.progressBar.isVisible = false
        binding.progressCircle.isVisible = false

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
                text = "$archivedString. ${timeLeft.text}",
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

    private fun bindColors() {
        val isGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived

        binding.title.setTextColor(if (isGreyedOut) primaryText01TintAlpha else primaryText01Tint)
        binding.date.setTextColor(if (isGreyedOut) primaryText02TintAlpha else primaryText02Tint)
        binding.lblStatus.setTextColor(if (isGreyedOut) primaryText02TintAlpha else primaryText02Tint)
        binding.artworkBox.elevation = if (isGreyedOut) 0f else 2.dpToPx(context).toFloat()
        binding.imgArtwork.alpha = if (isGreyedOut) 0.5f else 1f
        binding.imgBookmark.alpha = if (isGreyedOut) 0.5f else 1f
        binding.imgBookmark.imageTintList = ColorStateList.valueOf(tint)
        binding.imgIcon.alpha = if (isGreyedOut) 0.5f else 1f
    }

    private fun bindSwipeActions() {
        val actions = getSwipeActions(requireNotNull(boundItem), swipeRowActionsFactory) ?: SwipeRowActions.Empty
        actions.applyTo(swipeLayout)
    }

    private fun bindPlaybackButton() {
        val buttonType = PlayButton.calculateButtonType(episode, streamByDefault)
        binding.playButton.setButtonType(episode, buttonType, tint, fromListUuid = fromListUuid)
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

    private fun bindSelectedRow(isSelected: Boolean) {
        binding.checkbox.isChecked = isSelected
        binding.episodeRow.setBackgroundColor(if (isMultiSelectEnabled && isSelected) primaryUi02SelectedTint else primaryUi02Tint)
    }

    private fun bindMultiSelection(shouldAnimate: Boolean) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.episodeRow)
        constraintSet.setVisibility(binding.checkbox.id, if (isMultiSelectEnabled) View.VISIBLE else View.GONE)
        constraintSet.setVisibility(binding.playButton.id, if (isMultiSelectEnabled) View.GONE else View.VISIBLE)

        if (shouldAnimate) {
            binding.episodeRow.post {
                val transition = AutoTransition().setDuration(100)
                TransitionManager.beginDelayedTransition(binding.episodeRow, transition)
                constraintSet.applyTo(binding.episodeRow)
            }
        } else {
            TransitionManager.endTransitions(binding.episodeRow)
            constraintSet.applyTo(binding.episodeRow)
        }
    }
}
