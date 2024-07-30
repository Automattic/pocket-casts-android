package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.CloseButton
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun TranscriptPageWrapper(
    viewModel: PlayerViewModel,
    transcriptViewModel: TranscriptViewModel,
    theme: Theme,
) {
    AppTheme(theme.activeTheme) {
        val scrollState = rememberScrollState()
        val configuration = LocalConfiguration.current
        val connection = remember { object : NestedScrollConnection {} }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(connection),
        ) {
            TranscriptPage(
                viewModel = transcriptViewModel,
                theme = theme,
                scrollState = scrollState,
                modifier = Modifier
                    .height(configuration.screenHeightDp.dp),
            )

            CloseButton(
                modifier = Modifier.align(Alignment.TopStart),
                onClick = { viewModel.closeTranscript(withTransition = true) },
            )
        }
    }
}
