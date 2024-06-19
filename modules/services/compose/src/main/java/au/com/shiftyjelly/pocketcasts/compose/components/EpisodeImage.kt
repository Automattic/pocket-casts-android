package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed

@Composable
fun EpisodeImage(
    episode: BaseEpisode,
    useEpisodeArtwork: Boolean,
    modifier: Modifier = Modifier,
    placeholderType: PlaceholderType = PlaceholderType.Large,
) {
    val context = LocalContext.current

    val imageRequest = remember(episode.uuid, useEpisodeArtwork) {
        PocketCastsImageRequestFactory(context, placeholderType = placeholderType).themed().create(episode, useEpisodeArtwork)
    }

    CoilImage(
        imageRequest = imageRequest,
        title = "",
        showTitle = false,
        modifier = modifier.aspectRatio(1f),
    )
}
