package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip

@Composable
fun UpNextOptionsScreen(
    episodeScreenViewModelStoreOwner: ViewModelStoreOwner,
    onComplete: () -> Unit
) {
    val viewModel = hiltViewModel<EpisodeViewModel>(episodeScreenViewModelStoreOwner)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TextP50(
                text = "[[ PLACEHOLDER UI ]]",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            viewModel.upNextOptions.forEach { upNextOption ->
                with(upNextOption) {
                    WatchListChip(
                        titleRes = titleRes,
                        iconRes = iconRes,
                        onClick = {
                            onClick()
                            onComplete()
                        },
                    )
                }
            }
        }
    }
}
