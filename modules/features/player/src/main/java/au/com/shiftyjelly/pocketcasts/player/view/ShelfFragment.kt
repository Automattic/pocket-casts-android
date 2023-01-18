package au.com.shiftyjelly.pocketcasts.player.view

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterShelfItemBinding
import au.com.shiftyjelly.pocketcasts.player.databinding.AdapterShelfTitleBinding
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentShelfBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.setRippleBackground
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class ShelfFragment : BaseFragment(), ShelfTouchCallback.ItemTouchHelperAdapter {
    private var items = emptyList<Any>()

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    @Inject lateinit var settings: Settings

    private lateinit var itemTouchHelper: ItemTouchHelper
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val adapter = ShelfAdapter(editable = true, dragListener = this::onShelfItemStartDrag)
    private val shortcutTitle = ShelfTitle(LR.string.player_rearrange_actions_shown)
    private val moreActionsTitle = ShelfTitle(LR.string.player_rearrange_actions_hidden)
    private var binding: FragmentShelfBinding? = null
    private var dragStartPosition: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentShelfBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        val toolbar = binding.toolbar
        toolbar.setTitle(LR.string.rearrange_actions)
        toolbar.setTitleTextColor(toolbar.context.getThemeColor(UR.attr.player_contrast_01))
        toolbar.setNavigationOnClickListener {
            trackRearrangeFinishedEvent()
            (activity as? FragmentHostListener)?.closeModal(this)
        }
        toolbar.navigationIcon?.setTint(ThemeColor.playerContrast01(theme.activeTheme))

        val backgroundColor = theme.playerBackground2Color(playerViewModel.podcast)
        val toolbarColor = theme.playerBackgroundColor(playerViewModel.podcast)
        val selectedColor = theme.playerHighlight7Color(playerViewModel.podcast)

        view.setBackgroundColor(backgroundColor)
        adapter.normalBackground = backgroundColor
        toolbar.setBackgroundColor(toolbarColor)
        adapter.selectedBackground = ColorUtils.calculateCombinedColor(backgroundColor, selectedColor)

        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.changeDuration = 0

        val callback = ShelfTouchCallback(listener = this)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        playerViewModel.shelfLive.observe(viewLifecycleOwner) {
            val itemsPlusTitles = mutableListOf<Any>()
            itemsPlusTitles.addAll(it)
            itemsPlusTitles.add(4, moreActionsTitle)
            itemsPlusTitles.add(0, shortcutTitle)
            items = itemsPlusTitles
            adapter.submitList(items)
        }

        playerViewModel.playingEpisodeLive.observe(viewLifecycleOwner) { (playable, _) ->
            adapter.playable = playable
        }
    }

    override fun onBackPressed(): Boolean {
        trackRearrangeFinishedEvent()
        return super.onBackPressed()
    }

    override fun onShelfItemMove(fromPosition: Int, toPosition: Int) {
        val listData = items.toMutableList()

        Timber.d("Swapping $fromPosition to $toPosition")
        Timber.d("List: $listData")

        if (fromPosition < toPosition) {
            for (index in fromPosition until toPosition) {
                Collections.swap(listData, index, index + 1)
            }
        } else {
            for (index in fromPosition downTo toPosition + 1) {
                Collections.swap(listData, index, index - 1)
            }
        }

        // Make sure the titles are in the right spot
        listData.remove(moreActionsTitle)
        listData.remove(shortcutTitle)
        listData.add(4, moreActionsTitle)
        listData.add(0, shortcutTitle)

        adapter.submitList(listData)
        items = listData.toList()

        Timber.d("Swapped: $items")
    }

    override fun onShelfItemStartDrag(viewHolder: ShelfAdapter.ItemViewHolder) {
        dragStartPosition = viewHolder.bindingAdapterPosition
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onShelfItemTouchHelperFinished(position: Int) {
        trackShelfItemMovedEvent(position)
        settings.setShelfItems(items.filterIsInstance<ShelfItem>().map { it.id })
    }

    private fun sectionTitleAt(position: Int) =
        if (position < items.indexOf(moreActionsTitle)) AnalyticsProp.Value.SHELF else AnalyticsProp.Value.OVERFLOW_MENU

    private fun trackShelfItemMovedEvent(position: Int) {
        dragStartPosition?.let {
            val title = (items[position] as? ShelfItem)?.analyticsValue
                ?: AnalyticsProp.Value.UNKNOWN
            val movedFrom = sectionTitleAt(it)
            val movedTo = sectionTitleAt(position)
            val newPosition = if (movedTo == AnalyticsProp.Value.SHELF) {
                position - 1
            } else {
                position - (items.indexOf(moreActionsTitle) + 1)
            }
            analyticsTracker.track(
                AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_ACTION_MOVED,
                mapOf(
                    AnalyticsProp.Key.ACTION to title,
                    AnalyticsProp.Key.POSITION to newPosition, // it is the new position in section it was moved to
                    AnalyticsProp.Key.MOVED_FROM to movedFrom,
                    AnalyticsProp.Key.MOVED_TO to movedTo
                )
            )
            dragStartPosition = null
        }
    }

    private fun trackRearrangeFinishedEvent() {
        analyticsTracker.track(AnalyticsEvent.PLAYER_SHELF_OVERFLOW_MENU_REARRANGE_FINISHED)
    }

    companion object {
        object AnalyticsProp {
            object Key {
                const val FROM = "from"
                const val ACTION = "action"
                const val MOVED_FROM = "moved_from"
                const val MOVED_TO = "moved_to"
                const val POSITION = "position"
            }
            object Value {
                const val SHELF = "shelf"
                const val OVERFLOW_MENU = "overflow_menu"
                const val UNKNOWN = "unknown"
            }
        }
    }
}

