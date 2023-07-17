package au.com.shiftyjelly.pocketcasts.podcasts.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeViewSource
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentProfileEpisodeListBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.EpisodeListAdapter
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.playback.AutomaticUpNextSource
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.settings.AutoDownloadSettingsFragment
import au.com.shiftyjelly.pocketcasts.settings.ManualCleanupFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.images.PodcastImageLoaderThemed
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.setup
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.NavigationIcon.BackArrow
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectEpisodesHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

private const val ARG_MODE = "profile_list_mode"

@AndroidEntryPoint
class ProfileEpisodeListFragment : BaseFragment(), Toolbar.OnMenuItemClickListener {
    sealed class Mode(val index: Int, val showMenu: Boolean) {
        object Downloaded : Mode(0, true)
        object Starred : Mode(1, false)
        object History : Mode(2, true)
    }

    companion object {
        private const val SELECT_ALL_KEY = "select_all"
        private const val OPTION_KEY = "option"
        private const val AUTO_DOWNLOAD_SETTINGS = "auto_download_settings"
        private const val STOP_ALL_DOWNLOADS = "stop_all_downloads"
        private const val CLEAN_UP = "clean_up"
        private const val CLEAR_HISTORY = "clear_history"

        fun newInstance(mode: Mode): ProfileEpisodeListFragment {
            val bundle = Bundle().apply {
                putInt(ARG_MODE, mode.index)
            }
            val fragment = ProfileEpisodeListFragment()
            fragment.arguments = bundle
            return fragment
        }
    }

    @Inject lateinit var downloadManager: DownloadManager
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var playButtonListener: PlayButton.OnClickListener
    @Inject lateinit var settings: Settings
    @Inject lateinit var upNextQueue: UpNextQueue
    @Inject lateinit var multiSelectHelper: MultiSelectEpisodesHelper
    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private val viewModel: ProfileEpisodeListViewModel by viewModels()
    private lateinit var imageLoader: PodcastImageLoader
    private var binding: FragmentProfileEpisodeListBinding? = null

    val mode: Mode
        get() = when (arguments?.getInt(ARG_MODE)) {
            Mode.Downloaded.index -> Mode.Downloaded
            Mode.Starred.index -> Mode.Starred
            Mode.History.index -> Mode.History
            else -> throw IllegalStateException("Unknown mode")
        }

    val onRowClick = { episode: BaseEpisode ->
        if (episode is PodcastEpisode) {
            val episodeViewSource = when (mode) {
                Mode.Downloaded -> EpisodeViewSource.DOWNLOADS
                Mode.History -> EpisodeViewSource.LISTENING_HISTORY
                Mode.Starred -> EpisodeViewSource.STARRED
            }
            val fragment = EpisodeFragment.newInstance(episode = episode, source = episodeViewSource)
            fragment.show(parentFragmentManager, "episode_card")
        }
    }

    val adapter by lazy { EpisodeListAdapter(downloadManager, playbackManager, upNextQueue, settings, onRowClick, playButtonListener, imageLoader, multiSelectHelper, childFragmentManager) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentProfileEpisodeListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        imageLoader = PodcastImageLoaderThemed(context).apply {
            radiusPx = 4.dpToPx(context)
        }.smallPlaceholder()

        playButtonListener.source = getAnalyticsEventSource()
        multiSelectHelper.source = getAnalyticsEventSource()
    }

    override fun onPause() {
        super.onPause()
        binding?.recyclerView?.adapter = null
        if (multiSelectHelper.isMultiSelecting) {
            multiSelectHelper.isMultiSelecting = false
        }
    }

    override fun onResume() {
        super.onResume()
        binding?.recyclerView?.adapter = adapter
        when (mode) {
            Mode.Downloaded -> AutomaticUpNextSource.Companion.Predefined.downloads
            Mode.History -> null
            Mode.Starred -> AutomaticUpNextSource.Companion.Predefined.starred
        }.let { AutomaticUpNextSource.mostRecentList = it }
    }

