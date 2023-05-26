package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.wear.compose.foundation.lazy.items
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun UpNextOptionsScreen(
    columnState: ScalingLazyColumnState,
    episodeScreenViewModelStoreOwner: ViewModelStoreOwner,
    onComplete: () -> Unit,
) {
    val viewModel = hiltViewModel<EpisodeViewModel>(episodeScreenViewModelStoreOwner)
    Content(
        columnState = columnState,
        upNextOptions = viewModel.upNextOptions,
        onComplete = onComplete
    )
}

@Composable
private fun Content(
    columnState: ScalingLazyColumnState,
    upNextOptions: List<EpisodeViewModel.UpNextOption>,
    onComplete: () -> Unit,
) {
    ScalingLazyColumn(
        columnState = columnState,
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        item {
            ScreenHeaderChip(
                text = LR.string.add_to_up_next_question,
                textColor = Color.White,
            )
        }

        items(upNextOptions) { upNextOption ->
            WatchListChip(
                title = stringResource(upNextOption.titleRes),
                iconRes = upNextOption.iconRes,
                onClick = {
                    upNextOption.onClick()
                    onComplete()
                },
            )
        }
    }
}

@Preview
@Composable
private fun Preview() {
    WearAppTheme {
        Content(
            columnState = ScalingLazyColumnState(),
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
            ),
            onComplete = {},
        )
    }
}
