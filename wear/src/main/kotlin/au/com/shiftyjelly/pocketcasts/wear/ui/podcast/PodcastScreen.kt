package au.com.shiftyjelly.pocketcasts.wear.ui.podcast

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

object PodcastScreen {
    const val argument = "podcastUuid"
    const val route = "podcast/{$argument}"

    fun navigateRoute(podcastUuid: String) = "podcast/$podcastUuid"
}

@Composable
fun PodcastScreen(podcastUuid: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = podcastUuid
    )
}
