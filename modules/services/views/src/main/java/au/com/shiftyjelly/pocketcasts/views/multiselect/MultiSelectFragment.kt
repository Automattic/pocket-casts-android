package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.toLiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.databinding.FragmentMultiselectBinding
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.BackpressureStrategy
import timber.log.Timber
import java.util.Collections
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class MultiSelectFragment : BaseFragment(), MultiSelectTouchCallback.ItemTouchHelperAdapter {
    @Inject lateinit var settings: Settings
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper
    private val source: String
        get() = arguments?.getString(ARG_SOURCE) ?: AnalyticsSource.UNKNOWN.analyticsValue

    private val adapter = MultiSelectAdapter(editable = true, listener = null, dragListener = this::onItemStartDrag)
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var items = emptyList<Any>()
    private val shortcutTitle = MultiSelectAdapter.Title(LR.string.multiselect_actions_shown)
    private val overflowTitle = MultiSelectAdapter.Title(LR.string.multiselect_actions_hidden)
    private var binding: FragmentMultiselectBinding? = null
    private var dragStartPosition: Int? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentMultiselectBinding.inflate(inflater, container, false)
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
        toolbar.setup(
            title = getString(LR.string.rearrange_actions),
            navigationIcon = NavigationIcon.BackArrow,
            onNavigationClick = {
                trackRearrangeFinishedEvent()
                (activity as? FragmentHostListener)?.closeModal(this)
            },
            activity = activity,
            theme = theme
        )

        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.changeDuration = 0

        val callback = MultiSelectTouchCallback(listener = this)
        itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        settings.multiSelectItemsObservable.toFlowable(BackpressureStrategy.LATEST).toLiveData()
            .observe(viewLifecycleOwner) {
                val multiSelectActions: MutableList<Any> = MultiSelectAction.listFromIds(it).toMutableList()

                multiSelectActions.add(0, shortcutTitle)
                multiSelectActions.add(MultiSelectToolbar.MAX_ICONS + 1, overflowTitle)

                items = multiSelectActions.toList()
                adapter.submitList(multiSelectActions.toList())
            }
    }

    override fun onBackPressed(): Boolean {
        trackRearrangeFinishedEvent()
        return super.onBackPressed()
    }

    override fun onItemMove(fromPosition: Int, toPosition: Int) {
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
        listData.remove(shortcutTitle)
        listData.remove(overflowTitle)
        listData.add(MultiSelectToolbar.MAX_ICONS + 1, overflowTitle)
        listData.add(0, shortcutTitle)

        adapter.submitList(listData)
        items = listData.toList()

        Timber.d("Swapped: $items")
    }

    override fun onItemStartDrag(viewHolder: MultiSelectAdapter.ItemViewHolder) {
        dragStartPosition = viewHolder.bindingAdapterPosition
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onItemTouchFinished(position: Int) {
        settings.setMultiSelectItems(items.filterIsInstance<MultiSelectAction>().map { it.groupId })
        trackItemMovedEvent(position)
    }

    private fun sectionTitleAt(position: Int) =
        if (position <= MultiSelectToolbar.MAX_ICONS) AnalyticsProp.Value.SHELF else AnalyticsProp.Value.OVERFLOW_MENU

    private fun trackRearrangeFinishedEvent() {
        analyticsTracker.track(
            AnalyticsEvent.MULTI_SELECT_VIEW_OVERFLOW_MENU_REARRANGE_FINISHED,
            mapOf(AnalyticsProp.Key.SOURCE to source)
        )
    }

    private fun trackItemMovedEvent(position: Int) {
        dragStartPosition?.let {
            val title = (items[position] as? MultiSelectAction)?.analyticsValue
                ?: AnalyticsProp.Value.UNKNOWN
            val movedFrom = sectionTitleAt(it)
            val movedTo = sectionTitleAt(position)
            val newPosition = if (movedTo == AnalyticsProp.Value.SHELF) {
                position - 1
            } else {
                position - (items.indexOf(MultiSelectToolbar.MAX_ICONS) + 1)
            }
            analyticsTracker.track(
                AnalyticsEvent.MULTI_SELECT_VIEW_OVERFLOW_MENU_REARRANGE_ACTION_MOVED,
                mapOf(
                    AnalyticsProp.Key.ACTION to title,
                    AnalyticsProp.Key.POSITION to newPosition, // it is the new position in section it was moved to
                    AnalyticsProp.Key.MOVED_FROM to movedFrom,
                    AnalyticsProp.Key.MOVED_TO to movedTo,
                    AnalyticsProp.Key.SOURCE to source,
                )
            )
            dragStartPosition = null
        }
    }

    private object AnalyticsProp {
        object Key {
            const val ACTION = "action"
            const val MOVED_FROM = "moved_from"
            const val MOVED_TO = "moved_to"
            const val POSITION = "position"
            const val SOURCE = "source"
        }

        object Value {
            const val SHELF = "shelf"
            const val OVERFLOW_MENU = "overflow_menu"
            const val UNKNOWN = "unknown"
        }
    }

    companion object {
        private const val ARG_SOURCE = "source"
        fun newInstance(source: AnalyticsSource) = MultiSelectFragment().apply {
            arguments = bundleOf(
                ARG_SOURCE to source.analyticsValue,
            )
        }
    }
}
