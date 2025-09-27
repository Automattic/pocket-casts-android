package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentUpnextBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.UpNextViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.NavigationBarColor
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.hideShadow
import au.com.shiftyjelly.pocketcasts.views.extensions.quickScrollToTop
import au.com.shiftyjelly.pocketcasts.views.extensions.tintIcons
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragmentToolbar.ChromeCastButton
import au.com.shiftyjelly.pocketcasts.views.fragments.TopScrollable
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper.Companion.MULTI_SELECT_TOGGLE_PAYLOAD
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectToolbar
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeActionViewModel
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeSource
import au.com.shiftyjelly.pocketcasts.views.swipe.handleAction
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class UpNextFragment :
    BaseFragment(),
    UpNextListener,
    UpNextTouchCallback.ItemTouchHelperAdapter,
    TopScrollable {
    companion object {
        private const val ARG_EMBEDDED = "embedded"
        private const val ARG_SOURCE = "source"
        private const val SOURCE_KEY = "source"
        private const val SELECT_ALL_KEY = "select_all"
        private const val DIRECTION_KEY = "direction"
        private const val SLOTS_KEY = "slots"
        private const val IS_NEXT_KEY = "is_next"
        private const val DOWN = "down"
        private const val UP = "up"
        private const val UP_NEXT_ADAPTER_POSITION = 2

        fun newInstance(embedded: Boolean = false, source: UpNextSource): UpNextFragment {
            val fragment = UpNextFragment()
            fragment.arguments = bundleOf(ARG_EMBEDDED to embedded, ARG_SOURCE to source.analyticsValue)
            return fragment
        }
    }

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var episodeManager: EpisodeManager

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var multiSelectHelper: MultiSelectEpisodesHelper

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var swipeRowActionsFactory: SwipeRowActions.Factory

    lateinit var adapter: UpNextAdapter
    private val sourceView = SourceView.UP_NEXT
    private val playerViewModel by activityViewModels<PlayerViewModel>()
    private val upNextViewModel by viewModels<UpNextViewModel>()
    private val swipeActionViewModel by viewModels<SwipeActionViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SwipeActionViewModel.Factory> { factory ->
                factory.create(SwipeSource.Files, playlistUuid = null)
            }
        },
    )

    private var userRearrangingFrom: Int? = null
    private var userDraggingStart: Int? = null
    private var playingEpisodeAtStartOfDrag: String? = null

    private var realBinding: FragmentUpnextBinding? = null
    private val binding: FragmentUpnextBinding get() = realBinding ?: throw IllegalStateException("Trying to access the binding outside of the view lifecycle.")

    private var itemTouchHelper: ItemTouchHelper? = null

    private var upNextItems: List<Any> = emptyList()
    private val upNextEpisodes: List<BaseEpisode>
        get() = upNextItems.filterIsInstance<BaseEpisode>()

    val isEmbedded: Boolean
        get() = arguments?.getBoolean(ARG_EMBEDDED) ?: false

    val upNextSource: UpNextSource
        get() = arguments?.getString(ARG_SOURCE)?.let { UpNextSource.fromString(it) } ?: UpNextSource.UNKNOWN

    val overrideTheme: Theme.ThemeType
        get() = theme.getUpNextTheme(isFullScreen = upNextSource != UpNextSource.UP_NEXT_TAB)

    val multiSelectListener = object : MultiSelectHelper.Listener<BaseEpisode> {
        override fun multiSelectSelectAll() {
            trackUpNextEvent(
                AnalyticsEvent.UP_NEXT_SELECT_ALL_TAPPED,
                mapOf(SELECT_ALL_KEY to true),
            )
            multiSelectHelper.selectAllInList(upNextEpisodes)
            adapter.notifyDataSetChanged()
        }

        override fun multiSelectSelectNone() {
            trackUpNextEvent(
                AnalyticsEvent.UP_NEXT_SELECT_ALL_TAPPED,
                mapOf(SELECT_ALL_KEY to false),
            )
            multiSelectHelper.deselectAllInList(upNextEpisodes)
            adapter.notifyDataSetChanged()
        }

        override fun multiSelectSelectAllUp(multiSelectable: BaseEpisode) {
            val startIndex = upNextEpisodes.indexOf(multiSelectable)
            if (startIndex > -1) {
                val episodesAbove = upNextEpisodes.subList(0, startIndex + 1)
                multiSelectHelper.selectAllInList(episodesAbove)
            }

            adapter.notifyDataSetChanged()
        }

        override fun multiSelectSelectAllDown(multiSelectable: BaseEpisode) {
            val startIndex = upNextEpisodes.indexOf(multiSelectable)
            if (startIndex > -1) {
                val episodesBelow = upNextEpisodes.subList(startIndex, upNextEpisodes.size)
                multiSelectHelper.selectAllInList(episodesBelow)
            }

            adapter.notifyDataSetChanged()
        }

        override fun multiDeselectAllBelow(multiSelectable: BaseEpisode) {
            val startIndex = upNextEpisodes.indexOf(multiSelectable)
            if (startIndex > -1) {
                val episodesBelow = upNextEpisodes.subList(startIndex, upNextEpisodes.size)
                multiSelectHelper.deselectAllInList(episodesBelow)
            }
            adapter.notifyDataSetChanged()
        }

        override fun multiDeselectAllAbove(multiSelectable: BaseEpisode) {
            val startIndex = upNextEpisodes.indexOf(multiSelectable)
            if (startIndex > -1) {
                val episdesAbove = upNextEpisodes.subList(0, startIndex + 1)
                multiSelectHelper.deselectAllInList(episdesAbove)
            }
            adapter.notifyDataSetChanged()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val themedContext = ContextThemeWrapper(activity, overrideTheme.resourceId)
        val themedInflater = inflater.cloneInContext(themedContext)
        val binding = FragmentUpnextBinding.inflate(themedInflater, container, false).also {
            realBinding = it
        }
        if (upNextSource == UpNextSource.UP_NEXT_TAB) {
            analyticsTracker.track(AnalyticsEvent.UP_NEXT_SHOWN, mapOf("source" to "tab_bar"))
        }

        binding.emptyUpNextView.setContentWithViewCompositionStrategy {
            val upNextState by remember {
                playerViewModel.upNextStateObservable.asFlow()
            }.collectAsStateWithLifecycle(null)

            AppTheme(theme.activeTheme) {
                if (upNextState is UpNextQueue.State.Empty) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    ) {
                        UpNextNoContentBanner(
                            onDiscoverClick = ::onDiscoverTapped,
                            modifier = Modifier.padding(vertical = 24.dp),
                        )
                    }
                }
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        itemTouchHelper = null
        binding.recyclerView.adapter = null
        super.onDestroyView()
        multiSelectHelper.context = null
        multiSelectHelper.listener = null
        realBinding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        multiSelectHelper.source = SourceView.UP_NEXT
        adapter = UpNextAdapter(
            context = context,
            episodeManager = episodeManager,
            listener = this,
            multiSelectHelper = multiSelectHelper,
            fragmentManager = childFragmentManager,
            analyticsTracker = analyticsTracker,
            upNextSource = upNextSource,
            settings = settings,
            playbackManager = playbackManager,
            swipeRowActionsFactory = swipeRowActionsFactory,
            onSwipeAction = { episode, swipeAction ->
                viewLifecycleOwner.lifecycleScope.launch {
                    swipeActionViewModel.handleAction(swipeAction, episode.uuid, childFragmentManager)
                }
            },
        )
        adapter.theme = overrideTheme
    }

    override fun onResume() {
        super.onResume()
        if (!isEmbedded) {
            updateStatusAndNavColors()
        }
    }

    private fun updateStatusAndNavColors() {
        activity?.let {
            theme.updateWindowNavigationBarColor(window = it.window, navigationBarColor = NavigationBarColor.UpNext(isFullScreen = true))
            theme.updateWindowStatusBarIcons(window = it.window, statusBarIconColor = StatusBarIconColor.Theme)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appBarLayout.hideShadow()

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        setupToolbarAndStatusBar(
            toolbar = binding.toolbar,
            title = getString(LR.string.up_next),
            menu = R.menu.upnext,
            navigationIcon = if (upNextSource != UpNextSource.UP_NEXT_TAB) {
                NavigationIcon.Close
            } else {
                NavigationIcon.None
            },
            chromeCastButton = ChromeCastButton.Shown(chromeCastAnalytics),
            onNavigationClick = { close() },
            toolbarColors = null,
        )
        if (upNextSource != UpNextSource.UP_NEXT_TAB) {
            toolbar.setNavigationIcon(IR.drawable.ic_close)
        }
        toolbar.menu.findItem(R.id.media_route_menu_item)?.isVisible = upNextSource == UpNextSource.UP_NEXT_TAB
        toolbar.navigationIcon?.setTint(ThemeColor.secondaryIcon01(overrideTheme))
        toolbar.menu.tintIcons(ThemeColor.secondaryIcon01(overrideTheme))
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_select -> {
                    multiSelectHelper.isMultiSelecting = true
                    true
                }

                R.id.clear_up_next -> {
                    onClearUpNext()
                    true
                }

                else -> false
            }
        }

        val recyclerView = binding.recyclerView
        recyclerView.adapter = adapter
        playerViewModel.upNextLive.observe(viewLifecycleOwner) {
            if (userRearrangingFrom == null) {
                adapter.submitList(it)
                upNextItems = it
            }
        }

        playerViewModel.listDataLive.observe(viewLifecycleOwner) {
            adapter.isPlaying = it.podcastHeader.isPlaying
            adapter.updateUpNextEmptyState(it.upNextEpisodes.isNotEmpty())
            toolbar.menu.findItem(R.id.menu_select)?.isVisible = it.upNextEpisodes.isNotEmpty()
            toolbar.menu.findItem(R.id.clear_up_next)?.isVisible = it.upNextEpisodes.isNotEmpty()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            upNextViewModel.isSignedInAsPaidUser.collect { isSignedInAsPaidUser ->
                adapter.updateUserSignInState(isSignedInAsPaidUser)
                adapter.notifyDataSetChanged()
            }
        }

        view.isClickable = true

        val callback = UpNextTouchCallback(adapter = this)
        itemTouchHelper = ItemTouchHelper(callback).apply {
            attachToRecyclerView(recyclerView)
        }

        (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.changeDuration = 0

        val multiSelectToolbar = view.findViewById<MultiSelectToolbar>(R.id.multiSelectToolbar)
        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            val wasMultiSelecting = multiSelectToolbar.isVisible
            if (wasMultiSelecting == isMultiSelecting) {
                return@observe
            }

            multiSelectToolbar.isVisible = isMultiSelecting
            toolbar.isVisible = !isMultiSelecting

            /* Track only if not embedded. If it is an embedded fragment, then track only when in expanded state */
            if (!isEmbedded || isEmbeddedExpanded()) {
                if (isMultiSelecting) {
                    trackUpNextEvent(AnalyticsEvent.UP_NEXT_MULTI_SELECT_ENTERED)
                } else {
                    trackUpNextEvent(AnalyticsEvent.UP_NEXT_MULTI_SELECT_EXITED)
                }
            }

            multiSelectToolbar.setNavigationIcon(IR.drawable.ic_arrow_back)

            adapter.notifyItemRangeChanged(0, adapter.itemCount, MULTI_SELECT_TOGGLE_PAYLOAD)
        }
        multiSelectHelper.listener = multiSelectListener

        multiSelectHelper.context = view.context
        multiSelectToolbar.setup(viewLifecycleOwner, multiSelectHelper, menuRes = VR.menu.menu_multiselect_upnext, activity = requireActivity())

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect {
                    binding.recyclerView.updatePadding(bottom = it)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (multiSelectHelper.isMultiSelecting) {
            multiSelectHelper.isMultiSelecting = false
        }
    }

    fun onExpanded() {
        multiSelectHelper.listener = multiSelectListener
    }

    fun onCollapsed() {
        multiSelectHelper.listener = null
    }

    private fun close() {
        if (!isEmbedded) {
            (activity as? FragmentHostListener)?.closeBottomSheet()
        } else {
            (parentFragment as? PlayerContainerFragment)?.upNextBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun isEmbeddedExpanded() = isEmbedded &&
        (parentFragment as? PlayerContainerFragment)?.upNextBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED

    override fun onClearUpNext() {
        playerViewModel.clearUpNext(context = requireContext(), upNextSource = upNextSource)
            .showClearUpNextConfirmationDialog(parentFragmentManager, tag = "up_next_clear_dialog")
    }

    override fun onDiscoverTapped() {
        if (upNextSource == UpNextSource.NOW_PLAYING) {
            (activity as FragmentHostListener).closePlayer()
        } else if (upNextSource == UpNextSource.MINI_PLAYER) {
            close()
        }
        analyticsTracker.track(AnalyticsEvent.UP_NEXT_DISCOVER_BUTTON_TAPPED, mapOf("source" to upNextSource.analyticsValue))
        (activity as FragmentHostListener).openTab(VR.id.navigation_discover)
    }

    override fun onUpNextEpisodeStartDrag(viewHolder: RecyclerView.ViewHolder) {
        val itemTouchHelper = itemTouchHelper ?: return
        itemTouchHelper.startDrag(viewHolder)
        viewHolder.setIsRecyclable(false)
        userDraggingStart = viewHolder.bindingAdapterPosition
    }

    override fun onEpisodeActionsClick(episodeUuid: String, podcastUuid: String?) {
        if (settings.tapOnUpNextShouldPlay.value) {
            playerViewModel.playEpisode(uuid = episodeUuid, sourceView = sourceView)
        } else {
            (activity as? FragmentHostListener)?.openEpisodeDialog(
                episodeUuid = episodeUuid,
                source = EpisodeViewSource.UP_NEXT,
                podcastUuid = podcastUuid,
                forceDark = true,
                autoPlay = false,
            )
        }
    }

    override fun onEpisodeActionsLongPress(episodeUuid: String, podcastUuid: String?) {
        if (settings.tapOnUpNextShouldPlay.value) {
            (activity as? FragmentHostListener)?.openEpisodeDialog(
                episodeUuid = episodeUuid,
                source = EpisodeViewSource.UP_NEXT,
                podcastUuid = podcastUuid,
                forceDark = true,
                autoPlay = false,
            )
        } else {
            playerViewModel.playEpisode(uuid = episodeUuid, sourceView = sourceView)
        }
    }

    override fun onUpNextEpisodeMove(fromPosition: Int, toPosition: Int) {
        if (userRearrangingFrom == null) {
            userRearrangingFrom = fromPosition
        } else if (userRearrangingFrom != fromPosition) {
            Timber.d("Ignoring drag from $fromPosition because it doesn't match our dragging row")
            return
        }

        playingEpisodeAtStartOfDrag = playbackManager.upNextQueue.currentEpisode?.uuid

        Timber.d("Swapping $fromPosition to $toPosition")
        val listData = upNextItems.toMutableList()

        listData.add(toPosition, listData.removeAt(fromPosition))

        adapter.submitList(listData)
        upNextItems = listData.toList()

        userRearrangingFrom = toPosition
    }

    override fun onUpNextItemTouchHelperFinished(position: Int) {
        if (playingEpisodeAtStartOfDrag == playbackManager.upNextQueue.currentEpisode?.uuid) {
            playerViewModel.changeUpNextEpisodes(upNextItems.subList(1, upNextItems.size).filterIsInstance<BaseEpisode>())
        } else {
            playerViewModel.upNextLive.value?.let {
                upNextItems = it
                adapter.submitList(upNextItems)
            }
        }

        userDraggingStart?.let { dragStartPosition ->
            if (position != userDraggingStart) {
                trackUpNextEvent(
                    AnalyticsEvent.UP_NEXT_QUEUE_REORDERED,
                    mapOf(
                        SLOTS_KEY to abs(position.minus(dragStartPosition)),
                        DIRECTION_KEY to if (position > dragStartPosition) DOWN else UP,
                        IS_NEXT_KEY to (position == UP_NEXT_ADAPTER_POSITION),
                    ),
                )
            }
        }

        userRearrangingFrom = null
        userDraggingStart = null
        playingEpisodeAtStartOfDrag = null
    }

    override fun onNowPlayingClick() {
        (activity as? FragmentHostListener)?.openPlayer()
        close()
    }

    private fun trackUpNextEvent(event: AnalyticsEvent, props: Map<String, Any> = emptyMap()) {
        val properties = HashMap<String, Any>()
        properties[SOURCE_KEY] = upNextSource.analyticsValue
        properties.putAll(props)
        analyticsTracker.track(event, properties)
    }

    override fun scrollToTop(): Boolean {
        val canScroll = binding.recyclerView.canScrollVertically(-1)
        binding.recyclerView.quickScrollToTop()
        return canScroll
    }
}
