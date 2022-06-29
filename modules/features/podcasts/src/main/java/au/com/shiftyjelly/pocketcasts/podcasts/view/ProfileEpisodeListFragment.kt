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
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.podcasts.databinding.FragmentProfileEpisodeListBinding
import au.com.shiftyjelly.pocketcasts.podcasts.view.components.PlayButton
import au.com.shiftyjelly.pocketcasts.podcasts.view.episode.EpisodeFragment
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.EpisodeListAdapter
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
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
    @Inject lateinit var multiSelectHelper: MultiSelectHelper

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

    val onRowClick = { episode: Playable ->
        if (episode is Episode) {
            val fragment = EpisodeFragment.newInstance(episode)
            fragment.show(parentFragmentManager, "episode_card")
        }
    }

    val adapter by lazy { EpisodeListAdapter(downloadManager, playbackManager, upNextQueue, settings, onRowClick, playButtonListener, imageLoader, multiSelectHelper, childFragmentManager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(mode.showMenu)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentProfileEpisodeListBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        imageLoader = PodcastImageLoaderThemed(context).apply {
            radiusPx = 4.dpToPx(context)
        }.smallPlaceholder()
    }

    override fun onPause() {
        super.onPause()
        binding?.recyclerView?.adapter = null
        multiSelectHelper.isMultiSelecting = false
    }

    override fun onResume() {
        super.onResume()
        binding?.recyclerView?.adapter = adapter
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

        multiSelectHelper.isMultiSelectingLive.observe(viewLifecycleOwner) {
            binding?.multiSelectToolbar?.isVisible = it
            binding?.toolbar?.isVisible = !it
            binding?.multiSelectToolbar?.setNavigationIcon(R.drawable.ic_arrow_back)

            adapter.notifyDataSetChanged()
        }
        multiSelectHelper.listener = object : MultiSelectHelper.Listener {
            override fun multiSelectSelectAll() {
                val episodes = viewModel.episodeList.value
                if (episodes != null) {
                    multiSelectHelper.selectAllInList(episodes)
                    adapter.notifyDataSetChanged()
                }
            }

            override fun multiSelectSelectNone() {
                val episodes = viewModel.episodeList.value
                if (episodes != null) {
                    episodes.forEach { multiSelectHelper.deselect(it) }
                    adapter.notifyDataSetChanged()
                }
            }

            override fun multiSelectSelectAllUp(episode: Playable) {
                val episodes = viewModel.episodeList.value
                if (episodes != null) {
                    val startIndex = episodes.indexOf(episode)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(episodes.subList(0, startIndex + 1))
                    }

                    adapter.notifyDataSetChanged()
                }
            }

            override fun multiSelectSelectAllDown(episode: Playable) {
                val episodes = viewModel.episodeList.value
                if (episodes != null) {
                    val startIndex = episodes.indexOf(episode)
                    if (startIndex > -1) {
                        multiSelectHelper.selectAllInList(episodes.subList(startIndex, episodes.size))
                    }

                    adapter.notifyDataSetChanged()
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
                dialog.addTextOption(LR.string.profile_auto_download_settings, imageId = R.drawable.ic_settings_small, click = this::showAutodownloadSettings)
                if (downloadManager.hasPendingOrRunningDownloads()) {
                    dialog.addTextOption(LR.string.settings_auto_download_stop_all, imageId = IR.drawable.ic_stop, click = this::stopAllDownloads)
                }
                dialog.addTextOption(LR.string.profile_clean_up, imageId = VR.drawable.ic_delete, click = this::showCleanupSettings)
            } else if (mode is Mode.History) {
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

        (activity as AppCompatActivity).supportActionBar?.setTitle(LR.string.profile_auto_download_settings)
    }

    private fun stopAllDownloads() {
        downloadManager.stopAllDownloads()
    }

    private fun showCleanupSettings() {
        val fragment = ManualCleanupFragment.newInstance(showToolbar = true)
        showFragment(fragment)
    }

    private fun clearListeningHistory() {
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

    private fun episodeSwipedRightItem1(episode: Playable, index: Int) {
        when (settings.getUpNextSwipeAction()) {
            Settings.UpNextAction.PLAY_NEXT -> viewModel.episodeSwipeUpNext(episode)
            Settings.UpNextAction.PLAY_LAST -> viewModel.episodeSwipeUpLast(episode)
        }
        adapter.notifyItemChanged(index)
    }

    private fun episodeSwipedRightItem2(episode: Playable, index: Int) {
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
}
