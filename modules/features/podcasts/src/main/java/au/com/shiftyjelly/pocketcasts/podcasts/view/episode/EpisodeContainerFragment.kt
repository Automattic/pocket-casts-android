package au.com.shiftyjelly.pocketcasts.podcasts.view.episode

import android.app.Dialog
import android.content.res.ColorStateList
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.StringRes
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarksFragment
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.SharedBookmarksViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentEpisodeContainerBinding
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class EpisodeContainerFragment :
    BaseDialogFragment(),
    EpisodeFragment.EpisodeLoadedListener {
    companion object {
        const val ARG_EPISODE_UUID = "episodeUUID"
        const val ARG_EPISODE_VIEW_SOURCE = "episode_view_source"
        const val ARG_OVERRIDE_PODCAST_LINK = "override_podcast_link"
        const val ARG_PODCAST_UUID = "podcastUUID"
        const val ARG_FROMLIST_UUID = "fromListUUID"
        const val ARG_FORCE_DARK = "forceDark"

        fun newInstance(
            episode: PodcastEpisode,
            source: EpisodeViewSource,
            overridePodcastLink: Boolean = false,
            fromListUuid: String? = null,
            forceDark: Boolean = false,
        ) = newInstance(
            episodeUuid = episode.uuid,
            source = source,
            overridePodcastLink = overridePodcastLink,
            podcastUuid = episode.podcastUuid,
            fromListUuid = fromListUuid,
            forceDark = forceDark
        )

        fun newInstance(
            episodeUuid: String,
            source: EpisodeViewSource,
            overridePodcastLink: Boolean = false,
            podcastUuid: String? = null,
            fromListUuid: String? = null,
            forceDark: Boolean = false,
        ) = EpisodeContainerFragment().apply {
            arguments = bundleOf(
                ARG_EPISODE_UUID to episodeUuid,
                ARG_EPISODE_VIEW_SOURCE to source.value,
                ARG_OVERRIDE_PODCAST_LINK to overridePodcastLink,
                ARG_PODCAST_UUID to podcastUuid,
                ARG_FROMLIST_UUID to fromListUuid,
                ARG_FORCE_DARK to forceDark
            )
        }
    }

    override val statusBarColor: StatusBarColor
        get() = StatusBarColor.Custom(
            context?.getThemeColor(UR.attr.primary_ui_01)
                ?: Color.WHITE,
            theme.isDarkTheme
        )

    var binding: FragmentEpisodeContainerBinding? = null

    private val episodeUUID: String?
        get() = arguments?.getString(ARG_EPISODE_UUID)

    private val episodeViewSource: EpisodeViewSource
        get() = EpisodeViewSource.fromString(arguments?.getString(ARG_EPISODE_VIEW_SOURCE))

    private val overridePodcastLink: Boolean
        get() = arguments?.getBoolean(ARG_OVERRIDE_PODCAST_LINK) ?: false

    val podcastUuid: String?
        get() = arguments?.getString(ARG_PODCAST_UUID)

    val fromListUuid: String?
        get() = arguments?.getString(ARG_FROMLIST_UUID)

    private val forceDarkTheme: Boolean
        get() = arguments?.getBoolean(ARG_FORCE_DARK) ?: false

    val activeTheme: Theme.ThemeType
        get() = if (forceDarkTheme && theme.isLightTheme) Theme.ThemeType.DARK else theme.activeTheme

    private lateinit var adapter: ViewPagerAdapter
    @Inject
    lateinit var multiSelectHelper: MultiSelectBookmarksHelper
    private val viewModel: EpisodeContainerFragmentViewModel by viewModels()
    private val sharedBookmarksViewModel: SharedBookmarksViewModel by viewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        if (!forceDarkTheme || theme.isDarkTheme) {
            return super.onCreateDialog(savedInstanceState)
        }

        val context = ContextThemeWrapper(requireContext(), UR.style.ThemeDark)
        return BottomSheetDialog(context, UR.style.BottomSheetDialogThemeDark)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentEpisodeContainerBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bottomSheetDialog = dialog as? BottomSheetDialog
        bottomSheetDialog?.onBackPressedDispatcher?.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (multiSelectHelper.isMultiSelecting) {
                        multiSelectHelper.isMultiSelecting = false
                        return
                    }
                    dismiss()
                }
            }
        )
        bottomSheetDialog?.behavior?.apply {
            isFitToContents = false
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
        // Ensure the dialog ends up the full height of the screen
        // Bottom sheet dialogs get wrapped in a sheet that is WRAP_CONTENT so setting MATCH_PARENT on our
        // root view is ignored.
        bottomSheetDialog?.setOnShowListener {
            view.updateLayoutParams<ViewGroup.LayoutParams> {
                height = Resources.getSystem().displayMetrics.heightPixels
            }
        }

        val binding = binding ?: return

        binding.setupViewPager()

        binding.setupMultiSelectHelper()

        binding.btnClose.setOnClickListener { dismiss() }
    }

    private fun FragmentEpisodeContainerBinding.setupViewPager() {
        // HACK to fix bottom sheet drag, https://issuetracker.google.com/issues/135517665
        viewPager.getChildAt(0).isNestedScrollingEnabled = false

        adapter = ViewPagerAdapter(
            fragmentManager = childFragmentManager,
            lifecycle = viewLifecycleOwner.lifecycle,
            episodeUUID = episodeUUID,
            episodeViewSource = episodeViewSource,
            overridePodcastLink = overridePodcastLink,
            podcastUuid = podcastUuid,
            fromListUuid = fromListUuid,
            forceDarkTheme = forceDarkTheme
        )

        viewPager.adapter = adapter

        if (FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED)) {
            TabLayoutMediator(tabLayout, viewPager, true) { tab, position ->
                tab.setText(adapter.pageTitle(position))
            }.attach()
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                viewPager.isUserInputEnabled = !multiSelectHelper.isMultiSelecting
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                btnFav.isVisible = adapter.isDetailsTab(position)
                btnShare.isVisible = adapter.isDetailsTab(position)
                viewModel.onPageSelected(position)
            }
        })
    }

    private fun FragmentEpisodeContainerBinding.setupMultiSelectHelper() {
        sharedBookmarksViewModel.multiSelectHelper = multiSelectHelper
        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            multiSelectToolbar.isVisible = isMultiSelecting
            multiSelectToolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        }
        multiSelectHelper.context = context
        multiSelectToolbar.setup(
            lifecycleOwner = viewLifecycleOwner,
            multiSelectHelper = multiSelectHelper,
            menuRes = null,
            fragmentManager = parentFragmentManager,
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        multiSelectHelper.isMultiSelecting = false
        multiSelectHelper.context = null
    }

    private class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        private val episodeUUID: String?,
        private val episodeViewSource: EpisodeViewSource,
        private val overridePodcastLink: Boolean,
        private val podcastUuid: String?,
        private val fromListUuid: String?,
        private val forceDarkTheme: Boolean,
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {

        private sealed class Section(@StringRes val titleRes: Int) {
            object Details : Section(LR.string.details)
            object Bookmarks : Section(LR.string.bookmarks)
        }

        private var sections = mutableListOf<Section>(Section.Details).apply {
            if (FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED)) {
                add(Section.Bookmarks)
            }
        }.toList()

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
                Section.Details -> EpisodeFragment.newInstance(
                    episodeUuid = requireNotNull(episodeUUID),
                    source = episodeViewSource,
                    overridePodcastLink = overridePodcastLink,
                    podcastUuid = podcastUuid,
                    fromListUuid = fromListUuid,
                    forceDark = forceDarkTheme
                )

                Section.Bookmarks -> BookmarksFragment.newInstance(
                    sourceView = SourceView.EPISODE_DETAILS,
                    episodeUuid = requireNotNull(episodeUUID)
                )
            }
        }

        @StringRes
        fun pageTitle(position: Int): Int {
            return sections[position].titleRes
        }

        fun isDetailsTab(position: Int) = sections[position] is Section.Details
    }

    override fun onEpisodeLoaded(state: EpisodeFragment.EpisodeToolbarState) {
        binding?.apply {
            val iconColor = ThemeColor.podcastIcon02(activeTheme, state.tintColor)
            episode = state.episode
            toolbarTintColor = iconColor
            tabLayout.tabTextColors = ColorStateList.valueOf(iconColor)
            tabLayout.setSelectedTabIndicatorColor(iconColor)
            btnShare.setOnClickListener { state.onShareClicked() }
            btnFav.contentDescription = getString(if (state.episode.isStarred) LR.string.podcast_episode_starred else LR.string.podcast_episode_unstarred)
            btnFav.setOnClickListener { state.onFavClicked() }
        }
    }
}
