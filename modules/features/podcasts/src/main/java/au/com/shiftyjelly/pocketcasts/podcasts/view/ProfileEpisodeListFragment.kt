package au.com.shiftyjelly.pocketcasts.podcasts.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListViewModel.State
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeContainerFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.repositories.sync.StarredSyncWorker
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.settings.AutoDownloadSettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.ManualCleanupFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.ManualCleanupViewModel
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.isDeviceRunningOnLowStorage
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton
import au.com.shiftyjelly.pocketcasts.views.compose.EpisodeRow
import au.com.shiftyjelly.pocketcasts.views.compose.EpisodeRowTrailing
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectController
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectTopBar
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeActionViewModel
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeSource
import au.com.shiftyjelly.pocketcasts.views.swipe.handleAction
import com.automattic.eventhorizon.DownloadModalOptionType
import com.automattic.eventhorizon.DownloadsMultiSelectEnteredEvent
import com.automattic.eventhorizon.DownloadsMultiSelectExitedEvent
import com.automattic.eventhorizon.DownloadsOptionsButtonTappedEvent
import com.automattic.eventhorizon.DownloadsOptionsModalOptionTappedEvent
import com.automattic.eventhorizon.DownloadsSelectAllAboveTappedEvent
import com.automattic.eventhorizon.DownloadsSelectAllBelowTappedEvent
import com.automattic.eventhorizon.DownloadsSelectAllTappedEvent
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.FreeUpSpaceBannerShownEvent
import com.automattic.eventhorizon.FreeUpSpaceManageDownloadsTappedEvent
import com.automattic.eventhorizon.FreeUpSpaceMaybeLaterTappedEvent
import com.automattic.eventhorizon.ListeningHistoryClearHistoryButtonTappedEvent
import com.automattic.eventhorizon.ListeningHistoryDiscoverButtonTappedEvent
import com.automattic.eventhorizon.ListeningHistoryModalOptionType
import com.automattic.eventhorizon.ListeningHistoryMultiSelectEnteredEvent
import com.automattic.eventhorizon.ListeningHistoryMultiSelectExitedEvent
import com.automattic.eventhorizon.ListeningHistoryOptionsButtonTappedEvent
import com.automattic.eventhorizon.ListeningHistoryOptionsModalOptionTappedEvent
import com.automattic.eventhorizon.ListeningHistorySelectAllAboveTappedEvent
import com.automattic.eventhorizon.ListeningHistorySelectAllBelowTappedEvent
import com.automattic.eventhorizon.ListeningHistorySelectAllTappedEvent
import com.automattic.eventhorizon.StarredMultiSelectEnteredEvent
import com.automattic.eventhorizon.StarredMultiSelectExitedEvent
import com.automattic.eventhorizon.StarredSelectAllAboveTappedEvent
import com.automattic.eventhorizon.StarredSelectAllBelowTappedEvent
import com.automattic.eventhorizon.StarredSelectAllTappedEvent
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

private const val ARG_MODE = "profile_list_mode"

@AndroidEntryPoint
class ProfileEpisodeListFragment : BaseFragment() {
    sealed class Mode(
        val index: Int,
        val showMenu: Boolean,
        val showSearch: Boolean,
        val source: SourceView = SourceView.UNKNOWN,
    ) {
        data object Downloaded : Mode(0, true, false, SourceView.DOWNLOADS)
        data object Starred : Mode(1, false, false, SourceView.STARRED)
        data object History : Mode(2, true, true, SourceView.LISTENING_HISTORY)
    }

