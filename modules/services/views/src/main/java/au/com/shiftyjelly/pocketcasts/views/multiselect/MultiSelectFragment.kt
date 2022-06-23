package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LiveDataReactiveStreams
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.SimpleItemAnimator
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

    private val adapter = MultiSelectAdapter(editable = true, listener = null, dragListener = this::onItemStartDrag)
    private lateinit var itemTouchHelper: ItemTouchHelper
    private var items = emptyList<Any>()
    private val shortcutTitle = MultiSelectAdapter.Title(LR.string.multiselect_actions_shown)
    private val overflowTitle = MultiSelectAdapter.Title(LR.string.multiselect_actions_hidden)
    private var binding: FragmentMultiselectBinding? = null

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
            onNavigationClick = { (activity as? FragmentHostListener)?.closeModal(this) },
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

        LiveDataReactiveStreams.fromPublisher(settings.multiSelectItemsObservable.toFlowable(BackpressureStrategy.LATEST))
            .observe(viewLifecycleOwner) {
                val multiSelectActions: MutableList<Any> = MultiSelectAction.listFromIds(it).toMutableList()

                multiSelectActions.add(0, shortcutTitle)
                multiSelectActions.add(MultiSelectToolbar.MAX_ICONS + 1, overflowTitle)

                items = multiSelectActions.toList()
                adapter.submitList(multiSelectActions.toList())
            }
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
        itemTouchHelper.startDrag(viewHolder)
    }

    override fun onItemTouchFinished() {
        settings.setMultiSelectItems(items.filterIsInstance<MultiSelectAction>().map { it.groupId })
    }
}
