package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
internal class DownloadLimitFragment : BaseDialogFragment() {
    private val viewModel by viewModels<PlaylistViewModel>({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val uiState by viewModel.uiState.collectAsState()
        val playlist = uiState.playlist

        DialogBox(
            fillMaxHeight = false,
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            if (playlist != null) {
                DownloadLimitPage(
                    episodeLimit = playlist.settings.autoDownloadLimit,
                    onSelectEpisodeLimit = { limit ->
                        viewModel.updateAutoDownloadLimit(limit)
                        dismiss()
                    },
                )
            }
        }
    }
}