data class ShelfTitle(@StringRes val title: Int)

private val SHELF_ITEM_DIFF = object : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return if (oldItem is ShelfItem && newItem is ShelfItem) {
            oldItem.id == newItem.id
        } else {
            return oldItem == newItem
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return true
    }
}

class ShelfAdapter(val editable: Boolean, val listener: ((ShelfItem) -> Unit)? = null, val dragListener: ((ItemViewHolder) -> Unit)?) : ListAdapter<Any, RecyclerView.ViewHolder>(SHELF_ITEM_DIFF) {
    var playable: Playable? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    var normalBackground = Color.TRANSPARENT
    var selectedBackground = Color.BLACK

    class TitleViewHolder(val binding: AdapterShelfTitleBinding) : RecyclerView.ViewHolder(binding.root)

    inner class ItemViewHolder(val binding: AdapterShelfItemBinding) : RecyclerView.ViewHolder(binding.root), ShelfTouchCallback.ItemTouchHelperViewHolder {

        override fun onItemDrag() {
            AnimatorSet().apply {
                val backgroundView = itemView

                val elevation = ObjectAnimator.ofPropertyValuesHolder(backgroundView, PropertyValuesHolder.ofFloat(View.TRANSLATION_Z, 16.dpToPx(backgroundView.resources.displayMetrics).toFloat()))

                val color = ObjectAnimator.ofInt(backgroundView, "backgroundColor", normalBackground, selectedBackground)
                color.setEvaluator(ArgbEvaluator())

                playTogether(elevation, color)
                start()
            }
        }

        override fun onItemSwipe() {
        }

        override fun onItemClear() {
            AnimatorSet().apply {
                val backgroundView = itemView
                val elevation = ObjectAnimator.ofPropertyValuesHolder(backgroundView, PropertyValuesHolder.ofFloat(View.TRANSLATION_Z, 0.toFloat()))

                backgroundView.setRippleBackground(false)
                play(elevation)
                start()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.adapter_shelf_item -> {
                val binding = AdapterShelfItemBinding.inflate(inflater, parent, false)
                ItemViewHolder(binding)
            }
            R.layout.adapter_shelf_title -> {
                val binding = AdapterShelfTitleBinding.inflate(inflater, parent, false)
                TitleViewHolder(binding)
            }
            else -> throw IllegalStateException("Unknown view type in shelf")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        if (item is ShelfItem && holder is ItemViewHolder) {
            val binding = holder.binding

            binding.lblTitle.setText(item.title(playable))
            binding.imgIcon.setImageResource(item.iconRes(playable))
            binding.dragHandle.isVisible = editable

            if (listener != null) {
                holder.itemView.setOnClickListener { listener.invoke(item) }
            }

            val subtitle = item.subtitle
            binding.lblSubtitle.isVisible = editable && subtitle != null && playable is UserEpisode
            if (subtitle != null) {
                binding.lblSubtitle.setText(subtitle)
            }
            binding.lblTitle.updateLayoutParams<ConstraintLayout.LayoutParams> { bottomMargin = if (binding.lblSubtitle.isVisible) 0 else 8.dpToPx(binding.lblTitle.context) }

            binding.dragHandle.setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    dragListener?.invoke(holder)
                }
                false
            }
        } else if (item is ShelfTitle && holder is TitleViewHolder) {
            holder.binding.lblTitle.setText(item.title)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ShelfTitle -> R.layout.adapter_shelf_title
            is ShelfItem -> R.layout.adapter_shelf_item
            else -> throw IllegalStateException("Unknown item type in shelf")
        }
    }
}
