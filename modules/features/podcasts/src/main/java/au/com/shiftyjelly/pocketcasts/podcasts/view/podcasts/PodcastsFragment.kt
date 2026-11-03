package au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.ads.AdReportFragment
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.TipPosition
import au.com.shiftyjelly.pocketcasts.compose.components.Tooltip
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderCreateFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderCreateSharedViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderEditFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderEditPodcastsFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.SuggestedFoldersFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.notifications.EnableNotificationsPromptFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastsViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.search.SearchFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.extensions.getColor
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.compose.ToolbarCastButton
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.TopScrollable
import au.com.shiftyjelly.pocketcasts.views.helper.ToolbarColors
import com.automattic.eventhorizon.BannerAdImpressionEvent
import com.automattic.eventhorizon.BannerAdTappedEvent
import com.automattic.eventhorizon.CreateFolderSourceType
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.FolderChoosePodcastsShownEvent
import com.automattic.eventhorizon.FolderEditShownEvent
import com.automattic.eventhorizon.FolderModalOptionType
import com.automattic.eventhorizon.FolderOptionsButtonTappedEvent
import com.automattic.eventhorizon.FolderOptionsModalOptionTappedEvent
import com.automattic.eventhorizon.FolderSortByChangedEvent
import com.automattic.eventhorizon.PodcastsListDiscoverButtonTappedEvent
import com.automattic.eventhorizon.PodcastsListFolderButtonTappedEvent
import com.automattic.eventhorizon.PodcastsListFolderTappedEvent
import com.automattic.eventhorizon.PodcastsListOptionsButtonTappedEvent
import com.automattic.eventhorizon.PodcastsListPodcastTappedEvent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class PodcastsFragment :
    BaseFragment(),
    TopScrollable {

    companion object {
        const val ARG_FOLDER_UUID = "ARG_FOLDER_UUID"

        fun newInstance(folderUuid: String): PodcastsFragment {
            return PodcastsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FOLDER_UUID, folderUuid)
                }
            }
        }
    }

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var eventHorizon: EventHorizon

    private var podcastOptionsDialog: PodcastsOptionsDialog? = null
    private var folderOptionsDialog: FolderOptionsDialog? = null
    private lateinit var imageRequestFactory: PocketCastsImageRequestFactory

    private val viewModel: PodcastsViewModel by viewModels(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<PodcastsViewModel.Factory> { factory ->
                factory.create(folderUuid)
            }
        },
    )
    private val sharedViewModel: FolderCreateSharedViewModel by activityViewModels()

    private val scrollToTopSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private var canScrollBackward: () -> Boolean = { false }
    private var isRefreshing by mutableStateOf(false)
    private var isTooltipVisible by mutableStateOf(false)

    private val folderUuid: String?
        get() = arguments?.getString(ARG_FOLDER_UUID)

    private val pageListener = PodcastsPageListener(
        onNavigationClick = { activity?.onBackPressedDispatcher?.onBackPressed() },
        onFoldersLockedClick = {
            if (viewModel.areSuggestedFoldersAvailable.value) {
                showSuggestedFoldersCreation(SuggestedFoldersFragment.Source.ToolbarButton)
            } else {
                OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.Upsell(OnboardingUpgradeSource.FOLDERS))
            }
        },
        onCreateFolderClick = {
            eventHorizon.track(PodcastsListFolderButtonTappedEvent)
            handleFolderCreation()
        },
        onSearchClick = ::search,
        onOptionsClick = {
            val event = if (folderUuid != null) {
                FolderOptionsButtonTappedEvent
            } else {
                PodcastsListOptionsButtonTappedEvent
            }
            eventHorizon.track(event)
            openOptions()
        },
        onRefresh = {
            isRefreshing = true
            viewModel.refreshPodcasts()
        },
        onPodcastClick = { podcast ->
            eventHorizon.track(PodcastsListPodcastTappedEvent)
            val fragment = PodcastFragment.newInstance(podcastUuid = podcast.uuid, sourceView = SourceView.PODCAST_LIST)
            (activity as FragmentHostListener).addFragment(fragment)
        },
        onFolderClick = { folder -> onFolderClick(folder.uuid, isUserInitiated = true) },
        onAdClick = ::openAd,
        onAdOptionsClick = ::openAdReportFlow,
        onAdImpression = ::trackAdImpression,
        onReorder = { items -> viewModel.reorderItems(items) },
    )

    override fun onAttach(context: Context) {
        super.onAttach(context)
        imageRequestFactory = PocketCastsImageRequestFactory(context, placeholderType = PlaceholderType.None).themed()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            PodcastsContent()
        }
    }.also {
        if (savedInstanceState == null) {
            viewModel.trackScreenShown()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            if (viewModel.shouldShowTooltip()) {
                isTooltipVisible = true
            }
        }
    }

    @Composable
    private fun PodcastsContent() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        val activeAd by viewModel.activeAd.collectAsStateWithLifecycle()
        val layout by settings.podcastGridLayout.flow.collectAsStateWithLifecycle()
        val badgeType by settings.podcastBadgeType.flow.collectAsStateWithLifecycle()
        val badges by viewModel.podcastUuidToBadge.collectAsStateWithLifecycle(emptyMap())
        val bottomInset by settings.bottomInset.collectAsStateWithLifecycle(0)
        val shownState = remember { mutableStateOf<PodcastsViewModel.UiState?>(null) }
        if (folderUuid == null || uiState.folder != null) {
            shownState.value = uiState
        }
        val state = shownState.value
        val configuration = LocalConfiguration.current
        val gridState = rememberSaveable(layout, configuration.orientation, configuration.screenWidthDp, saver = LazyGridState.Saver) { LazyGridState() }
        canScrollBackward = { gridState.canScrollBackward }
        LaunchedEffect(gridState) {
            scrollToTopSignal.collectLatest { gridState.animateScrollToItem(0) }
        }
        var toolbarBottom by remember { mutableIntStateOf(0) }
        val isEmpty = state != null && state.items.isEmpty() && !state.hasHiddenPodcasts
        PodcastsPage(
            toolbar = toolbarState(state),
            content = PodcastsContent(state?.items.orEmpty(), layout, badgeType, badges, activeAd),
            showContent = !isEmpty,
            showEmpty = isEmpty && !state.isLoadingItems,
            isRefreshing = isRefreshing,
            gridState = gridState,
            bottomInsetPx = bottomInset,
            imageRequestFactory = imageRequestFactory,
            listener = pageListener,
            castButton = { tint -> ToolbarCastButton(tint, onClick = { chromeCastAnalytics.trackChromeCastViewShown() }) },
            emptyContent = { EmptyContent(activeAd) },
            toolbarModifier = Modifier
                .onGloballyPositioned { toolbarBottom = it.boundsInRoot().bottom.toInt() }
                .pointerInput(Unit) {
                    detectTapGestures(onLongPress = { theme.toggleDarkLightThemeActivity(requireActivity() as AppCompatActivity) })
                },
        )
        if (isTooltipVisible) {
            SortTooltip(toolbarBottom)
        }
    }

    private fun toolbarState(state: PodcastsViewModel.UiState?): PodcastsToolbarState {
        val folder = state?.folder
        val isPlus = state?.isSignedInAsPlusOrPatron == true
        return PodcastsToolbarState(
            title = folder?.name ?: getString(LR.string.podcasts),
            colors = if (folder == null) {
                ToolbarColors.theme(theme = theme, context = requireContext())
            } else {
                ToolbarColors.user(color = folder.getColor(requireContext()), theme = theme)
            },
            showBack = folder != null,
            showFoldersLocked = state != null && !isPlus,
            showCreateFolder = folder == null && isPlus,
            showSearch = folder == null,
        )
    }

    @Composable
    private fun EmptyContent(activeAd: BlazeAd?) {
        val folderUuid = folderUuid
        PodcastsEmptyContent(
            activeAd = activeAd,
            listener = pageListener,
        ) {
            if (folderUuid != null) {
                NoFolderPodcastsBanner(
                    onClickButton = {
                        FolderEditPodcastsFragment.newInstance(folderUuid).show(parentFragmentManager, "add_podcasts_card")
                    },
                )
            } else {
                NoPodcastsBanner(
                    onClickButton = {
                        eventHorizon.track(PodcastsListDiscoverButtonTappedEvent)
                        (activity as FragmentHostListener).openTab(VR.id.navigation_discover)
                    },
                )
            }
        }
    }

    @Composable
    private fun SortTooltip(toolbarBottom: Int) {
        CallOnce {
            viewModel.onTooltipShown()
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
                    .offset { IntOffset(x = -8.dp.roundToPx(), y = toolbarBottom - 8.dp.roundToPx()) }
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateStatusBar()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.refreshStateFlow.collect { refreshState ->
                    if (refreshState !is RefreshState.Refreshing) {
                        isRefreshing = false
                    }
                }
            }
        }

        sharedViewModel.folderUuidLive.observe(viewLifecycleOwner) { newFolderUuid ->
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

    private fun search() {
        val searchFragment = SearchFragment.newInstance(source = SourceView.PODCAST_LIST)
        (activity as FragmentHostListener).addFragment(searchFragment, onTop = true)
        scrollToTopSignal.tryEmit(Unit)
    }

    private fun openOptions() {
        if (folderUuid != null) {
            val folder = viewModel.uiState.value.folder ?: return
            val onOpenSortOptions = {
                eventHorizon.track(
                    FolderOptionsModalOptionTappedEvent(
                        option = FolderModalOptionType.SortBy,
                    ),
                )
            }
            val onSortTypeChanged = { sort: PodcastsSortType ->
                eventHorizon.track(
                    FolderSortByChangedEvent(
                        sortOrder = sort.analyticsValue,
                    ),
                )
                viewModel.updateFolderSort(folder.uuid, sort)
            }
            val onEditFolder = {
                eventHorizon.track(
                    FolderOptionsModalOptionTappedEvent(
                        option = FolderModalOptionType.EditFolder,
                    ),
                )
                eventHorizon.track(FolderEditShownEvent)
                val fragment = FolderEditFragment.newInstance(folderUuid = folder.uuid)
                fragment.show(parentFragmentManager, "edit_folder_card")
            }
            val onAddOrRemovePodcast = {
                eventHorizon.track(FolderChoosePodcastsShownEvent)
                eventHorizon.track(
                    FolderOptionsModalOptionTappedEvent(
                        option = FolderModalOptionType.AddOrRemovePodcasts,
                    ),
                )
                val fragment = FolderEditPodcastsFragment.newInstance(folderUuid = folder.uuid)
                fragment.show(parentFragmentManager, "add_podcasts_card")
            }
            folderOptionsDialog = FolderOptionsDialog(folder, onOpenSortOptions, onSortTypeChanged, onEditFolder, onAddOrRemovePodcast, this, settings).apply {
                show()
            }
        } else {
            podcastOptionsDialog = PodcastsOptionsDialog(this, settings, eventHorizon).apply {
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
        FolderCreateFragment
            .newInstance(CreateFolderSourceType.PodcastsList)
            .show(parentFragmentManager, "create_folder_card")
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

    override fun onResume() {
        super.onResume()
        viewModel.updateNotificationsPermissionState()
    }

    private fun onFolderClick(folderUuid: String, isUserInitiated: Boolean) {
        if (isUserInitiated) {
            eventHorizon.track(PodcastsListFolderTappedEvent)
        }
        val fragment = newInstance(folderUuid = folderUuid)
        (activity as FragmentHostListener).addFragment(fragment)
    }

    override fun scrollToTop(): Boolean {
        val canScroll = canScrollBackward()
        scrollToTopSignal.tryEmit(Unit)
        return canScroll
    }

    private fun closeTooltip() {
        isTooltipVisible = false
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
        eventHorizon.track(
            BannerAdImpressionEvent(
                id = ad.id,
                location = ad.location.analyticsValue,
            ),
        )
    }

    fun trackAdTapped(ad: BlazeAd) {
        eventHorizon.track(
            BannerAdTappedEvent(
                id = ad.id,
                location = ad.location.analyticsValue,
            ),
        )
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