    companion object {
        const val OPTION_KEY = "option"
        private const val CLEAR_HISTORY = "clear_history"

        fun newInstance(mode: Mode): ProfileEpisodeListFragment {
            val bundle = Bundle().apply {
                putInt(ARG_MODE, mode.index)
            }
            val fragment = ProfileEpisodeListFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject
    lateinit var downloadQueue: DownloadQueue

    @Inject
    lateinit var playButtonListener: PlayButton.OnClickListener

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var multiSelectHelper: MultiSelectEpisodesHelper

    @Inject
    lateinit var eventHorizon: EventHorizon

    @Inject
    lateinit var swipeRowActionsFactory: SwipeRowActions.Factory

    @Inject
    lateinit var rowDataProvider: EpisodeRowDataProvider

    @Inject
    lateinit var syncManager: SyncManager

    private val viewModel: ProfileEpisodeListViewModel by viewModels()
    private val cleanUpViewModel: ManualCleanupViewModel by viewModels()
    private val swipeActionViewModel by viewModels<SwipeActionViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SwipeActionViewModel.Factory> { factory ->
                val swipeSource = when (mode) {
                    Mode.Downloaded -> SwipeSource.Downloads
                    Mode.History -> SwipeSource.ListeningHistory
                    Mode.Starred -> SwipeSource.Starred
                }
                factory.create(swipeSource, playlistUuid = null)
            }
        },
    )

    private lateinit var imageRequestFactory: PocketCastsImageRequestFactory

    val mode: Mode
        get() = when (arguments?.getInt(ARG_MODE)) {
            Mode.Downloaded.index -> Mode.Downloaded
            Mode.Starred.index -> Mode.Starred
            Mode.History.index -> Mode.History
            else -> throw IllegalStateException("Unknown mode")
        }

    private val Mode.titleRes
        get() = when (this) {
            is Mode.Downloaded -> LR.string.profile_navigation_downloads
            is Mode.Starred -> LR.string.profile_navigation_starred
            is Mode.History -> LR.string.profile_navigation_listening_history
        }

    private val Mode.artworkElement
        get() = when (this) {
            Mode.Downloaded -> Element.Downloads
            Mode.History -> Element.ListeningHistory
            Mode.Starred -> Element.Starred
        }

    val onRowClick = { episode: BaseEpisode ->
        if (episode is PodcastEpisode) {
            val episodeViewSource = when (mode) {
                Mode.Downloaded -> EpisodeViewSource.DOWNLOADS
                Mode.History -> EpisodeViewSource.LISTENING_HISTORY
                Mode.Starred -> EpisodeViewSource.STARRED
            }
            val fragment = EpisodeContainerFragment.newInstance(episode = episode, source = episodeViewSource)
            fragment.show(parentFragmentManager, "episode_card")
        }
    }

    private val multiSelectController by lazy {
        MultiSelectController(multiSelectHelper, viewLifecycleOwner.lifecycleScope, mode.source)
    }

    private val manageDownloadsSize = MutableStateFlow<Long?>(null)

    private val listResetKey = MutableStateFlow(0)

