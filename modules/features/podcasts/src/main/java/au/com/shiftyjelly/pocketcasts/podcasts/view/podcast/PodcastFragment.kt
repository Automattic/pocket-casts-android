package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.MenuRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar.OnMenuItemClickListener
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import androidx.recyclerview.widget.SimpleItemAnimator
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarksSortByDialog
import au.com.shiftyjelly.pocketcasts.podcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentPodcastBinding
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentPodcastRedesignBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeContainerFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderChooserFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter.HeaderType
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts.PodcastsFragment
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.EpisodeListBookmarkViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastViewModel.PodcastTab
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.reimagine.podcast.SharePodcastFragment
import au.com.shiftyjelly.pocketcasts.reimagine.timestamp.ShareEpisodeTimestampFragment
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageColorAnalyzer
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.HeadphoneControlsSettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.SettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.sharing.SharingClient
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.ui.images.CoilManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.includeStatusBarPadding
import au.com.shiftyjelly.pocketcasts.views.extensions.setupChromeCastButton
import au.com.shiftyjelly.pocketcasts.views.extensions.smoothScrollToTop
import au.com.shiftyjelly.pocketcasts.views.extensions.tintIcons
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutFactory
import au.com.shiftyjelly.pocketcasts.views.helper.SwipeButtonLayoutViewModel
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper.NavigationState
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asObservable
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class PodcastFragment : BaseFragment() {

    companion object {
        private const val NEW_INSTANCE_ARGS = "PodcastFragmentArgs"
        private const val OPTION_KEY = "option"
        private const val IS_EXPANDED_KEY = "is_expanded"
        private const val PODCAST_UUID_KEY = "podcast_uuid"
        private const val LIST_ID_KEY = "list_id"
        private const val EPISODE_UUID_KEY = "episode_uuid"
        private const val SOURCE_KEY = "source"
        private const val REMOVE = "remove"
        private const val CHANGE = "change"
        private const val GO_TO = "go_to"
        private const val EPISODE_CARD = "episode_card"

        fun newInstance(
            podcastUuid: String,
            sourceView: SourceView,
            fromListUuid: String? = null,
            featuredPodcast: Boolean = false,
        ): PodcastFragment = PodcastFragment().apply {
            arguments = bundleOf(
                NEW_INSTANCE_ARGS to PodcastFragmentArgs(
                    podcastUuid = podcastUuid,
                    sourceView = sourceView,
                    fromListUuid = fromListUuid,
                    featuredPodcast = featuredPodcast,
                    isHeaderRedesigned = FeatureFlag.isEnabled(Feature.PODCAST_VIEW_CHANGES),
                ),
            )
        }

        private fun extractArgs(bundle: Bundle?) = bundle?.let {
            BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, PodcastFragmentArgs::class.java)
        }
    }

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var podcastManager: PodcastManager

    @Inject
    lateinit var episodeManager: EpisodeManager

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var downloadManager: DownloadManager

    @Inject
    lateinit var playButtonListener: PlayButton.OnClickListener

    @Inject
    lateinit var upNextQueue: UpNextQueue

    @Inject
    lateinit var bookmarkManager: BookmarkManager

    @Inject
    lateinit var coilManager: CoilManager

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var sharingClient: SharingClient

    @Inject
    lateinit var colorAnalyzer: PodcastImageColorAnalyzer

    @Inject
    lateinit var categoriesManager: CategoriesManager

    private val viewModel: PodcastViewModel by viewModels()
    private val ratingsViewModel: PodcastRatingsViewModel by viewModels()
    private val episodeListBookmarkViewModel: EpisodeListBookmarkViewModel by viewModels()
    private val swipeButtonLayoutViewModel: SwipeButtonLayoutViewModel by viewModels()

    private var binding: BindingWrapper? = null

    private var itemTouchHelper: EpisodeItemTouchHelper? = null
    private var adapter: PodcastAdapter? = null

    private var currentSnackBar: Snackbar? = null

    private val onScrollListener = object : RecyclerView.OnScrollListener() {
        private var transparencyThreshold = -1
        private var maxProgressDistance = -1

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (transparencyThreshold == -1) {
                transparencyThreshold = 40.dpToPx(recyclerView.context)
            }
            if (maxProgressDistance == -1) {
                maxProgressDistance = 100.dpToPx(recyclerView.context)
            }

            /*
             * Computing the correct scroll offset for toolbar animation is challenging.
             *
             * We can't simply accumulate 'dy' because it does not update correctly when items
             * are changed due to archiving episodes, swapping to bookmarks, and so on.
             *
             * 'dy' accumulation also encounters issues with configuration changes.
             * While we can save and restore the scroll position, it may not be accurate
             * after screen rotation or due to different layouts.
             *
             * Normally, we could achieve a correct animation using a CoordinatorLayout
             * with a custom behavior. However, this approach runs into issues with Compose interop,
             * varying layouts due to feature flags and maintenance concerns, etc.
             *
             * Fortunately, our header is large enough to allow for a reasonable scroll distance,
             * resulting in a visually pleasing animation. For simplicity, we use this workaround.
             */
            val layoutManager = (recyclerView.layoutManager as? LinearLayoutManager) ?: return
            val headerViewOffset = getHeaderViewOffset(layoutManager)
            binding?.setToolbarTransparency(computeTransparencyProgress(headerViewOffset))
        }

        private fun getHeaderViewOffset(layoutManager: LinearLayoutManager): Int {
            return layoutManager
                .findViewByPosition(layoutManager.findFirstVisibleItemPosition())
                ?.takeIf { it.getTag(UR.id.podcast_view_header_tag) == true }
                ?.let { view -> view.top.absoluteValue }
                ?: Int.MAX_VALUE
        }

        private fun computeTransparencyProgress(offset: Int): Float {
            return if (offset > transparencyThreshold) {
                lerp(
                    start = 1f,
                    stop = 0f,
                    fraction = (offset - transparencyThreshold).toFloat() / (maxProgressDistance),
                ).coerceIn(0f, 1f)
            } else {
                1f
            }
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                UiUtil.hideKeyboard(recyclerView)
                binding?.showBackgroundPlaceholder(false)
            }
        }
    }

    override var statusBarIconColor: StatusBarIconColor = StatusBarIconColor.Light

    private val onHeaderSummaryToggled: (
        expanded: Boolean,
        userInitiated: Boolean,
    ) -> Unit = { expanded, userInitiated ->
        if (userInitiated) {
            analyticsTracker.track(
                AnalyticsEvent.PODCAST_SCREEN_TOGGLE_SUMMARY,
                mapOf(IS_EXPANDED_KEY to expanded),
            )
        }
    }

    private val onSubscribeClicked: () -> Unit = {
        fromListUuid?.let {
            analyticsTracker.track(AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED, mapOf(LIST_ID_KEY to it, PODCAST_UUID_KEY to podcastUuid))
        }
        if (featuredPodcast) {
            viewModel.podcast.value?.uuid?.let { podcastUuid ->
                analyticsTracker.track(AnalyticsEvent.DISCOVER_FEATURED_PODCAST_SUBSCRIBED, mapOf(PODCAST_UUID_KEY to podcastUuid))
            }
        }
        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SUBSCRIBE_TAPPED)

        viewModel.subscribeToPodcast()
    }

    private val onUnsubscribeClicked: (successCallback: () -> Unit) -> Unit = { successCallback ->
        lifecycleScope.launch {
            val downloaded = withContext(Dispatchers.Default) { podcastManager.countEpisodesInPodcastWithStatusBlocking(podcastUuid, EpisodeStatusEnum.DOWNLOADED) }
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

    private fun <T> onRowLongPress(): (entity: T) -> Unit = {
        when (it) {
            is PodcastEpisode -> {
                if (viewModel.multiSelectEpisodesHelper.listener == null) {
                    binding?.setupMultiSelect()
                }
                viewModel.multiSelectEpisodesHelper
                    .defaultLongPress(multiSelectable = it, fragmentManager = childFragmentManager)
            }

            is Bookmark -> {
                if (viewModel.multiSelectBookmarksHelper.listener == null) {
                    binding?.setupMultiSelect()
                }
                viewModel.multiSelectBookmarksHelper
                    .defaultLongPress(multiSelectable = it, fragmentManager = childFragmentManager)
            }
        }
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
            analyticsTracker.track(
                AnalyticsEvent.DISCOVER_LIST_EPISODE_TAPPED,
                mapOf(LIST_ID_KEY to listUuid, PODCAST_UUID_KEY to episode.podcastUuid, EPISODE_UUID_KEY to episode.uuid),
            )
        }
        val episodeCard = EpisodeContainerFragment.newInstance(
            episode = episode,
            source = EpisodeViewSource.PODCAST_SCREEN,
            overridePodcastLink = true,
            fromListUuid = fromListUuid,
        )
        episodeCard.show(parentFragmentManager, EPISODE_CARD)
    }

    private val onSearchQueryChanged: (String) -> Unit = { searchQuery ->
        viewModel.searchQueryUpdated(searchQuery)
    }

    private val sortEpisodesTitleAZ = {
        adapter?.signalLargeDiff()
        viewModel.updateEpisodesSortType(EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC)
    }

    private val sortEpisodesTitleZA = {
        adapter?.signalLargeDiff()
        viewModel.updateEpisodesSortType(EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC)
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
                titleId = LR.string.episode_sort_title_a_z,
                checked = viewModel.podcast.value?.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC,
                click = sortEpisodesTitleAZ,
            )
            .addCheckedOption(
                titleId = LR.string.episode_sort_title_z_a,
                checked = viewModel.podcast.value?.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC,
                click = sortEpisodesTitleZA,
            )
            .addCheckedOption(
                titleId = LR.string.episode_sort_newest_to_oldest,
                checked = viewModel.podcast.value?.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_DATE_DESC,
                click = sortEpisodesNewestToOldest,
            )
            .addCheckedOption(
                titleId = LR.string.episode_sort_oldest_to_newest,
                checked = viewModel.podcast.value?.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_DATE_ASC,
                click = sortEpisodesOldestToNewest,
            )
            .addCheckedOption(
                titleId = LR.string.episode_sort_short_to_long,
                checked = viewModel.podcast.value?.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC,
                click = sortEpisodesLengthShortToLong,
            )
            .addCheckedOption(
                titleId = LR.string.episode_sort_long_to_short,
                checked = viewModel.podcast.value?.episodesSortType == EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC,
                click = sortEpisodesLengthLongToShort,
            )
        activity?.supportFragmentManager?.let {
            dialog.show(it, "episodes_sort_options_dialog")
        }
        Unit
    }

    private val showGroupingOptions = {
        val selected = viewModel.podcast.value?.grouping ?: PodcastGrouping.None
        var dialog = OptionsDialog()
        PodcastGrouping.All.forEach { grouping ->
            dialog = dialog.addCheckedOption(titleId = grouping.groupName, checked = grouping == selected, click = { viewModel.updatePodcastGrouping(grouping) })
        }
        activity?.supportFragmentManager?.let {
            dialog.show(it, "grouping_options")
        }
        Unit // This is dumb kotlin
    }

    private val showBookmarksOptionsDialog: () -> Unit = {
        activity?.supportFragmentManager?.let {
            BookmarksSortByDialog(
                settings = settings,
                changeSortOrder = viewModel::changeSortOrder,
                sourceView = SourceView.PODCAST_SCREEN,
            ).show(
                context = requireContext(),
                fragmentManager = it,
            )
        }
    }

    private val onEpisodesOptionsClicked: () -> Unit = {
        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_OPTIONS_TAPPED)
        var optionsDialog = OptionsDialog()

        if (FeatureFlag.isEnabled(Feature.PODCAST_FEED_UPDATE)) {
            optionsDialog = optionsDialog.addTextOption(
                titleId = LR.string.podcast_refresh_episodes,
                imageId = IR.drawable.ic_refresh,
                click = {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.onRefreshPodcast(PodcastViewModel.RefreshType.REFRESH_BUTTON)
                    }
                },
            )
        }

        optionsDialog
            .addTextOption(
                titleId = LR.string.podcast_sort_episodes,
                imageId = IR.drawable.ic_sort,
                valueId = selectedSortOrderStringId(),
                click = showEpisodeSortOptions,
            )
            .addTextOption(
                titleId = LR.string.podcast_group_episodes,
                imageId = R.drawable.ic_group,
                valueId = selectedGroupStringId(),
                click = showGroupingOptions,
            )
            .addTextOption(
                LR.string.podcast_download_all,
                imageId = IR.drawable.ic_download,
                click = { downloadAll() },
            )

        if (viewModel.shouldShowArchiveAll()) {
            optionsDialog = optionsDialog.addTextOption(
                titleId = LR.string.podcast_archive_all,
                imageId = R.drawable.ic_archive_all,
                click = onArchiveAllClicked,
            )
        }

        if (viewModel.shouldShowArchivePlayed()) {
            optionsDialog = optionsDialog.addTextOption(
                LR.string.podcast_archive_all_played,
                imageId = R.drawable.ic_archive_all,
                click = this::archiveAllPlayed,
            )
        }

        if (viewModel.shouldShowUnarchive()) {
            optionsDialog = optionsDialog.addTextOption(
                titleId = LR.string.unarchive_all,
                imageId = IR.drawable.ic_unarchive,
                click = viewModel::onUnarchiveClicked,
            )
        }

        activity?.supportFragmentManager?.let {
            optionsDialog.show(it, "podcast_options_dialog")
        }
    }

    private fun selectedGroupStringId(): Int {
        return viewModel.podcast.value?.grouping?.groupName ?: PodcastGrouping.None.groupName
    }

    private fun selectedSortOrderStringId(): Int {
        return when (viewModel.podcast.value?.episodesSortType) {
            EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC -> LR.string.episode_sort_title_a_z
            EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC -> LR.string.episode_sort_title_z_a
            EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> LR.string.episode_sort_oldest_to_newest
            EpisodesSortType.EPISODES_SORT_BY_DATE_DESC -> LR.string.episode_sort_newest_to_oldest
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> LR.string.episode_sort_short_to_long
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> LR.string.episode_sort_long_to_short
            null -> LR.string.empty
        }
    }

    private val onFoldersClicked: () -> Unit = {
        lifecycleScope.launch {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_FOLDER_TAPPED)
            val isSignedInAsPlusOrPatron = viewModel.signInState.value?.isSignedInAsPlusOrPatron == true
            if (!isSignedInAsPlusOrPatron) {
                OnboardingLauncher.openOnboardingFlow(activity, OnboardingFlow.Upsell(OnboardingUpgradeSource.FOLDERS_PODCAST_SCREEN))
                return@launch
            }
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
                activity = activity,
            )
            dialog.show()
        }
        viewModel.multiSelectEpisodesHelper.isMultiSelecting = false
        viewModel.multiSelectBookmarksHelper.isMultiSelecting = false
    }

    private val onNotificationsClicked: (Podcast, Boolean) -> Unit = { podcast, show ->
        viewModel.showNotifications(podcast.uuid, show)
        currentSnackBar?.dismiss()
        if (show) {
            showSnackBar(
                message = getString(LR.string.notifications_enabled_message, podcast.title),
                duration = 3000,
            )
        }
    }

    private val onSettingsClicked: () -> Unit = {
        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SETTINGS_TAPPED)
        (activity as FragmentHostListener).addFragment(PodcastSettingsFragment.newInstance(viewModel.podcastUuid))
        viewModel.multiSelectEpisodesHelper.isMultiSelecting = false
        viewModel.multiSelectBookmarksHelper.isMultiSelecting = false
    }

    private val onSearchFocus: () -> Unit = {
        // scroll to episode search
        val toolbarHeight = when (binding?.headerType) {
            null, HeaderType.SolidColor -> 0
            HeaderType.Blur, HeaderType.Scrim -> binding?.toolbar?.height ?: 0
        }
        binding?.episodesRecyclerView?.smoothScrollToTop(1, offset = toolbarHeight)
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
                },
            )

        activity?.supportFragmentManager?.let {
            dialog.show(it, "artwork_refresh_dialog")
        }
    }

    private val onTabClicked: (tab: PodcastTab) -> Unit = { tab ->
        viewModel.onTabClicked(tab)
    }

    private val onBookmarkPlayClicked: (bookmark: Bookmark) -> Unit = { bookmark ->
        viewModel.play(bookmark)
    }

    private fun onHeadsetSettingsClicked() {
        val fragmentHostListener = (activity as? FragmentHostListener)
        fragmentHostListener?.apply {
            openTab(VR.id.navigation_profile)
            addFragment(SettingsFragment())
            addFragment(HeadphoneControlsSettingsFragment())
        }
    }

    private val args: PodcastFragmentArgs
        get() = extractArgs(arguments) ?: error("$NEW_INSTANCE_ARGS argument is missing. Fragment must be created using newInstance function")

    val podcastUuid: String
        get() = args.podcastUuid

    private val sourceView: SourceView
        get() = args.sourceView

    private val featuredPodcast: Boolean
        get() = args.featuredPodcast

    private val fromListUuid: String?
        get() = args.fromListUuid

    private var lastSearchTerm: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter?.fromListUuid = fromListUuid
        if (savedInstanceState == null) {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SHOWN, mapOf(SOURCE_KEY to sourceView.analyticsValue))
        }
    }

    var currentToolbarColor: Color? = null
    var artworkDominantColor: Color? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = BindingWrapper.inflate(
            inflater = inflater,
            container = container,
            isHeaderRedesigned = args.isHeaderRedesigned,
            onToolbarColorChange = { color ->
                currentToolbarColor = color
                updateStausBarForBackground()
            },
        ).also { binding = it }

        binding.swipeRefreshLayout.isEnabled = FeatureFlag.isEnabled(Feature.PODCAST_FEED_UPDATE)
        binding.setToolbarStaticColor(requireContext().getThemeColor(UR.attr.support_09))
        binding.setUpToolbar(
            theme = theme,
            menuId = R.menu.podcast_menu,
            onChromeCast = {
                chromeCastAnalytics.trackChromeCastViewShown()
            },
            onShare = {
                share()
            },
            onNavigateBack = {
                @Suppress("DEPRECATION")
                activity?.onBackPressed()
            },
            onLongClick = {
                theme.toggleDarkLightThemeActivity(activity as AppCompatActivity)
            },
        )

        adapter = PodcastAdapter(
            context = requireContext(),
            headerType = binding.headerType,
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
            onBookmarksOptionsClicked = showBookmarksOptionsDialog,
            onEpisodeRowLongPress = onRowLongPress(),
            onBookmarkRowLongPress = onRowLongPress(),
            onFoldersClicked = onFoldersClicked,
            onNotificationsClicked = onNotificationsClicked,
            onSettingsClicked = onSettingsClicked,
            playButtonListener = playButtonListener,
            onRowClicked = onRowClicked,
            onSearchQueryChanged = onSearchQueryChanged,
            onSearchFocus = onSearchFocus,
            onShowArchivedClicked = onShowArchivedClicked,
            multiSelectEpisodesHelper = viewModel.multiSelectEpisodesHelper,
            multiSelectBookmarksHelper = viewModel.multiSelectBookmarksHelper,
            onArtworkLongClicked = onArtworkLongClicked,
            onTabClicked = onTabClicked,
            onBookmarkPlayClicked = onBookmarkPlayClicked,
            ratingsViewModel = ratingsViewModel,
            swipeButtonLayoutFactory = SwipeButtonLayoutFactory(
                swipeButtonLayoutViewModel = swipeButtonLayoutViewModel,
                onItemUpdated = ::notifyItemChanged,
                defaultUpNextSwipeAction = { settings.upNextSwipe.value },
                fragmentManager = parentFragmentManager,
                swipeSource = EpisodeItemTouchHelper.SwipeSource.PODCAST_DETAILS,
            ),
            onHeadsetSettingsClicked = ::onHeadsetSettingsClicked,
            sourceView = SourceView.PODCAST_SCREEN,
            podcastBookmarksObservable = bookmarkManager.findPodcastBookmarksFlow(
                podcastUuid = podcastUuid,
                sortType = settings.podcastBookmarksSortType.flow.value,
            ).asObservable(),
            onPodcastDescriptionClicked = {
                analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_PODCAST_DESCRIPTION_TAPPED)
            },
            onChangeHeaderExpanded = { uuid, isExpanded ->
                viewModel.updateIsHeaderExpanded(uuid, isExpanded)
            },
            onClickRating = { podcast, source ->
                ratingsViewModel.onRatingStarsTapped(
                    podcastUuid = podcast.uuid,
                    fragmentManager = parentFragmentManager,
                    source = source,
                )
            },
            onClickCategory = { podcast ->
                val categoryId = podcast.getFirstCategoryId()
                if (categoryId != null) {
                    analyticsTracker.track(
                        AnalyticsEvent.PODCAST_SCREEN_CATEGORY_TAPPED,
                        mapOf("category" to podcast.getFirstCategoryUnlocalised()),
                    )
                    categoriesManager.selectCategory(categoryId)
                    val hostListener = (requireActivity() as FragmentHostListener)
                    hostListener.closeToRoot()
                    hostListener.openTab(VR.id.navigation_discover)
                }
            },
            onArtworkAvailable = { podcast ->
                viewLifecycleOwner.lifecycleScope.launch {
                    artworkDominantColor = colorAnalyzer.getArtworkDominantColor(podcast.uuid)
                    updateStausBarForBackground()
                }
            },
        ).apply {
            stateRestorationPolicy = StateRestorationPolicy.PREVENT_WHEN_EMPTY
        }

        binding.episodesRecyclerView.let {
            it.adapter = adapter
            it.recycledViewPool.setMaxRecycledViews(PodcastAdapter.VIEW_TYPE_EPISODE_HEADER, 1)
            it.recycledViewPool.setMaxRecycledViews(PodcastAdapter.VIEW_TYPE_PODCAST_HEADER, 1)
            (it.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
            (it.itemAnimator as? SimpleItemAnimator)?.changeDuration = 0
            it.addOnScrollListener(onScrollListener)
        }

        itemTouchHelper = EpisodeItemTouchHelper().apply {
            attachToRecyclerView(binding.episodesRecyclerView)
        }

        binding.btnRetry.setOnClickListener {
            loadData()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            viewLifecycleOwner.lifecycleScope.launch {
                viewModel.onRefreshPodcast(PodcastViewModel.RefreshType.PULL_TO_REFRESH)
            }
        }

        val progressSpinnerPadding = 16.dpToPx(requireContext())
        binding.toolbar.addOnLayoutChangeListener { view, _, _, _, _, _, _, _, _ ->
            binding.swipeRefreshLayout.setProgressViewOffset(false, 0, view.height + progressSpinnerPadding)
        }

        playButtonListener.source = SourceView.PODCAST_SCREEN
        loadData()
        updateStatusBar()

        setupTooltip(binding, adapter!!)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.setupMultiSelect()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.multiSelectBookmarksHelper.navigationState
                    .collect { navigationState ->
                        when (navigationState) {
                            NavigationState.ShareBookmark -> onShareBookmarkClick()
                            NavigationState.EditBookmark -> onEditBookmarkClick()
                        }
                    }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                ratingsViewModel.stateFlow.collect { state ->
                    adapter?.setRatingState(state)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        settings.trackingAutoPlaySource.set(AutoPlaySource.fromId(podcastUuid), updateModifiedAt = false)
    }

    override fun onPause() {
        super.onPause()
        viewModel.multiSelectEpisodesHelper.isMultiSelecting = false
        viewModel.multiSelectBookmarksHelper.isMultiSelecting = false
    }

    private fun setupTooltip(binding: BindingWrapper, adapter: PodcastAdapter) {
        val isNewHeaderDesign = when (binding.headerType) {
            HeaderType.SolidColor -> false
            HeaderType.Blur -> true
            HeaderType.Scrim -> true
        }
        if (!isNewHeaderDesign || !settings.showPodcastHeaderChangesTooltip.value) {
            binding.composeTooltipHost.isGone = true
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val offset = adapter.awaitTooltipHeaderTopOffset()
            if (offset != null) {
                binding.composeTooltipHost.isVisible = true
                binding.composeTooltipHost.setContentWithViewCompositionStrategy {
                    AppTheme(theme.activeTheme) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f))
                                .clickable(
                                    interactionSource = null,
                                    indication = null,
                                    onClick = { dismissHeaderTooltip() },
                                ),
                        ) {
                            Layout(
                                content = {
                                    PodcastHeaderTooltip(
                                        onClickClose = { dismissHeaderTooltip() },
                                        modifier = Modifier.padding(horizontal = 38.dp),
                                    )
                                },
                                measurePolicy = MeasurePolicy { measurables, constraints ->
                                    val tooltip = measurables[0].measure(constraints)
                                    layout(tooltip.width, tooltip.height) {
                                        tooltip.place(0, offset.roundToPx() - tooltip.height)
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    private fun dismissHeaderTooltip() {
        settings.showPodcastHeaderChangesTooltip.set(false, updateModifiedAt = false)
        binding?.composeTooltipHost?.isGone = true
        binding?.composeTooltipHost?.disposeComposition()
    }

    private fun onShareBookmarkClick() {
        lifecycleScope.launch {
            val (podcast, episode, bookmark) = viewModel.getSharedBookmark() ?: return@launch
            viewModel.onBookmarkShare(podcast.uuid, episode.uuid, sourceView)
            val timestamp = bookmark.timeSecs.seconds
            if (FeatureFlag.isEnabled(Feature.REIMAGINE_SHARING)) {
                ShareEpisodeTimestampFragment
                    .forBookmark(episode, timestamp, podcast.backgroundColor, SourceView.PODCAST_SCREEN)
                    .show(parentFragmentManager, "share_screen")
            } else {
                val request = SharingRequest.bookmark(podcast, episode, timestamp)
                    .setSourceView(SourceView.PODCAST_SCREEN)
                    .build()
                sharingClient.share(request)
            }
        }
    }

    private fun onEditBookmarkClick() {
        viewModel.buildBookmarkArguments { arguments ->
            startActivity(arguments.getIntent(requireContext()))
        }
    }

    private fun BindingWrapper.setupMultiSelect() {
        viewModel.multiSelectEpisodesHelper.setUp(multiSelectEpisodesToolbar)
        viewModel.multiSelectBookmarksHelper.setUp(multiSelectBookmarksToolbar)
    }

    fun <T> MultiSelectHelper<T>.setUp(multiSelectToolbar: MultiSelectToolbar) {
        multiSelectToolbar.setup(
            lifecycleOwner = viewLifecycleOwner,
            multiSelectHelper = this,
            menuRes = null,
            activity = requireActivity(),
        )
        isMultiSelectingLive.observe(viewLifecycleOwner) {
            val episodeContainerFragment = parentFragmentManager.findFragmentByTag(EPISODE_CARD)
            if (episodeContainerFragment != null) return@observe
            multiSelectToolbar.isVisible = it
            binding?.showToolbar(!it)
            adapter?.notifyDataSetChanged()
        }
        coordinatorLayout = (activity as FragmentHostListener).snackBarView()
        context = requireActivity()
        source = SourceView.PODCAST_SCREEN
        listener = object : MultiSelectHelper.Listener<T> {
            override fun multiSelectSelectNone() {
                viewModel.multiSelectSelectNone()
                this@setUp.closeMultiSelect()
                adapter?.notifyDataSetChanged()
            }

            override fun multiSelectSelectAllUp(multiSelectable: T) {
                viewModel.multiSelectAllUp(multiSelectable)
                adapter?.notifyDataSetChanged()
            }

            override fun multiSelectSelectAllDown(multiSelectable: T) {
                viewModel.multiSelectSelectAllDown(multiSelectable)
                adapter?.notifyDataSetChanged()
            }

            override fun multiSelectSelectAll() {
                viewModel.multiSelectSelectAll()
                adapter?.notifyDataSetChanged()
            }

            override fun multiDeselectAllBelow(multiSelectable: T) {
                viewModel.multiDeselectAllBelow(multiSelectable)
                adapter?.notifyDataSetChanged()
            }

            override fun multiDeselectAllAbove(multiSelectable: T) {
                viewModel.multiDeselectAllAbove(multiSelectable)
                adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun notifyItemChanged(
        @Suppress("UNUSED_PARAMETER") episode: BaseEpisode,
        index: Int,
    ) {
        binding?.episodesRecyclerView?.let { recyclerView ->
            recyclerView.findViewHolderForAdapterPosition(index)?.let {
                itemTouchHelper?.clearView(recyclerView, it)
            }
        }

        adapter?.notifyItemChanged(index)
    }

    private fun loadData() {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Loading podcast page for $podcastUuid")
        viewModel.loadPodcast(podcastUuid, resources)

        viewModel.signInState.observe(viewLifecycleOwner) { signInState ->
            adapter?.setSignInState(signInState)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                episodeListBookmarkViewModel.stateFlow.collect {
                    adapter?.setBookmarksAvailable(it.isBookmarkFeatureAvailable)
                    adapter?.notifyDataSetChanged()
                }
            }
        }

        viewModel.podcast.observe(
            viewLifecycleOwner,
            Observer<Podcast> { podcast ->
                val backgroundColor = ThemeColor.podcastUi03(theme.activeTheme, podcast.backgroundColor)
                binding?.setToolbarStaticColor(backgroundColor)
                binding?.setToolbarTitle(podcast.title)

                adapter?.setPodcast(podcast)

                viewModel.archiveEpisodeLimit()
                updateStatusBar()
            },
        )

        viewModel.tintColor.observe(viewLifecycleOwner) { tintColor ->
            adapter?.setTint(tintColor)
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is PodcastViewModel.UiState.Loading -> {
                    binding?.loading?.visibility = View.VISIBLE
                    binding?.errorContainer?.visibility = View.GONE
                }

                is PodcastViewModel.UiState.Loaded -> {
                    binding?.loading?.visibility = View.GONE
                    binding?.errorContainer?.visibility = View.GONE
                    addPaddingForEpisodeSearch(state.episodes)
                    when (state.showTab) {
                        PodcastTab.EPISODES -> {
                            adapter?.setEpisodes(
                                episodes = state.episodes,
                                showingArchived = state.showingArchived,
                                episodeCount = state.episodeCount,
                                archivedCount = state.archivedCount,
                                searchTerm = state.searchTerm,
                                episodeLimit = state.episodeLimit,
                                episodeLimitIndex = state.episodeLimitIndex,
                                podcast = state.podcast,
                                context = requireContext(),
                            )
                        }

                        PodcastTab.BOOKMARKS -> {
                            adapter?.setBookmarks(
                                bookmarks = state.bookmarks,
                                episodes = state.episodes,
                                searchTerm = state.searchBookmarkTerm,
                                context = requireContext(),
                            )

                            adapter?.notifyDataSetChanged()
                        }
                    }
                    if (state.searchTerm.isNotEmpty() && state.searchTerm != lastSearchTerm) {
                        val toolbarHeight = when (binding?.headerType) {
                            null, HeaderType.SolidColor -> 0
                            HeaderType.Blur, HeaderType.Scrim -> binding?.toolbar?.height ?: 0
                        }
                        binding?.episodesRecyclerView?.smoothScrollToTop(1, offset = toolbarHeight)
                    }
                    lastSearchTerm = state.searchTerm
                }

                is PodcastViewModel.UiState.Error -> {
                    adapter?.setError()
                    binding?.loading?.visibility = View.GONE
                    binding?.errorContainer?.visibility = View.VISIBLE
                    binding?.errorMessage?.text = getString(LR.string.podcast_load_error)

                    if (BuildConfig.DEBUG) {
                        UiUtil.displayAlertError(requireContext(), state.errorMessage, null)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.refreshState.collect { state ->
                when (state) {
                    PodcastViewModel.RefreshState.NotStarted -> {}
                    PodcastViewModel.RefreshState.NewEpisodeFound -> {
                        binding?.swipeRefreshLayout?.isRefreshing = false
                        showSnackBar(getString(LR.string.podcast_refresh_new_episode_found))
                    }

                    PodcastViewModel.RefreshState.NoEpisodesFound -> {
                        binding?.swipeRefreshLayout?.isRefreshing = false
                        showSnackBar(getString(LR.string.podcast_refresh_no_episodes_found))
                    }

                    is PodcastViewModel.RefreshState.Refreshing -> {
                        if (state.type == PodcastViewModel.RefreshType.PULL_TO_REFRESH) {
                            binding?.swipeRefreshLayout?.isRefreshing = true
                        } else {
                            showSnackBar(getString(LR.string.podcast_refreshing_episode_list), Snackbar.LENGTH_INDEFINITE)
                        }
                    }
                }
            }
        }

        viewModel.castConnected.observe(viewLifecycleOwner) { castConnected ->
            adapter?.castConnected = castConnected
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect {
                    binding?.episodesRecyclerView?.updatePadding(bottom = it)
                }
            }
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
        itemTouchHelper = null

        viewModel.multiSelectEpisodesHelper.cleanup()
        viewModel.multiSelectBookmarksHelper.cleanup()

        super.onDestroyView()

        binding?.episodesRecyclerView?.removeOnScrollListener(onScrollListener)
        binding?.episodesRecyclerView?.adapter = null
        binding = null
        currentSnackBar?.dismiss()
        currentSnackBar = null
        (activity as? FragmentHostListener)?.setFullScreenDarkOverlayViewVisibility(false)
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

    private fun share() {
        val podcast = viewModel.podcast.value ?: return

        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SHARE_TAPPED)

        if (!podcast.canShare) {
            showSnackBar(getString(LR.string.sharing_is_not_available_for_private_podcasts))
            return
        }

        if (FeatureFlag.isEnabled(Feature.REIMAGINE_SHARING)) {
            SharePodcastFragment
                .newInstance(podcast, SourceView.PODCAST_SCREEN)
                .show(parentFragmentManager, "share_screen")
        } else {
            lifecycleScope.launch {
                val request = SharingRequest.podcast(podcast)
                    .setSourceView(SourceView.PODCAST_SCREEN)
                    .build()
                sharingClient.share(request)
            }
        }
    }

    private fun downloadAll() {
        val episodeCount = viewModel.episodeCount()
        val dialog = ConfirmationDialog.downloadWarningDialog(episodeCount, resources) {
            viewModel.downloadAll()
        }
        dialog?.show(parentFragmentManager, "download_confirm")
    }

    private fun showSnackBar(message: String, duration: Int = Snackbar.LENGTH_LONG) {
        (activity as? FragmentHostListener)?.snackBarView()?.let { snackBarView ->
            currentSnackBar = Snackbar.make(snackBarView, message, duration).apply {
                show()
            }
        }
    }

    override fun onBackPressed() = if (viewModel.multiSelectEpisodesHelper.isMultiSelecting) {
        viewModel.multiSelectEpisodesHelper.isMultiSelecting = false
        true
    } else if (viewModel.multiSelectBookmarksHelper.isMultiSelecting) {
        viewModel.multiSelectBookmarksHelper.isMultiSelecting = false
        true
    } else {
        super.onBackPressed()
    }

    override fun getBackstackCount() = super.getBackstackCount() +
        if (viewModel.multiSelectEpisodesHelper.isMultiSelecting || viewModel.multiSelectBookmarksHelper.isMultiSelecting) {
            1
        } else {
            0
        }

    private fun updateStausBarForBackground() {
        val headerType = binding?.headerType ?: return
        val toolbarColor = currentToolbarColor ?: return

        val currentStatusBarColor = statusBarIconColor
        statusBarIconColor = when (headerType) {
            HeaderType.SolidColor -> getStatusBarColorForSolidColor(toolbarColor)
            HeaderType.Blur -> getStatusBarColorForBlur(toolbarColor, artworkDominantColor)
            HeaderType.Scrim -> getStatusBarColorForTheme()
        }

        if (currentStatusBarColor != statusBarIconColor) {
            updateStatusBar()
        }
    }

    private fun getStatusBarColorForSolidColor(backgroundColor: Color) = if (backgroundColor.luminance() > 0.5f) {
        StatusBarIconColor.Dark
    } else {
        StatusBarIconColor.Light
    }

    private fun getStatusBarColorForTheme() = if (theme.isLightTheme) {
        StatusBarIconColor.Dark
    } else {
        StatusBarIconColor.Light
    }

    private fun getStatusBarColorForBlur(
        toolbarColor: Color,
        artworkColor: Color?,
    ) = when (toolbarColor.alpha) {
        in Float.NEGATIVE_INFINITY..0.5f -> when (val artworkLuminance = artworkColor?.luminance()) {
            null -> getStatusBarColorForTheme()
            in Float.NEGATIVE_INFINITY..0.6f -> StatusBarIconColor.Light
            else -> StatusBarIconColor.Dark
        }

        else -> getStatusBarColorForSolidColor(toolbarColor.copy(alpha = 1f))
    }

    @Parcelize
    data class PodcastFragmentArgs(
        val podcastUuid: String,
        val sourceView: SourceView,
        val fromListUuid: String?,
        val featuredPodcast: Boolean,
        val isHeaderRedesigned: Boolean,
    ) : Parcelable
}

private sealed interface BindingWrapper {
    val root: LinearLayout
    val multiSelectEpisodesToolbar: MultiSelectToolbar
    val multiSelectBookmarksToolbar: MultiSelectToolbar
    val toolbar: View
    val swipeRefreshLayout: SwipeRefreshLayout
    val episodesRecyclerView: RecyclerView
    val loading: ProgressBar
    val errorContainer: LinearLayout
    val errorMessage: TextView
    val btnRetry: MaterialButton
    val composeTooltipHost: ComposeView

    val headerType
        get() = when (this) {
            is RedesignBindingWrapper -> if (Build.VERSION.SDK_INT >= 31) {
                HeaderType.Blur
            } else {
                HeaderType.Scrim
            }

            is RegularBindingWrapper -> HeaderType.SolidColor
        }

    fun setUpToolbar(
        theme: Theme,
        @MenuRes menuId: Int,
        onChromeCast: () -> Unit,
        onShare: () -> Unit,
        onNavigateBack: () -> Unit,
        onLongClick: () -> Unit,
    )

    fun setToolbarTitle(title: String)

    fun setToolbarStaticColor(@ColorInt color: Int)

    fun setToolbarTransparency(@FloatRange(0.0, 1.0) progress: Float)

    fun showToolbar(show: Boolean)

    fun showBackgroundPlaceholder(show: Boolean)

    companion object {
        fun inflate(
            inflater: LayoutInflater,
            container: ViewGroup?,
            isHeaderRedesigned: Boolean,
            onToolbarColorChange: (Color) -> Unit,
        ): BindingWrapper = if (isHeaderRedesigned) {
            RedesignBindingWrapper(onToolbarColorChange, inflater, container)
        } else {
            RegularBindingWrapper(onToolbarColorChange, inflater, container)
        }
    }

    private class RegularBindingWrapper(
        private val onToolbarColorChange: (Color) -> Unit,
        inflater: LayoutInflater,
        container: ViewGroup?,
    ) : BindingWrapper {
        private val binding = FragmentPodcastBinding.inflate(inflater, container, false)

        override fun setUpToolbar(
            theme: Theme,
            @MenuRes menuId: Int,
            onChromeCast: () -> Unit,
            onShare: () -> Unit,
            onNavigateBack: () -> Unit,
            onLongClick: () -> Unit,
        ) {
            binding.toolbar.apply {
                inflateMenu(menuId)

                setNavigationOnClickListener { onNavigateBack() }
                menu.setupChromeCastButton(context, onChromeCast)
                setOnMenuItemClickListener(object : OnMenuItemClickListener {
                    override fun onMenuItemClick(item: MenuItem): Boolean {
                        when (item.itemId) {
                            R.id.share -> onShare()
                        }
                        return true
                    }
                })

                val iconsColor = context.getThemeColor(UR.attr.contrast_01)
                menu.tintIcons(iconsColor)
                navigationIcon?.setTint(iconsColor)
                navigationContentDescription = context.getString(LR.string.back)

                setOnLongClickListener {
                    onLongClick()
                    true
                }
                includeStatusBarPadding()
            }
        }

        override fun setToolbarTitle(title: String) = Unit

        override fun setToolbarStaticColor(color: Int) {
            binding.toolbar.setBackgroundColor(color)
            binding.headerBackgroundPlaceholder.setBackgroundColor(color)
            onToolbarColorChange(Color(color))
        }

        override fun setToolbarTransparency(progress: Float) = Unit

        override fun showToolbar(show: Boolean) {
            binding.toolbar.isVisible = show
        }

        override fun showBackgroundPlaceholder(show: Boolean) {
            binding.headerBackgroundPlaceholder.isVisible = false
        }

        override val root: LinearLayout
            get() = binding.root

        override val toolbar: View
            get() = binding.toolbar

        override val multiSelectEpisodesToolbar: MultiSelectToolbar
            get() = binding.multiSelectEpisodesToolbar

        override val multiSelectBookmarksToolbar: MultiSelectToolbar
            get() = binding.multiSelectBookmarksToolbar

        override val swipeRefreshLayout: SwipeRefreshLayout
            get() = binding.swipeRefreshLayout

        override val episodesRecyclerView: RecyclerView
            get() = binding.episodesRecyclerView

        override val loading: ProgressBar
            get() = binding.loading

        override val errorContainer: LinearLayout
            get() = binding.errorContainer

        override val errorMessage: TextView
            get() = binding.errorMessage

        override val btnRetry: MaterialButton
            get() = binding.btnRetry

        override val composeTooltipHost: ComposeView
            get() = binding.composeTooltipHost
    }

    private class RedesignBindingWrapper(
        private val onToolbarColorChange: (Color) -> Unit,
        inflater: LayoutInflater,
        container: ViewGroup?,
    ) : BindingWrapper {
        private val binding = FragmentPodcastRedesignBinding.inflate(inflater, container, false)

        init {
            binding.toolbar.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

        @OptIn(ExperimentalFoundationApi::class)
        override fun setUpToolbar(
            theme: Theme,
            @MenuRes menuId: Int,
            onChromeCast: () -> Unit,
            onShare: () -> Unit,
            onNavigateBack: () -> Unit,
            onLongClick: () -> Unit,
        ) {
            binding.toolbar.setContent {
                AppTheme(theme.activeTheme) {
                    PodcastToolbar(
                        title = toolbarText,
                        transparencyProgress = toolbarTransparencyProgress,
                        onGoBack = onNavigateBack,
                        onChromeCast = onChromeCast,
                        onShare = onShare,
                        onBackgroundColorChange = onToolbarColorChange,
                        modifier = Modifier
                            .combinedClickable(
                                interactionSource = null,
                                indication = null,
                                onClick = {},
                                onLongClick = onLongClick,
                            ),
                    )
                }
            }
        }

        private var toolbarText by mutableStateOf("")

        override fun setToolbarTitle(title: String) {
            toolbarText = title
        }

        override fun setToolbarStaticColor(color: Int) = Unit

        private var toolbarTransparencyProgress by mutableFloatStateOf(1f)

        override fun setToolbarTransparency(progress: Float) {
            toolbarTransparencyProgress = progress
        }

        override fun showToolbar(show: Boolean) {
            binding.toolbar.isInvisible = !show
        }

        override fun showBackgroundPlaceholder(show: Boolean) = Unit

        override val root: LinearLayout
            get() = binding.root

        override val toolbar: View
            get() = binding.toolbar

        override val multiSelectEpisodesToolbar: MultiSelectToolbar
            get() = binding.multiSelectEpisodesToolbar

        override val multiSelectBookmarksToolbar: MultiSelectToolbar
            get() = binding.multiSelectBookmarksToolbar

        override val swipeRefreshLayout: SwipeRefreshLayout
            get() = binding.swipeRefreshLayout

        override val episodesRecyclerView: RecyclerView
            get() = binding.episodesRecyclerView

        override val loading: ProgressBar
            get() = binding.loading

        override val errorContainer: LinearLayout
            get() = binding.errorContainer

        override val errorMessage: TextView
            get() = binding.errorMessage

        override val btnRetry: MaterialButton
            get() = binding.btnRetry

        override val composeTooltipHost: ComposeView
            get() = binding.composeTooltipHost
    }
}
