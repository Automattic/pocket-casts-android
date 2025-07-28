package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.TopScrollable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber

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
        val uiState by viewModel.uiState.collectAsState()

        getCanScrollBackward = { listState.canScrollBackward }

        AppThemeWithBackground(theme.activeTheme) {
            PlaylistsPage(
                uiState = uiState,
                listState = listState,
                onCreatePlaylist = { Timber.i("Create playlist clicked") },
                onDeletePlaylist = { playlist -> viewModel.deletePlaylist(playlist.uuid) },
                onReorderPlaylists = viewModel::updatePlaylistPosition,
                onShowOptions = { Timber.i("Show playlists options clicked") },
                onFreeAccountBannerCtaClick = {
                    viewModel.trackFreeAccountCtaClick()
                    OnboardingLauncher.openOnboardingFlow(
                        activity = requireActivity(),
                        onboardingFlow = OnboardingFlow.LoggedOut,
                    )
                },
                onFreeAccountBannerDismiss = viewModel::dismissFreeAccountBanner,
            )
        }

        ScrollToTopEffect(listState)
        ShowOnboardingEffect(uiState.showOnboarding)
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
                PlaylistsOnboardingFragment().show(childFragmentManager, "playlists_onboarding")
            }
        }
    }

    override fun scrollToTop(): Boolean {
        val canScroll = getCanScrollBackward()

        lifecycleScope.launch {
            scrollToTopSignal.emit(Unit)
        }

        return canScroll
    }
}
