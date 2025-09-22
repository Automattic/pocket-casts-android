package au.com.shiftyjelly.pocketcasts.profile.cloud

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.EpisodeListAdapter
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.profile.R
import au.com.shiftyjelly.pocketcasts.profile.databinding.FragmentCloudFilesBinding
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragmentToolbar.ChromeCastButton.Shown
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
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CloudFilesFragment :
    BaseFragment(),
    Toolbar.OnMenuItemClickListener {
    @Inject lateinit var playButtonListener: PlayButton.OnClickListener

    @Inject lateinit var settings: Settings

    @Inject lateinit var multiSelectHelper: MultiSelectEpisodesHelper

    @Inject lateinit var analyticsTracker: AnalyticsTracker

    @Inject lateinit var swipeRowActionsFactory: SwipeRowActions.Factory

    @Inject lateinit var rowDataProvider: EpisodeRowDataProvider

    private lateinit var imageRequestFactory: PocketCastsImageRequestFactory

    private val viewModel: CloudFilesViewModel by viewModels()
    private val swipeActionViewModel by viewModels<SwipeActionViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SwipeActionViewModel.Factory> { factory ->
                factory.create(SwipeSource.Files, playlistUuid = null)
            }
        },
    )

    private var binding: FragmentCloudFilesBinding? = null

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
            artworkContext = Element.Files,
            onSwipeAction = { episode, swipeAction ->
                viewLifecycleOwner.lifecycleScope.launch {
                    swipeActionViewModel.handleAction(swipeAction, episode.uuid, childFragmentManager)
                }
            },
        )
    }

    private val onRowClick = { episode: BaseEpisode ->
        CloudFileBottomSheetFragment
            .newInstance(episode.uuid, source = EpisodeViewSource.FILES)
            .show(parentFragmentManager, "cloud_bottom_sheet")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCloudFilesBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onDestroyView() {
        binding?.recyclerView?.adapter = null
        multiSelectHelper.cleanup()
        super.onDestroyView()
        binding = null
    }

    override fun onResume() {
        super.onResume()
        settings.trackingAutoPlaySource.set(AutoPlaySource.Predefined.Files, updateModifiedAt = false)
    }

    override fun onPause() {
        super.onPause()
        multiSelectHelper.isMultiSelecting = false
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        imageRequestFactory = PocketCastsImageRequestFactory(context, cornerRadius = 4).smallSize().themed()

        playButtonListener.source = SourceView.FILES
        multiSelectHelper.source = SourceView.FILES
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.emptyLayout?.isVisible = false
        binding?.emptyLayout?.setContentWithViewCompositionStrategy {
            AppTheme(theme.activeTheme) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    NoContentBanner(
                        title = stringResource(LR.string.profile_files_empty_title),
                        body = stringResource(LR.string.profile_files_empty_summary),
                        iconResourceId = IR.drawable.ic_file,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            }
        }

        binding?.toolbar?.setup(
            title = getString(LR.string.profile_navigation_files),
            navigationIcon = BackArrow,
            activity = activity,
            theme = theme,
            chromeCastButton = Shown(chromeCastAnalytics),
            menu = R.menu.menu_cloudfiles,
        )
        binding?.toolbar?.setOnMenuItemClickListener(this)

        binding?.recyclerView?.let {
            it.layoutManager = LinearLayoutManager(it.context, RecyclerView.VERTICAL, false)
            it.adapter = adapter
            (it.itemAnimator as SimpleItemAnimator).changeDuration = 0
        }

        viewModel.uiState.observe(viewLifecycleOwner) {
            binding?.emptyLayout?.isVisible = it.userEpisodes.isEmpty()
            adapter.submitList(it.userEpisodes)
            adapter.notifyDataSetChanged()
        }

        binding?.layoutUsage?.isVisible = false

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect { bottomInset ->
                    binding?.recyclerView?.updatePadding(bottom = bottomInset)
                    binding?.fab?.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        bottomMargin = bottomInset + resources.getDimensionPixelSize(R.dimen.files_fab_margin_bottom)
                    }
                }
            }
        }

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
            },
        )

        viewModel.refreshFiles(userInitiated = false)

        binding?.swipeRefreshLayout?.let {
            it.setOnRefreshListener {
                viewModel.refreshFiles(userInitiated = true)
                it.isRefreshing = false
            }
        }
        viewModel.signInState.observe(viewLifecycleOwner) {
            binding?.swipeRefreshLayout?.isEnabled = it is SignInState.SignedIn && it.subscription != null
        }

        binding?.fab?.setOnClickListener {
            analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_ADD_FILE_TAPPED)
            val intent = AddFileActivity.newFileChooser(it.context)
            startActivity(intent)
        }

        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            val wasMultiSelecting = binding?.multiSelectToolbar?.isVisible ?: false
            if (wasMultiSelecting == isMultiSelecting) {
                return@observe
            }
            binding?.multiSelectToolbar?.isVisible = isMultiSelecting
            binding?.toolbar?.isVisible = !isMultiSelecting
            binding?.multiSelectToolbar?.setNavigationIcon(IR.drawable.ic_arrow_back)

            if (isMultiSelecting) {
                analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_MULTI_SELECT_ENTERED)
            } else {
                analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_MULTI_SELECT_EXITED)
            }

            adapter.notifyItemRangeChanged(0, adapter.itemCount, MULTI_SELECT_TOGGLE_PAYLOAD)
        }
        multiSelectHelper.listener = object : MultiSelectHelper.Listener<BaseEpisode> {
            override fun multiSelectSelectAll() {
                val episodes = viewModel.uiState.value?.userEpisodes
                if (episodes != null) {
                    multiSelectHelper.selectAllInList(episodes)
                    adapter.notifyDataSetChanged()
                    analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_SELECT_ALL_TAPPED, mapOf(SELECT_ALL_KEY to true))
                }
            }

            override fun multiSelectSelectNone() {
                val episodes = viewModel.uiState.value?.userEpisodes
                if (episodes != null) {
                    multiSelectHelper.deselectAllInList(episodes)
                    adapter.notifyDataSetChanged()
                    analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_SELECT_ALL_TAPPED, mapOf(SELECT_ALL_KEY to false))
                }
            }

            override fun multiSelectSelectAllUp(multiSelectable: BaseEpisode) {
                val episodes = viewModel.uiState.value?.userEpisodes
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
                val episodes = viewModel.uiState.value?.userEpisodes
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
                val cloudFiles = viewModel.uiState.value?.userEpisodes
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
                val cloudFiles = viewModel.uiState.value?.userEpisodes
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
        binding?.multiSelectToolbar?.setup(viewLifecycleOwner, multiSelectHelper, menuRes = null, activity = requireActivity())
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
                titleId = LR.string.sort_by,
                imageId = IR.drawable.ic_sort,
                click = {
                    analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to SORT_BY))
                    showSortOptions()
                },
            )
            .addTextOption(
                titleId = LR.string.profile_cloud_settings,
                imageId = IR.drawable.ic_profile_settings,
                click = {
                    analyticsTracker.track(AnalyticsEvent.UPLOADED_FILES_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to FILE_SETTINGS))
                    showCloudSettings()
                },
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
                },
                checked = (viewModel.getSortOrder() == Settings.CloudSortOrder.NEWEST_OLDEST),
            )
            .addCheckedOption(
                titleId = LR.string.episode_sort_oldest_to_newest,
                click = {
                    viewModel.changeSort(Settings.CloudSortOrder.OLDEST_NEWEST)
                },
                checked = (viewModel.getSortOrder() == Settings.CloudSortOrder.OLDEST_NEWEST),
            )
            .addCheckedOption(
                titleId = LR.string.podcasts_sort_by_title,
                click = {
                    viewModel.changeSort(Settings.CloudSortOrder.A_TO_Z)
                },
                checked = (viewModel.getSortOrder() == Settings.CloudSortOrder.A_TO_Z),
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

    companion object {
        private const val OPTION_KEY = "option"
        private const val SELECT_ALL_KEY = "select_all"
        private const val SORT_BY = "sort_by"
        private const val FILE_SETTINGS = "file_settings"
    }
}
