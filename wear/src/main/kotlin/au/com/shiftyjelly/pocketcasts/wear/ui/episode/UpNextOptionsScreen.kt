package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .padding(horizontal = 16.dp)
        ) {
            viewModel.upNextOptions.forEach { upNextOption ->
                WatchListChip(
                    titleRes = upNextOption.titleRes,
                    iconRes = upNextOption.iconRes,
                    onClick = {
                        upNextOption.onClick()
                        onComplete()
                    },
                )
            }
        }
    }
}