    private var wasMultiSelecting = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        val state by viewModel.state.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQueryFlow.collectAsStateWithLifecycle()
        val isFreeAccountBannerVisible by freeAccountBannerVisibility().collectAsStateWithLifecycle(false)
        val manageDownloadsSize by manageDownloadsSize.collectAsStateWithLifecycle()
        val bottomInset by settings.bottomInset.collectAsStateWithLifecycle(0)
        val listResetKey by listResetKey.collectAsStateWithLifecycle()
        val isMultiSelecting by multiSelectController.isMultiSelecting.collectAsStateWithLifecycle()
        AppTheme(theme.activeTheme) {
            ProfileEpisodeListPage(
                title = stringResource(mode.titleRes),
                state = state,
                searchQuery = searchQuery,
                showMenu = mode.showMenu,
                showDiscoverButton = mode is Mode.History,
                isFreeAccountBannerVisible = isFreeAccountBannerVisible,
                manageDownloadsSize = manageDownloadsSize,
                bottomInsetPx = bottomInset,
                listResetKey = listResetKey,
                listener = pageListener,
                multiSelectTopBar = if (isMultiSelecting) {
                    { MultiSelectTopBar(multiSelectController, eventHorizon) }
                } else {
                    null
                },
                episodeRow = { episode -> EpisodeRow(episode, isMultiSelecting) },
                toolbarModifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { theme.toggleDarkLightThemeActivity(requireActivity() as AppCompatActivity) })
                },
            )
        }
    }

    @Composable
    private fun EpisodeRow(episode: PodcastEpisode, isMultiSelecting: Boolean) {
        val selected by multiSelectController.selected.collectAsStateWithLifecycle()
        EpisodeRow(
            episode = episode,
            rowDataProvider = rowDataProvider,
            imageRequestFactory = imageRequestFactory,
            swipeActions = { swipeRowActionsFactory.podcastEpisode(episode) },
            trailing = EpisodeRowTrailing.PlayButton(playButtonListener, eventHorizon),
            onClick = {
                if (multiSelectHelper.isMultiSelecting) {
                    multiSelectHelper.toggle(episode)
                } else {
                    onRowClick(episode)
                }
            },
            onLongClick = { multiSelectHelper.defaultLongPress(episode, childFragmentManager) },
            onSwipeAction = { swipeAction ->
                viewLifecycleOwner.lifecycleScope.launch {
                    swipeActionViewModel.handleAction(
                        action = swipeAction,
                        episodeUuid = episode.uuid,
                        podcastUuid = episode.podcastOrSubstituteUuid,
                        fragmentManager = childFragmentManager,
                    )
                }
            },
            useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork(mode.artworkElement),
            streamByDefault = settings.streamingMode.value,
            isMultiSelecting = isMultiSelecting,
            isSelected = selected.let { multiSelectHelper.isSelected(episode) },
            animateMultiSelection = true,
        )
    }

    private val pageListener = ProfileEpisodeListListener(
        onNavigationClick = { activity?.onBackPressedDispatcher?.onBackPressed() },
        onMoreOptionsClick = ::showMoreOptions,
        onSearchQueryChange = { viewModel.onSearchQueryChanged(it) },
        onCreateFreeAccountClick = {
            viewModel.onCreateFreeAccountClick()
            OnboardingLauncher.openOnboardingFlow(
                activity = requireActivity(),
                onboardingFlow = OnboardingFlow.LoggedOut,
            )
        },
        onDismissFreeAccountBanner = { viewModel.dismissFreeAccountBanner() },
        onManageDownloadsShown = { eventHorizon.track(FreeUpSpaceBannerShownEvent) },
        onManageDownloadsClick = {
            eventHorizon.track(
                FreeUpSpaceManageDownloadsTappedEvent(
                    source = SourceView.DOWNLOADS.analyticsValue,
                ),
            )
            showFragment(ManualCleanupFragment.newInstance())
        },
        onDismissManageDownloads = ::onDismissManageDownloadTapped,
        onDiscoverClick = {
            eventHorizon.track(ListeningHistoryDiscoverButtonTappedEvent)
            (activity as FragmentHostListener).openTab(VR.id.navigation_discover)
        },
    )

    private fun freeAccountBannerVisibility() = if (mode is Mode.History) {
        combine(
            viewModel.isFreeAccountBannerVisible,
            viewModel.state.filterIsInstance<State.Loaded>().map {
                !it.results.isNullOrEmpty()
            },
        ) { showBanner, hasAnyEpisodes -> showBanner && hasAnyEpisodes }
    } else {
        flowOf(false)
    }

    @SuppressLint("MissingSuperCall") // False positive
    override fun onAttach(context: Context) {
        super.onAttach(context)

        imageRequestFactory = PocketCastsImageRequestFactory(context, cornerRadius = 4).smallSize().themed()

        playButtonListener.source = mode.source
        multiSelectHelper.source = mode.source
    }

    override fun onPause() {
        super.onPause()
        listResetKey.value++
        if (multiSelectHelper.isMultiSelecting) {
            multiSelectHelper.isMultiSelecting = false
        }
    }

    override fun onResume() {
        super.onResume()
        when (mode) {
            Mode.Downloaded -> AutoPlaySource.Predefined.Downloads
            Mode.History -> AutoPlaySource.Predefined.None
            Mode.Starred -> AutoPlaySource.Predefined.Starred
        }.let { settings.trackingAutoPlaySource.set(it, updateModifiedAt = false) }
    }

    override fun onDestroyView() {
        multiSelectHelper.cleanup()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setup(mode)

        if (savedInstanceState == null) {
            startStarredSyncWorker()
        }

        if (mode is Mode.Downloaded) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    cleanUpViewModel.state.collect { state ->
                        updateManageDownloadsCard(state.diskSpaceViews.sumOf { it.episodesBytesSize })
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    if (state is State.Empty) {
                        manageDownloadsSize.value = null
                    }
                }
            }
        }

        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            if (wasMultiSelecting == isMultiSelecting) {
                return@observe
            }
            wasMultiSelecting = isMultiSelecting

            if ((activity as? FragmentHostListener)?.isUpNextShowing() == false) {
                if (isMultiSelecting) {
                    trackMultiSelectEntered()
                } else {
                    trackMultiSelectExited()
                }
            }
        }
        multiSelectHelper.listener = object : MultiSelectHelper.Listener<BaseEpisode> {
            override fun multiSelectSelectAll() {
                val episodes = (viewModel.state.value as? State.Loaded)?.results
                if (episodes != null) {
                    multiSelectHelper.selectAllInList(episodes)
                    trackSelectAll(true)
                }
            }

            override fun multiSelectSelectNone() {
                val episodes = (viewModel.state.value as? State.Loaded)?.results
                if (episodes != null) {
                    multiSelectHelper.deselectAllInList(episodes)
                    trackSelectAll(false)
                }
            }

            override fun multiSelectSelectAllUp(multiSelectable: BaseEpisode) {
                val episodes = (viewModel.state.value as? State.Loaded)?.results
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(episodes.subList(0, startIndex + 1))
                        trackSelectAllAbove()
                    }
                }
            }

            override fun multiSelectSelectAllDown(multiSelectable: BaseEpisode) {
                val episodes = (viewModel.state.value as? State.Loaded)?.results
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(episodes.subList(startIndex, episodes.size))
                        trackSelectAllBelow()
                    }
                }
            }

            override fun multiDeselectAllBelow(multiSelectable: BaseEpisode) {
                val episodes = (viewModel.state.value as? State.Loaded)?.results
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        val episodesBelow = episodes.subList(startIndex, episodes.size)
                        multiSelectHelper.deselectAllInList(episodesBelow)
                    }
                }
            }

            override fun multiDeselectAllAbove(multiSelectable: BaseEpisode) {
                val episodes = (viewModel.state.value as? State.Loaded)?.results
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        val episodesAbove = episodes.subList(0, startIndex + 1)
                        multiSelectHelper.deselectAllInList(episodesAbove)
                    }
                }
            }
        }
        multiSelectHelper.coordinatorLayout = (activity as FragmentHostListener).snackBarView()
    }

    private fun startStarredSyncWorker() {
        if (mode is Mode.Starred) {
            StarredSyncWorker.enqueue(
                syncManager = syncManager,
                context = requireContext(),
            )
        }
    }

    private suspend fun updateManageDownloadsCard(downloadedEpisodesSize: Long) {
        val isVisible = downloadedEpisodesSize != 0L && isDeviceRunningOnLowStorage() && settings.shouldShowLowStorageBannerAfterSnooze()
        manageDownloadsSize.value = downloadedEpisodesSize.takeIf { isVisible }
    }

    private fun onDismissManageDownloadTapped() {
        eventHorizon.track(
            FreeUpSpaceMaybeLaterTappedEvent(
                source = SourceView.DOWNLOADS.analyticsValue,
            ),
        )
        settings.setDismissLowStorageBannerTime(System.currentTimeMillis())
        manageDownloadsSize.value = null
    }

    private fun showMoreOptions() {
        val dialog = OptionsDialog()
        if (mode is Mode.Downloaded) {
            eventHorizon.track(DownloadsOptionsButtonTappedEvent)
            dialog.addTextOption(LR.string.profile_auto_download_settings, imageId = R.drawable.ic_settings_small, click = this::showAutodownloadSettings)
            if (downloadQueue.size > 0) {
                dialog.addTextOption(LR.string.settings_auto_download_stop_all, imageId = IR.drawable.ic_stop, click = this::stopAllDownloads)
            }
            dialog.addTextOption(LR.string.profile_clean_up, imageId = VR.drawable.ic_delete, click = this::showCleanupSettings)
        } else if (mode is Mode.History) {
            eventHorizon.track(ListeningHistoryOptionsButtonTappedEvent)
            dialog.addTextOption(LR.string.profile_clear_listening_history, imageId = R.drawable.ic_history, click = this::clearListeningHistory)
        }
        dialog.show(parentFragmentManager, "more_options")
    }

    private fun showAutodownloadSettings() {
        val fragment = AutoDownloadSettingsFragment()
        showFragment(fragment)
        eventHorizon.track(
            DownloadsOptionsModalOptionTappedEvent(
                option = DownloadModalOptionType.AutoDownloadSettings,
            ),
        )
        (activity as AppCompatActivity).supportActionBar?.setTitle(LR.string.profile_auto_download_settings)
    }

    private fun stopAllDownloads() {
        eventHorizon.track(
            DownloadsOptionsModalOptionTappedEvent(
                option = DownloadModalOptionType.StopAllDownloads,
            ),
        )
        viewModel.cancelAllDownloads()
    }

    private fun showCleanupSettings() {
        eventHorizon.track(
            DownloadsOptionsModalOptionTappedEvent(
                option = DownloadModalOptionType.CleanUp,
            ),
        )
        val fragment = ManualCleanupFragment.newInstance()
        showFragment(fragment)
    }

    private fun clearListeningHistory() {
        eventHorizon.track(
            ListeningHistoryOptionsModalOptionTappedEvent(
                option = ListeningHistoryModalOptionType.ClearHistory,
            ),
        )
        val dialog = ConfirmationDialog()
            .setIconId(R.drawable.ic_history)
            .setTitle(resources.getString(LR.string.profile_clear_listening_history_title))
            .setSummary(resources.getString(LR.string.profile_clear_cannot_be_undone))
            .setButtonType(ConfirmationDialog.ButtonType.Danger(resources.getString(LR.string.profile_clear_all)))
            .setOnConfirm {
                eventHorizon.track(ListeningHistoryClearHistoryButtonTappedEvent)
                viewModel.clearAllEpisodeHistory()
            }
        dialog.show(parentFragmentManager, "clear_history")
    }

    private fun showFragment(fragment: Fragment) {
        (activity as? FragmentHostListener)?.addFragment(fragment)
    }

    override fun onBackPressed(): Boolean {
        return if (multiSelectHelper.isMultiSelecting) {
            multiSelectHelper.isMultiSelecting = false
            true
        } else {
            super.onBackPressed()
        }
    }

    override fun getBackstackCount(): Int {
        return super.getBackstackCount() + if (multiSelectHelper.isMultiSelecting) 1 else 0
    }

    private fun trackSelectAll(selectAll: Boolean) {
        val event = when (mode) {
            Mode.Downloaded -> DownloadsSelectAllTappedEvent(selectAll = selectAll)
            Mode.History -> ListeningHistorySelectAllTappedEvent(selectAll = selectAll)
            Mode.Starred -> StarredSelectAllTappedEvent(selectAll = selectAll)
        }
        eventHorizon.track(event)
    }

    private fun trackSelectAllAbove() {
        val event = when (mode) {
            Mode.Downloaded -> DownloadsSelectAllAboveTappedEvent
            Mode.History -> ListeningHistorySelectAllAboveTappedEvent
            Mode.Starred -> StarredSelectAllAboveTappedEvent
        }
        eventHorizon.track(event)
    }

    private fun trackSelectAllBelow() {
        val event = when (mode) {
            Mode.Downloaded -> DownloadsSelectAllBelowTappedEvent
            Mode.History -> ListeningHistorySelectAllBelowTappedEvent
            Mode.Starred -> StarredSelectAllBelowTappedEvent
        }
        eventHorizon.track(event)
    }

    private fun trackMultiSelectEntered() {
        val event = when (mode) {
            Mode.Downloaded -> DownloadsMultiSelectEnteredEvent
            Mode.History -> ListeningHistoryMultiSelectEnteredEvent
            Mode.Starred -> StarredMultiSelectEnteredEvent
        }
        eventHorizon.track(event)
    }

    private fun trackMultiSelectExited() {
        val event = when (mode) {
            Mode.Downloaded -> DownloadsMultiSelectExitedEvent
            Mode.History -> ListeningHistoryMultiSelectExitedEvent
            Mode.Starred -> StarredMultiSelectExitedEvent
        }
        eventHorizon.track(event)
    }
}
