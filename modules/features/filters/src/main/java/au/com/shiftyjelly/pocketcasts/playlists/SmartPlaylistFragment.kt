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
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.PlaylistEpisodesAdapterFactory
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.filters.databinding.SmartPlaylistFragmentBinding
import au.com.shiftyjelly.pocketcasts.playlists.edit.SmartPlaylistsOptionsFragment
import au.com.shiftyjelly.pocketcasts.playlists.edit.SmartRulesEditFragment
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = SmartPlaylistFragmentBinding.inflate(inflater, container, false)
        binding.setupContent()
        binding.setupToolbar()
        return binding.root
    }

    private fun SmartPlaylistFragmentBinding.setupContent() {
        val leftButton = PlaylistHeaderData.ActionButton(
            iconId = IR.drawable.ic_playlist_smart_rules,
            label = getString(LR.string.smart_rules),
            onClick = ::openEditor,
        )
        val rightButton = PlaylistHeaderData.ActionButton(
            iconId = IR.drawable.ic_playlist_play,
            label = getString(LR.string.playlist_play_all),
            onClick = ::playAll,
        )

        val headerAdapter = PlaylistHeaderAdapter(
            themeType = theme.activeTheme,
        )
        val episodesAdapter = adapterFactory.create(
            multiSelectToolbar = multiSelectToolbar,
            onChangeMultiSelect = { isMultiSelecting -> Timber.i("Is multi selecting: $isMultiSelecting") },
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
                            episodeCount = playlist.totalEpisodeCount,
                            playbackDurationLeft = playlist.playbackDurationLeft,
                            artworkPodcasts = playlist.artworkPodcasts,
                            leftButton = leftButton,
                            rightButton = rightButton,
                        )
                    }
                    headerAdapter.submitHeader(playlistHeaderData)
                }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val initialPadding = content.paddingBottom
            viewModel.bottomInset
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { inset ->
                    content.updatePadding(bottom = initialPadding + inset)
                }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.startMultiSelectingSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect {
                    adapterFactory.startMultiSelecting()
                }
        }
    }

    private fun SmartPlaylistFragmentBinding.setupToolbar() {
        val toolbarAlpha = mutableFloatStateOf(0f)
        content.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            private var transparencyThreshold = -1
            private var maxProgressDistance = -1

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (transparencyThreshold == -1) {
                    transparencyThreshold = 40.dpToPx(recyclerView.context)
                }
                if (maxProgressDistance == -1) {
                    maxProgressDistance = 100.dpToPx(recyclerView.context)
                }

                val layoutManager = (recyclerView.layoutManager as? LinearLayoutManager) ?: return
                val headerViewOffset = getHeaderViewOffset(layoutManager)
                toolbarAlpha.floatValue = computeToolbarAlpha(headerViewOffset)
            }

            private fun getHeaderViewOffset(layoutManager: LinearLayoutManager): Int {
                return layoutManager
                    .findViewByPosition(layoutManager.findFirstVisibleItemPosition())
                    ?.takeIf { it.getTag(UR.id.playlist_view_header_tag) == true }
                    ?.top?.absoluteValue
                    ?: Int.MAX_VALUE
            }

            private fun computeToolbarAlpha(offset: Int): Float {
                return if (offset > transparencyThreshold) {
                    val scrollFraction = (offset - transparencyThreshold).toFloat() / (maxProgressDistance)
                    lerp(0f, 1f, scrollFraction).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
        })

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
        if (parentFragmentManager.findFragmentByTag("playlist_editor") != null) {
            return
        }
        SmartRulesEditFragment.newInstance(args.playlistUuid).show(parentFragmentManager, "playlist_editor")
    }

    private fun openOptionsSheet() {
        if (childFragmentManager.findFragmentByTag("playlist_options_sheet") != null) {
            return
        }
        SmartPlaylistsOptionsFragment().show(childFragmentManager, "playlist_options_sheet")
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
