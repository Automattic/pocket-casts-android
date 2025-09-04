package au.com.shiftyjelly.pocketcasts.playlists.manual

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.text.input.TextFieldState
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.filters.databinding.PlaylistFragmentBinding
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderAdapter
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderButtonData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderData
import au.com.shiftyjelly.pocketcasts.playlists.manual.episode.AddEpisodesFragment
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.extensions.hideKeyboardOnScroll
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlin.time.Duration
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PlaylistFragment : BaseFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    private val viewModel by viewModels<PlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<PlaylistViewModel.Factory> { factory ->
                factory.create(playlistUuid = args.playlistUuid)
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = PlaylistFragmentBinding.inflate(inflater, container, false)
        binding.setupContent()
        return binding.root
    }

    private fun PlaylistFragmentBinding.setupContent() {
        val headerAdapter = PlaylistHeaderAdapter(
            themeType = theme.activeTheme,
            leftButton = PlaylistHeaderButtonData(
                iconId = IR.drawable.ic_playlist_add_episodes,
                label = getString(LR.string.add_episodes),
                onClick = { openEditor() },
            ),
            rightButton = PlaylistHeaderButtonData(
                iconId = IR.drawable.ic_playlist_play,
                label = getString(LR.string.playlist_play_all),
                onClick = { Timber.i("Play all") },
            ),
            searchState = TextFieldState(),
            onChangeSearchFocus = { _, _ -> Timber.i("Scroll to content") },
        )
        headerAdapter.submitHeader(
            PlaylistHeaderData(
                title = "Playlist title",
                totalEpisodeCount = 0,
                displayedEpisodeCount = 0,
                playbackDurationLeft = Duration.ZERO,
                artworkPodcastUuids = emptyList(),
            ),
        )
        content.adapter = headerAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { uiState ->

                    val playlistHeaderData = uiState.manualPlaylist?.let { playlist ->
                        PlaylistHeaderData(
                            title = playlist.title,
                            totalEpisodeCount = playlist.totalEpisodeCount,
                            // TODO: Change displayed episode count to exclude archived episodes
                            displayedEpisodeCount = playlist.totalEpisodeCount,
                            playbackDurationLeft = playlist.playbackDurationLeft,
                            artworkPodcastUuids = playlist.artworkPodcastUuids,
                        )
                    }
                    headerAdapter.submitHeader(playlistHeaderData)
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

    private fun openEditor() {
        val episodeCount = viewModel.uiState.value.manualPlaylist?.totalEpisodeCount ?: Int.MAX_VALUE
        if (episodeCount >= PlaylistManager.MANUAL_PLAYLIST_EPISODE_LIMIT) {
            val snackbarView = (requireActivity() as FragmentHostListener).snackBarView()
            Snackbar.make(snackbarView, getString(LR.string.add_to_playlist_failure_message), Snackbar.LENGTH_LONG).show()
            return
        }
        if (parentFragmentManager.findFragmentByTag("playlist_episode_editor") != null) {
            return
        }
        AddEpisodesFragment.newInstance(args.playlistUuid).show(parentFragmentManager, "playlist_episode_editor")
    }

    @Parcelize
    private class Args(
        val playlistUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "ManualPlaylistsFragmentArgs"

        fun newInstance(playlistUuid: String) = PlaylistFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(playlistUuid))
        }
    }
}
