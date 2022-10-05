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
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.databinding.FragmentPlayerContainerBinding
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextSource
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import au.com.shiftyjelly.pocketcasts.views.tour.TourStep
import au.com.shiftyjelly.pocketcasts.views.tour.TourViewTag
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class PlayerContainerFragment : BaseFragment(), HasBackstack {
    @Inject lateinit var settings: Settings
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    lateinit var upNextBottomSheetBehavior: BottomSheetBehavior<View>

    private lateinit var adapter: ViewPagerAdapter
    private val viewModel: PlayerViewModel by activityViewModels()
    private var binding: FragmentPlayerContainerBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentPlayerContainerBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ViewPagerAdapter(childFragmentManager, viewLifecycleOwner.lifecycle)

        val upNextFragment = UpNextFragment.newInstance(embedded = true, source = UpNextSource.PLAYER)
        childFragmentManager.beginTransaction().replace(R.id.upNextFrameBottomSheet, upNextFragment).commitAllowingStateLoss()

        val binding = binding ?: return

        upNextBottomSheetBehavior = BottomSheetBehavior.from(binding.upNextFrameBottomSheet)
        upNextBottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    upNextBottomSheetBehavior.setPeekHeight(0, false)
                    updateUpNextVisibility(true)

                    activity?.let {
                        theme.setNavigationBarColor(it.window, true, ThemeColor.primaryUi03(Theme.ThemeType.DARK))
                        theme.updateWindowStatusBar(it.window, StatusBarColor.Custom(ThemeColor.primaryUi01(Theme.ThemeType.DARK), true), it)
                    }

                    upNextFragment.startTour()

                    FirebaseAnalyticsTracker.openedUpNext()
                } else if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                    updateUpNextVisibility(false)

                    (activity as? FragmentHostListener)?.updateSystemColors()
                }
            }
        })

        val viewPager = binding.viewPager
        viewPager.adapter = adapter
        viewPager.getChildAt(0).isNestedScrollingEnabled = false // HACK to fix bottom sheet drag, https://issuetracker.google.com/issues/135517665

        TabLayoutMediator(binding.tabLayout, viewPager, true) { tab, position ->
            tab.setText(adapter.pageTitle(position))
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                if (state == SCROLL_STATE_IDLE) {
                    (activity as? FragmentHostListener)?.updatePlayerView()
                }
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    1 -> FirebaseAnalyticsTracker.openedPlayerNotes()
                    2 -> FirebaseAnalyticsTracker.openedPlayerChapters()
                }
            }
        })

        viewModel.listDataLive.observe(viewLifecycleOwner) {
            val hasChapters = it.chapters.isNotEmpty()
            val hasNotes = !it.podcastHeader.isUserEpisode

            val updated = adapter.update(hasNotes, hasChapters)
            if (updated) {
                adapter.notifyDataSetChanged()
            }

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
    }

    fun openUpNext() {
        val upNextFragment = UpNextFragment.newInstance(source = UpNextSource.PLAYER)
        (activity as? FragmentHostListener)?.showBottomSheet(upNextFragment)
    }

    fun openPlayer() {
        val index = adapter.indexOfPlayer
        if (index == -1) return
        binding?.viewPager?.currentItem = index
    }

    fun openChaptersAt(chapter: Chapter) {
        val index = adapter.indexOfChapters
        if (index == -1) {
            return
        }
        binding?.viewPager?.currentItem = index
        // HACK: View pager 2 has no way to get the actual fragments
        childFragmentManager.fragments.filterIsInstance<ChaptersFragment>().firstOrNull()?.scrollToChapter(chapter)
    }

    fun updateUpNextVisibility(show: Boolean) {
        val bottomSheet = binding?.upNextFrameBottomSheet
        if (bottomSheet != null && bottomSheet.isVisible != show) {
            bottomSheet.isVisible = show
        }
        (activity as? FragmentHostListener)?.lockPlayerBottomSheet(show)
    }

    override fun getBackstackCount(): Int {
        return if (upNextBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) 1 else 0
    }

    override fun onBackPressed(): Boolean {
        return if (upNextBottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED) {
            upNextBottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            true
        } else {
            false
        }
    }

    companion object {
        private const val SOURCE_KEY = "source"
    }
}

private class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private sealed class Section(@StringRes val titleRes: Int) {
        object Player : Section(VR.string.player_tab_playing)
        object Notes : Section(LR.string.player_tab_notes)
        object Chapters : Section(LR.string.player_tab_chapters)
    }

    private var sections = listOf<Section>(Section.Player)

    val indexOfPlayer: Int
        get() = sections.indexOf(Section.Player)

    val indexOfChapters: Int
        get() = sections.indexOf(Section.Chapters)

    fun update(hasNotes: Boolean, hasChapters: Boolean): Boolean {
        val hadNotes = sections.contains(Section.Notes)
        val hadChapters = sections.contains(Section.Chapters)

        val newSections = mutableListOf<Section>()
        newSections.add(Section.Player)

        if (hasNotes) {
            newSections.add(Section.Notes)
        }

        if (hasChapters) {
            newSections.add(Section.Chapters)
        }

        this.sections = newSections

        if (hadNotes != hasNotes || hadChapters != hasChapters) {
            return true
        }

        return false
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
        Timber.d("Creating fragment for position $position ${sections[position]}")
        return when (sections[position]) {
            is Section.Player -> PlayerHeaderFragment()
            is Section.Notes -> NotesFragment()
            is Section.Chapters -> ChaptersFragment()
        }
    }

    @StringRes fun pageTitle(position: Int): Int {
        return sections[position].titleRes
    }
}

private const val PLAYER_TOUR_NAME = "player"
private val step1 = TourStep(
    "Explore the new player update",
    "We’ve made lots of improvements to the player in this update. To make sure you get the most out of this update, we prepared a quick tour.",
    "Take a quick tour",
    null,
    Gravity.BOTTOM
)
private val step2 = TourStep(
    "Tabbed Layout",
    "You can now swipe between Now Playing, Notes and Chapters (if available).",
    "Next",
    TourViewTag.ViewId(R.id.tabLayout),
    Gravity.BOTTOM
)
private val step3 = TourStep(
    "Up Next",
    "As well as swiping up to access Up Next, you can now see how many you have queued here.",
    "Next",
    TourViewTag.ViewId(R.id.upNextButton),
    Gravity.BOTTOM
)
private val step4 = TourStep(
    "More Actions",
    "You can now easily access more actions, as well as customise which actions appear in the player menu.",
    "Finish",
    TourViewTag.ViewId(R.id.playerActions),
    Gravity.TOP
)
private val tour = listOf(step1, step2, step3, step4)
