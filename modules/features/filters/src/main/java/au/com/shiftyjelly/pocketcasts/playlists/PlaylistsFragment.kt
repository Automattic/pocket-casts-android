package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.TopScrollable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class PlaylistsFragment :
    BaseFragment(),
    TopScrollable {
    private val scrollToTopSignal = MutableSharedFlow<Unit>()
    private val viewModel by viewModels<PlaylistsViewModel>()

    private var getCanScrollBackward: () -> Boolean = { false }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val listState = rememberLazyListState()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        getCanScrollBackward = { listState.canScrollBackward }

        AppThemeWithBackground(theme.activeTheme) {
            PlaylistsPage(
                uiState = uiState,
                listState = listState,
                getArtworkUuidsFlow = viewModel::getArtworkUuidsFlow,
                getEpisodeCountFlow = viewModel::getEpisodeCountFlow,
                refreshArtworkUuids = viewModel::refreshArtworkUuids,
                refreshEpisodeCount = viewModel::refreshEpisodeCount,
                onCreatePlaylist = {
                    viewModel.trackCreatePlaylistClicked()
                    val fragment = childFragmentManager.findFragmentByTag("create_playlist")
                    if (fragment == null) {
                        CreatePlaylistFragment().show(childFragmentManager, "create_playlist")
                    }
                },
                onDeletePlaylist = { playlist, settleRow ->
                    viewModel.trackPlaylistDeleteTriggered(playlist)
                    openDeleteConfirmation(playlist, settleRow)
                },
                onOpenPlaylist = { playlist ->
                    val fragment = PlaylistFragment.newInstance(playlist.uuid, playlist.type)
                    (requireActivity() as FragmentHostListener).addFragment(fragment)
                },
                onReorderPlaylists = viewModel::updatePlaylistsOrder,
                onShowPlaylists = { playlists -> viewModel.trackPlaylistsShown(playlists.size) },
                onFreeAccountBannerCtaClick = {
                    viewModel.trackFreeAccountCtaClicked()
                    OnboardingLauncher.openOnboardingFlow(
                        activity = requireActivity(),
                        onboardingFlow = OnboardingFlow.LoggedOut,
                    )
                },
                onFreeAccountBannerDismiss = viewModel::dismissFreeAccountBanner,
                onShowPremadePlaylistsTooltip = viewModel::trackTooltipShown,
                onDismissTooltip = viewModel::dismissTooltip,
            )
        }

        ScrollToTopEffect(listState)
        ShowOnboardingEffect(uiState.showOnboarding)
    }

    override fun onDestroyView() {
        getCanScrollBackward = { false }
        super.onDestroyView()
    }

    @Composable
    private fun ScrollToTopEffect(listState: LazyListState) {
        LaunchedEffect(listState) {
            scrollToTopSignal.collectLatest {
                listState.animateScrollToItem(0)
            }
        }
    }

    @Composable
    private fun ShowOnboardingEffect(show: Boolean) {
        if (show) {
            LaunchedEffect(show) {
                OnboardingFragment().show(childFragmentManager, "playlists_onboarding")
            }
        }
    }

    private fun openDeleteConfirmation(playlist: PlaylistPreview, settleRow: () -> Unit) {
        if (parentFragmentManager.findFragmentByTag("delete_playlist_confirmation") != null) {
            return
        }
        val dialog = ConfirmationDialog()
            .setTitle(getString(LR.string.delete_playlist_confirmation_title))
            .setSummary(getString(LR.string.delete_playlist_confirmation_body))
            .setIconId(IR.drawable.ic_warning)
            .setButtonType(ConfirmationDialog.ButtonType.Danger(getString(LR.string.delete)))
            .setOnConfirm { viewModel.deletePlaylist(playlist) }
            .setOnDismiss { isDismissedWithoutAction ->
                if (isDismissedWithoutAction) {
                    viewModel.trackPlaylistDeleteDismissed(playlist)
                    settleRow()
                }
            }
        dialog.show(parentFragmentManager, "delete_playlist_confirmation")
    }

    override fun scrollToTop(): Boolean {
        val canScroll = getCanScrollBackward()
        if (canScroll) {
            lifecycleScope.launch {
                scrollToTopSignal.emit(Unit)
            }
        }

        return canScroll
    }
}
