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
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.TopScrollable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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
                onCreatePlaylist = {
                    viewModel.trackCreatePlaylistClicked()
                    val fragment = childFragmentManager.findFragmentByTag("create_playlist")
                    if (fragment == null) {
                        CreatePlaylistFragment().show(childFragmentManager, "create_playlist")
                    }
                },
                onDeletePlaylist = { playlist -> viewModel.deletePlaylist(playlist.uuid) },
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
                onDismissPremadePlaylistsTooltip = viewModel::dismissPremadePlaylistsTooltip,
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
