package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

@OptIn(FlowPreview::class)
@AndroidEntryPoint
internal class SettingsFragment : BaseFragment() {
    private val viewModel by viewModels<PlaylistViewModel>({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val uiState by viewModel.uiState.collectAsState()
        val playlist = uiState.playlist
        val bottomPadding by viewModel.bottomInset.collectAsState(0)

        AppThemeWithBackground(theme.activeTheme) {
            if (playlist != null) {
                val nameState = rememberTextFieldState(initialText = playlist.title)
                UpdateNameEffect(nameState)

                SettingsPage(
                    state = nameState,
                    isAutoDownloadEnabled = playlist.settings.isAutoDownloadEnabled,
                    autoDownloadEpisodeLimit = playlist.settings.autoDownloadLimit,
                    onChangeAutoDownloadValue = viewModel::updateAutoDownload,
                    onClickEpisodeLimit = ::openDownloadLimit,
                    onClickBack = {
                        @Suppress("DEPRECATION")
                        requireActivity().onBackPressed()
                    },
                    modifier = Modifier.padding(
                        bottom = LocalDensity.current.run { bottomPadding.toDp() },
                    ),
                )
            }
        }
    }

    @Composable
    private fun UpdateNameEffect(state: TextFieldState) {
        LaunchedEffect(state) {
            snapshotFlow { state.text.toString() }
                .drop(1) // Skip initial name
                .debounce(300)
                .collect { newName ->
                    viewModel.updateName(newName)
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!requireActivity().isChangingConfigurations) {
            viewModel.trackEditDismissed()
        }
    }

    private fun openDownloadLimit() {
        if (parentFragmentManager.findFragmentByTag("auto_download_limit") != null) {
            return
        }
        DownloadLimitFragment().show(parentFragmentManager, "auto_download_limit")
    }
}
