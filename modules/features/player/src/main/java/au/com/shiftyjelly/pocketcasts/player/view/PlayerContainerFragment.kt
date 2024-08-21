package au.com.shiftyjelly.pocketcasts.player.view

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.doOnLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentPlayerContainerBinding
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarksFragment
import au.com.shiftyjelly.pocketcasts.player.view.chapters.ChaptersFragment
import au.com.shiftyjelly.pocketcasts.player.view.chapters.ChaptersViewModel
import au.com.shiftyjelly.pocketcasts.player.view.chapters.ChaptersViewModel.Mode.Player
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.tour.TourStep
import au.com.shiftyjelly.pocketcasts.views.tour.TourViewTag
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class PlayerContainerFragment : BaseFragment(), HasBackstack {
    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker
    private val bookmarksViewModel: BookmarksViewModel by viewModels()

    lateinit var upNextBottomSheetBehavior: BottomSheetBehavior<View>

    private lateinit var adapter: ViewPagerAdapter
    private val viewModel: PlayerViewModel by activityViewModels()
    private val chaptersViewModel by viewModels<ChaptersViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ChaptersViewModel.Factory> { factory ->
                factory.create(Player)
            }
        },
    )
    private var binding: FragmentPlayerContainerBinding? = null

    val overrideTheme: Theme.ThemeType
        get() = if (settings.useDarkUpNextTheme.value) Theme.ThemeType.DARK else theme.activeTheme

    private val closeUpNextCallback = object : BottomSheetBehavior.BottomSheetCallback() {
        override fun onStateChanged(bottomSheet: View, newState: Int) {
            if (newState in listOf(BottomSheetBehavior.STATE_COLLAPSED, BottomSheetBehavior.STATE_HIDDEN)) {
                upNextBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }

        override fun onSlide(bottomSheet: View, slideOffset: Float) = Unit
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPlayerContainerBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? FragmentHostListener)?.removePlayerBottomSheetCallback(closeUpNextCallback)
        binding = null
        bookmarksViewModel.multiSelectHelper.context = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val upNextFragment = UpNextFragment.newInstance(embedded = true, source = UpNextSource.NOW_PLAYING)
        childFragmentManager.beginTransaction().replace(R.id.upNextFrameBottomSheet, upNextFragment).commitAllowingStateLoss()

        val binding = binding ?: return

        // UpNext bottom sheet needs to be gone. Otherwise, dragging player bottom sheet doesn't work as
        // the motion events are intercepted.
        //
        // However, we make it gone after it is laid out to speed up initial fling motion to show it.
        // Having it gone from the beginning adds a small delay before it can be initially shown.
        binding.upNextFrameBottomSheet.doOnLayout {
            it.isGone = true
            (activity as? FragmentHostListener)?.addPlayerBottomSheetCallback(closeUpNextCallback)
        }
        upNextBottomSheetBehavior = BottomSheetBehavior.from(binding.upNextFrameBottomSheet)
        upNextBottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                updateUpNextVisibility(newState != BottomSheetBehavior.STATE_COLLAPSED)

                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    analyticsTracker.track(AnalyticsEvent.UP_NEXT_SHOWN, mapOf(SOURCE_KEY to UpNextSource.NOW_PLAYING.analyticsValue))

                    activity?.let {
                        theme.setNavigationBarColor(it.window, true, ThemeColor.primaryUi03(overrideTheme))
                        theme.updateWindowStatusBar(it.window, StatusBarColor.Custom(ThemeColor.secondaryUi01(overrideTheme), true), it)
                    }

                    upNextFragment.onExpanded()
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    analyticsTracker.track(AnalyticsEvent.UP_NEXT_DISMISSED)

                    (activity as? FragmentHostListener)?.updateSystemColors()
                    upNextFragment.onCollapsed()
                }
            }
        })

        val viewPager = binding.viewPager

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            private var previousPosition: Int = INVALID_TAB_POSITION
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                viewPager.isUserInputEnabled = !bookmarksViewModel.multiSelectHelper.isMultiSelecting
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when {
                    adapter.isPlayerTab(position) -> {
                        if (previousPosition == INVALID_TAB_POSITION) return
                        analyticsTracker.track(AnalyticsEvent.PLAYER_TAB_SELECTED, mapOf(TAB_KEY to "now_playing"))
                    }

                    adapter.isNotesTab(position) -> {
                        analyticsTracker.track(AnalyticsEvent.PLAYER_TAB_SELECTED, mapOf(TAB_KEY to "show_notes"))
                    }

                    adapter.isBookmarksTab(position) -> {
                        analyticsTracker.track(AnalyticsEvent.PLAYER_TAB_SELECTED, mapOf(TAB_KEY to "bookmarks"))
                    }

                    adapter.isChaptersTab(position) -> {
                        analyticsTracker.track(AnalyticsEvent.PLAYER_TAB_SELECTED, mapOf(TAB_KEY to "chapters"))
                    }

                    else -> {
                        Timber.e("Invalid tab selected")
                    }
                }
                previousPosition = position
            }
        })

        adapter = ViewPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)
        viewPager.adapter = adapter
        viewPager.getChildAt(0).isNestedScrollingEnabled = false // HACK to fix bottom sheet drag, https://issuetracker.google.com/issues/135517665
        TabLayoutMediator(binding.tabLayout, viewPager, true) { tab, position ->
            tab.setText(adapter.pageTitle(position))
        }.attach()

        viewModel.listDataLive.observe(viewLifecycleOwner) {
            adapter.updateNotes(addNotes = !it.podcastHeader.isUserEpisode)
            val upNextCount = it.upNextEpisodes.size
            val drawableId = when {
                upNextCount == 0 -> R.drawable.mini_player_upnext
                upNextCount < 10 -> R.drawable.mini_player_upnext_badge
                else -> R.drawable.mini_player_upnext_badge_large
            }
            val upNextDrawable: Drawable? = AppCompatResources.getDrawable(binding.upNextButton.context, drawableId)
            binding.upNextButton.setImageDrawable(upNextDrawable)
            binding.countText.text = if (upNextCount == 0) "" else upNextCount.toString()

            binding.upNextButton.setOnClickListener {
                analyticsTracker.track(AnalyticsEvent.UP_NEXT_SHOWN, mapOf(SOURCE_KEY to UpNextSource.PLAYER.analyticsValue))
                openUpNext()
            }

            view.setBackgroundColor(it.podcastHeader.backgroundColor)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chaptersViewModel.uiState.collect {
                    adapter.updateChapters(addChapters = it.chaptersCount > 0)
                }
            }
        }

        binding.btnClosePlayer.setOnClickListener { (activity as? FragmentHostListener)?.closePlayer() }
        view.doOnLayout {
            val tourView = binding.tourView
            if (settings.getSeenPlayerTour()) {
                (tourView.parent as? ViewGroup)?.removeView(tourView)
            } else {
                settings.setSeenPlayerTour(true)
                tourView.startTour(tour, PLAYER_TOUR_NAME)
            }
        }

        bookmarksViewModel.multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            binding.multiSelectToolbar.isVisible = isMultiSelecting
            binding.multiSelectToolbar.setNavigationIcon(IR.drawable.ic_arrow_back)
        }
        bookmarksViewModel.multiSelectHelper.context = context
        binding.multiSelectToolbar.setup(
            lifecycleOwner = viewLifecycleOwner,
            multiSelectHelper = bookmarksViewModel.multiSelectHelper,
            menuRes = null,
            activity = requireActivity(),
        )
    }

    fun openUpNext() {
        updateUpNextVisibility(true)
        upNextBottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    fun updateTabsVisibility(show: Boolean) {
        if (FeatureFlag.isEnabled(Feature.TRANSCRIPTS)) {
            binding?.tabHolder?.isVisible = show
            binding?.viewPager?.isUserInputEnabled = show
        }
    }

    fun onPlayerOpen() {
        try {
            if (isAdded) {
                ((childFragmentManager.fragments.firstOrNull { it is BookmarksFragment }) as? BookmarksFragment)
                    ?.onPlayerOpen()
            }
        } catch (e: IllegalStateException) {
            Timber.e(e)
        }
    }

    fun onPlayerClose() {
        try {
            if (isAdded) {
                ((childFragmentManager.fragments.firstOrNull { it is BookmarksFragment }) as? BookmarksFragment)
                    ?.onPlayerClose()
            }
        } catch (e: IllegalStateException) {
            Timber.e(e)
        }
    }

    fun openPlayer(sourceView: SourceView? = null) {
        val index = adapter.indexOfPlayer
        if (index == -1) return
        binding?.viewPager?.currentItem = index

        if (sourceView == SourceView.WHATS_NEW) {
            ((childFragmentManager.fragments.firstOrNull { it is PlayerHeaderFragment }) as? PlayerHeaderFragment)
                ?.onMoreClicked(sourceView)
        }
    }

    fun openBookmarks() {
        val index = adapter.indexOfBookmarks
        if (index == -1) return
        binding?.viewPager?.currentItem = index
    }

    fun openChaptersAt(chapter: Chapter) {
        val index = adapter.indexOfChapters
        if (index == -1) {
            return
        }
        binding?.viewPager?.currentItem = index

        // tapping on the chapter title on the now playing screen should scroll to that chapter when the fragment is available
        chaptersViewModel.scrollToChapter(chapter)
    }

    fun updateUpNextVisibility(show: Boolean) {
        binding?.upNextFrameBottomSheet?.isVisible = show
        (activity as? FragmentHostListener)?.lockPlayerBottomSheet(show)
    }

    override fun getBackstackCount(): Int {
        return if (upNextBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED ||
            bookmarksViewModel.multiSelectHelper.isMultiSelecting ||
            isTranscriptVisible
        ) {
            1
        } else {
            0
        }
    }

    override fun onBackPressed(): Boolean {
        return when {
            upNextBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED -> {
                upNextBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                true
            }

            bookmarksViewModel.multiSelectHelper.isMultiSelecting -> {
                bookmarksViewModel.multiSelectHelper.closeMultiSelect()
                binding?.viewPager?.isUserInputEnabled = true
                true
            }

            isTranscriptVisible -> {
                updateTabsVisibility(true)
                viewModel.closeTranscript(withTransition = true)
                true
            }

            else -> false
        }
    }

    private val isTranscriptVisible: Boolean
        get() = binding?.tabHolder?.isVisible == false

    companion object {
        private const val INVALID_TAB_POSITION = -1
        private const val SOURCE_KEY = "source"
        private const val TAB_KEY = "tab"
    }
}

