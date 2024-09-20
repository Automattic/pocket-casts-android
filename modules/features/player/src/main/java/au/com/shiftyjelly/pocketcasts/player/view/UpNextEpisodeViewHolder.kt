package au.com.shiftyjelly.pocketcasts.player.view

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginLeft
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterUpNextBinding
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.loadInto
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getAttrTextStyleColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.setRippleBackground
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.RowSwipeable
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayout
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutFactory
import au.com.shiftyjelly.pocketcasts.views.helper.setEpisodeTimeLeft
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.rx2.asFlowable
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class UpNextEpisodeViewHolder(
    val binding: AdapterUpNextBinding,
    val listener: UpNextListener?,
    val dateFormatter: RelativeDateFormatter,
    val imageRequestFactory: PocketCastsImageRequestFactory,
    val episodeManager: EpisodeManager,
    private val swipeButtonLayoutFactory: SwipeButtonLayoutFactory,
    private val settings: Settings,
) : RecyclerView.ViewHolder(binding.root),
    UpNextTouchCallback.ItemTouchHelperViewHolder,
    RowSwipeable {
    private val cardCornerRadius: Float = 4.dpToPx(itemView.context.resources.displayMetrics).toFloat()
    private val cardElevation: Float = 2.dpToPx(itemView.context.resources.displayMetrics).toFloat()
    private val elevatedBackground = ContextCompat.getColor(binding.root.context, R.color.elevatedBackground)
    private val selectedBackground = ContextCompat.getColor(binding.root.context, R.color.selectedBackground)

    private var episodeInstance: BaseEpisode? = null

    override lateinit var swipeButtonLayout: SwipeButtonLayout

    var disposable: Disposable? = null
        set(value) {
            field?.dispose()
            field = value
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

    fun bind(
        episode: BaseEpisode,
        isMultiSelecting: Boolean,
        isSelected: Boolean,
    ) {
        val tintColor = itemView.context.getAttrTextStyleColor(UR.attr.textSubtitle1)

        disposable = episodeManager
            .observeByUuid(episode.uuid)
            .asFlowable()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext {
                bindEpisode(it)
            }
            .subscribeBy(onError = { Timber.e(it) })

        swipeButtonLayout = swipeButtonLayoutFactory.forEpisode(episode)

        bindEpisode(episode)
        binding.date.text = episode.getSummaryText(dateFormatter = dateFormatter, tintColor = tintColor, showDuration = false, context = binding.date.context)
        binding.reorder.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                listener?.onUpNextEpisodeStartDrag(this)
            }
            false
        }

        imageRequestFactory.create(episode, settings.artworkConfiguration.value.useEpisodeArtwork(Element.UpNext)).loadInto(binding.image)

        val context = binding.itemContainer.context
        val transition = AutoTransition()
        transition.duration = 100
        TransitionManager.beginDelayedTransition(binding.itemContainer, transition)
        binding.checkbox.isVisible = isMultiSelecting
        binding.checkbox.isChecked = isSelected
        binding.checkbox.setOnClickListener { binding.itemContainer.performClick() }

        val selectedColor = context.getThemeColor(UR.attr.primary_ui_02_selected)
        val unselectedColor = context.getThemeColor(UR.attr.primary_ui_01)
        binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
            binding.itemContainer.setBackgroundColor(if (isMultiSelecting && isChecked) selectedColor else unselectedColor)
        }
        binding.itemContainer.setBackgroundColor(if (isMultiSelecting && isSelected) selectedColor else unselectedColor)

        binding.reorder.visibility = if (isMultiSelecting) View.INVISIBLE else View.VISIBLE
        binding.reorder.updateLayoutParams<ConstraintLayout.LayoutParams> { // Adjust the spacing of the play button to avoid line wrapping when turning on multiselect
            rightMargin = if (isMultiSelecting) -binding.checkbox.marginLeft else 0.dpToPx(itemView.context)
            width = if (isMultiSelecting) 16.dpToPx(itemView.context) else 52.dpToPx(itemView.context)
        }
        binding.imageCardView.radius = cardCornerRadius
        binding.imageCardView.elevation = cardElevation
    }

    private fun bindEpisode(episode: BaseEpisode) {
        episodeInstance = episode
        binding.title.text = episode.title
        binding.downloaded.isVisible = episode.isDownloaded
        binding.info.setEpisodeTimeLeft(episode)
    }

    fun clearDisposable() {
        disposable?.dispose()
    }

    override val episodeRow: ViewGroup
        get() = binding.itemContainer
    override val episode: BaseEpisode?
        get() = episodeInstance
    override val positionAdapter: Int
        get() = bindingAdapterPosition
    override val leftRightIcon1: ImageView
        get() = binding.leftRightIcon1
    override val leftRightIcon2: ImageView
        get() = binding.leftRightIcon2
    override val rightLeftIcon1: ImageView
        get() = binding.rightLeftIcon1
    override val rightLeftIcon2: ImageView
        get() = binding.rightLeftIcon2
    override val isMultiSelecting: Boolean
        get() = binding.checkbox.isVisible
    override val rightToLeftSwipeLayout: ViewGroup
        get() = binding.rightToLeftSwipeLayout
    override val leftToRightSwipeLayout: ViewGroup
        get() = binding.leftToRightSwipeLayout
    override val upNextAction: Settings.UpNextAction
        get() = Settings.UpNextAction.PLAY_NEXT
    override val leftIconDrawablesRes: List<EpisodeItemTouchHelper.IconWithBackground>
        get() = listOf(
            EpisodeItemTouchHelper.IconWithBackground(R.drawable.ic_upnext_movetotop, binding.itemContainer.context.getThemeColor(UR.attr.support_04)),
            EpisodeItemTouchHelper.IconWithBackground(R.drawable.ic_upnext_movetobottom, binding.itemContainer.context.getThemeColor(UR.attr.support_03)),
        )
    override val rightIconDrawablesRes: List<EpisodeItemTouchHelper.IconWithBackground>
        get() = listOf(EpisodeItemTouchHelper.IconWithBackground(R.drawable.ic_upnext_remove, binding.itemContainer.context.getThemeColor(UR.attr.support_05)))
}
