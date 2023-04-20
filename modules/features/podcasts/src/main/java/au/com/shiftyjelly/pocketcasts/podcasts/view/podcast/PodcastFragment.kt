package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.podcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentPodcastBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderChooserFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts.PodcastsFragment
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SharePodcastHelper
import au.com.shiftyjelly.pocketcasts.servers.ServerManager
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.ui.images.CoilManager
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.setupChromeCastButton
import au.com.shiftyjelly.pocketcasts.views.extensions.smoothScrollToTop
import au.com.shiftyjelly.pocketcasts.views.extensions.tintIcons
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class PodcastFragment : BaseFragment(), Toolbar.OnMenuItemClickListener, CoroutineScope {

    companion object {
        const val ARG_PODCAST_UUID = "ARG_PODCAST_UUID"
        const val ARG_LIST_UUID = "ARG_LIST_INDEX_UUID"
        const val ARG_FEATURED_PODCAST = "ARG_FEATURED_PODCAST"
        private const val OPTION_KEY = "option"
        private const val IS_EXPANDED_KEY = "is_expanded"
        private const val PODCAST_UUID_KEY = "podcast_uuid"
        private const val LIST_ID_KEY = "list_id"
        private const val EPISODE_UUID_KEY = "episode_uuid"
        private const val REMOVE = "remove"
        private const val CHANGE = "change"
        private const val GO_TO = "go_to"

        fun newInstance(podcastUuid: String, fromListUuid: String? = null, featuredPodcast: Boolean = false): PodcastFragment {
            return PodcastFragment().apply {
                arguments = bundleOf(
                    ARG_PODCAST_UUID to podcastUuid,
                    ARG_LIST_UUID to fromListUuid,
                    ARG_FEATURED_PODCAST to featuredPodcast
                )
            }
        }
    }

    private lateinit var itemTouchHelper: EpisodeItemTouchHelper
    @Inject lateinit var settings: Settings
    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var downloadManager: DownloadManager
    @Inject lateinit var serverManager: ServerManager
    @Inject lateinit var playButtonListener: PlayButton.OnClickListener
    @Inject lateinit var castManager: CastManager
    @Inject lateinit var upNextQueue: UpNextQueue
    @Inject lateinit var multiSelectHelper: MultiSelectHelper
    @Inject lateinit var coilManager: CoilManager
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private val viewModel: PodcastViewModel by viewModels()
    private val ratingsViewModel: PodcastRatingsViewModel by viewModels()
    private var adapter: PodcastAdapter? = null
    private var binding: FragmentPodcastBinding? = null

    private var featuredPodcast = false
    private var fromListUuid: String? = null
    private var listState: Parcelable? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {}

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                UiUtil.hideKeyboard(recyclerView)
                binding?.headerBackgroundPlaceholder?.isGone = true
            }
        }
    }

    override var statusBarColor: StatusBarColor = StatusBarColor.Custom(color = 0xFF1E1F1E.toInt(), isWhiteIcons = true)

    private val onHeaderSummaryToggled: (
        expanded: Boolean,
        userInitiated: Boolean,
    ) -> Unit = { expanded, userInitiated ->
        if (userInitiated) {
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SCREEN_TOGGLE_SUMMARY,
                mapOf(IS_EXPANDED_KEY to expanded)
            )
        }
    }

    private val onSubscribeClicked: () -> Unit = {
        fromListUuid?.let {
            FirebaseAnalyticsTracker.podcastSubscribedFromList(it, podcastUuid)
            analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED, mapOf(LIST_ID_KEY to it, PODCAST_UUID_KEY to podcastUuid))
        }
        if (featuredPodcast) {
            FirebaseAnalyticsTracker.subscribedToFeaturedPodcast()
            viewModel.podcast.value?.uuid?.let { podcastUuid ->
                analyticsTracker.track(AnalyticsEvent.DISCOVER_FEATURED_PODCAST_SUBSCRIBED, mapOf(PODCAST_UUID_KEY to podcastUuid))
            }
        }
        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SUBSCRIBE_TAPPED)

        viewModel.subscribeToPodcast()
    }

    private val onUnsubscribeClicked: (successCallback: () -> Unit) -> Unit = { successCallback ->
        launch {
            val downloaded = withContext(Dispatchers.Default) { podcastManager.countEpisodesInPodcastWithStatus(podcastUuid, EpisodeStatusEnum.DOWNLOADED) }
            val title = when (downloaded) {
                0 -> getString(LR.string.are_you_sure)
                1 -> getString(LR.string.podcast_unsubscribe_downloaded_file_singular)
                else -> getString(LR.string.podcast_unsubscribe_downloaded_file_plural, downloaded)
            }
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_UNSUBSCRIBE_TAPPED)
            val dialog = ConfirmationDialog().setButtonType(ConfirmationDialog.ButtonType.Danger(getString(LR.string.unsubscribe)))
                .setTitle(title)
                .setSummary(getString(LR.string.podcast_unsubscribe_warning))
                .setIconId(IR.drawable.ic_failedwarning)
                .setOnConfirm {
                    successCallback()
                    viewModel.unsubscribeFromPodcast()

                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                }
            dialog.show(parentFragmentManager, "unsubscribe")
        }
    }

    private val onRowLongPress: (episode: PodcastEpisode) -> Unit = { episode ->
        multiSelectHelper.defaultLongPress(episode = episode, fragmentManager = childFragmentManager)
        adapter?.notifyDataSetChanged()
    }

    private val onArchiveAllClicked: () -> Unit = {
        val count = viewModel.archiveAllCount()
        val buttonString = resources.getStringPlural(count = count, singular = LR.string.archive_episodes_singular, plural = LR.string.archive_episodes_plural)

        val dialog = ConfirmationDialog()
            .setTitle(getString(LR.string.podcast_archive_all))
            .setSummary(getString(LR.string.podcast_archive_played))
            .setIconId(R.drawable.ic_archive_all)
            .setButtonType(ConfirmationDialog.ButtonType.Danger(buttonString))
            .setOnConfirm(viewModel::onArchiveAllClicked)
        dialog.show(parentFragmentManager, "confirm_archive_all_")
    }

    private val onRowClicked: (PodcastEpisode) -> Unit = { episode ->
        fromListUuid?.let { listUuid ->
            FirebaseAnalyticsTracker.podcastEpisodeTappedFromList(listId = listUuid, podcastUuid = episode.podcastUuid, episodeUuid = episode.uuid)
            analyticsTracker.track(
                AnalyticsEvent.DISCOVER_LIST_EPISODE_TAPPED,
                mapOf(LIST_ID_KEY to listUuid, PODCAST_UUID_KEY to episode.podcastUuid, EPISODE_UUID_KEY to episode.uuid)
            )
        }
        val episodeCard = EpisodeFragment.newInstance(
            episode = episode,
            source = EpisodeViewSource.PODCAST_SCREEN,
            overridePodcastLink = true,
            fromListUuid = fromListUuid
        )
        episodeCard.show(parentFragmentManager, "episode_card")
    }

    private val onSearchQueryChanged: (String) -> Unit = { searchQuery ->
        viewModel.searchQueryUpdated(searchQuery)
    }

    private val sortEpisodesNewestToOldest = {
        adapter?.signalLargeDiff()
        viewModel.updateEpisodesSortType(EpisodesSortType.EPISODES_SORT_BY_DATE_DESC)
    }

    private val sortEpisodesOldestToNewest = {
        adapter?.signalLargeDiff()
        viewModel.updateEpisodesSortType(EpisodesSortType.EPISODES_SORT_BY_DATE_ASC)
    }

    private val sortEpisodesLengthShortToLong = {
        adapter?.signalLargeDiff()
        viewModel.updateEpisodesSortType(EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC)
    }

    private val sortEpisodesLengthLongToShort = {
        adapter?.signalLargeDiff()
        viewModel.updateEpisodesSortType(EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC)
    }

    private val showEpisodeSortOptions = {
        val dialog = OptionsDialog()
            .addCheckedOption(
                titleId = LR.string.episode_sort_newest_to_oldest,
                checked = binding?.podcast?.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_DATE_DESC,
                click = sortEpisodesNewestToOldest
            )
            .addCheckedOption(
                titleId = LR.string.episode_sort_oldest_to_newest,
                checked = binding?.podcast?.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_DATE_ASC,
                click = sortEpisodesOldestToNewest
            )
            .addCheckedOption(
                titleId = LR.string.episode_sort_short_to_long,
                checked = binding?.podcast?.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC,
                click = sortEpisodesLengthShortToLong
            )
            .addCheckedOption(
                titleId = LR.string.episode_sort_long_to_short,
                checked = binding?.podcast?.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC,
                click = sortEpisodesLengthLongToShort
            )
        activity?.supportFragmentManager?.let {
            dialog.show(it, "episodes_sort_options_dialog")
        }
        Unit
    }

    private val showGroupingOptions = {
        val selected = viewModel.podcast.value?.podcastGrouping ?: PodcastGrouping.None
        var dialog = OptionsDialog()
        PodcastGrouping.All.forEach { grouping ->
            dialog = dialog.addCheckedOption(titleId = grouping.groupName, checked = grouping == selected, click = { viewModel.updatePodcastGrouping(grouping) })
        }
        activity?.supportFragmentManager?.let {
            dialog.show(it, "grouping_options")
        }
        Unit // This is dumb kotlin
    }

    private val onEpisodesOptionsClicked: () -> Unit = {
        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_OPTIONS_TAPPED)
        var optionsDialog = OptionsDialog()
            .addTextOption(
                titleId = LR.string.podcast_sort_episodes,
                imageId = IR.drawable.ic_sort,
                valueId = selectedSortOrderStringId(),
                click = showEpisodeSortOptions
            )
            .addTextOption(
                titleId = LR.string.podcast_group_episodes,
                imageId = R.drawable.ic_group,
                valueId = selectedGroupStringId(),
                click = showGroupingOptions
            )
            .addTextOption(
                LR.string.podcast_download_all,
                imageId = IR.drawable.ic_download,
                click = { downloadAll() }
            )

        if (viewModel.shouldShowArchiveAll()) {
            optionsDialog = optionsDialog.addTextOption(
                titleId = LR.string.podcast_archive_all,
                imageId = R.drawable.ic_archive_all,
                click = onArchiveAllClicked
            )
        }

        if (viewModel.shouldShowArchivePlayed()) {
            optionsDialog = optionsDialog.addTextOption(
                LR.string.podcast_archive_all_played,
                imageId = R.drawable.ic_archive_all,
                click = this::archiveAllPlayed
            )
        }

        if (viewModel.shouldShowUnarchive()) {
            optionsDialog = optionsDialog.addTextOption(
                titleId = LR.string.unarchive_all,
                imageId = IR.drawable.ic_unarchive,
                click = viewModel::onUnarchiveClicked
            )
        }

        activity?.supportFragmentManager?.let {
            optionsDialog.show(it, "podcast_options_dialog")
        }
    }

    private fun selectedGroupStringId(): Int {
        return viewModel.podcast.value?.podcastGrouping?.groupName ?: PodcastGrouping.None.groupName
    }

    private fun selectedSortOrderStringId(): Int {
        return when (viewModel.podcast.value?.episodesSortType) {
            EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> LR.string.episode_sort_oldest_to_newest
            EpisodesSortType.EPISODES_SORT_BY_DATE_DESC -> LR.string.episode_sort_newest_to_oldest
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> LR.string.episode_sort_short_to_long
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> LR.string.episode_sort_long_to_short
            else -> LR.string.empty
        }
    }

    private val onFoldersClicked: () -> Unit = {
        lifecycleScope.launch {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_FOLDER_TAPPED)
            val folder = viewModel.getFolder()
            if (folder == null) {
                analyticsTracker.track(AnalyticsEvent.FOLDER_CHOOSE_SHOWN)
                FolderChooserFragment
                    .newInstance(viewModel.podcastUuid)
                    .show(parentFragmentManager, "folder_chooser_fragment")
                return@launch
            }
            analyticsTracker.track(AnalyticsEvent.FOLDER_CHOOSE_FOLDER_TAPPED)
            val dialog = PodcastFolderOptionsDialog(
                folder = folder,
                onRemoveFolder = {
                    analyticsTracker.track(AnalyticsEvent.FOLDER_PODCAST_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to REMOVE))
                    viewModel.removeFromFolder()
                },
                onChangeFolder = {
                    analyticsTracker.track(AnalyticsEvent.FOLDER_PODCAST_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to CHANGE))
                    FolderChooserFragment
                        .newInstance(viewModel.podcastUuid)
                        .show(parentFragmentManager, "folder_chooser_fragment")
                },
                onOpenFolder = {
                    analyticsTracker.track(AnalyticsEvent.FOLDER_PODCAST_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to GO_TO))
                    val fragment = PodcastsFragment.newInstance(folderUuid = folder.uuid)
                    (activity as FragmentHostListener).addFragment(fragment)
                },
                activity = activity
            )
            dialog.show()
        }
        multiSelectHelper.isMultiSelecting = false
    }

    private val onNotificationsClicked: () -> Unit = {
        context?.let {
            viewModel.toggleNotifications(it)
        }
    }

    private val onSettingsClicked: () -> Unit = {
        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SETTINGS_TAPPED)
        (activity as FragmentHostListener).addFragment(PodcastSettingsFragment.newInstance(viewModel.podcastUuid))
        multiSelectHelper.isMultiSelecting = false
    }

    private val onSearchFocus: () -> Unit = {
        // scroll to episode search
        binding?.episodesRecyclerView?.smoothScrollToTop(position = 1)
    }

    private val onShowArchivedClicked: () -> Unit = {
        viewModel.toggleShowArchived()
    }

    private val onArtworkLongClicked: (successCallback: () -> Unit) -> Unit = { successCallback ->
        val dialog = OptionsDialog()
            .addTextOption(
                titleId = LR.string.podcast_refresh_artwork,
                click = {
                    coilManager.clearCache(viewModel.podcastUuid)
                    successCallback()
                }
            )

        activity?.supportFragmentManager?.let {
            dialog.show(it, "artwork_refresh_dialog")
        }
    }

    val podcastUuid
        get() = arguments?.getString(ARG_PODCAST_UUID)!!

    private var lastSearchTerm: String? = null
    private var shouldCloseOnReturn = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val arguments = arguments ?: return
        fromListUuid = arguments.getString(ARG_LIST_UUID)
        featuredPodcast = arguments.getBoolean(ARG_FEATURED_PODCAST)

        adapter?.fromListUuid = fromListUuid

        if (savedInstanceState == null) {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SHOWN)
            FirebaseAnalyticsTracker.openedPodcast(podcastUuid)
        }
    }

    override fun onPause() {
        super.onPause()
        multiSelectHelper.isMultiSelecting = false
    }

    override fun onStop() {
        super.onStop()

        // Detach the adapter so when the app is in the background we don't update the row (needless battery)
        // We still want to remember the scroll state though so when we come back its not at the top of the page
        binding?.let {
            listState = it.episodesRecyclerView.layoutManager?.onSaveInstanceState()
            it.episodesRecyclerView.adapter = null
            UiUtil.hideKeyboard(it.root)
        }
    }

    override fun onStart() {
        super.onStart()

        if (shouldCloseOnReturn) {
            launch(Dispatchers.Main) {
                // You can't call back during onresume
                @Suppress("DEPRECATION")
                (activity as? AppCompatActivity)?.onBackPressed()
            }
        }

        updateStatusBar()

        binding?.episodesRecyclerView?.adapter = adapter
        binding?.episodesRecyclerView?.layoutManager?.onRestoreInstanceState(listState)
    }

    fun episodeSwipeArchive(episode: Episode, index: Int) {
        val binding = binding ?: return

        binding.episodesRecyclerView.findViewHolderForAdapterPosition(index)?.let {
            itemTouchHelper.clearView(binding.episodesRecyclerView, it)
        }

        viewModel.episodeSwipeArchive(episode, index)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val binding = FragmentPodcastBinding.inflate(inflater, container, false)
        this.binding = binding

        val context = binding.root.context
        val headerColor = context.getThemeColor(UR.attr.support_09)
        binding.headerColor = headerColor
        statusBarColor = StatusBarColor.Custom(headerColor, true)
        updateStatusBar()

        itemTouchHelper = EpisodeItemTouchHelper(this::episodeSwipedRightItem1, this::episodeSwipedRightItem2, this::episodeSwipeArchive)

        loadData()

        binding.toolbar.let {
            it.inflateMenu(R.menu.podcast_menu)
            it.setOnMenuItemClickListener(this)
            it.setNavigationOnClickListener {
                @Suppress("DEPRECATION")
                activity?.onBackPressed()
            }
            val iconColor = it.context.getThemeColor(UR.attr.contrast_01)
            it.menu.setupChromeCastButton(context) {
                chromeCastAnalytics.trackChromeCastViewShown()
            }
            it.menu.tintIcons(iconColor)
            it.navigationIcon?.setTint(iconColor)
            it.navigationContentDescription = getString(LR.string.back)
            it.setOnLongClickListener {
                theme.toggleDarkLightThemeActivity(activity as AppCompatActivity)
                true
            }
        }

        playButtonListener.source = AnalyticsSource.PODCAST_SCREEN
        if (adapter == null) {
            adapter = PodcastAdapter(
                downloadManager = downloadManager,
                playbackManager = playbackManager,
                upNextQueue = upNextQueue,
                settings = settings,
                theme = theme,
                fromListUuid = fromListUuid,
                onHeaderSummaryToggled = onHeaderSummaryToggled,
                onSubscribeClicked = onSubscribeClicked,
                onUnsubscribeClicked = onUnsubscribeClicked,
                onEpisodesOptionsClicked = onEpisodesOptionsClicked,
                onRowLongPress = onRowLongPress,
                onFoldersClicked = onFoldersClicked,
                onNotificationsClicked = onNotificationsClicked,
                onSettingsClicked = onSettingsClicked,
                playButtonListener = playButtonListener,
                onRowClicked = onRowClicked,
                onSearchQueryChanged = onSearchQueryChanged,
                onSearchFocus = onSearchFocus,
                onShowArchivedClicked = onShowArchivedClicked,
                multiSelectHelper = multiSelectHelper,
                onArtworkLongClicked = onArtworkLongClicked,
                ratingsViewModel = ratingsViewModel,
            )
        }

        binding.episodesRecyclerView.let {
            it.adapter = adapter
            it.recycledViewPool.setMaxRecycledViews(PodcastAdapter.VIEW_TYPE_EPISODE_HEADER, 1)
            it.recycledViewPool.setMaxRecycledViews(PodcastAdapter.VIEW_TYPE_PODCAST_HEADER, 1)
            (it.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            (it.itemAnimator as? SimpleItemAnimator)?.changeDuration = 0
            it.addOnScrollListener(onScrollListener)
        }

        itemTouchHelper.attachToRecyclerView(binding.episodesRecyclerView)

        binding.btnRetry.setOnClickListener {
            loadData()
            binding.error = null
            binding.executePendingBindings()
        }

        binding.episodesRecyclerView.requestFocus()

        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) {
            binding.multiSelectToolbar.isVisible = it
            binding.toolbar.isVisible = !it

            adapter?.notifyDataSetChanged()
        }
        multiSelectHelper.coordinatorLayout = (activity as FragmentHostListener).snackBarView()
        multiSelectHelper.listener = object : MultiSelectHelper.Listener {
            override fun multiSelectSelectNone() {
                val episodeState = viewModel.episodes.value
                if (episodeState is PodcastViewModel.EpisodeState.Loaded) {
                    episodeState.episodes.forEach { multiSelectHelper.deselect(it) }
                    adapter?.notifyDataSetChanged()
                }
            }

            override fun multiSelectSelectAllUp(episode: Episode) {
                val grouped = viewModel.groupedEpisodes.value
                if (grouped != null) {
                    val group = grouped.find { it.contains(episode) } ?: return
                    val startIndex = group.indexOf(episode)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(group.subList(0, startIndex + 1))
                    }

                    adapter?.notifyDataSetChanged()
                }
            }

            override fun multiSelectSelectAllDown(episode: Episode) {
                val grouped = viewModel.groupedEpisodes.value
                if (grouped != null) {
                    val group = grouped.find { it.contains(episode) } ?: return
                    val startIndex = group.indexOf(episode)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(group.subList(startIndex, group.size))
                    }

                    adapter?.notifyDataSetChanged()
                }
            }

            override fun multiSelectSelectAll() {
                val episodeState = viewModel.episodes.value
                if (episodeState is PodcastViewModel.EpisodeState.Loaded) {
                    multiSelectHelper.selectAllInList(episodeState.episodes)
                    adapter?.notifyDataSetChanged()
                }
            }
        }
        multiSelectHelper.source = AnalyticsSource.PODCAST_SCREEN
        binding.multiSelectToolbar.setup(viewLifecycleOwner, multiSelectHelper, menuRes = null, fragmentManager = parentFragmentManager)

        return binding.root
    }

    private fun loadData() {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Loading podcast page for $podcastUuid")
        viewModel.loadPodcast(podcastUuid, resources)

        viewModel.signInState.observe(viewLifecycleOwner) { signInState ->
            adapter?.setSignedInAsPlus(signInState.isSignedInAsPlus)
        }

        viewModel.podcast.observe(
            viewLifecycleOwner,
            Observer<Podcast> { podcast ->
                val binding = binding ?: return@Observer

                binding.podcast = podcast

                val backgroundColor = ThemeColor.podcastUi03(theme.activeTheme, podcast.backgroundColor)
                binding.headerColor = backgroundColor

                adapter?.setPodcast(podcast)

                viewModel.archiveEpisodeLimit()

                statusBarColor = StatusBarColor.Custom(backgroundColor, true)
                updateStatusBar()

                binding.executePendingBindings()
            }
        )

        viewModel.tintColor.observe(viewLifecycleOwner) { tintColor ->
            binding?.tintColor = tintColor
            adapter?.setTint(tintColor)
        }

        viewModel.episodes.observe(
            viewLifecycleOwner,
            Observer { state ->
                when (state) {
                    is PodcastViewModel.EpisodeState.Loaded -> {
                        addPaddingForEpisodeSearch(state.episodes)
                        val contextRequired = context ?: return@Observer
                        adapter?.setEpisodes(
                            episodes = state.episodes,
                            showingArchived = state.showingArchived,
                            episodeCount = state.episodeCount,
                            archivedCount = state.archivedCount,
                            searchTerm = state.searchTerm,
                            episodeLimit = state.episodeLimit,
                            episodeLimitIndex = state.episodeLimitIndex,
                            grouping = state.grouping,
                            episodesSortType = state.episodesSortType,
                            context = contextRequired
                        )
                        if (state.searchTerm.isNotEmpty() && state.searchTerm != lastSearchTerm) {
                            binding?.episodesRecyclerView?.smoothScrollToTop(1)
                        }
                        lastSearchTerm = state.searchTerm
                    }
                    is PodcastViewModel.EpisodeState.Error -> {
                        adapter?.setError()
                        binding?.error = getString(LR.string.podcast_load_error)

                        if (BuildConfig.DEBUG) {
                            UiUtil.displayAlertError(requireContext(), state.errorMessage, null)
                        }
                    }
                }
            }
        )

        viewModel.castConnected.observe(viewLifecycleOwner) { castConnected ->
            adapter?.castConnected = castConnected
        }
    }

    /**
     * Episode search needs at least a page worth of space under the search box so the user can see the results below.
     */
    private fun addPaddingForEpisodeSearch(episodes: List<PodcastEpisode>) {
        val rowCount = episodes.size
        val binding = binding ?: return
        val pageHeight = binding.episodesRecyclerView.height
        val context = binding.episodesRecyclerView.context
        val episodeHeaderHeightPx = 90.dpToPx(context)
        val rowHeightPx: Int = 80.dpToPx(context)

        val actualHeight = episodeHeaderHeightPx + (rowCount * rowHeightPx)

        val missingHeightPx = pageHeight - actualHeight

        // only add padding to stop the screen jumping to the wrong location
        if (binding.episodesRecyclerView.paddingBottom > missingHeightPx) {
            return
        }
        binding.episodesRecyclerView.updatePadding(bottom = if (missingHeightPx < 0) 0 else missingHeightPx)
    }

    override fun onDestroyView() {
        binding?.episodesRecyclerView?.adapter = null
        super.onDestroyView()

        binding?.episodesRecyclerView?.removeOnScrollListener(onScrollListener)
        binding?.episodesRecyclerView?.adapter = null
        binding = null
    }

    private fun episodeSwipedRightItem1(episode: Episode, index: Int) {
        when (settings.getUpNextSwipeAction()) {
            Settings.UpNextAction.PLAY_NEXT -> viewModel.episodeSwipeUpNext(episode)
            Settings.UpNextAction.PLAY_LAST -> viewModel.episodeSwipeUpLast(episode)
        }

        binding?.episodesRecyclerView?.let { recyclerView ->
            recyclerView.findViewHolderForAdapterPosition(index)?.let {
                itemTouchHelper.clearView(recyclerView, it)
            }
        }

        adapter?.notifyItemChanged(index)
    }

    private fun episodeSwipedRightItem2(episode: Episode, index: Int) {
        when (settings.getUpNextSwipeAction()) {
            Settings.UpNextAction.PLAY_NEXT -> viewModel.episodeSwipeUpLast(episode)
            Settings.UpNextAction.PLAY_LAST -> viewModel.episodeSwipeUpNext(episode)
        }

        binding?.episodesRecyclerView?.let { recyclerView ->
            recyclerView.findViewHolderForAdapterPosition(index)?.let {
                itemTouchHelper.clearView(recyclerView, it)
            }
        }
        adapter?.notifyItemChanged(index)
    }

    private fun archiveAllPlayed() {
        val count = viewModel.archivePlayedCount()
        val buttonString = resources.getStringPlural(count = count, singular = LR.string.archive_episodes_singular, plural = LR.string.archive_episodes_plural)

        val dialog = ConfirmationDialog()
            .setTitle(getString(LR.string.podcast_archive_all_played))
            .setSummary(getString(LR.string.podcast_archive_played))
            .setIconId(R.drawable.ic_archive_all)
            .setButtonType(ConfirmationDialog.ButtonType.Danger(buttonString))
            .setOnConfirm(viewModel::archivePlayed)
        dialog.show(parentFragmentManager, "confirm_archive_all_played")
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.share -> share()
        }
        return true
    }

    private fun share() {
        val context = context ?: return
        viewModel.podcast.value?.let { podcast ->
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SHARE_TAPPED)
            SharePodcastHelper(
                podcast,
                null,
                null,
                context,
                SharePodcastHelper.ShareType.PODCAST,
                AnalyticsSource.PODCAST_SCREEN,
                analyticsTracker
            ).showShareDialogDirect()
        }
    }

    private fun downloadAll() {
        val episodeCount = viewModel.episodeCount()
        val dialog = ConfirmationDialog.downloadWarningDialog(episodeCount, resources) {
            viewModel.downloadAll()
        }
        dialog?.show(parentFragmentManager, "download_confirm")
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
        return super.getBackstackCount() + if (multiSelectHelper.isMultiSelecting) 1 else 0
    }
}
