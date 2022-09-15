package au.com.shiftyjelly.pocketcasts.wear.ui.podcast

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.wear.theme.theme

object PodcastScreen {
    const val argument = "podcastUuid"
    const val route = "podcast/{$argument}"

    fun navigateRoute(podcastUuid: String) = "podcast/$podcastUuid"
}

@Composable
fun PodcastScreen(
    modifier: Modifier = Modifier,
    viewModel: PodcastViewModel = hiltViewModel(),
) {
    val podcast = viewModel.uiState.podcast ?: return

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = 26.dp, horizontal = 8.dp)
    ) {
        PodcastImage(
            uuid = podcast.uuid,
            modifier = Modifier.size(100.dp)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.theme.colors.primaryText01,
            text = podcast.title
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.theme.colors.primaryText02,
            text = podcast.author
        )
    }
}
