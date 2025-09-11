package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.ads.AdReportFragment
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.ad.AdBanner
import au.com.shiftyjelly.pocketcasts.compose.ad.rememberAdColors
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.TipPosition
import au.com.shiftyjelly.pocketcasts.compose.components.Tooltip
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentPodcastsBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderCreateFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderCreateSharedViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderEditFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderEditPodcastsFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.SuggestedFoldersFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.notifications.EnableNotificationsPromptFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastsViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.search.SearchFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.hideShadow
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.adapter.PodcastTouchCallback
import au.com.shiftyjelly.pocketcasts.views.extensions.quickScrollToTop
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragmentToolbar.ChromeCastButton.Shown
import au.com.shiftyjelly.pocketcasts.views.fragments.TopScrollable
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class PodcastsFragment :
    BaseFragment(),
    FolderAdapter.ClickListener,
    PodcastTouchCallback.ItemTouchHelperAdapter,
    Toolbar.OnMenuItemClickListener,
    TopScrollable {

    companion object {
        private const val LAST_ORIENTATION_NOT_SET = -1
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

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    private var podcastOptionsDialog: PodcastsOptionsDialog? = null
    private var folderOptionsDialog: FolderOptionsDialog? = null
    private var folderAdapter: FolderAdapter? = null
    private var bannerAdAdapter: BannerAdAdapter? = null
    private var adapter: ConcatAdapter? = null

    private var realBinding: FragmentPodcastsBinding? = null
    private val binding: FragmentPodcastsBinding get() = realBinding ?: throw IllegalStateException("Trying to access the binding outside of the view lifecycle.")

    private val viewModel: PodcastsViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<PodcastsViewModel.Factory> { factory ->
                factory.create(folderUuid)
            }
        },
    )
    private val sharedViewModel: FolderCreateSharedViewModel by activityViewModels()

    private var lastOrientationRefreshed = LAST_ORIENTATION_NOT_SET
    private var lastWidthPx: Int = 0
    private var listState: Parcelable? = null

    private val folderUuid: String?
        get() = arguments?.getString(ARG_FOLDER_UUID)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val context = context ?: return null
        realBinding = FragmentPodcastsBinding.inflate(inflater, container, false)

        val folderAdapter = FolderAdapter(
            clickListener = this,
            settings = settings,
            context = context,
            theme = theme,
        )
        this.folderAdapter = folderAdapter
        val bannerAdAdapter = BannerAdAdapter(
            themeType = theme.activeTheme,
            onAdClick = ::openAd,
            onAdOptionsClick = ::openAdReportFlow,
            onAdImpression = ::trackAdImpression,
        )
        this.bannerAdAdapter = bannerAdAdapter

        val config = ConcatAdapter.Config.Builder()
            .setIsolateViewTypes(false)
            .build()
        val adapter = ConcatAdapter(config, bannerAdAdapter, folderAdapter)
        this.adapter = adapter

        binding.appBarLayout.hideShadow()

        binding.recyclerView.let {
            it.adapter = adapter
            it.addItemDecoration(SpaceItemDecoration())
            ItemTouchHelper(PodcastTouchCallback(this, context)).attachToRecyclerView(it)
        }

        if (savedInstanceState == null) {
            viewModel.trackScreenShown()
        }

        val toolbar = binding.toolbar
        setupToolbarAndStatusBar(
            toolbar = toolbar,
            menu = R.menu.podcasts_menu,
            chromeCastButton = Shown(chromeCastAnalytics),
        )
        toolbar.setOnMenuItemClickListener(this)

        toolbar.menu.findItem(R.id.folders_locked).setOnMenuItemClickListener {
            if (viewModel.areSuggestedFoldersAvailable.value) {
                showSuggestedFoldersCreation(SuggestedFoldersFragment.Source.ToolbarButton)
            } else {
                OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.Upsell(OnboardingUpgradeSource.FOLDERS))
            }
            true
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshPodcasts()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.shouldShowTooltip()) {
                showTooltip()
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { uiState ->
                    if (folderUuid != null && uiState.folder == null) {
                        return@collect
                    }
                    val folder = uiState.folder
                    val rootFolder = folder == null
                    val isSignedInAsPlusOrPatron = uiState.isSignedInAsPlusOrPatron
                    val toolbar = binding.toolbar

                    val toolbarColors: ToolbarColors
                    val navigationIcon: NavigationIcon
                    if (folder == null) {
                        toolbarColors = ToolbarColors.theme(
                            theme = theme,
                            context = requireContext(),
                            excludeMenuItems = listOf(R.id.folders_locked),
                        )
                        navigationIcon = NavigationIcon.None
                    } else {
                        toolbarColors = ToolbarColors.user(color = folder.getColor(requireContext()), theme = theme)
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

                    folderAdapter?.setFolderItems(uiState.items)

                    val isEmpty = uiState.items.isEmpty()
                    binding.emptyView.isVisible = isEmpty && !uiState.isLoadingItems
                    binding.swipeRefreshLayout.isGone = isEmpty
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.activeAd.collect { activeAd ->
                bannerAdAdapter?.submitList(listOfNotNull(activeAd))
            }
        }

        setupEmptyStateView()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.layoutChangedFlow.collect {
                    setupGridView()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.podcastUuidToBadge.collect { podcastUuidToBadge ->
                    folderAdapter?.badgeType = settings.podcastBadgeType.value
                    folderAdapter?.setBadges(podcastUuidToBadge)
                }
            }
        }

        this.viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Once the refresh is complete stop the swipe to refresh animation
                viewModel.refreshStateFlow.collect { refreshState ->
                    // Once the refresh is complete stop the swipe to refresh animation
                    if (refreshState !is RefreshState.Refreshing) {
                        realBinding?.swipeRefreshLayout?.isRefreshing = false
                    }
                }
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.refreshSuggestedFolders()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.areSuggestedFoldersAvailable.combine(viewModel.notificationPromptState) { areFoldersAvailable, notificationsState ->
                    areFoldersAvailable to notificationsState
                }.collect { (areFoldersAvailable, notificationState) ->
                    // Don't stack popups, notification prompt takes precedence over suggested folders popup
                    if (!notificationState.hasPermission && !notificationState.hasShownPromptBefore && FeatureFlag.isEnabled(Feature.NOTIFICATIONS_REVAMP)) {
                        if (parentFragmentManager.findFragmentByTag("notifications_prompt") == null) {
                            EnableNotificationsPromptFragment
                                .newInstance()
                                .show(parentFragmentManager, "notifications_prompt")
                        }
                    } else {
                        (parentFragmentManager.findFragmentByTag("notifications_prompt") as? DialogFragment)?.dismiss()

                        if (areFoldersAvailable && viewModel.isEligibleForSuggestedFoldersPopup()) {
                            showSuggestedFoldersCreation(SuggestedFoldersFragment.Source.Popup)
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
                handleFolderCreation()
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
        if (folderUuid != null) {
            val folder = viewModel.uiState.value.folder ?: return
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

    private fun handleFolderCreation() {
        if (viewModel.areSuggestedFoldersAvailable.value) {
            showSuggestedFoldersCreation(SuggestedFoldersFragment.Source.ToolbarButton)
        } else {
            showCustomFolderCreation()
        }
    }

    private fun showCustomFolderCreation() {
        FolderCreateFragment.newInstance(PODCASTS_LIST).show(parentFragmentManager, "create_folder_card")
    }

    private fun showSuggestedFoldersCreation(
        source: SuggestedFoldersFragment.Source,
    ) {
        if (parentFragmentManager.findFragmentByTag("suggested_folders") == null) {
            SuggestedFoldersFragment
                .newInstance(source)
                .show(parentFragmentManager, "suggested_folders")
        }
    }

    override fun onPause() {
        super.onPause()
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

        viewModel.updateNotificationsPermissionState()

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

    private fun setupEmptyStateView() {
        val folderUuid = folderUuid

        val emptyViewVisibilityFlow = callbackFlow<Boolean> {
            val view = binding.emptyView
            var isVisible = view.isVisible
            send(isVisible)

            val listener = object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val newVisibility = view.isVisible
                    if (isVisible != newVisibility) {
                        isVisible = newVisibility
                    }
                    trySendBlocking(isVisible)
                }
            }
            view.viewTreeObserver.addOnGlobalLayoutListener(listener)
            awaitClose {
                view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
            }
        }

        binding.emptyView.setContentWithViewCompositionStrategy {
            val activeAd by viewModel.activeAd.collectAsState()
            val isViewVisible by emptyViewVisibilityFlow.collectAsState(false)

            AppTheme(themeType = theme.activeTheme) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                ) {
                    activeAd?.let { ad ->
                        AdBanner(
                            ad = ad,
                            colors = rememberAdColors().bannerAd,
                            onAdClick = { openAd(ad) },
                            onOptionsClick = { openAdReportFlow(ad) },
                        )

                        LaunchedEffect(ad.id, isViewVisible) {
                            if (isViewVisible) {
                                trackAdImpression(ad)
                            }
                        }
                    }

                    Spacer(
                        modifier = Modifier.weight(1f),
                    )

                    if (folderUuid != null) {
                        NoFolderPodcastsBanner(
                            onClickButton = {
                                FolderEditPodcastsFragment.newInstance(folderUuid).show(parentFragmentManager, "add_podcasts_card")
                            },
                        )
                    } else {
                        NoPodcastsBanner(
                            onClickButton = {
                                analyticsTracker.track(AnalyticsEvent.PODCASTS_LIST_DISCOVER_BUTTON_TAPPED)
                                (activity as FragmentHostListener).openTab(VR.id.navigation_discover)
                            },
                        )
                    }

                    Spacer(
                        modifier = Modifier.weight(2f),
                    )
                }
            }
        }
    }

    private fun setupGridView(savedInstanceState: Parcelable? = listState) {
        val layoutManager = when (settings.podcastGridLayout.value) {
            PodcastGridLayoutType.LARGE_ARTWORK -> createAdGridLayoutManager(UiUtil.getGridColumnCount(false, context))
            PodcastGridLayoutType.SMALL_ARTWORK -> createAdGridLayoutManager(UiUtil.getGridColumnCount(true, context))
            PodcastGridLayoutType.LIST_VIEW -> LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        }
        val badgeType = settings.podcastBadgeType.value
        val currentLayoutManager = realBinding?.recyclerView?.layoutManager

        // We only want to reset the adapter if something actually changed, or else it will flash
        if (folderAdapter?.badgeType != badgeType ||
            (currentLayoutManager != null && currentLayoutManager::class.java != layoutManager::class.java) ||
            (currentLayoutManager is GridLayoutManager && layoutManager is GridLayoutManager && currentLayoutManager.spanCount != layoutManager.spanCount)
        ) {
            folderAdapter?.badgeType = badgeType
            realBinding?.recyclerView?.adapter = adapter
        }

        val listOuterPadding = resources.getDimensionPixelSize(VR.dimen.list_outer_adding)
        val gridOuterPadding = resources.getDimensionPixelSize(VR.dimen.grid_outer_padding)
        viewLifecycleOwner.lifecycleScope.launch {
            settings.bottomInset.collect {
                val padding = when (settings.podcastGridLayout.value) {
                    PodcastGridLayoutType.LARGE_ARTWORK, PodcastGridLayoutType.SMALL_ARTWORK -> gridOuterPadding
                    PodcastGridLayoutType.LIST_VIEW -> listOuterPadding
                }
                realBinding?.recyclerView?.updatePadding(padding, padding, padding, padding + it)
            }
        }

        realBinding?.recyclerView?.layoutManager = layoutManager
        layoutManager.onRestoreInstanceState(savedInstanceState)
    }

    private fun createAdGridLayoutManager(spanCount: Int): GridLayoutManager {
        return GridLayoutManager(requireActivity(), spanCount).apply {
            val defaultLookup = GridLayoutManager.DefaultSpanSizeLookup()
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val itemViewTtype = adapter?.getItemViewType(position)
                    return when (itemViewTtype) {
                        AdapterViewTypeIds.BANNER_AD_ID -> spanCount
                        else -> defaultLookup.getSpanSize(position)
                    }
                }
            }
        }
    }

    override fun onPodcastMove(fromPosition: Int, toPosition: Int) {
        val newList = viewModel.moveFolderItem(fromPosition, toPosition)
        folderAdapter?.submitList(newList)
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

    override fun scrollToTop(): Boolean {
        val canScroll = binding.recyclerView.canScrollVertically(-1)
        binding.recyclerView.quickScrollToTop()
        return canScroll
    }

    private fun showTooltip() {
        binding.tooltipComposeView.apply {
            isVisible = true
            setContentWithViewCompositionStrategy {
                CallOnce {
                    viewModel.onTooltipShown()
                }
                AppTheme(theme.activeTheme) {
                    val configuration = LocalConfiguration.current
                    var toolbarY by remember { mutableIntStateOf(0) }

                    LaunchedEffect(configuration) {
                        with(binding.toolbar) {
                            val location = IntArray(2)
                            getLocationOnScreen(location)
                            toolbarY = location[1] + height
                        }
                    }

                    Box(
                        contentAlignment = Alignment.TopEnd,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable(
                                interactionSource = null,
                                indication = null,
                                onClick = ::closeTooltip,
                            )
                            .semantics { hideFromAccessibility() },
                    ) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(x = -8.dp.roundToPx(), y = toolbarY - 8.dp.roundToPx()) }
                                .widthIn(max = 320.dp),
                        ) {
                            Tooltip(
                                title = stringResource(LR.string.podcasts_sort_by_tooltip_title),
                                body = stringResource(LR.string.podcasts_sort_by_tooltip_message),
                                tipPosition = TipPosition.TopEnd,
                                modifier = Modifier.clickable(onClick = ::closeTooltip),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun closeTooltip() {
        binding.tooltipComposeView.isGone = true
        binding.tooltipComposeView.disposeComposition()
        viewModel.onTooltipClosed()
    }

    private fun openAd(ad: BlazeAd) {
        trackAdTapped(ad)
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, ad.url.toUri())
            startActivity(intent)
        }.onFailure { LogBuffer.e("Ads", it, "Failed to open an ad: ${ad.id}") }
    }

    private fun openAdReportFlow(ad: BlazeAd) {
        if (parentFragmentManager.findFragmentByTag("ad_report") == null) {
            AdReportFragment
                .newInstance(ad, podcastColors = null)
                .show(parentFragmentManager, "ad_report")
        }
    }

    private fun trackAdImpression(ad: BlazeAd) {
        analyticsTracker.trackBannerAdImpression(id = ad.id, location = ad.location.value)
    }

    fun trackAdTapped(ad: BlazeAd) {
        analyticsTracker.trackBannerAdTapped(id = ad.id, location = ad.location.value)
    }

    inner class SpaceItemDecoration : RecyclerView.ItemDecoration() {
        private val gridItemPadding = resources.getDimensionPixelSize(VR.dimen.grid_item_padding)
        override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
            when (settings.podcastGridLayout.value) {
                PodcastGridLayoutType.LARGE_ARTWORK, PodcastGridLayoutType.SMALL_ARTWORK -> {
                    outRect.set(gridItemPadding, gridItemPadding, gridItemPadding, gridItemPadding)
                }
                PodcastGridLayoutType.LIST_VIEW -> Unit
            }
        }
    }
}

@Composable
private fun NoPodcastsBanner(
    onClickButton: () -> Unit,
) {
    NoContentBanner(
        title = stringResource(LR.string.podcasts_time_to_add_some_podcasts),
        body = stringResource(LR.string.podcasts_time_to_add_some_podcasts_summary),
        iconResourceId = IR.drawable.ic_podcasts,
        primaryButtonText = stringResource(LR.string.podcasts_discover),
        onPrimaryButtonClick = onClickButton,
    )
}

@Composable
private fun NoFolderPodcastsBanner(
    onClickButton: () -> Unit,
) {
    NoContentBanner(
        title = stringResource(LR.string.podcasts_empty_folder),
        body = stringResource(LR.string.podcasts_empty_folder_summary),
        iconResourceId = IR.drawable.ic_folder,
        primaryButtonText = stringResource(LR.string.add_podcasts),
        onPrimaryButtonClick = onClickButton,
    )
}