private class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private sealed class Section(@StringRes val titleRes: Int) {
        data object Player : Section(VR.string.player_tab_playing)
        data object Notes : Section(LR.string.player_tab_notes)
        data object Bookmarks : Section(LR.string.player_tab_bookmarks)
        data object Chapters : Section(LR.string.player_tab_chapters)
    }

    private var sections = listOf(Section.Player, Section.Bookmarks)

    val indexOfPlayer: Int
        get() = sections.indexOf(Section.Player)

    val indexOfChapters: Int
        get() = sections.indexOf(Section.Chapters)

    val indexOfBookmarks: Int
        get() = sections.indexOf(Section.Bookmarks)

    fun updateNotes(addNotes: Boolean) {
        val currentSections = sections
        val hasChapters = sections.contains(Section.Chapters)

        val newSections = buildList {
            add(Section.Player)
            if (addNotes) {
                add(Section.Notes)
            }

            if (hasChapters) {
                add(Section.Chapters)
            }
            add(Section.Bookmarks)
        }

        if (currentSections != newSections) {
            sections = newSections
            if (addNotes) {
                notifyItemInserted(1)
            } else {
                notifyItemRemoved(1)
            }
        }
    }

    fun updateChapters(addChapters: Boolean) {
        val currentSections = sections
        val hasNotes = sections.contains(Section.Notes)

        val newSections = buildList {
            add(Section.Player)
            if (hasNotes) {
                add(Section.Notes)
            }

            if (addChapters) {
                add(Section.Chapters)
            }
            add(Section.Bookmarks)
        }

        if (currentSections != newSections) {
            sections = newSections
            val position = if (hasNotes) 2 else 1
            if (addChapters) {
                notifyItemInserted(position)
            } else {
                notifyItemRemoved(position)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return sections[position].hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return sections.map { it.hashCode().toLong() }.contains(itemId)
    }

    override fun getItemCount(): Int {
        return sections.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (sections[position]) {
            is Section.Player -> PlayerHeaderFragment()
            is Section.Notes -> NotesFragment()
            is Section.Bookmarks -> BookmarksFragment.newInstance(SourceView.PLAYER)
            is Section.Chapters -> ChaptersFragment.forPlayer()
        }
    }

    @StringRes
    fun pageTitle(position: Int): Int {
        return sections[position].titleRes
    }

    fun isPlayerTab(position: Int) = sections[position] is Section.Player
    fun isNotesTab(position: Int) = sections[position] is Section.Notes
    fun isBookmarksTab(position: Int) = sections[position] is Section.Bookmarks
    fun isChaptersTab(position: Int) = sections[position] is Section.Chapters
}

private const val PLAYER_TOUR_NAME = "player"
private val step1 = TourStep(
    "Explore the new player update",
    "We’ve made lots of improvements to the player in this update. To make sure you get the most out of this update, we prepared a quick tour.",
    "Take a quick tour",
    null,
    Gravity.BOTTOM,
)
private val step2 = TourStep(
    "Tabbed Layout",
    "You can now swipe between Now Playing, Notes and Chapters (if available).",
    "Next",
    TourViewTag.ViewId(R.id.tabLayout),
    Gravity.BOTTOM,
)
private val step3 = TourStep(
    "Up Next",
    "As well as swiping up to access Up Next, you can now see how many you have queued here.",
    "Next",
    TourViewTag.ViewId(R.id.upNextButton),
    Gravity.BOTTOM,
)
private val step4 = TourStep(
    "More Actions",
    "You can now easily access more actions, as well as customise which actions appear in the player menu.",
    "Finish",
    TourViewTag.ViewId(R.id.playerActions),
    Gravity.TOP,
)
private val tour = listOf(step1, step2, step3, step4)
