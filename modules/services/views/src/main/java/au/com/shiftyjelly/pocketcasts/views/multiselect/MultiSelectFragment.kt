package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.lifecycle.toLiveData
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.Tracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.databinding.FragmentMultiselectBinding
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.MultiSelectViewOverflowMenuRearrangeActionMovedEvent
import com.automattic.eventhorizon.MultiSelectViewOverflowMenuRearrangeFinishedEvent
import com.automattic.eventhorizon.ShelfActionSource
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.BackpressureStrategy
import java.util.Collections
import javax.inject.Inject
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class MultiSelectFragment :
    BaseFragment(),
    MultiSelectTouchCallback.ItemTouchHelperAdapter {
    @Inject lateinit var settings: Settings

    @Inject lateinit var eventHorizon: EventHorizon

    @Inject lateinit var multiSelectEpisodesHelper: MultiSelectEpisodesHelper

    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_KEY)

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
            theme = theme,
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
                val multiSelectActions: MutableList<Any> = MultiSelectEpisodeAction.listFromIds(it).toMutableList()

                if (!args.shouldShowRemoveListeningHistory) {
                    multiSelectActions.removeAll { action ->
                        action is MultiSelectEpisodeAction.RemoveListeningHistory
                    }
                }

                multiSelectActions.add(0, shortcutTitle)
                multiSelectActions.add(multiSelectEpisodesHelper.maxToolbarIcons + 1, overflowTitle)

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
        listData.add(multiSelectEpisodesHelper.maxToolbarIcons + 1, overflowTitle)
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

    private fun sectionTitleAt(position: Int) = if (position <= multiSelectEpisodesHelper.maxToolbarIcons) {
        ShelfActionSource.Shelf
    } else {
        ShelfActionSource.OverflowMenu
    }

    private fun trackRearrangeFinishedEvent() {
        eventHorizon.track(
            MultiSelectViewOverflowMenuRearrangeFinishedEvent(
                source = args.source.eventHorizonValue,
            ),
        )
    }

    private fun trackItemMovedEvent(position: Int) {
        dragStartPosition?.let {
            val title = (items[position] as? MultiSelectAction)?.analyticsValue ?: Tracker.INVALID_OR_NULL_VALUE
            val movedFrom = sectionTitleAt(it)
            val movedTo = sectionTitleAt(position)
            val newPosition = if (movedTo == ShelfActionSource.Shelf) {
                position - 1
            } else {
                position - (items.indexOf(multiSelectEpisodesHelper.maxToolbarIcons) + 1)
            }
            eventHorizon.track(
                MultiSelectViewOverflowMenuRearrangeActionMovedEvent(
                    source = args.source.eventHorizonValue,
                    title = title,
                    position = newPosition.toLong(),
                    movedFrom = movedFrom,
                    movedTo = movedTo,
                ),
            )
            dragStartPosition = null
        }
    }

    @Parcelize
    private class Args(
        val source: SourceView,
        val shouldShowRemoveListeningHistory: Boolean,
    ) : Parcelable

    companion object {
        const val NEW_INSTANCE_KEY = "new_instance_key"

        fun newInstance(
            source: SourceView,
            shouldShowRemoveListeningHistory: Boolean,
        ) = MultiSelectFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_KEY to Args(source, shouldShowRemoveListeningHistory))
        }
    }
}
