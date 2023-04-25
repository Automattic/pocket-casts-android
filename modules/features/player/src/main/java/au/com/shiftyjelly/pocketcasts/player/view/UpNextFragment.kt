package au.com.shiftyjelly.pocketcasts.player.view

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentUpnextBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.extensions.tintIcons
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper.SwipeSource
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectToolbar
import au.com.shiftyjelly.pocketcasts.views.tour.TourStep
import au.com.shiftyjelly.pocketcasts.views.tour.TourViewTag
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import kotlin.math.abs
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class UpNextFragment : BaseFragment(), UpNextListener, UpNextTouchCallback.ItemTouchHelperAdapter {
    companion object {
        private const val ARG_EMBEDDED = "embedded"
        private const val ARG_SOURCE = "source"
        private const val ACTION_KEY = "action"
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

    @Inject lateinit var settings: Settings
    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var multiSelectHelper: MultiSelectHelper
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    lateinit var adapter: UpNextAdapter
    private val playbackSource = AnalyticsSource.UP_NEXT
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private var userRearrangingFrom: Int? = null
    private var userDraggingStart: Int? = null
    private var playingEpisodeAtStartOfDrag: String? = null

    private var realBinding: FragmentUpnextBinding? = null
    private val binding: FragmentUpnextBinding get() = realBinding ?: throw IllegalStateException("Trying to access the binding outside of the view lifecycle.")

    private var episodeItemTouchHelper: EpisodeItemTouchHelper? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    private var upNextItems: List<Any> = emptyList()
    private val upNextEpisodes: List<BaseEpisode>
        get() = upNextItems.filterIsInstance<BaseEpisode>()

    val isEmbedded: Boolean
        get() = arguments?.getBoolean(ARG_EMBEDDED) ?: false

    val upNextSource: UpNextSource
        get() = arguments?.getString(ARG_SOURCE)?.let { UpNextSource.fromString(it) } ?: UpNextSource.UNKNOWN

    val overrideTheme: Theme.ThemeType
        get() = if (Theme.isDark(context)) theme.activeTheme else Theme.ThemeType.DARK

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val themedContext = ContextThemeWrapper(activity, UR.style.ThemeDark)
        val themedInflater = if (!Theme.isDark(context)) inflater.cloneInContext(themedContext) else inflater // If the theme is not dark we force it to ThemeDark
        val binding = FragmentUpnextBinding.inflate(themedInflater, container, false).also {
            realBinding = it
        }
        return binding.root
    }

    override fun onDestroyView() {
        episodeItemTouchHelper = null
        itemTouchHelper = null
        binding.recyclerView.adapter = null
        super.onDestroyView()
        multiSelectHelper.context = null
        realBinding = null
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val imageLoader = PodcastImageLoaderThemed(context)
        multiSelectHelper.source = AnalyticsSource.UP_NEXT
        adapter = UpNextAdapter(
            context = context,
            imageLoader = imageLoader,
            episodeManager = episodeManager,
            listener = this,
            multiSelectHelper = multiSelectHelper,
            fragmentManager = childFragmentManager,
            analyticsTracker = analyticsTracker,
            upNextSource = upNextSource,
            settings = settings
        )
        adapter.theme = overrideTheme

        if (!isEmbedded) {
            updateStatusAndNavColors()
            FirebaseAnalyticsTracker.openedUpNext()
        }
    }

    private fun updateStatusAndNavColors() {
        activity?.let {
            theme.setNavigationBarColor(it.window, true, ThemeColor.primaryUi03(overrideTheme))
            theme.updateWindowStatusBar(it.window, StatusBarColor.Custom(ThemeColor.secondaryUi01(overrideTheme), true), it)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
        toolbar.setTitle(LR.string.up_next)
        toolbar.setNavigationOnClickListener {
            close()
        }
        toolbar.navigationIcon?.setTint(ThemeColor.secondaryIcon01(overrideTheme))
        toolbar.inflateMenu(R.menu.upnext)
        toolbar.menu.tintIcons(ThemeColor.secondaryIcon01(overrideTheme))
        toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_select -> {
                    multiSelectHelper.isMultiSelecting = true
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
        }

        view.isClickable = true

        val callback = UpNextTouchCallback(adapter = this)
        itemTouchHelper = ItemTouchHelper(callback).apply {
            attachToRecyclerView(recyclerView)
        }

        episodeItemTouchHelper = EpisodeItemTouchHelper(this::moveToTop, this::moveToBottom, this::removeFromUpNext).apply {
            attachToRecyclerView(recyclerView)
        }

        (recyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        (recyclerView.itemAnimator as? SimpleItemAnimator)?.changeDuration = 0

        val multiSelectToolbar = view.findViewById<MultiSelectToolbar>(R.id.multiSelectToolbar)
        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            val wasMultiSelecting = multiSelectToolbar.isVisible
            multiSelectToolbar.isVisible = isMultiSelecting
            toolbar.isVisible = !isMultiSelecting

            /* Track only if not embedded. If it is an embedded fragment, then track only when in expanded state */
            if (!isEmbedded || isEmbeddedExpanded()) {
                if (isMultiSelecting) {
                    trackUpNextEvent(AnalyticsEvent.UP_NEXT_MULTI_SELECT_ENTERED)
                } else if (wasMultiSelecting) {
                    trackUpNextEvent(AnalyticsEvent.UP_NEXT_MULTI_SELECT_EXITED)
                }
            }

            multiSelectToolbar.setNavigationIcon(IR.drawable.ic_arrow_back)

            adapter.notifyDataSetChanged()
        }
        multiSelectHelper.listener = object : MultiSelectHelper.Listener {
            override fun multiSelectSelectAll() {
                trackUpNextEvent(AnalyticsEvent.UP_NEXT_SELECT_ALL_TAPPED, mapOf(SELECT_ALL_KEY to true))
                upNextEpisodes.forEach { multiSelectHelper.select(it) }
                adapter.notifyDataSetChanged()
            }

            override fun multiSelectSelectNone() {
                trackUpNextEvent(AnalyticsEvent.UP_NEXT_SELECT_ALL_TAPPED, mapOf(SELECT_ALL_KEY to false))
                upNextEpisodes.forEach { multiSelectHelper.deselect(it) }
                adapter.notifyDataSetChanged()
            }

            override fun multiSelectSelectAllUp(episode: BaseEpisode) {
                val startIndex = upNextEpisodes.indexOf(episode)
                if (startIndex > -1) {
                    upNextEpisodes.subList(0, startIndex + 1).forEach { multiSelectHelper.select(it) }
                }

                adapter.notifyDataSetChanged()
            }

            override fun multiSelectSelectAllDown(episode: BaseEpisode) {
                val startIndex = upNextEpisodes.indexOf(episode)
                if (startIndex > -1) {
                    upNextEpisodes.subList(startIndex, upNextEpisodes.size).forEach { multiSelectHelper.select(it) }
                }

                adapter.notifyDataSetChanged()
            }
        }

        multiSelectHelper.context = view.context
        multiSelectToolbar.setup(viewLifecycleOwner, multiSelectHelper, menuRes = VR.menu.menu_multiselect_upnext, fragmentManager = parentFragmentManager)

        if (!isEmbedded) {
            startTour()
        }
    }

    override fun onPause() {
        super.onPause()
        if (multiSelectHelper.isMultiSelecting) {
            multiSelectHelper.isMultiSelecting = false
        }
    }

    fun moveToTop(episode: BaseEpisode, position: Int) {
        val recyclerView = realBinding?.recyclerView ?: return
        recyclerView.findViewHolderForAdapterPosition(position)?.let {
            episodeItemTouchHelper?.clearView(recyclerView, it)
        }
        playbackManager.playEpisodesNext(episodes = listOf(episode), source = AnalyticsSource.UP_NEXT)
        trackSwipeAction(SwipeAction.UP_NEXT_MOVE_TOP)
    }

    fun moveToBottom(episode: BaseEpisode, position: Int) {
        val recyclerView = realBinding?.recyclerView ?: return
        recyclerView.findViewHolderForAdapterPosition(position)?.let {
            episodeItemTouchHelper?.clearView(recyclerView, it)
        }
        playbackManager.playEpisodesLast(episodes = listOf(episode), source = AnalyticsSource.UP_NEXT)
        trackSwipeAction(SwipeAction.UP_NEXT_MOVE_BOTTOM)
    }

    @Suppress("UNUSED_PARAMETER")
    fun removeFromUpNext(episode: BaseEpisode, position: Int) {
        onUpNextEpisodeRemove(position)
        trackSwipeAction(SwipeAction.UP_NEXT_REMOVE)
    }

    fun startTour() {
        val upNextTourView = realBinding?.upNextTourView ?: return
        if (settings.getSeenUpNextTour()) {
            (upNextTourView.parent as? ViewGroup)?.removeView(upNextTourView)
        } else {
            settings.setSeenUpNextTour(true)
            upNextTourView.startTour(tour, UPNEXT_TOUR_NAME)
        }
    }

    private fun close() {
        if (!isEmbedded) {
            (activity as? FragmentHostListener)?.bottomSheetClosePressed(this)
        } else {
            (parentFragment as? PlayerContainerFragment)?.upNextBottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    private fun isEmbeddedExpanded() =
        isEmbedded && (parentFragment as? PlayerContainerFragment)?.upNextBottomSheetBehavior?.state == BottomSheetBehavior.STATE_EXPANDED

    override fun onClearUpNext() {
        playerViewModel.clearUpNext(context = requireContext(), upNextSource = upNextSource)
            .showOrClear(parentFragmentManager)
    }

    override fun onUpNextEpisodeStartDrag(viewHolder: RecyclerView.ViewHolder) {
        val recyclerView = realBinding?.recyclerView ?: return
        val itemTouchHelper = itemTouchHelper ?: return

        itemTouchHelper.startDrag(viewHolder)
        viewHolder.setIsRecyclable(false)
        userDraggingStart = viewHolder.bindingAdapterPosition

        // Clear out any open swipes on drag
        val firstPosition = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val lastPosition = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

        (firstPosition..lastPosition).map { recyclerView.findViewHolderForAdapterPosition(it) }
            .forEach { episodeItemTouchHelper?.clearView(recyclerView, it) }
    }

    override fun onEpisodeActionsClick(episodeUuid: String, podcastUuid: String?) {
        if (settings.getTapOnUpNextShouldPlay()) {
            playerViewModel.playEpisode(uuid = episodeUuid, playbackSource = playbackSource)
        } else {
            (activity as? FragmentHostListener)?.openEpisodeDialog(
                episodeUuid = episodeUuid,
                source = EpisodeViewSource.UP_NEXT,
                podcastUuid = podcastUuid,
                forceDark = true
            )
        }
    }

    override fun onEpisodeActionsLongPress(episodeUuid: String, podcastUuid: String?) {
        if (settings.getTapOnUpNextShouldPlay()) {
            (activity as? FragmentHostListener)?.openEpisodeDialog(
                episodeUuid = episodeUuid,
                source = EpisodeViewSource.UP_NEXT,
                podcastUuid = podcastUuid,
                forceDark = true
            )
        } else {
            playerViewModel.playEpisode(uuid = episodeUuid, playbackSource = playbackSource)
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

    override fun onUpNextEpisodeRemove(position: Int) {
        (upNextItems.getOrNull(position) as? BaseEpisode)?.let {
            playerViewModel.removeFromUpNext(it)
        }
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
                    )
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

    private fun trackSwipeAction(swipeAction: SwipeAction) {
        analyticsTracker.track(
            AnalyticsEvent.EPISODE_SWIPE_ACTION_PERFORMED,
            mapOf(
                ACTION_KEY to swipeAction.analyticsValue,
                SOURCE_KEY to SwipeSource.UP_NEXT.analyticsValue
            )
        )
    }

    private fun trackUpNextEvent(event: AnalyticsEvent, props: Map<String, Any> = emptyMap()) {
        val properties = HashMap<String, Any>()
        properties[SOURCE_KEY] = upNextSource.analyticsValue
        properties.putAll(props)
        analyticsTracker.track(event, properties)
    }
}

private const val UPNEXT_TOUR_NAME = "upnext"
private val step1 = TourStep(
    "Discover the changes to Up Next",
    "In this update Up Next has been moved into its own screen. We’ve also added some new features.",
    "Take a quick tour",
    null,
    Gravity.BOTTOM
)
private val step2 = TourStep(
    "Now Playing",
    "The Now Playing row shows your progress in the current episode. You can tap here to quickly jump to the player.",
    "Next",
    TourViewTag.ViewId(R.id.itemContainer),
    Gravity.BOTTOM
)
private val step3 = TourStep(
    "Multi-select",
    "Tap the Select Button to enter multi-select mode. You can then select multiple episodes and perform actions in bulk. Actions will appear at the top of the screen.",
    "Finish",
    TourViewTag.ChildWithClass(R.id.toolbar, androidx.appcompat.widget.ActionMenuView::class.java),
    Gravity.BOTTOM
)
private val tour = listOf(step1, step2, step3)
