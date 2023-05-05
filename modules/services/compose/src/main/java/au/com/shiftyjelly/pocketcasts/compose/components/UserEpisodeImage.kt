package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getUrlForArtwork
import coil.compose.AsyncImage
import coil.request.ImageRequest
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun UserEpisodeImage(
    episode: UserEpisode,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: Int = IR.drawable.ic_uploadedfile,
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(episode.getUrlForArtwork(themeIsDark = true, thumbnail = true))
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        placeholder = painterResource(placeholder),
        modifier = modifier,
    )
}
