package au.com.shiftyjelly.pocketcasts.filters

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.filters.databinding.FragmentFiltersBinding
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragmentToolbar.ChromeCastButton
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.None
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class FiltersFragment : BaseFragment(), CoroutineScope, Toolbar.OnMenuItemClickListener {
    @Inject lateinit var settings: Settings

    @Inject lateinit var playlistManager: PlaylistManager

    @Inject lateinit var castManager: CastManager

    private val viewModel: FiltersFragmentViewModel by viewModels()
    private var trackFilterListShown = false
    var filterCount: Int? = null
    var lastFilterUuidShown: String? = null
    var previousLastFilter: Playlist? = null

    private var binding: FragmentFiltersBinding? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentFiltersBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    override fun onDestroyView() {
        binding?.recyclerView?.adapter = null
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = binding ?: return

        setupToolbarAndStatusBar(
            toolbar = binding.toolbar,
            title = getString(LR.string.filters),
            menu = R.menu.menu_filters,
            chromeCastButton = if (FeatureFlag.isEnabled(Feature.UPNEXT_IN_TAB_BAR)) {
                ChromeCastButton.None
            } else {
                ChromeCastButton.Shown(chromeCastAnalytics)
            },
            navigationIcon = None,
        )
        binding.toolbar.setOnMenuItemClickListener(this)
        binding.toolbar.menu.findItem(R.id.media_route_menu_item).isVisible = !FeatureFlag.isEnabled(Feature.UPNEXT_IN_TAB_BAR)

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        val adapter = FiltersAdapter(viewModel.countGenerator) {
            openPlaylist(it)
        }
        recyclerView.adapter = adapter
        viewModel.filters.observe(viewLifecycleOwner) {
            val filterCount = this.filterCount
            // Work out if a new filter has been added since last time, if yes we open it
            if (filterCount != null && it.size > filterCount && it.isNotEmpty() && it.last() != previousLastFilter && it.last().uuid != lastFilterUuidShown) {
                openPlaylist(it.last(), isNewFilter = true) // Open the newly added playlist
            }

            this.filterCount = it.size
            if (trackFilterListShown) {
                viewModel.trackFilterListShown(it.size)
                trackFilterListShown = false
            }

            previousLastFilter = it.lastOrNull()
            viewModel.adapterState = it.toMutableList()
            adapter.submitList(it)
        }

        val touchHelperCallback = FiltersListItemTouchCallback({ from, to ->
            val newList = viewModel.movePlaylist(from, to)
            adapter.submitList(newList)
        }) { from, to ->
            viewModel.commitMoves(from != to)
        }
        val itemTouchHelper = ItemTouchHelper(touchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        checkForSavedFilter()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.filter_create -> {
                openCreate()
                true
            }
            else -> false
        }
    }

    @Suppress("DEPRECATION")
    override fun setUserVisibleHint(visible: Boolean) {
        super.setUserVisibleHint(visible)
        if (visible && isAdded) {
            checkForSavedFilter()
        }
    }

    private fun checkForSavedFilter() {
        val shouldOpenSavedFilter = settings.selectedFilter() != null && lastFilterUuidShown != settings.selectedFilter()
        if (shouldOpenSavedFilter) {
            val playlistUuid = settings.selectedFilter() ?: return
            lastFilterUuidShown = playlistUuid
            viewModel.findPlaylistByUuid(playlistUuid) { playlist ->
                openPlaylist(playlist, isNewFilter = false)
            }
        } else if (!viewModel.isFragmentChangingConfigurations) {
            // Not showing a specific filter, so track showing of the filter list if not just a configuration change
            filterCount.let {
                if (it != null) {
                    viewModel.trackFilterListShown(it)
                } else {
                    trackFilterListShown = true
                }
            }
        }
    }

    private fun openCreate() {
        val fragment = CreateFilterContainerFragment.newInstance()
        (activity as FragmentHostListener).showModal(fragment)
    }

    fun openPlaylist(playlist: Playlist, isNewFilter: Boolean = false) {
        val context = context ?: return

        lastFilterUuidShown = playlist.uuid
        settings.setSelectedFilter(playlist.uuid)

        val playlistFragment = FilterEpisodeListFragment.newInstance(playlist, isNewFilter, context)
        (activity as? FragmentHostListener)?.addFragment(playlistFragment)

        playlistFragment.view?.requestFocus() // Jump to new page for talk back
    }
}

private class FiltersListItemTouchCallback(
    val onMoveListener: (from: Int, to: Int) -> Unit,
    val onFinish: (from: Int?, to: Int?) -> Unit,
) : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP.or(ItemTouchHelper.DOWN), 0) {
    private var moveFrom: Int? = null
    private var moveTo: Int? = null

    override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
        // Only update moveFrom if it is not initialized because it represents the position where the move started
        if (moveFrom == null) {
            moveFrom = viewHolder.bindingAdapterPosition
        }

        // Always update moveTo because it represents the final position
        moveTo = target.bindingAdapterPosition

        onMoveListener(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {}

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        onFinish(moveFrom, moveTo)
        moveFrom = null
        moveTo = null
    }
}
