package au.com.shiftyjelly.pocketcasts.player.view

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.MotionEvent
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterUpNextBinding
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getAttrTextStyleColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.setRippleBackground
import au.com.shiftyjelly.pocketcasts.views.helper.setEpisodeTimeLeft
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowLayout
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class UpNextEpisodeViewHolder(
    private val binding: AdapterUpNextBinding,
    private val episodeManager: EpisodeManager,
    private val imageRequestFactory: PocketCastsImageRequestFactory,
    private val swipeRowActionsFactory: SwipeRowActions.Factory,
    private val listener: UpNextListener,
    private val onRowClick: (BaseEpisode) -> Unit,
    private val onRowLongClick: (BaseEpisode) -> Unit,
    private val onSwipeAction: (BaseEpisode, SwipeAction) -> Unit,
) : RecyclerView.ViewHolder(binding.root),
    UpNextTouchCallback.ItemTouchHelperViewHolder {

    private var boundEpisode: BaseEpisode? = null

    private val episode get() = requireNotNull(boundEpisode)

    private inline val context get() = itemView.context

    private val dateFormatter = RelativeDateFormatter(context)

    private val disposable = CompositeDisposable()

    private var isMultiSelectEnabled = false

    @Suppress("UNCHECKED_CAST")
    private val swipeLayout = binding.root as SwipeRowLayout<SwipeAction>

    private val elevatedBackground = ContextCompat.getColor(binding.root.context, R.color.elevatedBackground)
    private val selectedBackground = ContextCompat.getColor(binding.root.context, R.color.selectedBackground)

    private val primaryUi01Tint = context.getThemeColor(UR.attr.primary_ui_01)
    private val primaryUi02SelectedTint = context.getThemeColor(UR.attr.primary_ui_02_selected)
    private val tint = context.getAttrTextStyleColor(UR.attr.textSubtitle1)

    init {
        binding.imageCardView.radius = 4.dpToPx(context).toFloat()
        binding.imageCardView.elevation = 2.dpToPx(context).toFloat()

        binding.itemContainer.setOnClickListener {
            onRowClick(episode)
            swipeLayout.settle()
        }
        binding.itemContainer.setOnLongClickListener {
            onRowLongClick(episode)
            swipeLayout.settle()
            true
        }
        binding.checkbox.setOnClickListener {
            binding.itemContainer.performClick()
        }
        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            bindSelectedRow(isChecked)
        }

        @SuppressLint("ClickableViewAccessibility")
        binding.reorder.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                listener.onUpNextEpisodeStartDrag(this)
            }
            false
        }

        swipeLayout.addOnSwipeActionListener { action -> onSwipeAction(episode, action) }
    }

    fun bind(
        episode: BaseEpisode,
        isMultiSelectEnabled: Boolean,
        isSelected: Boolean,
        useEpisodeArtwork: Boolean,
        animateMultiSelection: Boolean = false,
    ) {
        val wasMultiSelecting = this.isMultiSelectEnabled
        val previousUuid = boundEpisode?.uuid
        setupInitialState(episode, isMultiSelectEnabled)

        if (previousUuid != episode.uuid) {
            observeEpisode()
        }
        bindArtwork(useEpisodeArtwork)
        bindEpisode(episode)
        bindDate()
        bindSwipeActions()
        bindSelectedRow(isSelected)
        if (wasMultiSelecting != isMultiSelectEnabled) {
            bindMultiSelection(animateMultiSelection)
        }
    }

    private fun setupInitialState(episode: BaseEpisode, isMultiSelectEnabled: Boolean) {
        if (episode.uuid != boundEpisode?.uuid) {
            swipeLayout.clearTranslation()
        }
        boundEpisode = episode
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
    }

    private fun observeEpisode() {
        disposable.clear()
        disposable += episodeManager
            .findEpisodeByUuidRxFlowable(episode.uuid)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onNext = { episode ->
                bindEpisode(episode)
                bindSwipeActions()
            })
    }

    private fun bindEpisode(episode: BaseEpisode) {
        binding.title.text = episode.title
        binding.downloaded.isVisible = episode.isDownloaded
        binding.info.setEpisodeTimeLeft(episode)
    }

    private fun bindArtwork(useEpisodeArtwork: Boolean) {
        imageRequestFactory.create(episode, useEpisodeArtwork).loadInto(binding.image)
    }

    private fun bindDate() {
        binding.date.text = episode.getSummaryText(dateFormatter = dateFormatter, tintColor = tint, showDuration = false, context = context)
    }

    private fun bindSwipeActions() {
        swipeRowActionsFactory.upNextEpisode(episode).applyTo(swipeLayout)
    }

    private fun bindSelectedRow(isSelected: Boolean) {
        binding.checkbox.isChecked = isSelected
        binding.itemContainer.setBackgroundColor(if (isMultiSelectEnabled && isSelected) primaryUi02SelectedTint else primaryUi01Tint)
    }

    private fun bindMultiSelection(shouldAnimate: Boolean) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(binding.itemContainer)
        constraintSet.setVisibility(binding.checkbox.id, if (isMultiSelectEnabled) View.VISIBLE else View.GONE)
        constraintSet.setVisibility(binding.reorder.id, if (isMultiSelectEnabled) View.GONE else View.VISIBLE)

        if (shouldAnimate) {
            binding.itemContainer.post {
                val transition = AutoTransition().setDuration(100)
                TransitionManager.beginDelayedTransition(binding.itemContainer, transition)
                constraintSet.applyTo(binding.itemContainer)
            }
        } else {
            TransitionManager.endTransitions(binding.itemContainer)
            constraintSet.applyTo(binding.itemContainer)
        }
    }

    override fun onItemDrag() {
        AnimatorSet().apply {
            val backgroundView = binding.root
            val elevation = ObjectAnimator.ofPropertyValuesHolder(backgroundView, PropertyValuesHolder.ofFloat(View.TRANSLATION_Z, 16.dpToPx(backgroundView.resources.displayMetrics).toFloat()))

            val color = ObjectAnimator.ofInt(backgroundView, "backgroundColor", elevatedBackground, selectedBackground)
            color.setEvaluator(ArgbEvaluator())

            playTogether(elevation, color)
            start()
        }
    }

    override fun onItemClear() {
        AnimatorSet().apply {
            val backgroundView = binding.root
            val elevation = ObjectAnimator.ofPropertyValuesHolder(backgroundView, PropertyValuesHolder.ofFloat(View.TRANSLATION_Z, 0.toFloat()))

            backgroundView.setRippleBackground(false)
            play(elevation)
            start()
        }
    }
}
