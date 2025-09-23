package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.util.lerp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Observer
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter.StateRestorationPolicy
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.analytics.discoverListPodcastSubscribed
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarkActivity
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.BookmarksSortByDialog
import au.com.shiftyjelly.pocketcasts.podcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentPodcastBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeContainerFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.FolderChooserFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastAdapter.HeaderType
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcasts.PodcastsFragment
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastRatingsViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.viewmodel.PodcastViewModel.PodcastTab
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoPlaySource
import au.com.shiftyjelly.pocketcasts.reimagine.podcast.SharePodcastFragment
import au.com.shiftyjelly.pocketcasts.reimagine.timestamp.ShareEpisodeTimestampFragment
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageColorAnalyzer
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.settings.HeadphoneControlsSettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.SettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.extensions.openUrl
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarIconColor
import au.com.shiftyjelly.pocketcasts.ui.images.CoilManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.smoothScrollToTop
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper.NavigationState
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper.Companion.MULTI_SELECT_TOGGLE_PAYLOAD
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectToolbar
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeActionViewModel
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeSource
import au.com.shiftyjelly.pocketcasts.views.swipe.handleAction
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import timber.log.Timber
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
            fromListDate: String? = null,
            featuredPodcast: Boolean = false,
        ): PodcastFragment = PodcastFragment().apply {
            arguments = bundleOf(
                NEW_INSTANCE_ARGS to PodcastFragmentArgs(
                    podcastUuid = podcastUuid,
                    sourceView = sourceView,
                    fromListUuid = fromListUuid,
                    fromListDate = fromListDate,
                    featuredPodcast = featuredPodcast,
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
    lateinit var playButtonListener: PlayButton.OnClickListener

    @Inject
    lateinit var coilManager: CoilManager

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var colorAnalyzer: PodcastImageColorAnalyzer

    @Inject
    lateinit var categoriesManager: CategoriesManager

    @Inject
    lateinit var swipeRowActionsFactory: SwipeRowActions.Factory

    @Inject
    lateinit var rowDataProvider: EpisodeRowDataProvider

    private val viewModel: PodcastViewModel by viewModels()
    private val ratingsViewModel: PodcastRatingsViewModel by viewModels()
    private val swipeActionViewModel by viewModels<SwipeActionViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SwipeActionViewModel.Factory> { factory ->
                factory.create(SwipeSource.PodcastDetails, playlistUuid = null)
            }
        },
    )

    private var binding: FragmentPodcastBinding? = null
    private var toolbarController = ToolbarController()
    private val headerType = if (Build.VERSION.SDK_INT >= 31) {
        HeaderType.Blur
    } else {
        HeaderType.Scrim
    }

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
            toolbarController.setToolbarTransparency(computeTransparencyProgress(headerViewOffset))
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
        analyticsTracker.discoverListPodcastSubscribed(podcastUuid = podcastUuid, listId = fromListUuid, listDate = fromListDate)
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
            .addTextOption(
                titleId = LR.string.podcast_refresh_episodes,
                imageId = IR.drawable.ic_refresh,
                click = {
                    viewLifecycleOwner.lifecycleScope.launch {
                        viewModel.onRefreshPodcast(PodcastViewModel.RefreshType.REFRESH_BUTTON)
                    }
                },
            )
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
                OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.Upsell(OnboardingUpgradeSource.FOLDERS_PODCAST_SCREEN))
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
    }

    private val onDonateClicked: (Uri?) -> Unit = { uri ->
        viewModel.onDonateClicked()
        uri?.let {
            openUrl(it.toString())
        } ?: run {
            Timber.e("Donate URI is null")
        }
    }

    private val onSettingsClicked: () -> Unit = callback@{
        val podcast = viewModel.podcast.value ?: return@callback
        analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SETTINGS_TAPPED)
        val fragment = PodcastSettingsFragment.newInstance(
            uuid = podcast.uuid,
            title = podcast.title,
            darkTint = podcast.darkThemeTint(),
            lightTint = podcast.lightThemeTint(),
        )
        (activity as FragmentHostListener).addFragment(fragment)
        viewModel.multiSelectEpisodesHelper.isMultiSelecting = false
        viewModel.multiSelectBookmarksHelper.isMultiSelecting = false
    }

    private val onSearchFocus: () -> Unit = {
        binding?.episodesRecyclerView?.smoothScrollToTop(1, offset = binding?.toolbar?.height ?: 0)
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
        viewModel.onHeadsetSettingsClicked()
        fragmentHostListener?.apply {
            openTab(VR.id.navigation_profile)
            addFragment(SettingsFragment())
            addFragment(HeadphoneControlsSettingsFragment())
        }
    }

    private fun onGetBookmarksClicked() {
        viewModel.onGetBookmarksClicked()
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

    private val fromListDate: String
        get() = args.fromListDate.orEmpty()

    private var lastSearchTerm: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        adapter?.fromListUuid = fromListUuid
        if (savedInstanceState == null) {
            analyticsTracker.track(AnalyticsEvent.PODCAST_SCREEN_SHOWN, mapOf(SOURCE_KEY to sourceView.analyticsValue))
        }
    }

    var artworkDominantColor: Color? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentPodcastBinding.inflate(inflater, container, false).also { binding = it }

        toolbarController.setUpToolbar(
            view = binding.toolbar,
            theme = theme,
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
            onColorChange = {
                updateStausBarForBackground()
            },
        )
        adapter = PodcastAdapter(
            context = requireContext(),
            headerType = headerType,
            rowDataProvider = rowDataProvider,
            settings = settings,
            swipeRowActionsFactory = swipeRowActionsFactory,
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
            onDonateClicked = onDonateClicked,
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
            onHeadsetSettingsClicked = ::onHeadsetSettingsClicked,
            onGetBookmarksClicked = ::onGetBookmarksClicked,
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
            onClickWebsite = { podcast ->
                podcast.podcastUrl?.let { url ->
                    if (url.isNotBlank()) {
                        analyticsTracker.track(
                            AnalyticsEvent.PODCAST_SCREEN_PODCAST_DETAILS_LINK_TAPPED,
                            mapOf("podcast_uuid" to podcast.uuid),
                        )
                        try {
                            var uri = Uri.parse(url)
                            if (uri.scheme.isNullOrBlank() && !url.contains("://")) {
                                uri = Uri.parse("http://$url")
                            }
                            startActivity(Intent(Intent.ACTION_VIEW, uri), null)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to open podcast web page.")
                        }
                    }
                }
            },
            onArtworkAvailable = { podcast ->
                viewLifecycleOwner.lifecycleScope.launch {
                    artworkDominantColor = colorAnalyzer.getArtworkDominantColor(podcast.uuid)
                    updateStausBarForBackground()
                }
            },
            onRecommendedPodcastClicked = { podcastUuid, listDate ->
                viewModel.onRecommendedPodcastClicked(podcastUuid = podcastUuid, listDate = listDate)
                val fragment = newInstance(podcastUuid = podcastUuid, fromListUuid = "recommendations_podcast", sourceView = sourceView)
                (activity as FragmentHostListener).addFragment(fragment)
            },
            onRecommendedPodcastSubscribeClicked = { podcastUuid, listDate ->
                viewModel.onRecommendedPodcastSubscribeClicked(podcastUuid = podcastUuid, listDate = listDate)
            },
            onPodrollHeaderClicked = {
                showPodrollInformationModal()
            },
            onPodrollPodcastClicked = { podcastUuid ->
                viewModel.onPodrollPodcastClicked(podcastUuid = podcastUuid)
                val fragment = newInstance(podcastUuid = podcastUuid, fromListUuid = "podroll", sourceView = sourceView)
                (activity as FragmentHostListener).addFragment(fragment)
            },
            onPodrollPodcastSubscribeClicked = { podcastUuid ->
                viewModel.onPodrollPodcastSubscribeClicked(podcastUuid = podcastUuid)
            },
            onRecommendedRetryClicked = {
                viewModel.onRecommendedRetryClicked()
            },
            onSwipeAction = { episode, swipeAction ->
                viewLifecycleOwner.lifecycleScope.launch {
                    swipeActionViewModel.handleAction(swipeAction, episode.uuid, childFragmentManager)
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

        return binding.root
    }

    private fun showPodrollInformationModal() {
        viewModel.onPodrollInformationModalShown()
        val dialog = ConfirmationDialog()
            .setIconId(R.drawable.ic_author)
            .setTitle(getString(LR.string.podroll_information_title))
            .setSummary(getString(LR.string.podroll_information_summary))
            .setButtonType(ConfirmationDialog.ButtonType.Normal(getString(LR.string.podroll_information_button)))
        dialog.show(parentFragmentManager, "podroll_information")
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showNotificationSnack.flowWithLifecycle(lifecycle)
                .collect { message ->
                    currentSnackBar?.dismiss()
                    when (message) {
                        is PodcastViewModel.SnackBarMessage.ShowNotificationsDisabledMessage -> showSnackBar(
                            message = message.message.asString(requireContext()),
                            cta = message.cta.asString(requireContext()),
                            onCtaClick = {
                                viewModel.onOpenNotificationSettingsClicked(requireActivity())
                            },
                        )

                        is PodcastViewModel.SnackBarMessage.ShowNotifyOnNewEpisodesMessage -> showSnackBar(
                            message = message.message.asString(requireContext()),
                            duration = 3000,
                        )
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

    private fun onShareBookmarkClick() {
        lifecycleScope.launch {
            val (podcast, episode, bookmark) = viewModel.getSharedBookmark() ?: return@launch
            viewModel.onBookmarkShare(podcast.uuid, episode.uuid, sourceView)
            val timestamp = bookmark.timeSecs.seconds
            ShareEpisodeTimestampFragment
                .forBookmark(episode, timestamp, podcast.backgroundColor, SourceView.PODCAST_SCREEN)
                .show(parentFragmentManager, "share_screen")
        }
    }

    private suspend fun onEditBookmarkClick() {
        val bookmarkArguments = viewModel.createBookmarkArguments()
        if (bookmarkArguments != null) {
            startActivity(BookmarkActivity.launchIntent(requireContext(), bookmarkArguments))
        }
    }

    private fun FragmentPodcastBinding.setupMultiSelect() {
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

            val wasMultiSelecting = multiSelectToolbar.isVisible
            if (wasMultiSelecting == isMultiSelecting) {
                return@observe
            }
            multiSelectToolbar.isVisible = it
            binding?.toolbar?.isInvisible = it
            adapter?.notifyItemRangeChanged(0, adapter?.itemCount ?: 0, MULTI_SELECT_TOGGLE_PAYLOAD)
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

    private fun loadData() {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Loading podcast page for $podcastUuid")
        viewModel.loadPodcast(podcastUuid, resources)

        viewModel.signInState.observe(viewLifecycleOwner) { signInState ->
            adapter?.setSignInState(signInState)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.cachedSubscription.flow.collect { subscription ->
                    adapter?.setBookmarksAvailable(subscription != null)
                }
            }
        }

        viewModel.podcast.observe(
            viewLifecycleOwner,
            Observer<Podcast> { podcast ->
                toolbarController.setToolbarTitle(podcast.title)

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

                        PodcastTab.RECOMMENDATIONS -> {
                            adapter?.setRecommendations(state.recommendations)
                        }
                    }
                    if (state.searchTerm.isNotEmpty() && state.searchTerm != lastSearchTerm) {
                        binding?.episodesRecyclerView?.smoothScrollToTop(1, offset = binding?.toolbar?.height ?: 0)
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
                            showSnackBar(message = getString(LR.string.podcast_refreshing_episode_list), duration = Snackbar.LENGTH_INDEFINITE)
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
        val binding = binding ?: return
        val rowCount = episodes.size

        // No padding for empty state to avoid excessive whitespace
        if (rowCount == 0) {
            binding.episodesRecyclerView.updatePadding(bottom = 0)
            return
        }

        val pageHeight = binding.episodesRecyclerView.height
        val context = binding.episodesRecyclerView.context
        val episodeHeaderHeightPx = 90.dpToPx(context)
        val rowHeightPx = 80.dpToPx(context)
        val actualHeight = episodeHeaderHeightPx + (rowCount * rowHeightPx)
        val missingHeightPx = pageHeight - actualHeight

        // Only add padding if needed to prevent screen jump
        if (binding.episodesRecyclerView.paddingBottom <= missingHeightPx && missingHeightPx > 0) {
            binding.episodesRecyclerView.updatePadding(bottom = missingHeightPx)
        }
    }

    override fun onDestroyView() {
        binding?.episodesRecyclerView?.adapter = null

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
            showSnackBar(message = getString(LR.string.sharing_is_not_available_for_private_podcasts))
            return
        }

        SharePodcastFragment
            .newInstance(podcast, SourceView.PODCAST_SCREEN)
            .show(parentFragmentManager, "share_screen")
    }

    private fun downloadAll() {
        val episodeCount = viewModel.episodeCount()
        val dialog = ConfirmationDialog.downloadWarningDialog(episodeCount, resources) {
            viewModel.downloadAll()
        }
        dialog?.show(parentFragmentManager, "download_confirm")
    }

    private fun showSnackBar(
        message: String,
        cta: String? = null,
        onCtaClick: (() -> Unit)? = null,
        duration: Int = Snackbar.LENGTH_LONG,
    ) {
        (activity as? FragmentHostListener)?.snackBarView()?.let { snackBarView ->
            currentSnackBar = Snackbar.make(snackBarView, message, duration).apply {
                if (onCtaClick != null && cta != null) {
                    setAction(cta) {
                        onCtaClick()
                    }
                }
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
        val toolbarColor = toolbarController.color ?: return

        val currentStatusBarColor = statusBarIconColor
        statusBarIconColor = when (headerType) {
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
        val fromListDate: String?,
        val featuredPodcast: Boolean,
    ) : Parcelable
}

private class ToolbarController {
    private var text by mutableStateOf("")

    fun setToolbarTitle(title: String) {
        text = title
    }

    var color: Color? = null
        private set

    private var toolbarTransparencyProgress by mutableFloatStateOf(1f)

    fun setToolbarTransparency(progress: Float) {
        toolbarTransparencyProgress = progress
    }

    @OptIn(ExperimentalFoundationApi::class)
    fun setUpToolbar(
        view: ComposeView,
        theme: Theme,
        onChromeCast: () -> Unit,
        onShare: () -> Unit,
        onNavigateBack: () -> Unit,
        onLongClick: () -> Unit,
        onColorChange: (Color) -> Unit,
    ) {
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            AppTheme(theme.activeTheme) {
                PodcastToolbar(
                    title = text,
                    transparencyProgress = toolbarTransparencyProgress,
                    onGoBack = onNavigateBack,
                    onChromeCast = onChromeCast,
                    onShare = onShare,
                    onBackgroundColorChange = {
                        color = it
                        onColorChange(it)
                    },
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
}
