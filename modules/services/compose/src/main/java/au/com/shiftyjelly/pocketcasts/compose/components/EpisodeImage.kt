package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed

@Composable
fun EpisodeImage(
    episode: BaseEpisode,
    useRssArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val imageRequest = remember(episode.uuid, useRssArtwork) {
        PocketCastsImageRequestFactory(context).themed().create(episode, useRssArtwork)
    }

    CoilImage(
        imageRequest = imageRequest,
        title = "",
        showTitle = false,
        modifier = modifier.fillMaxSize(),
    )
}
