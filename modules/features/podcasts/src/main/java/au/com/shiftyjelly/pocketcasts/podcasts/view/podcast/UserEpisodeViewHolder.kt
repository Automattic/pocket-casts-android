package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.AdapterUserEpisodeBinding
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
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class UserEpisodeViewHolder(
    private val binding: AdapterUserEpisodeBinding,
    private val imageRequestFactory: PocketCastsImageRequestFactory,
    private val swipeRowActionsFactory: SwipeRowActions.Factory,
    private val rowDataProvider: EpisodeRowDataProvider,
    private val playButtonListener: PlayButton.OnClickListener,
    private val onRowClick: (UserEpisode) -> Unit,
    private val onRowLongClick: (UserEpisode) -> Unit,
    private val onSwipeAction: (UserEpisode, SwipeAction) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    private inline val context get() = itemView.context

    @Suppress("UNCHECKED_CAST")
    private val swipeLayout = binding.root as SwipeRowLayout<SwipeAction>

    private val primaryText01Tint = context.getThemeColor(UR.attr.primary_text_01)
    private val primaryText01TintAlpha = ColorUtils.colorWithAlpha(primaryText01Tint, alpha = 128)
    private val primaryText02Tint = context.getThemeColor(UR.attr.primary_text_02)
    private val primaryText02TintAlpha = ColorUtils.colorWithAlpha(primaryText02Tint, alpha = 128)
    private val primaryUi02Tint = context.getThemeColor(UR.attr.primary_ui_02)
    private val primaryUi02SelectedTint = context.getThemeColor(UR.attr.primary_ui_02_selected)
    private var tint = context.getThemeColor(UR.attr.primary_icon_01)

    private val dateFormatter = RelativeDateFormatter(context)

    private val disposable: CompositeDisposable = CompositeDisposable()

    private var boundEpisode: UserEpisode? = null
    private val episode get() = requireNotNull(boundEpisode)
    private var isMultiSelectEnabled = false
    private var streamByDefault = false

    init {
        binding.episodeRow.setOnClickListener {
            onRowClick(episode)
            swipeLayout.settle()
        }
        binding.episodeRow.setOnLongClickListener {
            onRowLongClick(episode)
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
        swipeLayout.addOnSwipeActionListener { action -> onSwipeAction(episode, action) }
    }

    fun bind(
        episode: UserEpisode,
        @ColorInt tint: Int,
        isMultiSelectEnabled: Boolean,
        isSelected: Boolean,
        useEpisodeArtwork: Boolean,
        streamByDefault: Boolean,
        animateMultiSelection: Boolean,
    ) {
        val wasMultiSelecting = this.isMultiSelectEnabled
        val previousUuid = boundEpisode?.uuid
        setupInitialState(episode, tint, isMultiSelectEnabled, streamByDefault)

        if (previousUuid != episode.uuid) {
            observeRowData()
            observeFileStatus()
        }
        bindArtwork(useEpisodeArtwork)
        bindPlaybackButton()
        bindTitle()
        bindDate()
        bindContentDescription()
        bindColors()
        bindSwipeActions()
        bindSelectedRow(isSelected)
        if (wasMultiSelecting != isMultiSelectEnabled) {
            bindMultiSelection(animateMultiSelection)
        }
    }

    private fun setupInitialState(episode: UserEpisode, tint: Int?, isMultiSelectEnabled: Boolean, streamByDefault: Boolean) {
        if (episode.uuid != this.boundEpisode?.uuid) {
            swipeLayout.clearTranslation()
        }
        this.boundEpisode = episode
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
        binding.fileStatusIconsView.clearObservers()
        binding.episodeRow.handler?.removeCallbacksAndMessages(null)
    }

    private fun observeRowData() {
        disposable.clear()
        disposable += rowDataProvider.episodeRowDataObservable(episode.uuid).subscribeBy(onNext = { data ->
            bindPlaybackButton()
            bindDate()
            bindSwipeActions()
            bindContentDescription()
        })
    }

    private fun observeFileStatus() {
        binding.video.isVisible = episode.isVideo
        binding.fileStatusIconsView.clearObservers()
        binding.fileStatusIconsView.setup(
            episode = episode,
            tintColor = tint,
            rowDataObservable = rowDataProvider.episodeRowDataObservable(episode.uuid),
        )
    }

    private fun bindArtwork(useEpisodeArtwork: Boolean) {
        imageRequestFactory.create(episode, useEpisodeArtwork).loadInto(binding.imgArtwork)
    }

    private fun bindTitle() {
        binding.title.text = episode.title
    }

    private fun bindDate() {
        binding.date.text = episode.getSummaryText(dateFormatter, tint, showDuration = false, context)
    }

    private fun bindColors() {
        val isGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived

        binding.title.setTextColor(if (isGreyedOut) primaryText01TintAlpha else primaryText01Tint)
        binding.date.setTextColor(if (isGreyedOut) primaryText02TintAlpha else primaryText02Tint)
        binding.artworkBox.elevation = if (isGreyedOut) 0f else 2.dpToPx(context).toFloat()
        binding.imgArtwork.alpha = if (isGreyedOut) 0.5f else 1f
    }

    private fun bindPlaybackButton() {
        val buttonType = PlayButton.calculateButtonType(episode, streamByDefault)
        binding.playButton.setButtonType(episode, buttonType, tint, fromListUuid = null)
    }

    private fun bindContentDescription() {
        val status = binding.fileStatusIconsView.statusText
        binding.episodeRow.contentDescription = "${binding.title.text} ${binding.date.text} $status"
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

    private fun bindSwipeActions() {
        swipeRowActionsFactory.userEpisode(episode).applyTo(swipeLayout)
    }
}
