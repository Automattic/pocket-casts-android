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
import androidx.core.os.BundleCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarksFragment
import au.com.shiftyjelly.pocketcasts.player.view.chapters.ChaptersFragment
import au.com.shiftyjelly.pocketcasts.player.view.chapters.ChaptersViewModel
import au.com.shiftyjelly.pocketcasts.player.view.chapters.ChaptersViewModel.Mode.Episode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentEpisodeContainerBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragment.EpisodeFragmentArgs
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlin.time.Duration
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class EpisodeContainerFragment :
    BaseDialogFragment(),
    EpisodeFragment.EpisodeLoadedListener {
    companion object {
        private const val NEW_INSTANCE_ARG = "EpisodeContainerFragmentArg"

        fun newInstance(
            episode: PodcastEpisode,
            source: EpisodeViewSource,
            overridePodcastLink: Boolean = false,
            fromListUuid: String? = null,
            forceDark: Boolean = false,
            autoPlay: Boolean = false,
        ) = newInstance(
            episodeUuid = episode.uuid,
            source = source,
            overridePodcastLink = overridePodcastLink,
            podcastUuid = episode.podcastUuid,
            fromListUuid = fromListUuid,
            forceDark = forceDark,
            autoPlay = autoPlay,
        )

        fun newInstance(
            episodeUuid: String,
            source: EpisodeViewSource,
            overridePodcastLink: Boolean = false,
            podcastUuid: String? = null,
            fromListUuid: String? = null,
            forceDark: Boolean = false,
            timestamp: Duration? = null,
            autoPlay: Boolean = false,
        ) = EpisodeContainerFragment().apply {
            arguments = Bundle().apply {
                putParcelable(
                    NEW_INSTANCE_ARG,
                    EpisodeFragmentArgs(
                        episodeUuid = episodeUuid,
                        source = source,
                        overridePodcastLink = overridePodcastLink,
                        podcastUuid = podcastUuid,
                        fromListUuid = fromListUuid,
                        forceDark = forceDark,
                        timestamp = timestamp,
                        autoPlay = autoPlay,
                    ),
                )
            }
        }
        private fun extractArgs(bundle: Bundle?): EpisodeFragmentArgs? =
            bundle?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, EpisodeFragmentArgs::class.java) }
    }

    override val statusBarColor: StatusBarColor
        get() = StatusBarColor.Custom(
            context?.getThemeColor(UR.attr.primary_ui_01)
                ?: Color.WHITE,
            theme.isDarkTheme,
        )

    var binding: FragmentEpisodeContainerBinding? = null

    private val args: EpisodeFragmentArgs
        get() = extractArgs(arguments) ?: throw IllegalStateException("${this::class.java.simpleName} is missing arguments. It must be created with newInstance function")

    private val episodeUUID: String
        get() = args.episodeUuid

    private val timestamp: Duration?
        get() = args.timestamp

    private val episodeViewSource: EpisodeViewSource
        get() = args.source

    private val overridePodcastLink: Boolean
        get() = args.overridePodcastLink

    val podcastUuid: String?
        get() = args.podcastUuid

    val fromListUuid: String?
        get() = args.fromListUuid

    private val forceDarkTheme: Boolean
        get() = args.forceDark

    private val autoPlay: Boolean
        get() = args.autoPlay

    val activeTheme: Theme.ThemeType
        get() = if (forceDarkTheme && theme.isLightTheme) Theme.ThemeType.DARK else theme.activeTheme

    private lateinit var adapter: ViewPagerAdapter

    private val viewModel: EpisodeContainerFragmentViewModel by viewModels()
    private val bookmarksViewModel: BookmarksViewModel by viewModels()
    private val chaptersViewModel by viewModels<ChaptersViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ChaptersViewModel.Factory> { factory ->
                factory.create(Episode(episodeUUID))
            }
        },
    )

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
                    if (bookmarksViewModel.multiSelectHelper.isMultiSelecting) {
                        bookmarksViewModel.multiSelectHelper.isMultiSelecting = false
                        return
                    }
                    dismiss()
                }
            },
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
            timestamp = timestamp,
            episodeViewSource = episodeViewSource,
            overridePodcastLink = overridePodcastLink,
            podcastUuid = podcastUuid,
            fromListUuid = fromListUuid,
            forceDarkTheme = forceDarkTheme,
            autoPlay = autoPlay,
        )

        viewPager.adapter = adapter

        TabLayoutMediator(tabLayout, viewPager, true) { tab, position ->
            tab.setText(adapter.pageTitle(position))
        }.attach()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrollStateChanged(state: Int) {
                super.onPageScrollStateChanged(state)
                viewPager.isUserInputEnabled = !bookmarksViewModel.multiSelectHelper.isMultiSelecting
            }

            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                btnFav.isVisible = adapter.isDetailsTab(position)
                btnShare.isVisible = adapter.isDetailsTab(position)
                viewModel.onPageSelected(adapter.pageKey(position))
            }
        })

        if (episodeViewSource == EpisodeViewSource.NOTIFICATION_BOOKMARK) {
            openBookmarks()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                chaptersViewModel.uiState.collect {
                    adapter.update(addChapters = it.chaptersCount > 0)
                }
            }
        }
    }

    private fun FragmentEpisodeContainerBinding.setupMultiSelectHelper() {
        bookmarksViewModel.multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            multiSelectToolbar.isVisible = isMultiSelecting
            multiSelectToolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        }
        bookmarksViewModel.multiSelectHelper.context = context
        multiSelectToolbar.setup(
            lifecycleOwner = viewLifecycleOwner,
            multiSelectHelper = bookmarksViewModel.multiSelectHelper,
            menuRes = null,
            activity = requireActivity(),
        )
    }

    private fun openBookmarks() {
        val index = adapter.indexOfBookmarks
        if (index == -1) return
        binding?.viewPager?.currentItem = index
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        with(bookmarksViewModel.multiSelectHelper) {
            isMultiSelecting = false
            context = null
            listener = null
        }
    }

    private class ViewPagerAdapter(
        fragmentManager: FragmentManager,
        lifecycle: Lifecycle,
        private val episodeUUID: String?,
        private val timestamp: Duration?,
        private val episodeViewSource: EpisodeViewSource,
        private val overridePodcastLink: Boolean,
        private val podcastUuid: String?,
        private val fromListUuid: String?,
        private val forceDarkTheme: Boolean,
        private val autoPlay: Boolean,
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {
        val indexOfBookmarks: Int
            get() = sections.indexOf(Section.Bookmarks)

        private sealed class Section(@StringRes val titleRes: Int, val analyticsValue: String) {
            data object Details : Section(LR.string.details, "details")
            data object Chapters : Section(LR.string.chapters, "chapters")
            data object Bookmarks : Section(LR.string.bookmarks, "bookmarks")
        }

        private var sections = listOf(
            Section.Details,
            Section.Bookmarks,
        )

        fun update(addChapters: Boolean) {
            val currentSections = sections
            val newSections = buildList {
                add(Section.Details)
                if (addChapters) {
                    add(Section.Chapters)
                }
                add(Section.Bookmarks)
            }
            if (currentSections != newSections) {
                sections = newSections
                if (addChapters) {
                    notifyItemInserted(1)
                } else {
                    notifyItemRemoved(1)
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
            Timber.d("Creating fragment for position $position ${sections[position]}")
            return when (sections[position]) {
                Section.Details ->
                    EpisodeFragment
                        .newInstance(
                            episodeUuid = requireNotNull(episodeUUID),
                            timestamp = timestamp,
                            source = episodeViewSource,
                            overridePodcastLink = overridePodcastLink,
                            podcastUuid = podcastUuid,
                            fromListUuid = fromListUuid,
                            forceDark = forceDarkTheme,
                            autoPlay = autoPlay,
                        )
                Section.Bookmarks -> BookmarksFragment.newInstance(
                    sourceView = SourceView.EPISODE_DETAILS,
                    episodeUuid = requireNotNull(episodeUUID),
                    forceDarkTheme = forceDarkTheme,
                )
                Section.Chapters -> ChaptersFragment.forEpisode(
                    episodeUuid = requireNotNull(episodeUUID),
                )
            }
        }

        @StringRes
        fun pageTitle(position: Int): Int {
            return sections[position].titleRes
        }

        fun pageKey(position: Int) = sections[position].analyticsValue

        fun isDetailsTab(position: Int) = sections[position] is Section.Details
    }

    override fun onEpisodeLoaded(state: EpisodeFragment.EpisodeToolbarState) {
        binding?.apply {
            val iconColor = ThemeColor.podcastIcon02(activeTheme, state.tintColor)
            val iconTint = ColorStateList.valueOf(iconColor)
            btnFav.setImageResource(if (state.episode.isStarred) R.drawable.ic_star_filled else R.drawable.ic_star)
            btnFav.imageTintList = iconTint
            btnClose.imageTintList = iconTint
            btnShare.imageTintList = iconTint
            tabLayout.tabTextColors = iconTint
            tabLayout.setSelectedTabIndicatorColor(iconColor)
            btnShare.setOnClickListener { state.onShareClicked() }
            btnFav.contentDescription = getString(if (state.episode.isStarred) LR.string.podcast_episode_starred else LR.string.podcast_episode_unstarred)
            btnFav.setOnClickListener { state.onFavClicked() }
        }
    }
}
