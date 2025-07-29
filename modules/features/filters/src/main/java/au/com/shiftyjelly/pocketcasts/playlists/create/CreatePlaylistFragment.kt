package au.com.shiftyjelly.pocketcasts.playlists.create

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.slideInToEnd
import au.com.shiftyjelly.pocketcasts.compose.extensions.slideInToStart
import au.com.shiftyjelly.pocketcasts.compose.extensions.slideOutToEnd
import au.com.shiftyjelly.pocketcasts.compose.extensions.slideOutToStart
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class CreatePlaylistFragment : BaseDialogFragment() {
    private val viewModel by viewModels<CreatePlaylistViewModel>(
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<CreatePlaylistViewModel.Factory> { factory ->
                factory.create(initialPlaylistName = getString(LR.string.new_playlist))
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        Box(
            modifier = Modifier
                .fillMaxHeight(0.93f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        ) {
            AppThemeWithBackground(
                themeType = theme.activeTheme,
            ) {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = NavigationRoutes.NEW_PLAYLIST,
                    enterTransition = { slideInToStart() },
                    exitTransition = { slideOutToStart() },
                    popEnterTransition = { slideInToEnd() },
                    popExitTransition = { slideOutToEnd() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    composable(NavigationRoutes.NEW_PLAYLIST) {
                        NewPlaylistPage(
                            titleState = viewModel.playlistNameState,
                            onCreateManualPlaylist = { Timber.i("Create Manual Playlist") },
                            onContinueToSmartPlaylist = {
                                navController.navigate(NavigationRoutes.SMART_PLAYLIST_PREVIEW) {
                                    popUpTo(NavigationRoutes.NEW_PLAYLIST) {
                                        inclusive = true
                                    }
                                }
                            },
                            onClickClose = ::dismiss,
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                                .nestedScroll(rememberNestedScrollInteropConnection())
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                    composable(NavigationRoutes.SMART_PLAYLIST_PREVIEW) {
                        SmartPlaylistPreviewPage(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding()
                                .nestedScroll(rememberNestedScrollInteropConnection())
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }
            }
        }
    }
}

private object NavigationRoutes {
    const val NEW_PLAYLIST = "new_playlist"
    const val SMART_PLAYLIST_PREVIEW = "smart_playlist_preview"
}
