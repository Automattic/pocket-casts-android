package au.com.shiftyjelly.pocketcasts.podcasts.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.components.Banner
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentProfileEpisodeListBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.ProfileEpisodeListViewModel.State
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeContainerFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.EpisodeListAdapter
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.settings.AutoDownloadSettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.ManualCleanupFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.ManualCleanupViewModel
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.isDeviceRunningOnLowStorage
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper.Companion.MULTI_SELECT_TOGGLE_PAYLOAD
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeActionViewModel
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeSource
import au.com.shiftyjelly.pocketcasts.views.swipe.handleAction
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

private const val ARG_MODE = "profile_list_mode"

@AndroidEntryPoint
class ProfileEpisodeListFragment :
    BaseFragment(),
    Toolbar.OnMenuItemClickListener {
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
        const val CLEAN_UP = "clean_up"
        private const val SELECT_ALL_KEY = "select_all"
        private const val AUTO_DOWNLOAD_SETTINGS = "auto_download_settings"
        private const val STOP_ALL_DOWNLOADS = "stop_all_downloads"
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
    lateinit var downloadManager: DownloadManager

    @Inject
    lateinit var playButtonListener: PlayButton.OnClickListener

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var multiSelectHelper: MultiSelectEpisodesHelper

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var bookmarkManager: BookmarkManager

    @Inject
    lateinit var swipeRowActionsFactory: SwipeRowActions.Factory

    @Inject
    lateinit var rowDataProvider: EpisodeRowDataProvider

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

    private var binding: FragmentProfileEpisodeListBinding? = null

    val mode: Mode
        get() = when (arguments?.getInt(ARG_MODE)) {
            Mode.Downloaded.index -> Mode.Downloaded
            Mode.Starred.index -> Mode.Starred
            Mode.History.index -> Mode.History
            else -> throw IllegalStateException("Unknown mode")
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

    val adapter by lazy {
        EpisodeListAdapter(
            rowDataProvider = rowDataProvider,
            settings = settings,
            onRowClick = onRowClick,
            playButtonListener = playButtonListener,
            imageRequestFactory = imageRequestFactory,
            swipeRowActionsFactory = swipeRowActionsFactory,
            multiSelectHelper = multiSelectHelper,
            fragmentManager = childFragmentManager,
            artworkContext = when (mode) {
                Mode.Downloaded -> Element.Downloads
                Mode.History -> Element.ListeningHistory
                Mode.Starred -> Element.Starred
            },
            onSwipeAction = { episode, swipeAction ->
                viewLifecycleOwner.lifecycleScope.launch {
                    swipeActionViewModel.handleAction(swipeAction, episode.uuid, childFragmentManager)
                }
            },
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentProfileEpisodeListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        imageRequestFactory = PocketCastsImageRequestFactory(context, cornerRadius = 4).smallSize().themed()

        playButtonListener.source = mode.source
        multiSelectHelper.source = mode.source
    }

    override fun onPause() {
        super.onPause()
        binding?.recyclerView?.adapter = null
        if (multiSelectHelper.isMultiSelecting) {
            multiSelectHelper.isMultiSelecting = false
        }
    }

    override fun onResume() {
        super.onResume()
        binding?.recyclerView?.adapter = adapter
        when (mode) {
            Mode.Downloaded -> AutoPlaySource.Predefined.Downloads
            Mode.History -> AutoPlaySource.Predefined.None
            Mode.Starred -> AutoPlaySource.Predefined.Starred
        }.let { settings.trackingAutoPlaySource.set(it, updateModifiedAt = false) }
    }

    override fun onDestroyView() {
        binding?.recyclerView?.adapter = null
        multiSelectHelper.cleanup()
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setup(mode)

        updateToolbar()

        binding?.recyclerView?.let {
            it.layoutManager = LinearLayoutManager(it.context, RecyclerView.VERTICAL, false)
            it.adapter = adapter
            (it.itemAnimator as SimpleItemAnimator).changeDuration = 0
        }

        binding?.layoutSearch?.setContent {
            ProfileEpisodeListSearchBar(
                activeTheme = theme.activeTheme,
            )
        }

        if (mode is Mode.Downloaded) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    cleanUpViewModel.state.collect { state ->
                        updateManageDownloadsCard(state.diskSpaceViews.sumOf { it.episodesBytesSize })
                    }
                }
            }
        } else {
            binding?.manageDownloadsCard?.isVisible = false
        }

        if (mode is Mode.History) {
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    combine(
                        viewModel.isFreeAccountBannerVisible,
                        viewModel.state.filterIsInstance<State.Loaded>().map {
                            !it.results.isNullOrEmpty()
                        },
                        ::Pair,
                    ).collect { (showBanner, hasAnyEpisodes) ->
                        binding?.freeAccountBanner?.isVisible = showBanner && hasAnyEpisodes
                    }
                }
            }
            binding?.freeAccountBanner?.setContentWithViewCompositionStrategy {
                AppTheme(
                    themeType = theme.activeTheme,
                ) {
                    Banner(
                        title = stringResource(LR.string.encourage_account_history_banner_title),
                        description = stringResource(LR.string.encourage_account_history_banner_description),
                        actionLabel = stringResource(LR.string.encourage_account_banner_action_label),
                        icon = painterResource(IR.drawable.ic_filters_clock),
                        onActionClick = {
                            viewModel.onCreateFreeAccountClick()
                            OnboardingLauncher.openOnboardingFlow(
                                activity = requireActivity(),
                                onboardingFlow = OnboardingFlow.LoggedOut,
                            )
                        },
                        onDismiss = {
                            viewModel.dismissFreeAccountBanner()
                        },
                        modifier = Modifier
                            .background(MaterialTheme.theme.colors.primaryUi02)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    updateEmptyStateView(state)

                    when (state) {
                        is State.Empty -> {
                            binding?.recyclerView?.isVisible = false
                            binding?.manageDownloadsCard?.isVisible = false
                        }

                        State.Loading -> Unit

                        is State.Loaded -> {
                            binding?.recyclerView?.updatePadding(
                                top = if (state.showSearchBar) 0 else 16.dpToPx(requireContext()),
                            )
                            binding?.recyclerView?.isVisible = true
                            adapter.submitList(state.results)
                        }
                    }
                }
            }
        }

        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            val wasMultiSelecting = binding?.multiSelectToolbar?.isVisible == true
            if (wasMultiSelecting == isMultiSelecting) {
                return@observe
            }
            binding?.multiSelectToolbar?.isVisible = isMultiSelecting

            binding?.toolbar?.isVisible = !isMultiSelecting
            binding?.multiSelectToolbar?.setNavigationIcon(R.drawable.ic_arrow_back)

            if ((activity as? FragmentHostListener)?.isUpNextShowing() == false) {
                if (isMultiSelecting) {
                    trackMultiSelectEntered()
                } else {
                    trackMultiSelectExited()
                }
            }

            adapter.notifyItemRangeChanged(0, adapter.itemCount, MULTI_SELECT_TOGGLE_PAYLOAD)
        }
        multiSelectHelper.listener = object : MultiSelectHelper.Listener<BaseEpisode> {
            override fun multiSelectSelectAll() {
                val episodes = (viewModel.state.value as? State.Loaded)?.results
                if (episodes != null) {
                    multiSelectHelper.selectAllInList(episodes)
                    adapter.notifyDataSetChanged()
                    trackSelectAll(true)
                }
            }

            override fun multiSelectSelectNone() {
                val episodes = (viewModel.state.value as? State.Loaded)?.results
                if (episodes != null) {
                    multiSelectHelper.deselectAllInList(episodes)
                    adapter.notifyDataSetChanged()
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

                    adapter.notifyDataSetChanged()
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

                    adapter.notifyDataSetChanged()
                }
            }

            override fun multiDeselectAllBelow(multiSelectable: BaseEpisode) {
                val episodes = (viewModel.state.value as? State.Loaded)?.results
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        val episodesBelow = episodes.subList(startIndex, episodes.size)
                        multiSelectHelper.deselectAllInList(episodesBelow)
                        adapter.notifyDataSetChanged()
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
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
        multiSelectHelper.coordinatorLayout = (activity as FragmentHostListener).snackBarView()

        val sourceView = when (mode) {
            Mode.Downloaded -> SourceView.DOWNLOADS
            Mode.History -> SourceView.LISTENING_HISTORY
            Mode.Starred -> SourceView.STARRED
        }

        binding?.multiSelectToolbar?.setup(viewLifecycleOwner, multiSelectHelper, menuRes = null, activity = requireActivity(), sourceView = sourceView)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect {
                    binding?.recyclerView?.updatePadding(bottom = it)
                }
            }
        }
    }

    private fun updateEmptyStateView(state: State) {
        binding?.emptyLayout?.isVisible = state is State.Empty

        if (state is State.Empty) {
            binding?.emptyLayout?.setContentWithViewCompositionStrategy {
                AppTheme(theme.activeTheme) {
                    val buttonText = if (mode is Mode.History) stringResource(LR.string.go_to_discover) else null

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        NoContentBanner(
                            title = stringResource(state.titleRes),
                            body = stringResource(state.summaryRes),
                            iconResourceId = state.iconRes,
                            primaryButtonText = buttonText,
                            onPrimaryButtonClick = {
                                analyticsTracker.track(AnalyticsEvent.LISTENING_HISTORY_DISCOVER_BUTTON_TAPPED)
                                (activity as FragmentHostListener).openTab(VR.id.navigation_discover)
                            },
                        )
                    }
                }
            }
        }
    }

    private fun updateToolbar() {
        val toolbar = binding?.toolbar ?: return
        val title = when (mode) {
            is Mode.Downloaded -> LR.string.profile_navigation_downloads
            is Mode.Starred -> LR.string.profile_navigation_starred
            is Mode.History -> LR.string.profile_navigation_listening_history
        }
        toolbar.setup(
            title = getString(title),
            navigationIcon = BackArrow,
            activity = activity,
            theme = theme,
            menu = if (mode.showMenu) R.menu.menu_profile_list else null,
        )
        toolbar.setOnMenuItemClickListener(this)
    }

    private suspend fun updateManageDownloadsCard(downloadedEpisodesSize: Long) {
        binding?.manageDownloadsCard?.apply {
            isVisible = downloadedEpisodesSize != 0L && isDeviceRunningOnLowStorage() && settings.shouldShowLowStorageBannerAfterSnooze()
            if (isVisible) {
                setContent {
                    AppTheme(theme.activeTheme) {
                        CallOnce {
                            analyticsTracker.track(AnalyticsEvent.FREE_UP_SPACE_BANNER_SHOWN)
                        }
                        Box(
                            modifier = Modifier
                                .background(color = MaterialTheme.theme.colors.primaryUi02)
                                .padding(start = 16.dp, end = 16.dp, top = 16.dp),
                        ) {
                            ManageDownloadsCard(
                                totalDownloadSize = downloadedEpisodesSize,
                                onManageDownloadsClick = {
                                    analyticsTracker.track(AnalyticsEvent.FREE_UP_SPACE_MANAGE_DOWNLOADS_TAPPED, mapOf("source" to SourceView.DOWNLOADS.analyticsValue))
                                    showFragment(ManualCleanupFragment.newInstance())
                                },
                                onDismissClick = {
                                    onDismissManageDownloadTapped()
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun onDismissManageDownloadTapped() {
        analyticsTracker.track(
            AnalyticsEvent.FREE_UP_SPACE_MAYBE_LATER_TAPPED,
            mapOf("source" to SourceView.DOWNLOADS.analyticsValue),
        )
        settings.setDismissLowStorageBannerTime(System.currentTimeMillis())
        binding?.manageDownloadsCard?.isVisible = false
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return if (item.itemId == R.id.more_options) {
            val dialog = OptionsDialog()
            if (mode is Mode.Downloaded) {
                analyticsTracker.track(AnalyticsEvent.DOWNLOADS_OPTIONS_BUTTON_TAPPED)
                dialog.addTextOption(LR.string.profile_auto_download_settings, imageId = R.drawable.ic_settings_small, click = this::showAutodownloadSettings)
                if (downloadManager.hasPendingOrRunningDownloads()) {
                    dialog.addTextOption(LR.string.settings_auto_download_stop_all, imageId = IR.drawable.ic_stop, click = this::stopAllDownloads)
                }
                dialog.addTextOption(LR.string.profile_clean_up, imageId = VR.drawable.ic_delete, click = this::showCleanupSettings)
            } else if (mode is Mode.History) {
                analyticsTracker.track(AnalyticsEvent.LISTENING_HISTORY_OPTIONS_BUTTON_TAPPED)
                dialog.addTextOption(LR.string.profile_clear_listening_history, imageId = R.drawable.ic_history, click = this::clearListeningHistory)
            }
            dialog.show(parentFragmentManager, "more_options")
            true
        } else {
            false
        }
    }

    private fun showAutodownloadSettings() {
        val fragment = AutoDownloadSettingsFragment()
        showFragment(fragment)
        analyticsTracker.track(AnalyticsEvent.DOWNLOADS_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to AUTO_DOWNLOAD_SETTINGS))
        (activity as AppCompatActivity).supportActionBar?.setTitle(LR.string.profile_auto_download_settings)
    }

    private fun stopAllDownloads() {
        analyticsTracker.track(AnalyticsEvent.DOWNLOADS_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to STOP_ALL_DOWNLOADS))
        downloadManager.stopAllDownloads()
    }

    private fun showCleanupSettings() {
        analyticsTracker.track(AnalyticsEvent.DOWNLOADS_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to CLEAN_UP))
        val fragment = ManualCleanupFragment.newInstance()
        showFragment(fragment)
    }

    private fun clearListeningHistory() {
        analyticsTracker.track(AnalyticsEvent.LISTENING_HISTORY_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to CLEAR_HISTORY))
        val dialog = ConfirmationDialog()
            .setIconId(R.drawable.ic_history)
            .setTitle(resources.getString(LR.string.profile_clear_listening_history_title))
            .setSummary(resources.getString(LR.string.profile_clear_cannot_be_undone))
            .setButtonType(ConfirmationDialog.ButtonType.Danger(resources.getString(LR.string.profile_clear_all)))
            .setOnConfirm {
                analyticsTracker.track(AnalyticsEvent.LISTENING_HISTORY_CLEAR_HISTORY_BUTTON_TAPPED)
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
            val handled = super.onBackPressed()
            if (handled) {
                updateToolbar()
            }
            return handled
        }
    }

    override fun getBackstackCount(): Int {
        return super.getBackstackCount() + if (multiSelectHelper.isMultiSelecting) 1 else 0
    }

    private fun trackSelectAll(selectAll: Boolean) {
        val analyticsEvent = when (mode) {
            Mode.Downloaded -> AnalyticsEvent.DOWNLOADS_SELECT_ALL_TAPPED
            Mode.History -> AnalyticsEvent.LISTENING_HISTORY_SELECT_ALL_TAPPED
            Mode.Starred -> AnalyticsEvent.STARRED_SELECT_ALL_TAPPED
        }
        analyticsTracker.track(analyticsEvent, mapOf(SELECT_ALL_KEY to selectAll))
    }

    private fun trackSelectAllAbove() {
        val analyticsEvent = when (mode) {
            Mode.Downloaded -> AnalyticsEvent.DOWNLOADS_SELECT_ALL_ABOVE_TAPPED
            Mode.History -> AnalyticsEvent.LISTENING_HISTORY_SELECT_ALL_ABOVE_TAPPED
            Mode.Starred -> AnalyticsEvent.STARRED_SELECT_ALL_ABOVE_TAPPED
        }
        analyticsTracker.track(analyticsEvent)
    }

    private fun trackSelectAllBelow() {
        val analyticsEvent = when (mode) {
            Mode.Downloaded -> AnalyticsEvent.DOWNLOADS_SELECT_ALL_BELOW_TAPPED
            Mode.History -> AnalyticsEvent.LISTENING_HISTORY_SELECT_ALL_BELOW_TAPPED
            Mode.Starred -> AnalyticsEvent.STARRED_SELECT_ALL_BELOW_TAPPED
        }
        analyticsTracker.track(analyticsEvent)
    }

    private fun trackMultiSelectEntered() {
        val analyticsEvent = when (mode) {
            Mode.Downloaded -> AnalyticsEvent.DOWNLOADS_MULTI_SELECT_ENTERED
            Mode.History -> AnalyticsEvent.LISTENING_HISTORY_MULTI_SELECT_ENTERED
            Mode.Starred -> AnalyticsEvent.STARRED_MULTI_SELECT_ENTERED
        }
        analyticsTracker.track(analyticsEvent)
    }

    private fun trackMultiSelectExited() {
        val analyticsEvent = when (mode) {
            Mode.Downloaded -> AnalyticsEvent.DOWNLOADS_MULTI_SELECT_EXITED
            Mode.History -> AnalyticsEvent.LISTENING_HISTORY_MULTI_SELECT_EXITED
            Mode.Starred -> AnalyticsEvent.STARRED_MULTI_SELECT_EXITED
        }
        analyticsTracker.track(analyticsEvent)
    }
}
