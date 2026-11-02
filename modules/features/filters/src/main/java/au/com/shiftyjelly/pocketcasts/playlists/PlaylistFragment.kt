package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.mediarouter.app.MediaRouteButton
import au.com.shiftyjelly.pocketcasts.PlayAllFragment
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentData
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.filters.R
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderButtonData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderData
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddEpisodesFragment
import au.com.shiftyjelly.pocketcasts.playlists.manual.UnavailableEpisodeFragment
import au.com.shiftyjelly.pocketcasts.playlists.smart.EditRulesFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeContainerFragment
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayAllResponse
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton
import au.com.shiftyjelly.pocketcasts.views.compose.EpisodeRow
import au.com.shiftyjelly.pocketcasts.views.compose.EpisodeRowTrailing
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectController
import au.com.shiftyjelly.pocketcasts.views.compose.MultiSelectTopBar
import au.com.shiftyjelly.pocketcasts.views.compose.UnavailableEpisodeRow
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeActionViewModel
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeSource
import au.com.shiftyjelly.pocketcasts.views.swipe.handleAction
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.FilterDeselectAllAboveEvent
import com.automattic.eventhorizon.FilterDeselectAllBelowEvent
import com.automattic.eventhorizon.FilterDeselectAllEvent
import com.automattic.eventhorizon.FilterMultiSelectEnteredEvent
import com.automattic.eventhorizon.FilterMultiSelectExitedEvent
import com.automattic.eventhorizon.FilterSelectAllAboveEvent
import com.automattic.eventhorizon.FilterSelectAllBelowEvent
import com.automattic.eventhorizon.FilterSelectAllEvent
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class PlaylistFragment :
    BaseFragment(),
    HasBackstack {

    @Inject
    lateinit var rowDataProvider: EpisodeRowDataProvider

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var playButtonListener: PlayButton.OnClickListener

    @Inject
    lateinit var multiSelectHelper: MultiSelectEpisodesHelper

    @Inject
    lateinit var swipeRowActionsFactory: SwipeRowActions.Factory

    @Inject
    lateinit var eventHorizon: EventHorizon

    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_ARGS)

    private val viewModel by viewModels<PlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<PlaylistViewModel.Factory> { factory ->
                factory.create(args.uuid, args.type)
            }
        },
    )

    private val swipeActionViewModel by viewModels<SwipeActionViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SwipeActionViewModel.Factory> { factory ->
                factory.create(SwipeSource.Filters, args.uuid)
            }
        },
    )

    private val imageRequestFactory by lazy { PocketCastsImageRequestFactory(requireContext()).themed().smallSize() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            viewModel.trackFilterShown()
            viewModel.setSelectedPlaylist()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val chromeCastButton = MediaRouteButton(requireContext()).apply { isVisible = false }
        setupMultiSelect()
        setupChromeCast(chromeCastButton)
        setupSettings()
        setupUpPlayAllAction()
        setupUpNextSavedAsPlaylist()
        val content = contentWithoutConsumedInsets {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val bottomInset by viewModel.bottomInset.collectAsStateWithLifecycle(0)
            val controller = remember { MultiSelectController(multiSelectHelper, viewLifecycleOwner.lifecycleScope) }
            val isMultiSelecting by controller.isMultiSelecting.collectAsStateWithLifecycle()
            AppTheme(theme.activeTheme) {
                PlaylistPage(
                    headerData = uiState.playlist?.let { playlist -> PlaylistHeaderData(playlist.title, playlist.metadata) },
                    episodes = uiState.playlist?.episodes.orEmpty(),
                    contentState = uiState.contentState(),
                    noContentData = rememberNoContentData(uiState.isAnyPodcastFollowed),
                    leftButton = leftHeaderButton,
                    rightButton = rightHeaderButton,
                    searchState = viewModel.searchState.textState,
                    bottomInsetPx = bottomInset,
                    listener = pageListener,
                    multiSelectTopBar = if (isMultiSelecting) {
                        { MultiSelectTopBar(controller, eventHorizon) }
                    } else {
                        null
                    },
                    episodeRow = { episode -> EpisodeRow(episode, controller, isMultiSelecting) },
                )
            }
        }
        return FrameLayout(requireContext()).apply {
            addView(chromeCastButton)
            addView(content)
            addView(FragmentContainerView(context).apply { id = R.id.playlistFragmentContainer })
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.updateAutoPlaySource()
    }

    private fun setupChromeCast(chromeCastButton: MediaRouteButton) {
        CastButtonFactory.setUpMediaRouteButton(requireContext(), chromeCastButton)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chromeCastSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { chromeCastButton.performClick() }
        }
    }

    private fun setupSettings() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showSettingsSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { openSettings() }
        }
    }

    private fun setupUpPlayAllAction() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.playAllResponseSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { response ->
                    if (parentFragmentManager.findFragmentByTag("confirm_and_play") != null) {
                        return@collect
                    }
                    when (response) {
                        PlayAllResponse.ShowWarning -> {
                            PlayAllFragment().show(childFragmentManager, "confirm_play_all")
                        }

                        PlayAllResponse.ShowNoEpisodesToPlay -> {
                            val snackbarView = (requireActivity() as FragmentHostListener).snackBarView()
                            Snackbar.make(snackbarView, getString(LR.string.play_all_no_episodes_message), Snackbar.LENGTH_LONG).show()
                        }

                        PlayAllResponse.DoNothing -> Unit
                    }
                }
        }
    }

    private fun setupUpNextSavedAsPlaylist() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.upNextSavedAsPlaylistSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect {
                    val hostListener = requireActivity() as FragmentHostListener
                    Snackbar.make(hostListener.snackBarView(), getString(LR.string.up_next_as_playlist_saved), Snackbar.LENGTH_LONG)
                        .setAction(LR.string.view) { hostListener.closeFiltersToRoot() }
                        .show()
                }
        }
    }

    private val leftHeaderButton by lazy {
        when (args.type) {
            Playlist.Type.Manual -> PlaylistHeaderButtonData(
                iconId = IR.drawable.ic_playlist_add_episode,
                label = getString(LR.string.add_episodes),
                onClick = {
                    viewModel.trackAddEpisodesTapped()
                    openEditor()
                },
            )

            Playlist.Type.Smart -> PlaylistHeaderButtonData(
                iconId = IR.drawable.ic_playlist_smart_rules,
                label = getString(LR.string.smart_rules),
                onClick = {
                    viewModel.trackEditRulesTapped()
                    openEditor()
                },
            )
        }
    }

    private val rightHeaderButton by lazy {
        PlaylistHeaderButtonData(
            iconId = IR.drawable.ic_playlist_play,
            label = getString(LR.string.play_all),
            onClick = {
                viewModel.trackPlayAllTapped()
                viewModel.handlePlayAllAction()
            },
        )
    }

    private val pageListener = PlaylistPageListener(
        onClickBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
        onClickOptions = {
            viewModel.trackFilterOptionsButtonTapped()
            openOptionsSheet()
        },
        onShowArchivedToggle = {
            viewModel.trackToggleShowArchived()
            viewModel.toggleShowArchived()
        },
        onClickShowArchivedCta = {
            viewModel.trackShowArchivedCtaTapped()
            viewModel.toggleShowArchived()
        },
        onHideKeyboard = { view?.let(UiUtil::hideKeyboard) },
    )

    private fun PlaylistViewModel.UiState.contentState() = when (playlist?.episodes?.size) {
        null -> ContentState.Uninitialized

        0 -> if (playlist.metadata.totalEpisodeCount == 0) {
            ContentState.HasNoEpisodes
        } else {
            ContentState.HasEpisode
        }

        else -> ContentState.HasEpisode
    }

    @Composable
    private fun EpisodeRow(
        episode: PlaylistEpisode,
        controller: MultiSelectController<BaseEpisode>,
        isMultiSelecting: Boolean,
    ) {
        when (episode) {
            is PlaylistEpisode.Available -> {
                val selected by controller.selected.collectAsStateWithLifecycle()
                EpisodeRow(
                    episode = episode.episode,
                    rowDataProvider = rowDataProvider,
                    imageRequestFactory = imageRequestFactory,
                    swipeActions = { swipeRowActionsFactory.availablePlaylistEpisode(args.type, episode.episode) },
                    trailing = EpisodeRowTrailing.PlayButton(playButtonListener, eventHorizon),
                    onClick = {
                        if (multiSelectHelper.isMultiSelecting) {
                            multiSelectHelper.toggle(episode.episode)
                        } else {
                            openEpisode(episode)
                        }
                    },
                    onLongClick = { multiSelectHelper.defaultLongPress(episode.episode, childFragmentManager) },
                    onSwipeAction = { action -> handleSwipeAction(episode, action) },
                    useEpisodeArtwork = settings.artworkConfiguration.value.useEpisodeArtwork(ArtworkConfiguration.Element.Filters),
                    streamByDefault = settings.streamingMode.value,
                    isMultiSelecting = isMultiSelecting,
                    isSelected = selected.any { it.uuid == episode.uuid },
                    animateMultiSelection = true,
                )
            }

            is PlaylistEpisode.Unavailable -> UnavailableEpisodeRow(
                episode = episode.episode,
                imageRequestFactory = imageRequestFactory,
                swipeActions = swipeRowActionsFactory.unavailablePlaylistEpisode(),
                onClick = { openUnavailableEpisode(episode) },
                onSwipeAction = { action -> handleSwipeAction(episode, action) },
                isMultiSelecting = isMultiSelecting,
            )
        }
    }

    private fun openEpisode(episode: PlaylistEpisode.Available) {
        if (parentFragmentManager.findFragmentByTag("episode_card") == null) {
            EpisodeContainerFragment.newInstance(
                episode = episode.episode,
                source = EpisodeViewSource.FILTERS,
            ).show(parentFragmentManager, "episode_card")
        }
    }

    private fun openUnavailableEpisode(episode: PlaylistEpisode.Unavailable) {
        if (childFragmentManager.findFragmentByTag("unavailable_episode_sheet") == null) {
            UnavailableEpisodeFragment.newInstance(
                episodeUuid = episode.uuid,
                podcastUuid = episode.podcastUuid,
            ).show(childFragmentManager, "unavailable_episode_sheet")
        }
    }

    private fun handleSwipeAction(episode: PlaylistEpisode, action: SwipeAction) {
        viewLifecycleOwner.lifecycleScope.launch {
            swipeActionViewModel.handleAction(
                action = action,
                episodeUuid = episode.uuid,
                podcastUuid = episode.podcastUuid,
                fragmentManager = childFragmentManager,
            )
        }
    }

    private fun setupMultiSelect() {
        viewLifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                multiSelectHelper.isMultiSelecting = false
            }

            override fun onDestroy(owner: LifecycleOwner) {
                multiSelectHelper.cleanup()
            }
        })
        playButtonListener.source = SourceView.FILTERS
        multiSelectHelper.context = requireActivity()
        multiSelectHelper.source = SourceView.FILTERS
        var wasMultiSelecting = false
        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            if (wasMultiSelecting == isMultiSelecting) {
                return@observe
            }
            wasMultiSelecting = isMultiSelecting
            val filterType = args.type.analyticsValue
            eventHorizon.track(
                if (isMultiSelecting) FilterMultiSelectEnteredEvent(filterType) else FilterMultiSelectExitedEvent(filterType),
            )
        }
        multiSelectHelper.listener = PlaylistMultiSelectListener()
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.startMultiSelectingSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { multiSelectHelper.isMultiSelecting = true }
        }
    }

    private inner class PlaylistMultiSelectListener : MultiSelectHelper.Listener<BaseEpisode> {
        private val filterType get() = args.type.analyticsValue

        private val episodes get() = viewModel.uiState.value.playlist?.episodes.orEmpty().map(PlaylistEpisode::toMultiselectEpisode)

        override fun multiSelectSelectAll() {
            eventHorizon.track(FilterSelectAllEvent(filterType))
            multiSelectHelper.selectAllInList(episodes)
        }

        override fun multiSelectSelectNone() {
            eventHorizon.track(FilterDeselectAllEvent(filterType))
            multiSelectHelper.deselectAllInList(episodes)
        }

        override fun multiSelectSelectAllUp(multiSelectable: BaseEpisode) {
            eventHorizon.track(FilterSelectAllAboveEvent(filterType))
            val episodes = episodes
            val startIndex = episodes.indexOf(multiSelectable)
            if (startIndex > -1) {
                multiSelectHelper.selectAllInList(episodes.subList(0, startIndex + 1))
            }
        }

        override fun multiDeselectAllBelow(multiSelectable: BaseEpisode) {
            eventHorizon.track(FilterDeselectAllBelowEvent(filterType))
            val episodes = episodes
            val startIndex = episodes.indexOf(multiSelectable)
            if (startIndex > -1) {
                multiSelectHelper.deselectAllInList(episodes.subList(startIndex, episodes.size))
            }
        }

        override fun multiDeselectAllAbove(multiSelectable: BaseEpisode) {
            eventHorizon.track(FilterDeselectAllAboveEvent(filterType))
            val episodes = episodes
            val startIndex = episodes.indexOf(multiSelectable)
            if (startIndex > -1) {
                multiSelectHelper.deselectAllInList(episodes.subList(0, startIndex + 1))
            }
        }

        override fun multiSelectSelectAllDown(multiSelectable: BaseEpisode) {
            eventHorizon.track(FilterSelectAllBelowEvent(filterType))
            val episodes = episodes
            val startIndex = episodes.indexOf(multiSelectable)
            if (startIndex > -1) {
                multiSelectHelper.selectAllInList(episodes.subList(startIndex, episodes.size))
            }
        }
    }

    @Composable
    private fun rememberNoContentData(isAnyPodcastFollowed: Boolean) = remember(args.type, isAnyPodcastFollowed) {
        when (args.type) {
            Playlist.Type.Smart -> NoContentData(
                title = getString(LR.string.smart_playlist_no_content_title),
                body = getString(LR.string.smart_playlist_no_content_body),
                iconId = IR.drawable.ic_info,
                primaryButton = NoContentData.Button(
                    text = getString(LR.string.smart_rules),
                    onClick = {
                        viewModel.trackEditRulesCtaTapped()
                        openEditor()
                    },
                ),
            )

            Playlist.Type.Manual -> {
                if (isAnyPodcastFollowed) {
                    NoContentData(
                        title = getString(LR.string.manual_playlist_no_content_title_alternative),
                        body = getString(LR.string.manual_playlist_no_content_body_alternative),
                        iconId = IR.drawable.ic_playlists,
                        primaryButton = NoContentData.Button(
                            text = getString(LR.string.add_episodes),
                            onClick = {
                                viewModel.trackAddEpisodeCtaTapped()
                                openEditor()
                            },
                        ),
                    )
                } else {
                    NoContentData(
                        title = getString(LR.string.manual_playlist_no_content_title),
                        body = getString(LR.string.manual_playlist_no_content_body),
                        iconId = IR.drawable.ic_playlists,
                        primaryButton = NoContentData.Button(
                            text = getString(LR.string.browse_podcasts),
                            onClick = {
                                viewModel.trackBrowseShowsCtaTapped()
                                val hostListener = (requireActivity() as FragmentHostListener)
                                hostListener.closeToRoot()
                                hostListener.openTab(VR.id.navigation_discover)
                            },
                        ),
                    )
                }
            }
        }
    }

    private fun openEditor() {
        when (args.type) {
            Playlist.Type.Manual -> openManualEditor()
            Playlist.Type.Smart -> openSmartEditor()
        }
    }

    private fun openManualEditor() {
        val episodeCount = viewModel.uiState.value.playlist?.metadata?.totalEpisodeCount ?: Int.MAX_VALUE
        if (episodeCount >= PlaylistManager.MANUAL_PLAYLIST_EPISODE_LIMIT) {
            val snackbarView = (requireActivity() as FragmentHostListener).snackBarView()
            Snackbar.make(snackbarView, getString(LR.string.add_to_playlist_failure_message), Snackbar.LENGTH_LONG).show()
            return
        }
        if (parentFragmentManager.findFragmentByTag("playlist_episode_editor") != null) {
            return
        }
        AddEpisodesFragment.newInstance(args.uuid).show(parentFragmentManager, "playlist_episode_editor")
    }

    private fun openSmartEditor() {
        if (parentFragmentManager.findFragmentByTag("playlist_rules_editor") != null) {
            return
        }
        EditRulesFragment.newInstance(args.uuid).show(parentFragmentManager, "playlist_rules_editor")
    }

    private fun openOptionsSheet() {
        if (childFragmentManager.findFragmentByTag("playlist_options_sheet") != null) {
            return
        }
        OptionsFragment().show(childFragmentManager, "playlist_options_sheet")
    }

    private fun openSettings() {
        if (childFragmentManager.findFragmentByTag("playlist_settings") != null) {
            return
        }
        childFragmentManager.commit {
            addToBackStack("playlist_settings")
            add(R.id.playlistFragmentContainer, SettingsFragment(), "playlist_settings")
        }
    }

    override fun onPause() {
        if (isRemoving) {
            viewModel.clearSelectedPlaylist()
        }
        super.onPause()
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
        return (if (multiSelectHelper.isMultiSelecting) 1 else 0) + super.getBackstackCount()
    }

    @Parcelize
    private class Args(
        val uuid: String,
        val type: Playlist.Type,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "PlaylistsFragmentArgs"

        fun newInstance(
            uuid: String,
            type: Playlist.Type,
        ) = PlaylistFragment().apply {
            arguments = Bundle().apply {
                putParcelable(NEW_INSTANCE_ARGS, Args(uuid, type))
            }
        }
    }
}

private fun PlaylistEpisode.toMultiselectEpisode() = when (this) {
    is PlaylistEpisode.Available -> episode
    is PlaylistEpisode.Unavailable -> unavailableMultiselectPlaceholder
}

private val unavailableMultiselectPlaceholder = PodcastEpisode(uuid = "unavailable", publishedDate = Date(0))
