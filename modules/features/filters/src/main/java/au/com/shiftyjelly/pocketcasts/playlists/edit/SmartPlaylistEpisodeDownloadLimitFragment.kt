package au.com.shiftyjelly.pocketcasts.playlists.edit

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
import au.com.shiftyjelly.pocketcasts.playlists.SmartPlaylistViewModel
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SmartPlaylistEpisodeDownloadLimitFragment : BaseDialogFragment() {
    private val viewModel by viewModels<SmartPlaylistViewModel>({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val uiState by viewModel.uiState.collectAsState()
        val playlist = uiState.smartPlaylist

        DialogBox(
            fillMaxHeight = false,
            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
        ) {
            if (playlist != null) {
                SmartPlaylistEpisodeDownloadLimitPage(
                    episodeLimit = playlist.autoDownloadLimit,
                    onSelectEpisodeLimit = { limit ->
                        viewModel.updateAutoDownloadLimit(limit)
                        dismiss()
                    },
                )
            }
        }
    }
}
