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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun UpNextOptionsScreen(
    episodeScreenViewModelStoreOwner: ViewModelStoreOwner,
    onComplete: () -> Unit
) {
    val viewModel = hiltViewModel<EpisodeViewModel>(episodeScreenViewModelStoreOwner)
    Content(
        upNextOptions = viewModel.upNextOptions,
        onComplete = onComplete
    )
}

@Composable
private fun Content(upNextOptions: List<EpisodeViewModel.UpNextOption>, onComplete: () -> Unit) {
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
            upNextOptions.forEach { upNextOption ->
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

@Preview
@Composable
private fun Preview() {
    WearAppTheme(Theme.ThemeType.DARK) {
        Content(
            onComplete = {},
            upNextOptions = listOf(
                EpisodeViewModel.UpNextOption(
                    iconRes = IR.drawable.ic_upnext_playnext,
                    titleRes = LR.string.play_next,
                    onClick = {},
                ),
                EpisodeViewModel.UpNextOption(
                    iconRes = IR.drawable.ic_upnext_playlast,
                    titleRes = LR.string.play_last,
                    onClick = {},
                ),
            )
        )
    }
}
