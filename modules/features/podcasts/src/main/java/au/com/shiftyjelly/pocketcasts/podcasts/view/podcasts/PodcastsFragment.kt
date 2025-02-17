package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentPodcastsBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderCreateFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderCreateSharedViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderEditFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderEditPodcastsFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.SuggestedFolders
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.SuggestedFoldersPaywallBottomSheet
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastsViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.search.SearchFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.hideShadow
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.adapter.PodcastTouchCallback
import au.com.shiftyjelly.pocketcasts.views.extensions.showIf
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragmentToolbar.ChromeCastButton.Shown
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class PodcastsFragment : BaseFragment(), FolderAdapter.ClickListener, PodcastTouchCallback.ItemTouchHelperAdapter, Toolbar.OnMenuItemClickListener {

    companion object {
        private const val LAST_ORIENTATION_NOT_SET = -1
        private const val SOURCE_KEY = "source"
        private const val PODCASTS_LIST = "podcasts_list"
        private const val SORT_ORDER_KEY = "sort_order"
        private const val OPTION_KEY = "option"
        private const val SORT_BY = "sort_by"
        private const val EDIT_FOLDER = "edit_folder"
        const val ARG_FOLDER_UUID = "ARG_FOLDER_UUID"

        fun newInstance(folderUuid: String): PodcastsFragment {
            return PodcastsFragment().apply {
                arguments = bundleOf(
                    ARG_FOLDER_UUID to folderUuid,
                )
            }
        }
    }

    @Inject lateinit var settings: Settings

    @Inject lateinit var castManager: CastManager

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    private var podcastOptionsDialog: PodcastsOptionsDialog? = null
    private var folderOptionsDialog: FolderOptionsDialog? = null
    private var adapter: FolderAdapter? = null

    private var realBinding: FragmentPodcastsBinding? = null
    private val binding: FragmentPodcastsBinding get() = realBinding ?: throw IllegalStateException("Trying to access the binding outside of the view lifecycle.")

    private val viewModel: PodcastsViewModel by viewModels()
    private val sharedViewModel: FolderCreateSharedViewModel by activityViewModels()

    private var lastOrientationRefreshed = LAST_ORIENTATION_NOT_SET
    private var lastWidthPx: Int = 0
    private var listState: Parcelable? = null

    private val folderUuid: String?
        get() = arguments?.getString(ARG_FOLDER_UUID)

    private var gridOuterPadding: Int = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context ?: return null
        realBinding = FragmentPodcastsBinding.inflate(inflater, container, false)

        if (adapter == null) {
            adapter = FolderAdapter(this, settings, context, theme)
        }

        binding.appBarLayout.hideShadow()

        gridOuterPadding = resources.getDimensionPixelSize(VR.dimen.grid_outer_padding)
        binding.recyclerView.let {
            it.adapter = adapter
            it.addItemDecoration(SpaceItemDecoration())
            ItemTouchHelper(PodcastTouchCallback(this, context)).attachToRecyclerView(it)
        }

        viewModel.folderState.observe(viewLifecycleOwner) { folderState ->
            if (folderUuid != null && folderState.folder == null) {
                return@observe
            }
            val folder = folderState.folder
            val rootFolder = folder == null
            val isSignedInAsPlusOrPatron = folderState.isSignedInAsPlusOrPatron
            val toolbar = binding.toolbar

            val toolbarColors: ToolbarColors
            val navigationIcon: NavigationIcon
            if (folder == null) {
                toolbarColors = ToolbarColors.theme(theme = theme, context = context, excludeMenuItems = listOf(R.id.folders_locked))
                navigationIcon = NavigationIcon.None
            } else {
                toolbarColors = ToolbarColors.user(color = folder.getColor(context), theme = theme)
                navigationIcon = NavigationIcon.BackArrow
            }
            setupToolbarAndStatusBar(
                toolbar = toolbar,
                title = folder?.name ?: getString(LR.string.podcasts),
                toolbarColors = toolbarColors,
                navigationIcon = navigationIcon,
            )

            toolbar.menu.findItem(R.id.folders_locked)?.isVisible = !isSignedInAsPlusOrPatron
            toolbar.menu.findItem(R.id.create_folder)?.isVisible = rootFolder && isSignedInAsPlusOrPatron
            toolbar.menu.findItem(R.id.search_podcasts)?.isVisible = rootFolder

            adapter?.setFolderItems(folderState.items)

            val isEmpty = folderState.items.isEmpty()
            binding.emptyViewPodcasts.showIf(isEmpty && rootFolder)
            binding.emptyViewFolders.showIf(isEmpty && !rootFolder)
            binding.swipeRefreshLayout.showIf(!isEmpty)
        }

        viewModel.layoutChangedLiveData.observe(viewLifecycleOwner) {
            setupGridView()
        }

        viewModel.podcastUuidToBadge.observe(viewLifecycleOwner) { podcastUuidToBadge ->
            adapter?.badgeType = settings.podcastBadgeType.value
            adapter?.setBadges(podcastUuidToBadge)
        }

        viewModel.refreshObservable.observe(viewLifecycleOwner) {
            // Once the refresh is complete stop the swipe to refresh animation
            if (it !is RefreshState.Refreshing) {
                realBinding?.swipeRefreshLayout?.isRefreshing = false
            }
        }

        sharedViewModel.folderUuidLive.observe(viewLifecycleOwner) { newFolderUuid ->
            // after creating a folder open it
            val inRootFolder = folderUuid == null

            if (inRootFolder && newFolderUuid != null) {
                sharedViewModel.folderUuid = null
                onFolderClick(newFolderUuid, isUserInitiated = false)
            }
        }

        if (!viewModel.isFragmentChangingConfigurations) {
            folderUuid?.let { viewModel.trackFolderShown(it) } ?: viewModel.trackPodcastsListShown()
        }

        val toolbar = binding.toolbar
        setupToolbarAndStatusBar(
            toolbar = toolbar,
            menu = R.menu.podcasts_menu,
            chromeCastButton = Shown(chromeCastAnalytics),
        )
        toolbar.setOnMenuItemClickListener(this)

        toolbar.menu.findItem(R.id.folders_locked).setOnMenuItemClickListener {
            if (FeatureFlag.isEnabled(Feature.SUGGESTED_FOLDERS)) {
                SuggestedFoldersPaywallBottomSheet().show(childFragmentManager, "suggested_folders_paywall")
            } else {
                OnboardingLauncher.openOnboardingFlow(activity, OnboardingFlow.Upsell(OnboardingUpgradeSource.FOLDERS))
            }
            true
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshPodcasts()
        }

        binding.btnDiscover.setOnClickListener {
            analyticsTracker.track(AnalyticsEvent.PODCASTS_LIST_DISCOVER_BUTTON_TAPPED)
            (activity as FragmentHostListener).openTab(VR.id.navigation_discover)
        }

        binding.addToFolderButton.setOnClickListener {
            val folder = viewModel.folder ?: return@setOnClickListener
            FolderEditPodcastsFragment.newInstance(folderUuid = folder.uuid).show(parentFragmentManager, "add_podcasts_card")
        }

        viewModel.refreshSuggestedFolders()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.loadSuggestedFolders()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userSuggestedFoldersState.collect { (signInState, state) ->
                    if (state is PodcastsViewModel.SuggestedFoldersState.Loaded) {
                        val existingModal = parentFragmentManager.findFragmentByTag("suggested_folders_paywall")
                        if (viewModel.showSuggestedFoldersPaywallOnOpen(signInState.isSignedInAsPlusOrPatron) && existingModal == null) {
                            SuggestedFoldersPaywallBottomSheet().show(parentFragmentManager, "suggested_folders_paywall")
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        listState = binding.recyclerView.layoutManager?.onSaveInstanceState()
        binding.recyclerView.adapter = null
        super.onDestroyView()
        realBinding = null
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.more_options -> {
                val event = folderUuid?.let { AnalyticsEvent.FOLDER_OPTIONS_BUTTON_TAPPED } ?: AnalyticsEvent.PODCASTS_LIST_OPTIONS_BUTTON_TAPPED
                analyticsTracker.track(event)
                openOptions()
                true
            }
            R.id.search_podcasts -> {
                search()
                true
            }
            R.id.create_folder -> {
                analyticsTracker.track(AnalyticsEvent.PODCASTS_LIST_FOLDER_BUTTON_TAPPED)
                createFolder()
                true
            }
            else -> false
        }
    }

    private fun search() {
        val searchFragment = SearchFragment.newInstance(source = SourceView.PODCAST_LIST)
        (activity as FragmentHostListener).addFragment(searchFragment, onTop = true)
        realBinding?.recyclerView?.smoothScrollToPosition(0)
    }

    private fun openOptions() {
        if (viewModel.isFolderOpen()) {
            val folder = viewModel.folder ?: return
            val onOpenSortOptions = {
                analyticsTracker.track(AnalyticsEvent.FOLDER_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to SORT_BY))
            }
            val onSortTypeChanged = { sort: PodcastsSortType ->
                analyticsTracker.track(AnalyticsEvent.FOLDER_SORT_BY_CHANGED, mapOf(SORT_ORDER_KEY to sort.analyticsValue))
                viewModel.updateFolderSort(folder.uuid, sort)
            }
            val onEditFolder = {
                analyticsTracker.track(AnalyticsEvent.FOLDER_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to EDIT_FOLDER))
                analyticsTracker.track(AnalyticsEvent.FOLDER_EDIT_SHOWN)
                val fragment = FolderEditFragment.newInstance(folderUuid = folder.uuid)
                fragment.show(parentFragmentManager, "edit_folder_card")
            }
            val onAddOrRemovePodcast = {
                analyticsTracker.track(AnalyticsEvent.FOLDER_ADD_PODCASTS_BUTTON_TAPPED)
                analyticsTracker.track(AnalyticsEvent.FOLDER_CHOOSE_PODCASTS_SHOWN)
                val fragment = FolderEditPodcastsFragment.newInstance(folderUuid = folder.uuid)
                fragment.show(parentFragmentManager, "add_podcasts_card")
            }
            folderOptionsDialog = FolderOptionsDialog(folder, onOpenSortOptions, onSortTypeChanged, onEditFolder, onAddOrRemovePodcast, this, settings).apply {
                show()
            }
        } else {
            podcastOptionsDialog = PodcastsOptionsDialog(this, settings, analyticsTracker).apply {
                show()
            }
        }
    }

    private fun createFolder() {
        if (FeatureFlag.isEnabled(Feature.SUGGESTED_FOLDERS)) {
            SuggestedFolders().show(parentFragmentManager, "suggested_folders")
        } else {
            FolderCreateFragment.newInstance(PODCASTS_LIST).show(parentFragmentManager, "create_folder_card")
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
        podcastOptionsDialog?.dismiss()
        folderOptionsDialog?.dismiss()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        realBinding?.recyclerView?.post {
            activity?.let {
                adjustViewIfNeeded()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.setFolderUuid(folderUuid)

        adjustViewIfNeeded()
    }

    private fun adjustViewIfNeeded() {
        val context = activity ?: return
        val orientation = resources.configuration.orientation
        val widthPx = UiUtil.getWindowWidthPx(context)
        if (orientation == lastOrientationRefreshed && lastWidthPx == widthPx) return

        // screen has rotated, redraw the grid to the right size
        setupGridView()

        lastOrientationRefreshed = orientation
        lastWidthPx = widthPx
    }

    private fun setupGridView(savedInstanceState: Parcelable? = listState) {
        val layoutManager = when (settings.podcastGridLayout.value) {
            PodcastGridLayoutType.LARGE_ARTWORK -> GridLayoutManager(activity, UiUtil.getGridColumnCount(false, context))
            PodcastGridLayoutType.SMALL_ARTWORK -> GridLayoutManager(activity, UiUtil.getGridColumnCount(true, context))
            PodcastGridLayoutType.LIST_VIEW -> LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        }
        val badgeType = settings.podcastBadgeType.value
        val currentLayoutManager = realBinding?.recyclerView?.layoutManager

        // We only want to reset the adapter if something actually changed, or else it will flash
        if (adapter?.badgeType != badgeType ||
            (currentLayoutManager != null && currentLayoutManager::class.java != layoutManager::class.java) ||
            (currentLayoutManager is GridLayoutManager && layoutManager is GridLayoutManager && currentLayoutManager.spanCount != layoutManager.spanCount)
        ) {
            adapter?.badgeType = badgeType
            realBinding?.recyclerView?.adapter = adapter
        }

        viewLifecycleOwner.lifecycleScope.launch {
            settings.bottomInset.collect {
                val gridOuterPadding = if (settings.podcastGridLayout.value == PodcastGridLayoutType.LIST_VIEW) 0 else gridOuterPadding
                realBinding?.recyclerView?.updatePadding(gridOuterPadding, gridOuterPadding, gridOuterPadding, gridOuterPadding + it)
            }
        }

        realBinding?.recyclerView?.layoutManager = layoutManager
        layoutManager.onRestoreInstanceState(savedInstanceState)
    }

    override fun onPodcastMove(fromPosition: Int, toPosition: Int) {
        val newList = viewModel.moveFolderItem(fromPosition, toPosition)
        adapter?.submitList(newList)
    }

    override fun onPodcastMoveFinished() {
        viewModel.commitMoves()
        analyticsTracker.track(AnalyticsEvent.PODCASTS_LIST_REORDERED)
    }

    override fun onPodcastClick(podcast: Podcast, view: View) {
        analyticsTracker.track(AnalyticsEvent.PODCASTS_LIST_PODCAST_TAPPED)
        val fragment = PodcastFragment.newInstance(podcastUuid = podcast.uuid, sourceView = SourceView.PODCAST_LIST)
        (activity as FragmentHostListener).addFragment(fragment)
    }

    override fun onFolderClick(folderUuid: String, isUserInitiated: Boolean) {
        if (isUserInitiated) {
            analyticsTracker.track(AnalyticsEvent.PODCASTS_LIST_FOLDER_TAPPED)
        }
        val fragment = newInstance(folderUuid = folderUuid)
        (activity as FragmentHostListener).addFragment(fragment)
    }

    inner class SpaceItemDecoration : RecyclerView.ItemDecoration() {
        private val spacing = resources.getDimensionPixelSize(VR.dimen.grid_item_padding)
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            val margin = if (settings.podcastGridLayout.value != PodcastGridLayoutType.LIST_VIEW) spacing else 0
            outRect.set(margin, margin, margin, margin)
        }
    }
}
