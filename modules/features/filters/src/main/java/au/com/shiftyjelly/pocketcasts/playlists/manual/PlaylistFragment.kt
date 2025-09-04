package au.com.shiftyjelly.pocketcasts.playlists.manual

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.util.lerp
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
import au.com.shiftyjelly.pocketcasts.filters.databinding.PlaylistFragmentBinding
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderAdapter
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderButtonData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistToolbar
import au.com.shiftyjelly.pocketcasts.playlists.manual.episode.AddEpisodesFragment
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.extensions.hideKeyboardOnScroll
import au.com.shiftyjelly.pocketcasts.views.extensions.smoothScrollToTop
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.EpisodeItemTouchHelper
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class PlaylistFragment :
    BaseFragment(),
    HasBackstack {

    @Inject
    lateinit var adapterFactory: PlaylistEpisodesAdapterFactory

    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARGS, Args::class.java) })

    private val viewModel by viewModels<PlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<PlaylistViewModel.Factory> { factory ->
                factory.create(playlistUuid = args.playlistUuid)
            }
        },
    )

    private var isKeyboardOpen by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = PlaylistFragmentBinding.inflate(inflater, container, false)
        binding.setupContent()
        binding.setupToolbar()
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
            searchState = viewModel.searchState.textState,
            onChangeSearchFocus = { hasFocus, searchTopOffset ->
                if (hasFocus) {
                    content.smoothScrollToTop(0, offset = -searchTopOffset.roundToInt())
                }
            },
        )
        val episodesAdapter = adapterFactory.createForManualPlaylist(
            multiSelectToolbar = multiSelectToolbar,
            getEpisodes = { viewModel.uiState.value.manualPlaylist?.episodes.orEmpty() },
        )
        content.adapter = ConcatAdapter(headerAdapter, episodesAdapter)
        EpisodeItemTouchHelper().attachToRecyclerView(content)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { uiState ->
                    val episodes = uiState.manualPlaylist?.episodes.orEmpty()
                    episodesAdapter.submitList(episodes)

                    val playlistHeaderData = uiState.manualPlaylist?.let { playlist ->
                        PlaylistHeaderData(
                            title = playlist.title,
                            totalEpisodeCount = playlist.totalEpisodeCount,
                            // TODO: Change displayed episode count to exclude archived episodes
                            displayedEpisodeCount = playlist.totalEpisodeCount,
                            playbackDurationLeft = playlist.playbackDurationLeft,
                            artworkPodcastUuids = playlist.artworkPodcastUuids.also { Timber.tag("LOG_TAG").i("uuids: $it") },
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
            isKeyboardOpen = keyboardInset != 0
            content.updatePadding(bottom = initialPadding + miniPlayerInset + keyboardInset)
            windowInsets
        }
        content.hideKeyboardOnScroll()
    }

    private fun PlaylistFragmentBinding.setupToolbar() {
        var toolbarAlpha by mutableFloatStateOf(0f)
        val transparencyThreshold = 40.dpToPx(requireContext())
        val maxProgressDistance = 100.dpToPx(requireContext())

        fun updateToolbarAlpha() {
            val layoutManager = (content.layoutManager as? LinearLayoutManager) ?: return
            val headerViewOffset = layoutManager
                .findViewByPosition(layoutManager.findFirstVisibleItemPosition())
                ?.takeIf { it.getTag(UR.id.playlist_view_header_tag) == true }
                ?.top?.absoluteValue
                ?: Int.MAX_VALUE
            toolbarAlpha = if (headerViewOffset > transparencyThreshold) {
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
                viewModel.uiState.map { it.manualPlaylist?.title.orEmpty() }
            }.collectAsState("")

            AppTheme(theme.activeTheme) {
                PlaylistToolbar(
                    title = title,
                    backgroundAlpha = if (isKeyboardOpen) 1f else toolbarAlpha,
                    onClickBack = {
                        @Suppress("DEPRECATION")
                        requireActivity().onBackPressed()
                    },
                    onClickOptions = {
                        Timber.i("Open options")
                    },
                )
            }
        }
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
        private const val NEW_INSTANCE_ARGS = "ManualPlaylistsFragmentArgs"

        fun newInstance(playlistUuid: String) = PlaylistFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(playlistUuid))
        }
    }
}
