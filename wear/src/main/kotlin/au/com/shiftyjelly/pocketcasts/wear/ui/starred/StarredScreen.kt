package au.com.shiftyjelly.pocketcasts.wear.ui.starred

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration.Element
import au.com.shiftyjelly.pocketcasts.wear.ui.component.EpisodeListScreen
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object StarredScreen {
    const val ROUTE = "starred_screen"
}

@Composable
fun StarredScreen(
    columnState: ScalingLazyColumnState,
    viewModel: StarredScreenViewModel = hiltViewModel(),
    onItemClick: (PodcastEpisode) -> Unit,
) {
    val uiState by viewModel.stateFlow.collectAsState()
    val artworkConfiguration by viewModel.artworkConfiguration.collectAsState()

    val context = LocalContext.current
    CallOnce {
        viewModel.refresh(context)
    }

    EpisodeListScreen(
        columnState = columnState,
        uiState = uiState,
        title = LR.string.profile_navigation_starred,
        useEpisodeArtwork = artworkConfiguration.useEpisodeArtwork(Element.Starred),
        onItemClick = onItemClick,
    )
}
