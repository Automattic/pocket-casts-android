package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.util.lerp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.PlaylistEpisodesAdapterFactory
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.filters.R
import au.com.shiftyjelly.pocketcasts.filters.databinding.SmartPlaylistFragmentBinding
import au.com.shiftyjelly.pocketcasts.playlists.edit.SmartPlaylistSettingsFragment
import au.com.shiftyjelly.pocketcasts.playlists.edit.SmartPlaylistsOptionsFragment
import au.com.shiftyjelly.pocketcasts.playlists.edit.SmartRulesEditFragment
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.hideKeyboardOnScroll
import au.com.shiftyjelly.pocketcasts.views.extensions.smoothScrollToTop
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import com.google.android.gms.cast.framework.CastButtonFactory
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class SmartPlaylistFragment :
    BaseFragment(),
    HasBackstack {

    @Inject
    lateinit var adapterFactory: PlaylistEpisodesAdapterFactory

    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    private val viewModel by viewModels<SmartPlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<SmartPlaylistViewModel.Factory> { factory ->
                factory.create(playlistUuid = args.playlistUuid)
            }
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            viewModel.trackFilterShown()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = SmartPlaylistFragmentBinding.inflate(inflater, container, false)
        binding.setupContent()
        binding.setupToolbar()
        binding.setupChromeCast()
        binding.setupSettings()
        return binding.root
    }

    private fun SmartPlaylistFragmentBinding.setupContent() {
        val leftButton = PlaylistHeaderData.ActionButton(
            iconId = IR.drawable.ic_playlist_smart_rules,
            label = getString(LR.string.smart_rules),
            onClick = {
                viewModel.trackEditRulesTapped()
                openEditor()
            },
        )
        val rightButton = PlaylistHeaderData.ActionButton(
            iconId = IR.drawable.ic_playlist_play,
            label = getString(LR.string.playlist_play_all),
            onClick = {
                viewModel.trackPlayAllTapped()
                playAll()
            },
        )

        val headerAdapter = PlaylistHeaderAdapter(
            themeType = theme.activeTheme,
            onChangeSearchFocus = { hasFocus, searchTopOffset ->
                if (hasFocus) {
                    content.smoothScrollToTop(0, offset = -searchTopOffset.roundToInt())
                }
            },
        )
        val episodesAdapter = adapterFactory.create(
            multiSelectToolbar = multiSelectToolbar,
            getEpisodes = { viewModel.uiState.value.smartPlaylist?.episodes.orEmpty() },
        )
        content.adapter = ConcatAdapter(headerAdapter, episodesAdapter)
        EpisodeItemTouchHelper().attachToRecyclerView(content)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { uiState ->
                    val episodes = uiState.smartPlaylist?.episodes.orEmpty()
                    episodesAdapter.submitList(episodes)

                    val playlistHeaderData = uiState.smartPlaylist?.let { playlist ->
                        PlaylistHeaderData(
                            title = playlist.title,
                            totalEpisodeCount = playlist.totalEpisodeCount,
                            displayedEpisodeCount = playlist.episodes.size,
                            playbackDurationLeft = playlist.playbackDurationLeft,
                            artworkPodcasts = playlist.artworkPodcasts,
                            leftButton = leftButton,
                            rightButton = rightButton,
                            searchState = viewModel.searchState,
                        )
                    }
                    headerAdapter.submitHeader(playlistHeaderData)
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.startMultiSelectingSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect {
                    adapterFactory.startMultiSelecting()
                }
        }

        val initialPadding = content.paddingBottom
        var miniPlayerInset = 0
        var keyboardInset = 0
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bottomInset
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { inset ->
                    miniPlayerInset = inset
                    content.updatePadding(bottom = initialPadding + miniPlayerInset + keyboardInset)
                }
        }
        ViewCompat.setOnApplyWindowInsetsListener(content) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            keyboardInset = insets.bottom
            content.updatePadding(bottom = initialPadding + miniPlayerInset + keyboardInset)
            windowInsets
        }
        content.hideKeyboardOnScroll()
    }

    private fun SmartPlaylistFragmentBinding.setupToolbar() {
        val toolbarAlpha = mutableFloatStateOf(0f)
        val transparencyThreshold = 40.dpToPx(requireContext())
        val maxProgressDistance = 100.dpToPx(requireContext())

        fun updateToolbarAlpha() {
            val layoutManager = (content.layoutManager as? LinearLayoutManager) ?: return
            val headerViewOffset = layoutManager
                .findViewByPosition(layoutManager.findFirstVisibleItemPosition())
                ?.takeIf { it.getTag(UR.id.playlist_view_header_tag) == true }
                ?.top?.absoluteValue
                ?: Int.MAX_VALUE
            toolbarAlpha.floatValue = if (headerViewOffset > transparencyThreshold) {
                val scrollFraction = (headerViewOffset - transparencyThreshold).toFloat() / (maxProgressDistance)
                lerp(0f, 1f, scrollFraction).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
        content.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                updateToolbarAlpha()
            }
        })
        content.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateToolbarAlpha()
        }

        playlistToolbar.setContentWithViewCompositionStrategy {
            val title by remember {
                viewModel.uiState.map { it.smartPlaylist?.title.orEmpty() }
            }.collectAsState("")

            AppTheme(theme.activeTheme) {
                PlaylistToolbar(
                    title = title,
                    backgroundAlpha = toolbarAlpha.floatValue,
                    onClickBack = {
                        @Suppress("DEPRECATION")
                        requireActivity().onBackPressed()
                    },
                    onClickOptions = ::openOptionsSheet,
                )
            }
        }
    }

    private fun SmartPlaylistFragmentBinding.setupChromeCast() {
        CastButtonFactory.setUpMediaRouteButton(requireContext(), chromeCastButton)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chromeCastSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect {
                    chromeCastButton.performClick()
                }
        }
    }

    private fun SmartPlaylistFragmentBinding.setupSettings() {
        CastButtonFactory.setUpMediaRouteButton(requireContext(), chromeCastButton)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showSettingsSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect {
                    openSettings()
                }
        }
    }

    private fun playAll() {
        if (parentFragmentManager.findFragmentByTag("confirm_and_play") != null) {
            return
        }
        if (viewModel.shouldShowPlayAllWarning()) {
            val episodeCount = viewModel.uiState.value.smartPlaylist?.episodes.orEmpty().size
            val buttonString = getString(LR.string.filters_play_episodes, episodeCount)

            val dialog = ConfirmationDialog()
                .setTitle(getString(LR.string.filters_play_all))
                .setSummary(getString(LR.string.filters_play_all_summary))
                .setIconId(IR.drawable.ic_play_all)
                .setButtonType(ConfirmationDialog.ButtonType.Danger(buttonString))
                .setOnConfirm { viewModel.playAll() }
            dialog.show(parentFragmentManager, "confirm_play_all")
        } else {
            viewModel.playAll()
        }
    }

    private fun openEditor() {
        if (parentFragmentManager.findFragmentByTag("playlist_rules_editor") != null) {
            return
        }
        SmartRulesEditFragment.newInstance(args.playlistUuid).show(parentFragmentManager, "playlist_rules_editor")
    }

    private fun openOptionsSheet() {
        if (childFragmentManager.findFragmentByTag("playlist_options_sheet") != null) {
            return
        }
        SmartPlaylistsOptionsFragment().show(childFragmentManager, "playlist_options_sheet")
    }

    private fun openSettings() {
        if (childFragmentManager.findFragmentByTag("playlist_settings") != null) {
            return
        }
        childFragmentManager.commit {
            addToBackStack("playlist_settings")
            add(R.id.playlistFragmentContainer, SmartPlaylistSettingsFragment(), "playlist_settings")
        }
    }

    override fun onBackPressed(): Boolean {
        return if (adapterFactory.onBackPressed()) {
            true
        } else {
            super.onBackPressed()
        }
    }

    override fun getBackstackCount(): Int {
        return adapterFactory.getBackstackCount() + super.getBackstackCount()
    }

    @Parcelize
    private class Args(
        val playlistUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "SmartPlaylistsFragmentArgs"

        fun newInstance(playlistUuid: String) = SmartPlaylistFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(playlistUuid))
        }
    }
}
