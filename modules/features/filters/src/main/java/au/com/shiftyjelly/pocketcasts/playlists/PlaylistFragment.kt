package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentData
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.filters.R
import au.com.shiftyjelly.pocketcasts.filters.databinding.PlaylistFragmentBinding
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistEpisodeAdapter
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistEpisodesAdapterFactory
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderAdapter
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderButtonData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistToolbar
import au.com.shiftyjelly.pocketcasts.playlists.component.ToolbarConfig
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddEpisodesFragment
import au.com.shiftyjelly.pocketcasts.playlists.smart.EditRulesFragment
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.extensions.hideKeyboardOnScroll
import au.com.shiftyjelly.pocketcasts.views.extensions.smoothScrollToTop
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.HasBackstack
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.material.snackbar.Snackbar
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
import au.com.shiftyjelly.pocketcasts.views.R as VR

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
                factory.create(args.uuid, args.type)
            }
        },
    )

    private var isKeyboardOpen by mutableStateOf(false)
    private var isAnyPodcastFollowed by mutableStateOf(false)
    private var contentState by mutableStateOf(ContentState.Uninitialized)

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
        val binding = PlaylistFragmentBinding.inflate(inflater, container, false)
        binding.setupContent()
        binding.setupNoContent()
        binding.setupToolbar()
        binding.setupChromeCast()
        binding.setupSettings()
        return binding.root
    }

    private fun PlaylistFragmentBinding.setupContent() {
        val headerAdapter = createHeaderAdapter(this)
        val episodesAdapter = createEpisodesAdapter(this)

        content.adapter = ConcatAdapter(headerAdapter, episodesAdapter)
        content.hideKeyboardOnScroll()

        observeUiState(headerAdapter, episodesAdapter)
        observeMultiSelectSignal()
        observePaddingState(this)
    }

    private fun PlaylistFragmentBinding.setupNoContent() {
        noContentBox.setContentWithViewCompositionStrategy {
            NoContentOverlay()
        }
    }

    private fun PlaylistFragmentBinding.setupToolbar() {
        playlistToolbar.setContentWithViewCompositionStrategy {
            PlaylistToolbar()
        }
    }

    private fun PlaylistFragmentBinding.setupChromeCast() {
        CastButtonFactory.setUpMediaRouteButton(requireContext(), chromeCastButton)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chromeCastSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { chromeCastButton.performClick() }
        }
    }

    private fun PlaylistFragmentBinding.setupSettings() {
        CastButtonFactory.setUpMediaRouteButton(requireContext(), chromeCastButton)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.showSettingsSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { openSettings() }
        }
    }

    private fun createHeaderAdapter(binding: PlaylistFragmentBinding): PlaylistHeaderAdapter {
        return PlaylistHeaderAdapter(
            themeType = theme.activeTheme,
            leftButton = when (args.type) {
                Playlist.Type.Manual -> PlaylistHeaderButtonData(
                    iconId = IR.drawable.ic_playlist_add_episode,
                    label = getString(LR.string.add_episodes),
                    onClick = {
                        viewModel.trackAddEpisodesTapped()
                        openEditor()
                    },
                )

                Playlist.Type.Smart -> PlaylistHeaderButtonData(
                    iconId = IR.drawable.ic_playlist_smart_rules,
                    label = getString(LR.string.smart_rules),
                    onClick = {
                        viewModel.trackEditRulesTapped()
                        openEditor()
                    },
                )
            },
            rightButton = PlaylistHeaderButtonData(
                iconId = IR.drawable.ic_playlist_play,
                label = getString(LR.string.playlist_play_all),
                onClick = {
                    viewModel.trackPlayAllTapped()
                    playAll()
                },
            ),
            searchState = viewModel.searchState.textState,
            onShowArchivedToggle = {
                viewModel.trackToggleShowArchived()
                viewModel.toggleShowArchived()
            },
            onChangeSearchFocus = { hasFocus, searchTopOffset ->
                if (hasFocus) {
                    binding.content.smoothScrollToTop(0, offset = -searchTopOffset.roundToInt())
                }
            },
        )
    }

    private fun createEpisodesAdapter(binding: PlaylistFragmentBinding): PlaylistEpisodeAdapter {
        return adapterFactory.create(
            playlistType = args.type,
            playlistUuid = args.uuid,
            multiSelectToolbar = binding.multiSelectToolbar,
            getEpisodes = { viewModel.uiState.value.playlist?.episodes.orEmpty() },
        )
    }

    private fun observeUiState(
        headerAdapter: PlaylistHeaderAdapter,
        episodesAdapter: PlaylistEpisodeAdapter,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { uiState ->
                    contentState = when (uiState.playlist?.episodes?.size) {
                        null -> ContentState.Uninitialized

                        0 -> if (uiState.playlist.metadata.archivedEpisodeCount == 0) {
                            ContentState.HasNoEpisodes
                        } else {
                            ContentState.HasEpisode
                        }

                        else -> ContentState.HasEpisode
                    }
                    isAnyPodcastFollowed = uiState.isAnyPodcastFollowed

                    val playlistHeaderData = uiState.playlist?.let { playlist ->
                        PlaylistHeaderData(
                            title = playlist.title,
                            metadata = playlist.metadata,
                        )
                    }
                    headerAdapter.submitHeader(playlistHeaderData)

                    val episodes = uiState.playlist?.episodes.orEmpty()
                    episodesAdapter.submitList(episodes)
                }
        }
    }

    private fun observeMultiSelectSignal() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.startMultiSelectingSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { adapterFactory.startMultiSelecting() }
        }
    }

    private fun observePaddingState(binding: PlaylistFragmentBinding) {
        val initialPadding = binding.content.paddingBottom
        var miniPlayerInset = 0
        var keyboardInset = 0
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.bottomInset
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { inset ->
                    miniPlayerInset = inset
                    binding.content.updatePadding(bottom = initialPadding + miniPlayerInset + keyboardInset)
                }
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.content) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.ime())
            keyboardInset = insets.bottom
            isKeyboardOpen = keyboardInset != 0
            binding.content.updatePadding(bottom = initialPadding + miniPlayerInset + keyboardInset)
            windowInsets
        }
    }

    @Composable
    private fun NoContentOverlay() {
        val noContentData = rememberNoContentData()
        val transition = updateTransition(contentState)
        AppTheme(theme.activeTheme) {
            Box {
                if (transition.currentState == ContentState.Uninitialized) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.theme.colors.primaryUi02),
                    )
                }
                transition.AnimatedVisibility(
                    visible = { it == ContentState.HasNoEpisodes },
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.theme.colors.primaryUi02),
                    ) {
                        NoContentBanner(
                            data = noContentData,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun rememberNoContentData() = remember(args.type, isAnyPodcastFollowed) {
        when (args.type) {
            Playlist.Type.Smart -> NoContentData(
                title = getString(LR.string.smart_playlist_no_content_title),
                body = getString(LR.string.smart_playlist_no_content_body),
                iconId = IR.drawable.ic_info,
                primaryButton = NoContentData.Button(
                    text = getString(LR.string.smart_rules),
                    onClick = ::openEditor,
                ),
            )

            Playlist.Type.Manual -> {
                if (isAnyPodcastFollowed) {
                    NoContentData(
                        title = getString(LR.string.manual_playlist_no_content_title_alternative),
                        body = "",
                        iconId = IR.drawable.ic_playlists,
                        primaryButton = NoContentData.Button(
                            text = getString(LR.string.add_episodes),
                            onClick = ::openEditor,
                        ),
                    )
                } else {
                    NoContentData(
                        title = getString(LR.string.manual_playlist_no_content_title),
                        body = getString(LR.string.manual_playlist_no_content_body),
                        iconId = IR.drawable.ic_playlists,
                        primaryButton = NoContentData.Button(
                            text = getString(LR.string.browse_shows),
                            onClick = {
                                val hostListener = (requireActivity() as FragmentHostListener)
                                hostListener.closeToRoot()
                                hostListener.openTab(VR.id.navigation_discover)
                            },
                        ),
                    )
                }
            }
        }
    }

    @Composable
    private fun PlaylistFragmentBinding.PlaylistToolbar() {
        val toolbarAlpha = produceToolbarAlpha(this)
        val title = producePlaylistTitle()

        AppTheme(theme.activeTheme) {
            PlaylistToolbar(
                title = title,
                config = when (contentState) {
                    ContentState.Uninitialized -> ToolbarConfig.WithoutTitle
                    ContentState.HasNoEpisodes -> ToolbarConfig.WithTitle
                    ContentState.HasEpisode -> ToolbarConfig.ForAlpha(toolbarAlpha)
                },
                onClickBack = {
                    @Suppress("DEPRECATION")
                    requireActivity().onBackPressed()
                },
                onClickOptions = ::openOptionsSheet,
            )
        }
    }

    @Composable
    private fun produceToolbarAlpha(binding: PlaylistFragmentBinding): Float {
        var toolbarAlpha by remember { mutableFloatStateOf(0f) }

        LaunchedEffect(Unit) {
            val transparencyThreshold = 40.dpToPx(requireContext())
            val maxProgressDistance = 100.dpToPx(requireContext())

            fun updateToolbarAlpha() {
                val layoutManager = (binding.content.layoutManager as? LinearLayoutManager) ?: return
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
            binding.content.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    updateToolbarAlpha()
                }
            })
            binding.content.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                updateToolbarAlpha()
            }
        }
        return if (isKeyboardOpen) 1f else toolbarAlpha
    }

    @Composable
    private fun producePlaylistTitle(): String {
        return remember { viewModel.uiState.map { it.playlist?.title.orEmpty() } }
            .collectAsState("")
            .value
    }

    private fun playAll() {
        if (parentFragmentManager.findFragmentByTag("confirm_and_play") != null) {
            return
        }
        if (viewModel.shouldShowPlayAllWarning()) {
            val episodeCount = viewModel.uiState.value.playlist?.episodes.orEmpty().size
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
        when (args.type) {
            Playlist.Type.Manual -> openManualEditor()
            Playlist.Type.Smart -> openSmartEditor()
        }
    }

    private fun openManualEditor() {
        val episodeCount = viewModel.uiState.value.playlist?.metadata?.totalEpisodeCount ?: Int.MAX_VALUE
        if (episodeCount >= PlaylistManager.MANUAL_PLAYLIST_EPISODE_LIMIT) {
            val snackbarView = (requireActivity() as FragmentHostListener).snackBarView()
            Snackbar.make(snackbarView, getString(LR.string.add_to_playlist_failure_message), Snackbar.LENGTH_LONG).show()
            return
        }
        if (parentFragmentManager.findFragmentByTag("playlist_episode_editor") != null) {
            return
        }
        AddEpisodesFragment.newInstance(args.uuid).show(parentFragmentManager, "playlist_episode_editor")
    }

    private fun openSmartEditor() {
        if (parentFragmentManager.findFragmentByTag("playlist_rules_editor") != null) {
            return
        }
        EditRulesFragment.newInstance(args.uuid).show(parentFragmentManager, "playlist_rules_editor")
    }

    private fun openOptionsSheet() {
        if (childFragmentManager.findFragmentByTag("playlist_options_sheet") != null) {
            return
        }
        OptionsFragment().show(childFragmentManager, "playlist_options_sheet")
    }

    private fun openSettings() {
        if (childFragmentManager.findFragmentByTag("playlist_settings") != null) {
            return
        }
        childFragmentManager.commit {
            addToBackStack("playlist_settings")
            add(R.id.playlistFragmentContainer, SettingsFragment(), "playlist_settings")
        }
    }

    override fun onBackPressed(): Boolean {
        return adapterFactory.onBackPressed() || super.onBackPressed()
    }

    override fun getBackstackCount(): Int {
        return adapterFactory.getBackstackCount() + super.getBackstackCount()
    }

    @Parcelize
    private class Args(
        val uuid: String,
        val type: Playlist.Type,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "PlaylistsFragmentArgs"

        fun newInstance(
            uuid: String,
            type: Playlist.Type,
        ) = PlaylistFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(uuid, type))
        }
    }
}

private enum class ContentState {
    Uninitialized,
    HasNoEpisodes,
    HasEpisode,
}
