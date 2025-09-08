package au.com.shiftyjelly.pocketcasts.playlists.smart

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
import au.com.shiftyjelly.pocketcasts.PlaylistEpisodesAdapterFactory
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentData
import au.com.shiftyjelly.pocketcasts.compose.extensions.setContentWithViewCompositionStrategy
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.filters.R
import au.com.shiftyjelly.pocketcasts.filters.databinding.PlaylistFragmentBinding
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderAdapter
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderButtonData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistToolbar
import au.com.shiftyjelly.pocketcasts.playlists.component.ToolbarConfig
import au.com.shiftyjelly.pocketcasts.playlists.smart.rules.EditRulesFragment
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
        val headerAdapter = PlaylistHeaderAdapter(
            themeType = theme.activeTheme,
            leftButton = PlaylistHeaderButtonData(
                iconId = IR.drawable.ic_playlist_smart_rules,
                label = getString(LR.string.smart_rules),
                onClick = {
                    viewModel.trackEditRulesTapped()
                    openEditor()
                },
            ),
            rightButton = PlaylistHeaderButtonData(
                iconId = IR.drawable.ic_playlist_play,
                label = getString(LR.string.playlist_play_all),
                onClick = {
                    viewModel.trackPlayAllTapped()
                    playAll()
                },
            ),
            searchState = viewModel.searchState.textState,
            onChangeSearchFocus = { hasFocus, searchTopOffset ->
                if (hasFocus) {
                    content.smoothScrollToTop(0, offset = -searchTopOffset.roundToInt())
                }
            },
        )
        val episodesAdapter = adapterFactory.create(
            multiSelectToolbar = multiSelectToolbar,
            getEpisodes = { viewModel.uiState.value.playlist?.episodes.orEmpty() },
        )
        content.adapter = ConcatAdapter(headerAdapter, episodesAdapter)
        EpisodeItemTouchHelper().attachToRecyclerView(content)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect { uiState ->
                    contentState = when (uiState.playlist?.episodes?.size) {
                        null -> ContentState.Uninitialized
                        0 -> ContentState.HasNoEpisodes
                        else -> ContentState.HasEpisode
                    }

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
            isKeyboardOpen = keyboardInset != 0
            content.updatePadding(bottom = initialPadding + miniPlayerInset + keyboardInset)
            windowInsets
        }
        content.hideKeyboardOnScroll()
    }

    private fun PlaylistFragmentBinding.setupNoContent() {
        val noContentData = NoContentData(
            title = getString(LR.string.smart_playlist_no_content_title),
            body = getString(LR.string.smart_playlist_no_content_body),
            iconId = IR.drawable.ic_info,
            primaryButton = NoContentData.Button(
                text = getString(LR.string.smart_rules),
                onClick = ::openEditor,
            ),
        )

        noContentBox.setContentWithViewCompositionStrategy {
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
                viewModel.uiState.map { it.playlist?.title.orEmpty() }
            }.collectAsState("")

            AppTheme(theme.activeTheme) {
                PlaylistToolbar(
                    title = title,
                    config = when (contentState) {
                        ContentState.Uninitialized -> ToolbarConfig.WithoutTitle
                        ContentState.HasNoEpisodes -> ToolbarConfig.WithTitle
                        ContentState.HasEpisode -> ToolbarConfig.ForAlpha(if (isKeyboardOpen) 1f else toolbarAlpha)
                    },
                    onClickBack = {
                        @Suppress("DEPRECATION")
                        requireActivity().onBackPressed()
                    },
                    onClickOptions = ::openOptionsSheet,
                )
            }
        }
    }

    private fun PlaylistFragmentBinding.setupChromeCast() {
        CastButtonFactory.setUpMediaRouteButton(requireContext(), chromeCastButton)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.chromeCastSignal
                .flowWithLifecycle(viewLifecycleOwner.lifecycle)
                .collect {
                    chromeCastButton.performClick()
                }
        }
    }

    private fun PlaylistFragmentBinding.setupSettings() {
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
        if (parentFragmentManager.findFragmentByTag("playlist_rules_editor") != null) {
            return
        }
        EditRulesFragment.newInstance(args.playlistUuid).show(parentFragmentManager, "playlist_rules_editor")
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
        val playlistUuid: String,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARGS = "SmartPlaylistsFragmentArgs"

        fun newInstance(playlistUuid: String) = PlaylistFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARGS to Args(playlistUuid))
        }
    }
}

private enum class ContentState {
    Uninitialized,
    HasNoEpisodes,
    HasEpisode,
}
