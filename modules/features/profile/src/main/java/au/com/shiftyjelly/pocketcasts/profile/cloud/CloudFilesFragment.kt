package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.EpisodeListAdapter
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.profile.R
import au.com.shiftyjelly.pocketcasts.profile.databinding.FragmentCloudFilesBinding
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.playback.AutomaticUpNextSource
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragmentToolbar.ChromeCastButton.Shown
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutFactory
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutViewModel
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.roundToInt
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CloudFilesFragment : BaseFragment(), Toolbar.OnMenuItemClickListener {
    @Inject lateinit var downloadManager: DownloadManager
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var playButtonListener: PlayButton.OnClickListener
    @Inject lateinit var settings: Settings
    @Inject lateinit var upNextQueue: UpNextQueue
    @Inject lateinit var multiSelectHelper: MultiSelectEpisodesHelper
    @Inject lateinit var castManager: CastManager
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private lateinit var imageLoader: PodcastImageLoader
    lateinit var itemTouchHelper: EpisodeItemTouchHelper

    private val viewModel: CloudFilesViewModel by viewModels()
    private val swipeButtonLayoutViewModel: SwipeButtonLayoutViewModel by viewModels()
    private var binding: FragmentCloudFilesBinding? = null

    val adapter by lazy {
        EpisodeListAdapter(
            downloadManager = downloadManager,
            playbackManager = playbackManager,
            upNextQueue = upNextQueue,
            settings = settings,
            onRowClick = onRowClick,
            playButtonListener = playButtonListener,
            imageLoader = imageLoader,
            multiSelectHelper = multiSelectHelper,
            fragmentManager = childFragmentManager,
            swipeButtonLayoutFactory = SwipeButtonLayoutFactory(
                swipeButtonLayoutViewModel = swipeButtonLayoutViewModel,
                onItemUpdated = ::lazyNotifyItemChanged,
                defaultUpNextSwipeAction = { settings.upNextSwipe.flow.value },
                context = requireContext(),
                fragmentManager = parentFragmentManager,
                swipeSource = EpisodeItemTouchHelper.SwipeSource.FILES,
            )
        )
    }

    // Cannot call notify.notifyItemChanged directly because the compiler gets confused
    // when the adapter's constructor includes references to the adapter
    private fun lazyNotifyItemChanged(
        @Suppress("UNUSED_PARAMETER") episode: BaseEpisode,
        index: Int
    ) {
        val recyclerView = binding?.recyclerView
        recyclerView?.findViewHolderForAdapterPosition(index)?.let {
            itemTouchHelper.clearView(recyclerView, it)
        }

        adapter.notifyItemChanged(index)
    }

    private val onRowClick = { episode: BaseEpisode ->
        analyticsTracker.track(AnalyticsEvent.USER_FILE_DETAIL_SHOWN)
        CloudFileBottomSheetFragment.newInstance(episode.uuid)
            .show(parentFragmentManager, "cloud_bottom_sheet")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCloudFilesBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        binding?.recyclerView?.adapter = null
        super.onDestroyView()
        binding = null
    }

    override fun onResume() {
        super.onResume()
        AutomaticUpNextSource.mostRecentList = AutomaticUpNextSource.Companion.Predefined.files
    }

    override fun onPause() {
        super.onPause()
        multiSelectHelper.isMultiSelecting = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        imageLoader = PodcastImageLoaderThemed(context).apply {
            radiusPx = 4.dpToPx(context)
        }.smallPlaceholder()

        playButtonListener.source = SourceView.FILES
        multiSelectHelper.source = SourceView.FILES
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.emptyLayout?.isVisible = false
        binding?.lblEmptyTitle?.text = getString(LR.string.profile_files_empty_title)
        binding?.lblEmptySummary?.text = getString(LR.string.profile_files_empty_summary)

        binding?.toolbar?.setup(
            title = getString(LR.string.profile_navigation_files),
            navigationIcon = BackArrow,
            activity = activity,
            theme = theme,
            chromeCastButton = Shown(chromeCastAnalytics),
            menu = R.menu.menu_cloudfiles
        )
        binding?.toolbar?.setOnMenuItemClickListener(this)

        binding?.recyclerView?.let {
            it.layoutManager = LinearLayoutManager(it.context, RecyclerView.VERTICAL, false)
            it.adapter = adapter
            (it.itemAnimator as SimpleItemAnimator).changeDuration = 0
            itemTouchHelper = EpisodeItemTouchHelper()
            itemTouchHelper.attachToRecyclerView(it)
        }

        viewModel.cloudFilesList.observe(viewLifecycleOwner) {
            binding?.emptyLayout?.isVisible = it.isEmpty()
            adapter.submitList(it)
        }

        binding?.layoutUsage?.isVisible = false

        viewModel.accountUsage.observe(
            viewLifecycleOwner,
            Observer { accountOptional ->
                val binding = binding ?: return@Observer

                accountOptional.get()?.let {
                    binding.lblFilecount.text = getString(LR.string.profile_cloud_count_files, it.totalFiles)

                    val context = context ?: return@let
                    val used = Util.formattedBytes(it.usedSize, context = context, minimumBytes = 0)
                    val total = Util.formattedBytes(it.totalSize, context = context, minimumBytes = 0)
                    val percentage = ((it.usedSize.toDouble() / it.totalSize.toDouble()) * 100f).roundToInt()

                    if (percentage in 0..100) {
                        binding.lblPercentage.text = getString(LR.string.profile_cloud_percentage_full, percentage)

                        val color = when (percentage) {
                            in 80..95 -> ThemeColor.support08(theme.activeTheme)
                            in 95..101 -> ThemeColor.support05(theme.activeTheme)
                            else -> ThemeColor.primaryText01(theme.activeTheme)
                        }
                        binding.lblPercentage.setTextColor(color)
                    }

                    binding.lblUsage.text = "$used / $total"
                }

                binding.layoutUsage.isVisible = accountOptional.isPresent()
                binding.layoutUsageLocked.isVisible = !binding.layoutUsage.isVisible
            }
        )

        viewModel.refreshFiles(userInitiated = false)

        binding?.swipeRefreshLayout?.let {
            it.setOnRefreshListener {
                viewModel.refreshFiles(userInitiated = true)
                it.isRefreshing = false
            }
        }
        viewModel.signInState.observe(viewLifecycleOwner) {
            binding?.swipeRefreshLayout?.isEnabled =
                it is SignInState.SignedIn && it.subscriptionStatus is SubscriptionStatus.Paid
        }

        binding?.fab?.setOnClickListener {
            analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_ADD_FILE_TAPPED)
            val intent = AddFileActivity.newFileChooser(it.context)
            startActivity(intent)
        }

        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            val wasMultiSelecting = binding?.multiSelectToolbar?.isVisible ?: false
            binding?.multiSelectToolbar?.isVisible = isMultiSelecting
            binding?.toolbar?.isVisible = !isMultiSelecting
            binding?.multiSelectToolbar?.setNavigationIcon(IR.drawable.ic_arrow_back)

            if (isMultiSelecting) {
                analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_MULTI_SELECT_ENTERED)
            } else if (wasMultiSelecting) {
                analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_MULTI_SELECT_EXITED)
            }

            adapter.notifyDataSetChanged()
        }
        multiSelectHelper.listener = object : MultiSelectHelper.Listener<BaseEpisode> {
            override fun multiSelectSelectAll() {
                val episodes = viewModel.cloudFilesList.value
                if (episodes != null) {
                    multiSelectHelper.selectAllInList(episodes)
                    adapter.notifyDataSetChanged()
                    analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_SELECT_ALL_TAPPED, mapOf(SELECT_ALL_KEY to true))
                }
            }

            override fun multiSelectSelectNone() {
                val episodes = viewModel.cloudFilesList.value
                if (episodes != null) {
                    episodes.forEach { multiSelectHelper.deselect(it) }
                    adapter.notifyDataSetChanged()
                    analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_SELECT_ALL_TAPPED, mapOf(SELECT_ALL_KEY to false))
                }
            }

            override fun multiSelectSelectAllUp(multiSelectable: BaseEpisode) {
                val episodes = viewModel.cloudFilesList.value
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(episodes.subList(0, startIndex + 1))
                    }

                    adapter.notifyDataSetChanged()
                    analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_SELECT_ALL_ABOVE_TAPPED)
                }
            }

            override fun multiSelectSelectAllDown(multiSelectable: BaseEpisode) {
                val episodes = viewModel.cloudFilesList.value
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(episodes.subList(startIndex, episodes.size))
                    }

                    adapter.notifyDataSetChanged()
                    analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_SELECT_ALL_BELOW_TAPPED)
                }
            }

            override fun multiDeselectAllBelow(multiSelectable: BaseEpisode) {
                val cloudFiles = viewModel.cloudFilesList.value
                if (cloudFiles != null) {
                    val startIndex = cloudFiles.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        val episodesBelow = cloudFiles.subList(startIndex, cloudFiles.size)
                        multiSelectHelper.deselectAllInList(episodesBelow)
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun multiDeselectAllAbove(multiSelectable: BaseEpisode) {
                val cloudFiles = viewModel.cloudFilesList.value
                if (cloudFiles != null) {
                    val startIndex = cloudFiles.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        val episodesAbove = cloudFiles.subList(0, startIndex + 1)
                        multiSelectHelper.deselectAllInList(episodesAbove)
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
        multiSelectHelper.coordinatorLayout = (activity as FragmentHostListener).snackBarView()
        multiSelectHelper.source = SourceView.FILES
        binding?.multiSelectToolbar?.setup(viewLifecycleOwner, multiSelectHelper, menuRes = null, fragmentManager = parentFragmentManager)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_options -> {
                analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_OPTIONS_BUTTON_TAPPED)
                showOptionsDialog()
                true
            }
            else -> false
        }
    }

    private fun showOptionsDialog() {
        val dialog = OptionsDialog()
            .addTextOption(
                titleId = LR.string.sort_by, imageId = IR.drawable.ic_sort,
                click = {
                    analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to SORT_BY))
                    showSortOptions()
                }
            )
            .addTextOption(
                titleId = LR.string.profile_cloud_settings, imageId = IR.drawable.ic_profile_settings,
                click = {
                    analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to FILE_SETTINGS))
                    showCloudSettings()
                }
            )
        dialog.show(parentFragmentManager, "cloud_options")
    }

    private fun showCloudSettings() {
        val settingsFragment = CloudSettingsFragment()
        (activity as FragmentHostListener).addFragment(settingsFragment)
    }

    private fun showSortOptions() {
        val dialog = OptionsDialog()
            .setTitle(getString(LR.string.sort_by))
            .addCheckedOption(
                titleId = LR.string.episode_sort_newest_to_oldest,
                click = {
                    viewModel.changeSort(Settings.CloudSortOrder.NEWEST_OLDEST)
                    analyticsTracker.track(
                        AnalyticsEvent.UPLOADED_FILES_SORT_BY_CHANGED,
                        mapOf(SORT_BY to SortOrder.NEWEST_TO_OLDEST.analyticsValue)
                    )
                },
                checked = (viewModel.getSortOrder() == Settings.CloudSortOrder.NEWEST_OLDEST)
            )
            .addCheckedOption(
                titleId = LR.string.episode_sort_oldest_to_newest,
                click = {
                    viewModel.changeSort(Settings.CloudSortOrder.OLDEST_NEWEST)
                    analyticsTracker.track(
                        AnalyticsEvent.UPLOADED_FILES_SORT_BY_CHANGED,
                        mapOf(SORT_BY to SortOrder.OLDEST_TO_NEWEST.analyticsValue)
                    )
                },
                checked = (viewModel.getSortOrder() == Settings.CloudSortOrder.OLDEST_NEWEST)
            )
            .addCheckedOption(
                titleId = LR.string.podcasts_sort_by_title,
                click = {
                    viewModel.changeSort(Settings.CloudSortOrder.A_TO_Z)
                    analyticsTracker.track(
                        AnalyticsEvent.UPLOADED_FILES_SORT_BY_CHANGED,
                        mapOf(SORT_BY to SortOrder.TITLE_A_TO_Z.analyticsValue)
                    )
                },
                checked = (viewModel.getSortOrder() == Settings.CloudSortOrder.A_TO_Z)
            )
        dialog.show(parentFragmentManager, "sort_options")
    }

    override fun onBackPressed(): Boolean {
        return if (multiSelectHelper.isMultiSelecting) {
            multiSelectHelper.isMultiSelecting = false
            true
        } else {
            false
        }
    }

    enum class SortOrder(val analyticsValue: String) {
        NEWEST_TO_OLDEST("newest_to_oldest"),
        OLDEST_TO_NEWEST("oldest_to_newest"),
        TITLE_A_TO_Z("title_a_to_z")
    }

    companion object {
        private const val OPTION_KEY = "option"
        private const val SELECT_ALL_KEY = "select_all"
        private const val SORT_BY = "sort_by"
        private const val FILE_SETTINGS = "file_settings"
    }
}