    override fun onDestroyView() {
        binding?.recyclerView?.adapter = null
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.setup(mode)

        updateToolbar()

        binding?.recyclerView?.let {
            it.layoutManager = LinearLayoutManager(it.context, RecyclerView.VERTICAL, false)
            it.adapter = adapter
            (it.itemAnimator as SimpleItemAnimator).changeDuration = 0
            val itemTouchHelper = EpisodeItemTouchHelper(this::episodeSwipedRightItem1, this::episodeSwipedRightItem2, viewModel::episodeSwiped)
            itemTouchHelper.attachToRecyclerView(it)
        }

        viewModel.episodeList.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            binding?.emptyLayout?.isVisible = it.isEmpty()
        }

        val emptyTitleId = when (mode) {
            is Mode.Downloaded -> LR.string.profile_empty_downloaded
            is Mode.Starred -> LR.string.profile_empty_starred
            is Mode.History -> LR.string.profile_empty_history
        }
        val emptySummaryId = when (mode) {
            is Mode.Downloaded -> LR.string.profile_empty_downloaded_summary
            is Mode.Starred -> LR.string.profile_empty_starred_summary
            is Mode.History -> LR.string.profile_empty_history_summary
        }

        binding?.lblEmptyTitle?.setText(emptyTitleId)
        binding?.lblEmptySummary?.setText(emptySummaryId)

        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) { isMultiSelecting ->
            val wasMultiSelecting = binding?.multiSelectToolbar?.isVisible == true
            binding?.multiSelectToolbar?.isVisible = isMultiSelecting
            binding?.toolbar?.isVisible = !isMultiSelecting
            binding?.multiSelectToolbar?.setNavigationIcon(R.drawable.ic_arrow_back)

            if ((activity as? FragmentHostListener)?.isUpNextShowing() == false) {
                if (isMultiSelecting) {
                    trackMultiSelectEntered()
                } else if (wasMultiSelecting) {
                    trackMultiSelectExited()
                }
            }

            adapter.notifyDataSetChanged()
        }
        multiSelectHelper.listener = object : MultiSelectHelper.Listener<BaseEpisode> {
            override fun multiSelectSelectAll() {
                val episodes = viewModel.episodeList.value
                if (episodes != null) {
                    multiSelectHelper.selectAllInList(episodes)
                    adapter.notifyDataSetChanged()
                    trackSelectAll(true)
                }
            }

            override fun multiSelectSelectNone() {
                val episodes = viewModel.episodeList.value
                if (episodes != null) {
                    episodes.forEach { multiSelectHelper.deselect(it) }
                    adapter.notifyDataSetChanged()
                    trackSelectAll(false)
                }
            }

            override fun multiSelectSelectAllUp(multiSelectable: BaseEpisode) {
                val episodes = viewModel.episodeList.value
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(episodes.subList(0, startIndex + 1))
                        trackSelectAllAbove()
                    }

                    adapter.notifyDataSetChanged()
                }
            }

            override fun multiSelectSelectAllDown(multiSelectable: BaseEpisode) {
                val episodes = viewModel.episodeList.value
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(episodes.subList(startIndex, episodes.size))
                        trackSelectAllBelow()
                    }

                    adapter.notifyDataSetChanged()
                }
            }

            override fun multiDeselectAll() {
                val episodes = viewModel.episodeList.value
                if (episodes != null) {
                    episodes.forEach { multiSelectHelper.deselect(it) }
                    adapter.notifyDataSetChanged()
                }
            }

            override fun multiDeselectAllBelow(multiSelectable: BaseEpisode) {
                val episodes = viewModel.episodeList.value
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        episodes.subList(startIndex, episodes.size).forEach { multiSelectHelper.deselect(it) }
                        adapter.notifyDataSetChanged()
                    }
                }
            }

            override fun multiDeselectAllAbove(multiSelectable: BaseEpisode) {
                val episodes = viewModel.episodeList.value
                if (episodes != null) {
                    val startIndex = episodes.indexOf(multiSelectable)
                    if (startIndex > -1) {
                        episodes.subList(0, startIndex + 1).forEach { multiSelectHelper.deselect(it) }
                        adapter.notifyDataSetChanged()
                    }
                }
            }
        }
        multiSelectHelper.coordinatorLayout = (activity as FragmentHostListener).snackBarView()
        binding?.multiSelectToolbar?.setup(viewLifecycleOwner, multiSelectHelper, menuRes = null, fragmentManager = parentFragmentManager)
    }

    private fun updateToolbar() {
        val toolbar = binding?.toolbar ?: return
        val title = when (mode) {
            is Mode.Downloaded -> LR.string.profile_navigation_downloads
            is Mode.Starred -> LR.string.profile_navigation_starred
            is Mode.History -> LR.string.profile_navigation_listening_history
        }
        toolbar.setup(
            title = getString(title),
            navigationIcon = BackArrow,
            activity = activity,
            theme = theme,
            menu = if (mode.showMenu) R.menu.menu_profile_list else null
        )
        toolbar.setOnMenuItemClickListener(this)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        return if (item.itemId == R.id.more_options) {
            val dialog = OptionsDialog()
            if (mode is Mode.Downloaded) {
                analyticsTracker.track(AnalyticsEvent.DOWNLOADS_OPTIONS_BUTTON_TAPPED)
                dialog.addTextOption(LR.string.profile_auto_download_settings, imageId = R.drawable.ic_settings_small, click = this::showAutodownloadSettings)
                if (downloadManager.hasPendingOrRunningDownloads()) {
                    dialog.addTextOption(LR.string.settings_auto_download_stop_all, imageId = IR.drawable.ic_stop, click = this::stopAllDownloads)
                }
                dialog.addTextOption(LR.string.profile_clean_up, imageId = VR.drawable.ic_delete, click = this::showCleanupSettings)
            } else if (mode is Mode.History) {
                analyticsTracker.track(AnalyticsEvent.LISTENING_HISTORY_OPTIONS_BUTTON_TAPPED)
                dialog.addTextOption(LR.string.profile_clear_listening_history, imageId = R.drawable.ic_history, click = this::clearListeningHistory)
            }
            dialog.show(parentFragmentManager, "more_options")
            true
        } else {
            false
        }
    }

    private fun showAutodownloadSettings() {
        val fragment = AutoDownloadSettingsFragment.newInstance(showToolbar = true)
        showFragment(fragment)
        analyticsTracker.track(AnalyticsEvent.DOWNLOADS_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to AUTO_DOWNLOAD_SETTINGS))
        (activity as AppCompatActivity).supportActionBar?.setTitle(LR.string.profile_auto_download_settings)
    }

    private fun stopAllDownloads() {
        analyticsTracker.track(AnalyticsEvent.DOWNLOADS_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to STOP_ALL_DOWNLOADS))
        downloadManager.stopAllDownloads()
    }

    private fun showCleanupSettings() {
        analyticsTracker.track(AnalyticsEvent.DOWNLOADS_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to CLEAN_UP))
        val fragment = ManualCleanupFragment.newInstance()
        showFragment(fragment)
    }

    private fun clearListeningHistory() {
        analyticsTracker.track(AnalyticsEvent.LISTENING_HISTORY_OPTIONS_MODAL_OPTION_TAPPED, mapOf(OPTION_KEY to CLEAR_HISTORY))
        val dialog = ConfirmationDialog()
            .setIconId(R.drawable.ic_history)
            .setTitle(resources.getString(LR.string.profile_clear_listening_history_title))
            .setSummary(resources.getString(LR.string.profile_clear_cannot_be_undone))
            .setButtonType(ConfirmationDialog.ButtonType.Danger(resources.getString(LR.string.profile_clear_all)))
            .setOnConfirm { viewModel.clearAllEpisodeHistory() }
        dialog.show(parentFragmentManager, "clear_history")
    }

    private fun showFragment(fragment: Fragment) {
        (activity as? FragmentHostListener)?.addFragment(fragment)
    }

    private fun episodeSwipedRightItem1(episode: BaseEpisode, index: Int) {
        when (settings.getUpNextSwipeAction()) {
            Settings.UpNextAction.PLAY_NEXT -> viewModel.episodeSwipeUpNext(episode)
            Settings.UpNextAction.PLAY_LAST -> viewModel.episodeSwipeUpLast(episode)
        }
        adapter.notifyItemChanged(index)
    }

    private fun episodeSwipedRightItem2(episode: BaseEpisode, index: Int) {
        when (settings.getUpNextSwipeAction()) {
            Settings.UpNextAction.PLAY_NEXT -> viewModel.episodeSwipeUpLast(episode)
            Settings.UpNextAction.PLAY_LAST -> viewModel.episodeSwipeUpNext(episode)
        }
        adapter.notifyItemChanged(index)
    }

    override fun onBackPressed(): Boolean {
        return if (multiSelectHelper.isMultiSelecting) {
            multiSelectHelper.isMultiSelecting = false
            true
        } else {
            val handled = super.onBackPressed()
            if (handled) {
                updateToolbar()
            }
            return handled
        }
    }

    override fun getBackstackCount(): Int {
        return super.getBackstackCount() + if (multiSelectHelper.isMultiSelecting) 1 else 0
    }

    private fun trackSelectAll(selectAll: Boolean) {
        val analyticsEvent = when (mode) {
            Mode.Downloaded -> AnalyticsEvent.DOWNLOADS_SELECT_ALL_TAPPED
            Mode.History -> AnalyticsEvent.LISTENING_HISTORY_SELECT_ALL_TAPPED
            Mode.Starred -> AnalyticsEvent.STARRED_SELECT_ALL_TAPPED
        }
        analyticsTracker.track(analyticsEvent, mapOf(SELECT_ALL_KEY to selectAll))
    }

    private fun trackSelectAllAbove() {
        val analyticsEvent = when (mode) {
            Mode.Downloaded -> AnalyticsEvent.DOWNLOADS_SELECT_ALL_ABOVE_TAPPED
            Mode.History -> AnalyticsEvent.LISTENING_HISTORY_SELECT_ALL_ABOVE_TAPPED
            Mode.Starred -> AnalyticsEvent.STARRED_SELECT_ALL_ABOVE_TAPPED
        }
        analyticsTracker.track(analyticsEvent)
    }

    private fun trackSelectAllBelow() {
        val analyticsEvent = when (mode) {
            Mode.Downloaded -> AnalyticsEvent.DOWNLOADS_SELECT_ALL_BELOW_TAPPED
            Mode.History -> AnalyticsEvent.LISTENING_HISTORY_SELECT_ALL_BELOW_TAPPED
            Mode.Starred -> AnalyticsEvent.STARRED_SELECT_ALL_BELOW_TAPPED
        }
        analyticsTracker.track(analyticsEvent)
    }

    private fun trackMultiSelectEntered() {
        val analyticsEvent = when (mode) {
            Mode.Downloaded -> AnalyticsEvent.DOWNLOADS_MULTI_SELECT_ENTERED
            Mode.History -> AnalyticsEvent.LISTENING_HISTORY_MULTI_SELECT_ENTERED
            Mode.Starred -> AnalyticsEvent.STARRED_MULTI_SELECT_ENTERED
        }
        analyticsTracker.track(analyticsEvent)
    }

    private fun trackMultiSelectExited() {
        val analyticsEvent = when (mode) {
            Mode.Downloaded -> AnalyticsEvent.DOWNLOADS_MULTI_SELECT_EXITED
            Mode.History -> AnalyticsEvent.LISTENING_HISTORY_MULTI_SELECT_EXITED
            Mode.Starred -> AnalyticsEvent.STARRED_MULTI_SELECT_EXITED
        }
        analyticsTracker.track(analyticsEvent)
    }

    private fun getAnalyticsEventSource() = when (mode) {
        Mode.Downloaded -> SourceView.DOWNLOADS
        Mode.Starred -> SourceView.STARRED
        Mode.History -> SourceView.LISTENING_HISTORY
    }
}
