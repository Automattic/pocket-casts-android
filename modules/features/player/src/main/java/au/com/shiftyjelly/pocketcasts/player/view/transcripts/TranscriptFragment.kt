package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

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

    @OptIn(UnstableApi::class)
    @Composable
    fun TranscriptContent(
        viewModel: TranscriptViewModel,
    ) {
        val state = viewModel.uiState.collectAsState()

        LazyColumn {
            items(state.value.cues) { cues ->
                Text(text = cues.startTimeUs.microseconds.format())
                cues.cues.forEach {
                    Text(text = it.text.toString())
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun Duration.format() = toComponents { hours, minutes, seconds, _ ->
    String.format(
        Locale.getDefault(),
        "%02d:%02d:%02d",
        hours,
        minutes,
        seconds,
    )
}
