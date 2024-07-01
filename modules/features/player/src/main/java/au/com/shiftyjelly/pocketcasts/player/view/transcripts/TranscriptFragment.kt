package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TranscriptFragment : BaseFragment() {
    companion object {
        fun newInstance() = TranscriptFragment()
    }
    private val viewModel by viewModels<TranscriptViewModel>({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppTheme(theme.activeTheme) {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                Surface(modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())) {
                    TranscriptContent(viewModel)
                }
            }
        }
    }

    @Composable
    fun TranscriptContent(
        viewModel: TranscriptViewModel,
    ) {
        val state = viewModel.uiState.collectAsState()
        Text(
            text = "Transcript for episode ${state.value.episodeId} url ${state.value.transcript?.url}",
        )
    }
}
