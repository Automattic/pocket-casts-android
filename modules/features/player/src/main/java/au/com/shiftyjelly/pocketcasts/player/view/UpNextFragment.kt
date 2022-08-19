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
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentUpnextBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.AnalyticsHelper
import au.com.shiftyjelly.pocketcasts.views.extensions.tintIcons
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectToolbar
import au.com.shiftyjelly.pocketcasts.views.tour.TourStep
import au.com.shiftyjelly.pocketcasts.views.tour.TourViewTag
import com.google.android.material.bottomsheet.BottomSheetBehavior
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class UpNextFragment : BaseFragment(), UpNextListener, UpNextTouchCallback.ItemTouchHelperAdapter {
    companion object {
        private const val ARG_EMBEDDED = "embedded"

        fun newInstance(embedded: Boolean = false): UpNextFragment {
            val fragment = UpNextFragment()
            fragment.arguments = bundleOf(ARG_EMBEDDED to embedded)
            return fragment
        }
    }

    @Inject lateinit var settings: Settings
    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var multiSelectHelper: MultiSelectHelper

    lateinit var adapter: UpNextAdapter
    private val playerViewModel: PlayerViewModel by activityViewModels()
    private var userRearrangingFrom: Int? = null
    private var playingEpisodeAtStartOfDrag: String? = null

    private var realBinding: FragmentUpnextBinding? = null
    private val binding: FragmentUpnextBinding get() = realBinding ?: throw IllegalStateException("Trying to access the binding outside of the view lifecycle.")

    private var episodeItemTouchHelper: EpisodeItemTouchHelper? = null
    private var itemTouchHelper: ItemTouchHelper? = null

    private var upNextEpisodes: List<Any> = emptyList()
    private val upNextPlayables: List<Playable>
        get() = upNextEpisodes.filterIsInstance<Playable>()

    val isEmbedded: Boolean
        get() = arguments?.getBoolean(ARG_EMBEDDED) ?: false

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
        adapter = UpNextAdapter(context, imageLoader, episodeManager, this, multiSelectHelper, childFragmentManager)
        adapter.theme = overrideTheme

        if (!isEmbedded) {
            updateStatusAndNavColors()
            AnalyticsHelper.openedUpNext()
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
                upNextEpisodes = it
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
        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) {
            multiSelectToolbar.isVisible = it
            toolbar.isVisible = !it
            multiSelectToolbar.setNavigationIcon(IR.drawable.ic_arrow_back)

            adapter.notifyDataSetChanged()
        }
        multiSelectHelper.listener = object : MultiSelectHelper.Listener {
            override fun multiSelectSelectAll() {
                upNextPlayables.forEach { multiSelectHelper.select(it) }
                adapter.notifyDataSetChanged()
            }

            override fun multiSelectSelectNone() {
                upNextPlayables.forEach { multiSelectHelper.deselect(it) }
                adapter.notifyDataSetChanged()
            }

            override fun multiSelectSelectAllUp(episode: Playable) {
                val startIndex = upNextPlayables.indexOf(episode)
                if (startIndex > -1) {
                    upNextPlayables.subList(0, startIndex + 1).forEach { multiSelectHelper.select(it) }
                }

                adapter.notifyDataSetChanged()
            }

            override fun multiSelectSelectAllDown(episode: Playable) {
                val startIndex = upNextPlayables.indexOf(episode)
                if (startIndex > -1) {
                    upNextPlayables.subList(startIndex, upNextPlayables.size).forEach { multiSelectHelper.select(it) }
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
        multiSelectHelper.isMultiSelecting = false
    }

    fun moveToTop(episode: Playable, position: Int) {
        val recyclerView = realBinding?.recyclerView ?: return
        recyclerView.findViewHolderForAdapterPosition(position)?.let {
            episodeItemTouchHelper?.clearView(recyclerView, it)
        }
        playbackManager.playEpisodesNext(listOf(episode))
    }

    fun moveToBottom(episode: Playable, position: Int) {
        val recyclerView = realBinding?.recyclerView ?: return
        recyclerView.findViewHolderForAdapterPosition(position)?.let {
            episodeItemTouchHelper?.clearView(recyclerView, it)
        }
        playbackManager.playEpisodesLast(listOf(episode))
    }

    @Suppress("UNUSED_PARAMETER")
    fun removeFromUpNext(episode: Playable, position: Int) {
        onUpNextEpisodeRemove(position)
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

    override fun onClearUpNext() {
        playerViewModel.clearUpNext(requireContext()).showOrClear(parentFragmentManager)
    }

    override fun onUpNextEpisodeStartDrag(viewHolder: RecyclerView.ViewHolder) {
        val recyclerView = realBinding?.recyclerView ?: return
        val itemTouchHelper = itemTouchHelper ?: return

        itemTouchHelper.startDrag(viewHolder)
        viewHolder.setIsRecyclable(false)

        // Clear out any open swipes on drag
        val firstPosition = (recyclerView.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val lastPosition = (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()

        (firstPosition..lastPosition).map { recyclerView.findViewHolderForAdapterPosition(it) }
            .forEach { episodeItemTouchHelper?.clearView(recyclerView, it) }
    }

    override fun onEpisodeActionsClick(episodeUuid: String, podcastUuid: String?) {
        if (settings.getTapOnUpNextShouldPlay()) {
            playerViewModel.playEpisode(episodeUuid)
        } else {
            (activity as? FragmentHostListener)?.openEpisodeDialog(episodeUuid, podcastUuid, forceDark = true)
        }
    }

    override fun onEpisodeActionsLongPress(episodeUuid: String, podcastUuid: String?) {
        if (settings.getTapOnUpNextShouldPlay()) {
            (activity as? FragmentHostListener)?.openEpisodeDialog(episodeUuid, podcastUuid, forceDark = true)
        } else {
            playerViewModel.playEpisode(episodeUuid)
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
        val listData = upNextEpisodes.toMutableList()

        listData.add(toPosition, listData.removeAt(fromPosition))

        adapter.submitList(listData)
        upNextEpisodes = listData.toList()

        userRearrangingFrom = toPosition
    }

    override fun onUpNextEpisodeRemove(position: Int) {
        (upNextEpisodes.getOrNull(position) as? Playable)?.let {
            playerViewModel.removeFromUpNext(it)
        }
    }

    override fun onUpNextItemTouchHelperFinished() {
        if (playingEpisodeAtStartOfDrag == playbackManager.upNextQueue.currentEpisode?.uuid) {
            playerViewModel.changeUpNextEpisodes(upNextEpisodes.subList(1, upNextEpisodes.size).filterIsInstance<Playable>())
        } else {
            playerViewModel.upNextLive.value?.let {
                upNextEpisodes = it
                adapter.submitList(upNextEpisodes)
            }
        }

        userRearrangingFrom = null
        playingEpisodeAtStartOfDrag = null
    }

    override fun onNowPlayingClick() {
        (activity as? FragmentHostListener)?.openPlayer()
        close()
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
