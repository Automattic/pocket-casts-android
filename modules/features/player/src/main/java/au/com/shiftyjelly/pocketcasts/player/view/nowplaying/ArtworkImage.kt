package au.com.shiftyjelly.pocketcasts.player.view.nowplaying

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.ui.extensions.themed
import coil.compose.rememberAsyncImagePainter

@Composable
internal fun ArtworkImage(
    state: ArtworkImageState,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val factory = remember(context) {
        val placeholderType = if (isPreview) {
            PocketCastsImageRequestFactory.PlaceholderType.Large
        } else {
            PocketCastsImageRequestFactory.PlaceholderType.None
        }
        PocketCastsImageRequestFactory(context, placeholderType = placeholderType).themed()
    }
    val imageRequest = remember(state, factory) {
        when (state) {
            is ArtworkImageState.Podcast -> factory.create(state.episode, useEpisodeArtwork = false)
            is ArtworkImageState.Episode -> factory.create(state.episode, useEpisodeArtwork = true)
            is ArtworkImageState.Chapter -> factory.createForFileOrUrl(state.uriPath)
        }
    }

    Image(
        painter = rememberAsyncImagePainter(
            model = imageRequest,
            contentScale = ContentScale.Fit,
        ),
        contentDescription = null,
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(cornerRadius)),
    )
}

sealed interface ArtworkImageState {
    data class Podcast(
        val episode: BaseEpisode,
    ) : ArtworkImageState

    data class Episode(
        val episode: BaseEpisode,
    ) : ArtworkImageState

    data class Chapter(
        val uriPath: String,
    ) : ArtworkImageState
}
